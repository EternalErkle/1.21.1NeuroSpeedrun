package com.eternalerkle.speedrun.world;

import com.eternalerkle.speedrun.SpeedrunCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Creates, prepares and deletes the per-run dimensions, and answers which run dimension a portal leads to.
 * Holds at most one current set and one set being prepared for the next run.
 */
public final class RunWorlds {
	private static final TicketType<ChunkPos> PREPARE_TICKET = TicketType.create("speedrun_prepare", Comparator.comparingLong(ChunkPos::toLong));
	/** Region ticket radius around the spawn chunk. Covers the vanilla 11x11 spawn search. */
	private static final int PREPARE_RADIUS = 5;

	private final MinecraftServer server;
	private int counter;
	private RunWorldSet current;
	private RunWorldSet next;
	private Preparation preparation;

	private record Preparation(RunWorldSet set, ChunkPos center, Consumer<RunWorldSet> onReady) {
	}

	public RunWorlds(MinecraftServer server) {
		this.server = server;
	}

	@Nullable
	public RunWorldSet current() {
		return current;
	}

	@Nullable
	public RunWorldSet next() {
		return next;
	}

	/** Starts creating the next run's worlds. {@code onReady} runs on the server thread once the spawn area is generated. */
	public void prepareNext(long seed, Consumer<RunWorldSet> onReady) {
		if (next != null) {
			delete(next);
		}
		counter++;
		String id = "run" + counter + "_" + Long.toHexString(System.nanoTime() & 0xFFFFFFL);
		Fantasy fantasy = Fantasy.get(server);
		RuntimeWorldHandle overworld = fantasy.openTemporaryWorld(key(id + "_overworld"), config(seed, BuiltinDimensionTypes.OVERWORLD, overworldGenerator(), true));
		RuntimeWorldHandle nether = fantasy.openTemporaryWorld(key(id + "_nether"), config(seed, BuiltinDimensionTypes.NETHER, netherGenerator(), false));
		RuntimeWorldHandle end = fantasy.openTemporaryWorld(key(id + "_end"), config(seed, BuiltinDimensionTypes.END, endGenerator(), false));
		RunWorldSet set = new RunWorldSet(seed, overworld, nether, end);
		ServerLevel endLevel = set.end();
		endLevel.setDragonFight(new EndDragonFight(endLevel, seed, EndDragonFight.Data.DEFAULT));

		ServerLevel level = set.overworld();
		ChunkPos center = new ChunkPos(level.getChunkSource().randomState().sampler().findSpawnPosition());
		level.getChunkSource().addRegionTicket(PREPARE_TICKET, center, PREPARE_RADIUS, center);
		next = set;
		preparation = new Preparation(set, center, onReady);
		SpeedrunCore.LOGGER.info("Preparing run worlds {} with seed {}", id, seed);
	}

	/** Promotes the prepared set to current and deletes the old one. */
	public RunWorldSet promoteNext() {
		if (next == null || !next.ready) {
			throw new IllegalStateException("next run worlds are not ready");
		}
		if (current != null) {
			delete(current);
		}
		current = next;
		next = null;
		return current;
	}

	public void deleteCurrent() {
		if (current != null) {
			delete(current);
			current = null;
		}
	}

	public boolean isPreparing() {
		return preparation != null;
	}

	public void tick() {
		Preparation prep = preparation;
		if (prep == null) {
			return;
		}
		ServerLevel level = prep.set.overworld();
		int r = PREPARE_RADIUS;
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				if (level.getChunkSource().getChunkNow(prep.center.x + x, prep.center.z + z) == null) {
					return;
				}
			}
		}
		prep.set.spawn = findSpawn(level, prep.center);
		prep.set.ready = true;
		preparation = null;
		SpeedrunCore.LOGGER.info("Run worlds ready, spawn at {}", prep.set.spawn);
		prep.onReady.accept(prep.set);
	}

	/** Mirrors MinecraftServer.setInitialSpawn. Runs only after the surrounding chunks are loaded, so it never blocks on generation. */
	private static BlockPos findSpawn(ServerLevel level, ChunkPos center) {
		int height = level.getChunkSource().getGenerator().getSpawnHeight(level);
		if (height < level.getMinBuildHeight()) {
			BlockPos corner = center.getWorldPosition();
			height = level.getHeight(Heightmap.Types.WORLD_SURFACE, corner.getX() + 8, corner.getZ() + 8);
		}
		BlockPos spawn = center.getWorldPosition().offset(8, height, 8);
		int x = 0;
		int z = 0;
		int dx = 0;
		int dz = -1;
		for (int i = 0; i < Mth.square(11); i++) {
			if (x >= -5 && x <= 5 && z >= -5 && z <= 5) {
				BlockPos found = PlayerRespawnLogic.getSpawnPosInChunk(level, new ChunkPos(center.x + x, center.z + z));
				if (found != null) {
					spawn = found;
					break;
				}
			}
			if (x == z || x < 0 && x == -z || x > 0 && x == 1 - z) {
				int t = dx;
				dx = -dz;
				dz = t;
			}
			x += dx;
			z += dz;
		}
		return spawn;
	}

	private void delete(RunWorldSet set) {
		if (set.deleted) {
			return;
		}
		set.deleted = true;
		if (preparation != null && preparation.set == set) {
			preparation = null;
		}
		if (next == set) {
			next = null;
		}
		ServerLevel fallback = server.overworld();
		BlockPos fallbackPos = fallback.getSharedSpawnPos();
		for (ServerLevel level : new ServerLevel[]{set.overworld(), set.nether(), set.end()}) {
			for (ServerPlayer player : List.copyOf(level.players())) {
				player.teleportTo(fallback, fallbackPos.getX() + 0.5, fallbackPos.getY(), fallbackPos.getZ() + 0.5, 0, 0);
			}
		}
		set.overworldHandle.delete();
		set.netherHandle.delete();
		set.endHandle.delete();
		SpeedrunCore.LOGGER.info("Deleting run worlds with seed {}", set.seed);
	}

	/** The run dimension a nether portal in {@code from} leads to, or null when {@code from} is not a current run dimension. */
	@Nullable
	public ServerLevel netherPortalTarget(ServerLevel from) {
		RunWorldSet set = current;
		if (set == null || set.deleted) {
			return null;
		}
		if (from == set.overworld()) {
			return set.nether();
		}
		if (from == set.nether()) {
			return set.overworld();
		}
		return null;
	}

	public boolean isRunLevel(ServerLevel level) {
		RunWorldSet set = current;
		return set != null && set.contains(level);
	}

	private RuntimeWorldConfig config(long seed, ResourceKey<DimensionType> type, ChunkGenerator generator, boolean tickTime) {
		return new RuntimeWorldConfig()
			.setSeed(seed)
			.setDimensionType(type)
			.setGenerator(generator)
			.setDifficulty(Difficulty.HARD)
			.setShouldTickTime(tickTime)
			.setTimeOfDay(0)
			.setGameRule(GameRules.RULE_DOMOBSPAWNING, true)
			.setGameRule(GameRules.RULE_WEATHER_CYCLE, true)
			.setGameRule(GameRules.RULE_SHOWDEATHMESSAGES, false)
			.setGameRule(GameRules.RULE_SENDCOMMANDFEEDBACK, false);
	}

	private ChunkGenerator overworldGenerator() {
		return noiseGenerator(MultiNoiseBiomeSourceParameterLists.OVERWORLD, NoiseGeneratorSettings.OVERWORLD);
	}

	private ChunkGenerator netherGenerator() {
		return noiseGenerator(MultiNoiseBiomeSourceParameterLists.NETHER, NoiseGeneratorSettings.NETHER);
	}

	private ChunkGenerator noiseGenerator(ResourceKey<MultiNoiseBiomeSourceParameterList> biomes, ResourceKey<NoiseGeneratorSettings> settings) {
		HolderGetter<MultiNoiseBiomeSourceParameterList> params = server.registryAccess().lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
		HolderGetter<NoiseGeneratorSettings> noise = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
		return new NoiseBasedChunkGenerator(MultiNoiseBiomeSource.createFromPreset(params.getOrThrow(biomes)), noise.getOrThrow(settings));
	}

	private ChunkGenerator endGenerator() {
		HolderGetter<NoiseGeneratorSettings> noise = server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
		return new NoiseBasedChunkGenerator(
			TheEndBiomeSource.create(server.registryAccess().lookupOrThrow(Registries.BIOME)),
			noise.getOrThrow(NoiseGeneratorSettings.END));
	}

	private static ResourceLocation key(String path) {
		return ResourceLocation.fromNamespaceAndPath("speedrun", path);
	}
}
