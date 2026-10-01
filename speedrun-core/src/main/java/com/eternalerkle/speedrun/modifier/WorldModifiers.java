package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.stats.RunRecord;
import com.eternalerkle.speedrun.util.Time;
import com.eternalerkle.speedrun.util.Titles;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The world and time modifiers: day cycle, world border, view distance, time bomb, portals, beds, block breaking and
 * placing, timed disasters (pig bomb, lightning, meteors, anvils, floor is lava, blackouts, spawners) and loot chests.
 * Also builds the overworld generator for the worldgen modifiers.
 */
public final class WorldModifiers implements RunFeature {
	private static final long SECOND = 1000L;
	private static final long MINUTE = 60L * SECOND;
	private static final long NOON = 6000L;
	private static final int FAST_DAY_EXTRA_TICKS = 3;

	private static final double TIGHT_BORDER_SIZE = 1000.0;
	private static final double SHRINK_START_SIZE = 4000.0;
	private static final double SHRINK_END_SIZE = 100.0;
	private static final long SHRINK_MILLIS = 60L * MINUTE;
	private static final int SHORT_SIGHT_CHUNKS = 4;

	private static final long TIME_BOMB_MILLIS = 60L * MINUTE;
	/** Remaining times at which the time bomb warns everyone. */
	private static final long[] TIME_BOMB_WARNINGS = {30L * MINUTE, 10L * MINUTE, 5L * MINUTE, MINUTE,
		10 * SECOND, 5 * SECOND, 4 * SECOND, 3 * SECOND, 2 * SECOND, SECOND};

	private static final int PIG_BOMB_FUSE = 80;
	private static final long FLOOR_WARNING_MILLIS = 3L * SECOND;
	private static final long FLOOR_MILLIS = 20L * SECOND;
	private static final int METEORS_PER_SHOWER = 8;

	private static final List<TagKey<Block>> ORES = List.of(BlockTags.COAL_ORES, BlockTags.IRON_ORES, BlockTags.COPPER_ORES,
		BlockTags.GOLD_ORES, BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES);
	private static final List<EntityType<? extends Mob>> SPAWNER_MOBS = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.CAVE_SPIDER);
	private static final List<EntityType<? extends Mob>> TRAP_MOBS = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER);

	/** Structure states of close_stronghold worlds, read by the ring position mixin. */
	private static final Set<ChunkGeneratorStructureState> CLOSE_STRONGHOLD_STATES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	private static final Set<ChunkGenerator> CLOSE_STRONGHOLD_GENERATORS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	private static final Set<ChunkGenerator> NO_VILLAGE_GENERATORS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
	/** First ring distance for close_stronghold, in vanilla's unit. Vanilla uses 32, placing the first ring 1280 to 2816 blocks out. */
	public static final int CLOSE_STRONGHOLD_DISTANCE = 4;

	private static final Random RANDOM = new Random();
	private static boolean eventsRegistered;

	private final RunManager runs;
	private final Map<ServerLevel, Map<GameRules.Key<GameRules.BooleanValue>, Boolean>> savedBooleans = new HashMap<>();
	private final Map<ServerLevel, Map<GameRules.Key<GameRules.IntegerValue>, Integer>> savedInts = new HashMap<>();
	private final List<ServerLevel> shortSighted = new ArrayList<>();
	private final List<PigBomb> pigBombs = new ArrayList<>();

	/** Next due times of the timed modifiers, in run milliseconds. */
	private long nextPigBomb;
	private long nextThunder;
	private long nextMeteors;
	private long nextAnvil;
	private long nextFloor;
	private long nextBlackout;
	private long nextSpawner;
	/** floor_is_lava's current window, in run milliseconds. -1 when none is pending. */
	private long floorStart = -1;
	private long floorEnd = -1;
	private int meteorsLeft;
	private long lastElapsed;

	private record PigBomb(Pig pig, PrimedTnt tnt, UUID target) {
	}

	private WorldModifiers(RunManager runs) {
		this.runs = runs;
	}

	static void register(RunManager runs) {
		runs.addFeature(new WorldModifiers(runs));
		if (!eventsRegistered) {
			eventsRegistered = true;
			registerEvents();
		}
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		ServerLevel overworld = run.worlds.overworld();
		if (run.modifiers.contains(ModifierCatalog.ETERNAL_DAY)) {
			setRule(overworld, GameRules.RULE_DAYLIGHT, false);
			overworld.setDayTime(NOON);
		}
		if (run.modifiers.contains(ModifierCatalog.INSTANT_PORTALS)) {
			for (ServerLevel level : List.of(overworld, run.worlds.nether())) {
				setRule(level, GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY, 1);
			}
		}
		if (run.modifiers.contains(ModifierCatalog.SHORT_SIGHT)) {
			for (ServerLevel level : levels(run)) {
				level.getChunkSource().setViewDistance(SHORT_SIGHT_CHUNKS);
				shortSighted.add(level);
			}
		}
		if (run.modifiers.contains(ModifierCatalog.TIGHT_BORDER)) {
			setBorder(run, TIGHT_BORDER_SIZE, TIGHT_BORDER_SIZE, 0);
		}
		if (run.modifiers.contains(ModifierCatalog.SHRINKING_BORDER)) {
			updateShrinkingBorder(run, true);
		}
		long now = run.realMillis();
		lastElapsed = now;
		nextPigBomb = now + randomMillis(30 * SECOND, 3 * MINUTE);
		nextThunder = now + randomMillis(MINUTE, 4 * MINUTE);
		nextMeteors = now + randomMillis(3 * MINUTE, 6 * MINUTE);
		nextAnvil = now + randomMillis(2 * MINUTE, 5 * MINUTE);
		nextBlackout = now + randomMillis(2 * MINUTE, 4 * MINUTE);
		nextFloor = nextMark(now, 10 * MINUTE);
		nextSpawner = nextMark(now, 10 * MINUTE);
		floorStart = -1;
		floorEnd = -1;
		meteorsLeft = 0;
		pigBombs.clear();
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		JsonObject world = new JsonObject();
		world.addProperty("pigBomb", nextPigBomb);
		world.addProperty("thunder", nextThunder);
		world.addProperty("meteors", nextMeteors);
		world.addProperty("anvil", nextAnvil);
		world.addProperty("floor", nextFloor);
		world.addProperty("blackout", nextBlackout);
		world.addProperty("spawner", nextSpawner);
		out.add("worldModifiers", world);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (!in.has("worldModifiers")) {
			return;
		}
		JsonObject world = in.getAsJsonObject("worldModifiers");
		nextPigBomb = read(world, "pigBomb", nextPigBomb);
		nextThunder = read(world, "thunder", nextThunder);
		nextMeteors = read(world, "meteors", nextMeteors);
		nextAnvil = read(world, "anvil", nextAnvil);
		nextFloor = read(world, "floor", nextFloor);
		nextBlackout = read(world, "blackout", nextBlackout);
		nextSpawner = read(world, "spawner", nextSpawner);
	}

	private static long read(JsonObject json, String key, long fallback) {
		return json.has(key) ? json.get(key).getAsLong() : fallback;
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		savedBooleans.forEach((level, rules) -> rules.forEach((rule, value) -> level.getGameRules().getRule(rule).set(value, runs.server())));
		savedInts.forEach((level, rules) -> rules.forEach((rule, value) -> level.getGameRules().getRule(rule).set(value, runs.server())));
		savedBooleans.clear();
		savedInts.clear();
		for (ServerLevel level : shortSighted) {
			level.getChunkSource().setViewDistance(runs.server().getPlayerList().getViewDistance());
		}
		shortSighted.clear();
		if (run.modifiers.contains(ModifierCatalog.TIGHT_BORDER) || run.modifiers.contains(ModifierCatalog.SHRINKING_BORDER)) {
			for (ServerLevel level : levels(run)) {
				WorldBorder border = level.getWorldBorder();
				border.setCenter(0, 0);
				border.setSize(WorldBorder.MAX_SIZE);
			}
		}
		pigBombs.clear();
		meteorsLeft = 0;
		for (ServerPlayer player : runs.server().getPlayerList().getPlayers()) {
			clearBlindness(run, player);
			resetSight(run, player);
		}
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		// Run start teleports players before the border is set, so send it again.
		if (run.worlds.contains(player.serverLevel())) {
			player.connection.send(new ClientboundInitializeBorderPacket(player.serverLevel().getWorldBorder()));
		}
		if (run.modifiers.contains(ModifierCatalog.SHORT_SIGHT)) {
			// The client's render distance and fog follow the server's radius.
			player.connection.send(new ClientboundSetChunkCacheRadiusPacket(SHORT_SIGHT_CHUNKS));
		}
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		clearBlindness(run, player);
		resetSight(run, player);
	}

	private void resetSight(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.SHORT_SIGHT)) {
			player.connection.send(new ClientboundSetChunkCacheRadiusPacket(runs.server().getPlayerList().getViewDistance()));
		}
	}

	private static void clearBlindness(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.BLACKOUTS)) {
			player.removeEffect(MobEffects.BLINDNESS);
		}
	}

	@Override
	public void tick(ActiveRun run) {
		if (run.modifiers.isEmpty()) {
			return;
		}
		List<String> mods = run.modifiers;
		long now = run.realMillis();
		long before = lastElapsed;
		lastElapsed = now;
		if (mods.contains(ModifierCatalog.TIME_BOMB) && tickTimeBomb(before, now)) {
			return;
		}
		if (mods.contains(ModifierCatalog.FAST_DAYS)) {
			ServerLevel overworld = run.worlds.overworld();
			if (overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
				overworld.setDayTime(overworld.getDayTime() + FAST_DAY_EXTRA_TICKS);
			}
		}
		if (mods.contains(ModifierCatalog.SHRINKING_BORDER) && run.gameTicks % 20 == 0) {
			updateShrinkingBorder(run, false);
		}
		List<ServerPlayer> players = Modifiers.playersIn(run);
		if (mods.contains(ModifierCatalog.PIG_BOMB)) {
			tickPigBombs(run);
			if (now >= nextPigBomb) {
				ServerPlayer target = pick(players);
				nextPigBomb = now + (target != null && spawnPigBomb(target) ? randomMillis(30 * SECOND, 3 * MINUTE) : 5 * SECOND);
			}
		}
		if (mods.contains(ModifierCatalog.THUNDERSTRUCK) && now >= nextThunder) {
			nextThunder = now + randomMillis(MINUTE, 4 * MINUTE);
			ServerPlayer target = pick(players);
			if (target != null) {
				strikeNear(target);
			}
		}
		if (mods.contains(ModifierCatalog.METEOR_SHOWER)) {
			if (now >= nextMeteors) {
				nextMeteors = now + randomMillis(3 * MINUTE, 6 * MINUTE);
				meteorsLeft = METEORS_PER_SHOWER * Math.max(1, players.size());
				runs.broadcast(Component.literal("Meteor shower! Look up.").withStyle(ChatFormatting.GOLD));
			}
			if (meteorsLeft > 0 && run.gameTicks % 8 == 0) {
				ServerPlayer target = pick(players);
				if (target != null) {
					meteorsLeft--;
					dropMeteor(target);
				}
			}
		}
		if (mods.contains(ModifierCatalog.ANVIL_RAIN) && now >= nextAnvil) {
			nextAnvil = now + randomMillis(2 * MINUTE, 5 * MINUTE);
			ServerPlayer target = pick(players);
			if (target != null) {
				dropAnvil(target);
			}
		}
		if (mods.contains(ModifierCatalog.FLOOR_IS_LAVA)) {
			tickFloor(run, players, now);
		}
		if (mods.contains(ModifierCatalog.BLACKOUTS) && now >= nextBlackout) {
			nextBlackout = now + randomMillis(2 * MINUTE, 4 * MINUTE);
			int ticks = Math.round(5 * run.tickRate);
			for (ServerPlayer player : players) {
				player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false));
			}
		}
		if (mods.contains(ModifierCatalog.SPAWNER_SURPRISE) && now >= nextSpawner) {
			nextSpawner = nextMark(now, 10 * MINUTE);
			boolean placed = false;
			for (ServerPlayer player : players) {
				placed |= placeSpawner(player);
			}
			if (placed) {
				runs.broadcast(Component.literal("Spawner surprise! A mob spawner appeared nearby.").withStyle(ChatFormatting.DARK_RED));
			}
		}
	}

	// ---- time bomb ----

	/** Warns as the deadline nears and fails the run once it passes. Returns true when the run was failed. */
	private boolean tickTimeBomb(long before, long now) {
		long left = TIME_BOMB_MILLIS - now;
		if (left <= 0) {
			runs.abortRun(RunRecord.Result.DIED, "Time Bomb! The hour is up and the run is over.", null);
			return true;
		}
		long leftBefore = TIME_BOMB_MILLIS - before;
		for (long warning : TIME_BOMB_WARNINGS) {
			if (leftBefore > warning && left <= warning) {
				Component text = Component.literal(Time.format(warning)).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
				if (warning <= 10 * SECOND) {
					Titles.show(runs.server(), text, Component.literal("Time Bomb").withStyle(ChatFormatting.GRAY), 0, 0.9, 0.1);
				} else {
					runs.broadcast(Component.literal("Time Bomb: ").withStyle(ChatFormatting.RED).append(text).append(Component.literal(" left.").withStyle(ChatFormatting.RED)));
				}
			}
		}
		return false;
	}

	// ---- world border ----

	/** Sets the border of the overworld and nether around spawn and sends it to the players there. */
	private void setBorder(ActiveRun run, double from, double to, long millis) {
		BlockPos spawn = run.worlds.spawn();
		for (ServerLevel level : List.of(run.worlds.overworld(), run.worlds.nether())) {
			// Each run level has its own border; center the Nether's on the spawn's Nether coordinates.
			double scale = level.dimensionType().coordinateScale();
			WorldBorder border = level.getWorldBorder();
			border.setCenter((spawn.getX() + 0.5) / scale, (spawn.getZ() + 0.5) / scale);
			if (millis > 0) {
				border.lerpSizeBetween(from, to, millis);
			} else {
				border.setSize(to);
			}
			for (ServerPlayer player : level.players()) {
				player.connection.send(new ClientboundInitializeBorderPacket(border));
			}
		}
	}

	/**
	 * shrinking_border follows run time, which stands still while the run is paused, while the vanilla border moves
	 * in wall-clock time. Re-aims the border whenever the two drift apart, such as after a pause or a restore.
	 */
	private void updateShrinkingBorder(ActiveRun run, boolean force) {
		long elapsed = run.realMillis();
		double progress = Math.min(1.0, (double) elapsed / SHRINK_MILLIS);
		double expected = SHRINK_START_SIZE + (SHRINK_END_SIZE - SHRINK_START_SIZE) * progress;
		if (!force && Math.abs(run.worlds.overworld().getWorldBorder().getSize() - expected) < 2.0) {
			return;
		}
		setBorder(run, expected, SHRINK_END_SIZE, Math.max(0, SHRINK_MILLIS - elapsed));
	}

	// ---- pig bomb ----

	/** Spawns a pig carrying lit TNT near the player, which then chases them. Returns false when no spot was found. */
	private boolean spawnPigBomb(ServerPlayer target) {
		ServerLevel level = target.serverLevel();
		BlockPos spot = findSpot(level, target.blockPosition(), 6, 10, EntityType.PIG);
		Pig pig = spot == null ? null : spawnPigBomb(level, spot);
		if (pig == null) {
			return false;
		}
		pigBombs.add(new PigBomb(pig, (PrimedTnt) pig.getFirstPassenger(), target.getUUID()));
		return true;
	}

	/** An adult pig at {@code spot} with primed TNT on its back, which explodes with normal TNT power after 4 seconds. */
	@Nullable
	public static Pig spawnPigBomb(ServerLevel level, BlockPos spot) {
		Pig pig = EntityType.PIG.create(level);
		if (pig == null) {
			return null;
		}
		pig.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, RANDOM.nextFloat() * 360.0F, 0);
		pig.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
		pig.setAge(0);
		level.addFreshEntity(pig);
		PrimedTnt tnt = new PrimedTnt(level, pig.getX(), pig.getY(), pig.getZ(), null);
		tnt.setFuse(PIG_BOMB_FUSE);
		level.addFreshEntity(tnt);
		tnt.startRiding(pig, true);
		level.playSound(null, pig.getX(), pig.getY(), pig.getZ(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
		level.playSound(null, pig.getX(), pig.getY(), pig.getZ(), SoundEvents.PIG_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.0F);
		return pig;
	}

	/** Steers each live pig bomb at its target and makes it flash faster as the fuse runs down. */
	private void tickPigBombs(ActiveRun run) {
		for (Iterator<PigBomb> it = pigBombs.iterator(); it.hasNext(); ) {
			PigBomb bomb = it.next();
			if (bomb.tnt.isRemoved() || bomb.pig.isRemoved() || !run.worlds.contains((ServerLevel) bomb.pig.level())) {
				if (!bomb.pig.isRemoved()) {
					bomb.pig.setGlowingTag(false);
				}
				it.remove();
				continue;
			}
			int fuse = bomb.tnt.getFuse();
			int period = fuse > 40 ? 8 : fuse > 20 ? 4 : 2;
			bomb.pig.setGlowingTag(fuse / period % 2 == 0);
			if (run.gameTicks % 5 == 0) {
				ServerPlayer target = runs.server().getPlayerList().getPlayer(bomb.target);
				if (target != null && target.level() == bomb.pig.level() && !target.isSpectator()) {
					bomb.pig.getNavigation().moveTo(target, 1.6);
				}
			}
		}
	}

	// ---- other timed disasters ----

	private static void strikeNear(ServerPlayer target) {
		ServerLevel level = target.serverLevel();
		BlockPos spot = findSpot(level, target.blockPosition(), 2, 6, null);
		if (spot == null) {
			spot = target.blockPosition();
		}
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
		if (bolt != null) {
			bolt.moveTo(Vec3.atBottomCenterOf(spot));
			level.addFreshEntity(bolt);
		}
	}

	/** A fireball falling from the sky somewhere within 16 blocks of the player. */
	private static void dropMeteor(ServerPlayer target) {
		ServerLevel level = target.serverLevel();
		double x = target.getX() + RANDOM.nextInt(33) - 16;
		double z = target.getZ() + RANDOM.nextInt(33) - 16;
		double y;
		if (level.dimensionType().hasCeiling()) {
			y = target.getY() + 12;
		} else {
			y = Math.max(target.getY(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z))) + 30;
		}
		y = Math.min(y, level.getMaxBuildHeight() - 2);
		LargeFireball fireball = new LargeFireball(EntityType.FIREBALL, level);
		fireball.moveTo(x, y, z, 0, 90);
		fireball.setDeltaMovement((RANDOM.nextDouble() - 0.5) * 0.2, -0.6, (RANDOM.nextDouble() - 0.5) * 0.2);
		level.addFreshEntity(fireball);
	}

	/** An anvil falling from up to 12 blocks above the player's head, with a short warning. */
	private static void dropAnvil(ServerPlayer target) {
		ServerLevel level = target.serverLevel();
		BlockPos head = target.blockPosition().above(2);
		BlockPos spot = null;
		for (int dy = 0; dy < 12; dy++) {
			BlockPos pos = head.above(dy);
			if (!level.getBlockState(pos).isAir() || pos.getY() >= level.getMaxBuildHeight()) {
				break;
			}
			spot = pos;
		}
		if (spot == null) {
			return;
		}
		FallingBlockEntity anvil = FallingBlockEntity.fall(level, spot, Blocks.ANVIL.defaultBlockState());
		anvil.setHurtsEntities(2.0F, 40);
		level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.6F, 0.5F);
		target.displayClientMessage(Component.literal("Look up!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
	}

	private void tickFloor(ActiveRun run, List<ServerPlayer> players, long now) {
		if (floorStart < 0 && now >= nextFloor) {
			nextFloor = nextMark(now, 10 * MINUTE);
			floorStart = now + FLOOR_WARNING_MILLIS;
			floorEnd = floorStart + FLOOR_MILLIS;
			Titles.show(runs.server(), Component.literal("The floor is lava!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.literal("Get off the ground for 20 seconds").withStyle(ChatFormatting.YELLOW), 0.2, 2.5, 0.5);
		}
		if (floorStart < 0 || now < floorStart) {
			return;
		}
		if (now >= floorEnd) {
			floorStart = -1;
			floorEnd = -1;
			runs.broadcast(Component.literal("The floor is safe again.").withStyle(ChatFormatting.GREEN));
			return;
		}
		if (run.gameTicks % 20 == 0) {
			for (ServerPlayer player : players) {
				if (player.onGround()) {
					player.hurt(player.damageSources().hotFloor(), 1.0F);
				}
			}
		}
	}

	private static boolean placeSpawner(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos spot = findSpot(level, player.blockPosition(), 4, 8, null);
		if (spot == null) {
			return false;
		}
		level.setBlockAndUpdate(spot, Blocks.SPAWNER.defaultBlockState());
		if (level.getBlockEntity(spot) instanceof SpawnerBlockEntity spawner) {
			// Custom light rules let it spawn in daylight too.
			CompoundTag entity = new CompoundTag();
			entity.putString("id", EntityType.getKey(SPAWNER_MOBS.get(RANDOM.nextInt(SPAWNER_MOBS.size()))).toString());
			InclusiveRange<Integer> anyLight = new InclusiveRange<>(0, 15);
			SpawnData data = new SpawnData(entity, Optional.of(new SpawnData.CustomSpawnRules(anyLight, anyLight)), Optional.empty());
			CompoundTag tag = new CompoundTag();
			SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().ifPresent(encoded -> tag.put("SpawnData", encoded));
			tag.putShort("Delay", (short) 40);
			spawner.getSpawner().load(level, spot, tag);
			spawner.setChanged();
			level.sendBlockUpdated(spot, spawner.getBlockState(), spawner.getBlockState(), Block.UPDATE_CLIENTS);
		}
		return true;
	}

	// ---- events ----

	private static void registerEvents() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(world instanceof ServerLevel level) || player.isSpectator()) {
				return InteractionResult.PASS;
			}
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			// Vanilla places the held item instead of using the block when sneaking.
			boolean placesInstead = player.isSecondaryUseActive() && !player.getItemInHand(hand).isEmpty();
			if (placesInstead) {
				return InteractionResult.PASS;
			}
			if (state.getBlock() instanceof BedBlock) {
				boolean bedWorks = BedBlock.canSetSpawn(level);
				if (!bedWorks && Modifiers.isActive(ModifierCatalog.NO_BED_BOMBS, level) && isEnd(level)) {
					player.displayClientMessage(Component.literal("Beds do not explode this run.").withStyle(ChatFormatting.RED), true);
					return InteractionResult.SUCCESS;
				}
				if (bedWorks && Modifiers.isActive(ModifierCatalog.EXPLOSIVE_BEDS, level)) {
					explodeBed(level, pos, state);
					return InteractionResult.SUCCESS;
				}
			}
			if (Modifiers.isActive(ModifierCatalog.TRAPPED_CHESTS, level) && level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container
				&& container.getLootTable() != null && RANDOM.nextInt(4) == 0) {
				springTrap(level, pos);
			}
			return InteractionResult.PASS;
		});
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(world instanceof ServerLevel level) || player.isCreative()) {
				return;
			}
			if (Modifiers.isActive(ModifierCatalog.SILVERFISH_STONE, level) && state.is(BlockTags.BASE_STONE_OVERWORLD) && RANDOM.nextInt(50) == 0) {
				Silverfish silverfish = EntityType.SILVERFISH.create(level);
				if (silverfish != null) {
					silverfish.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
					level.addFreshEntity(silverfish);
					silverfish.spawnAnim();
				}
			}
			if (Modifiers.isActive(ModifierCatalog.EXPLOSIVE_ORES, level) && isOre(state) && RANDOM.nextInt(20) == 0) {
				PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
				level.addFreshEntity(tnt);
				level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
			}
			if (Modifiers.isActive(ModifierCatalog.CAVE_INS, level) && RANDOM.nextInt(12) == 0) {
				caveIn(level, player);
			}
		});
	}

	private static boolean isEnd(ServerLevel level) {
		return level.dimensionTypeRegistration().is(BuiltinDimensionTypes.END);
	}

	private static boolean isOre(BlockState state) {
		if (state.is(Blocks.NETHER_QUARTZ_ORE) || state.is(Blocks.ANCIENT_DEBRIS)) {
			return true;
		}
		for (TagKey<Block> tag : ORES) {
			if (state.is(tag)) {
				return true;
			}
		}
		return false;
	}

	/** Same as vanilla's bed explosion in the Nether and End. */
	private static void explodeBed(ServerLevel level, BlockPos pos, BlockState state) {
		BlockPos head = pos;
		if (state.getValue(BedBlock.PART) != BedPart.HEAD) {
			head = pos.relative(state.getValue(BedBlock.FACING));
		}
		BlockState headState = level.getBlockState(head);
		if (headState.getBlock() instanceof BedBlock) {
			level.removeBlock(head, false);
			BlockPos foot = head.relative(headState.getValue(BedBlock.FACING).getOpposite());
			if (level.getBlockState(foot).getBlock() instanceof BedBlock) {
				level.removeBlock(foot, false);
			}
		} else {
			level.removeBlock(pos, false);
			head = pos;
		}
		Vec3 center = head.getCenter();
		level.explode(null, level.damageSources().badRespawnPointExplosion(center), null, center, 5.0F, true, Level.ExplosionInteraction.BLOCK);
	}

	/** Half the time lit TNT on top of the chest, otherwise a few hostile mobs around it. */
	private static void springTrap(ServerLevel level, BlockPos chest) {
		if (RANDOM.nextBoolean()) {
			PrimedTnt tnt = new PrimedTnt(level, chest.getX() + 0.5, chest.getY() + 1, chest.getZ() + 0.5, null);
			tnt.setFuse(60);
			level.addFreshEntity(tnt);
			level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
			return;
		}
		int count = 2 + RANDOM.nextInt(2);
		for (int i = 0; i < count; i++) {
			EntityType<? extends Mob> type = TRAP_MOBS.get(RANDOM.nextInt(TRAP_MOBS.size()));
			BlockPos spot = findSpot(level, chest, 1, 3, type);
			Mob mob = type.create(level);
			if (spot == null || mob == null) {
				continue;
			}
			mob.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, RANDOM.nextFloat() * 360.0F, 0);
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null);
			level.addFreshEntity(mob);
			mob.spawnAnim();
		}
		level.playSound(null, chest, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.8F);
	}

	/** Turns the stone just above the player's head, and maybe its neighbours, into falling gravel. Only underground. */
	private static void caveIn(ServerLevel level, Player player) {
		BlockPos feet = player.blockPosition();
		if (level.canSeeSky(feet)) {
			return;
		}
		int fell = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				if ((dx != 0 || dz != 0) && RANDOM.nextInt(3) != 0) {
					continue;
				}
				for (int dy = 2; dy <= 4; dy++) {
					BlockPos pos = feet.offset(dx, dy, dz);
					BlockState state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					if (!state.is(BlockTags.BASE_STONE_OVERWORLD) && !state.is(BlockTags.BASE_STONE_NETHER) && !state.is(Blocks.DIRT) && !state.is(Blocks.GRAVEL)) {
						break;
					}
					FallingBlockEntity.fall(level, pos, Blocks.GRAVEL.defaultBlockState());
					fell++;
				}
			}
		}
		if (fell > 0) {
			level.playSound(null, feet, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
		}
	}

	// ---- called from mixins ----

	/** gravity_blocks: called after a player placed a block. Drops it like sand when nothing holds it up. */
	public static void afterPlace(Level world, BlockPos pos, BlockItem item, @Nullable Player player) {
		if (player == null || !(world instanceof ServerLevel level) || !Modifiers.isActive(ModifierCatalog.GRAVITY_BLOCKS, level)) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		if (!state.is(item.getBlock()) || block instanceof FallingBlock || block instanceof ScaffoldingBlock
			|| state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)
			|| pos.getY() <= level.getMinBuildHeight() || !FallingBlock.isFree(level.getBlockState(pos.below()))) {
			return;
		}
		FallingBlockEntity.fall(level, pos, state);
	}

	/** cursed_loot: called after a loot table filled a container. */
	public static void afterLootFill(Container container, ServerLevel level) {
		if (!Modifiers.isActive(ModifierCatalog.CURSED_LOOT, level)) {
			return;
		}
		var enchantments = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> binding = enchantments.getHolderOrThrow(Enchantments.BINDING_CURSE);
		Holder<Enchantment> vanishing = enchantments.getHolderOrThrow(Enchantments.VANISHING_CURSE);
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			boolean wearable = stack.getItem() instanceof Equipable equipable && equipable.getEquipmentSlot().isArmor();
			if (wearable) {
				stack.enchant(RANDOM.nextBoolean() ? binding : vanishing, 1);
			} else if (stack.isDamageableItem()) {
				stack.enchant(vanishing, 1);
			}
		}
	}

	/** tight_border and shrinking_border: vanilla sends the main overworld's border on every dimension change and respawn. */
	public static void afterSendLevelInfo(ServerPlayer player, ServerLevel level) {
		if (Modifiers.isActive(ModifierCatalog.TIGHT_BORDER, level) || Modifiers.isActive(ModifierCatalog.SHRINKING_BORDER, level)) {
			player.connection.send(new ClientboundInitializeBorderPacket(level.getWorldBorder()));
		}
	}

	/** close_stronghold: the ring distance to use for a structure state, called by the ring position mixin. */
	public static int strongholdDistance(ChunkGeneratorStructureState state, int vanilla) {
		return CLOSE_STRONGHOLD_STATES.contains(state) ? CLOSE_STRONGHOLD_DISTANCE : vanilla;
	}

	// ---- worldgen ----

	/** The overworld generator for these modifiers, or null when none of them changes world generation. */
	@Nullable
	public static ChunkGenerator overworldGenerator(RegistryAccess registries, Collection<String> modifiers) {
		boolean large = modifiers.contains(ModifierCatalog.LARGE_BIOMES);
		boolean amplified = modifiers.contains(ModifierCatalog.AMPLIFIED);
		boolean noVillages = modifiers.contains(ModifierCatalog.NO_VILLAGES);
		boolean closeStronghold = modifiers.contains(ModifierCatalog.CLOSE_STRONGHOLD);
		if (!large && !amplified && !noVillages && !closeStronghold) {
			return null;
		}
		HolderGetter<MultiNoiseBiomeSourceParameterList> params = registries.lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
		HolderGetter<NoiseGeneratorSettings> noise = registries.lookupOrThrow(Registries.NOISE_SETTINGS);
		var settings = noise.getOrThrow(amplified ? NoiseGeneratorSettings.AMPLIFIED : large ? NoiseGeneratorSettings.LARGE_BIOMES : NoiseGeneratorSettings.OVERWORLD);
		MultiNoiseBiomeSource biomes = MultiNoiseBiomeSource.createFromPreset(params.getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD));
		ChunkGenerator generator = new NoiseBasedChunkGenerator(biomes, settings);
		if (noVillages) {
			NO_VILLAGE_GENERATORS.add(generator);
		}
		if (closeStronghold) {
			CLOSE_STRONGHOLD_GENERATORS.add(generator);
		}
		return generator;
	}

	/** no_villages: the structure sets a generator may place, called by the createState mixin. */
	public static HolderLookup<StructureSet> structureSets(ChunkGenerator generator, HolderLookup<StructureSet> sets) {
		if (!NO_VILLAGE_GENERATORS.contains(generator) || !(sets instanceof HolderLookup.RegistryLookup<StructureSet> registry)) {
			return sets;
		}
		StructureSet villages = sets.get(BuiltinStructureSets.VILLAGES).map(Holder::value).orElse(null);
		return registry.filterElements(set -> set != villages);
	}

	/** close_stronghold: remembers the structure state a generator created, called by the createState mixin. */
	public static void onStructureState(ChunkGenerator generator, ChunkGeneratorStructureState state) {
		if (CLOSE_STRONGHOLD_GENERATORS.contains(generator)) {
			CLOSE_STRONGHOLD_STATES.add(state);
		}
	}

	// ---- helpers ----

	private static List<ServerLevel> levels(ActiveRun run) {
		return List.of(run.worlds.overworld(), run.worlds.nether(), run.worlds.end());
	}

	private void setRule(ServerLevel level, GameRules.Key<GameRules.BooleanValue> rule, boolean value) {
		GameRules.BooleanValue current = level.getGameRules().getRule(rule);
		savedBooleans.computeIfAbsent(level, key -> new HashMap<>()).putIfAbsent(rule, current.get());
		current.set(value, runs.server());
	}

	private void setRule(ServerLevel level, GameRules.Key<GameRules.IntegerValue> rule, int value) {
		GameRules.IntegerValue current = level.getGameRules().getRule(rule);
		savedInts.computeIfAbsent(level, key -> new HashMap<>()).putIfAbsent(rule, current.get());
		current.set(value, runs.server());
	}

	private static long randomMillis(long min, long max) {
		return min + (long) (RANDOM.nextDouble() * (max - min));
	}

	/** The next multiple of {@code interval} after {@code now}. */
	private static long nextMark(long now, long interval) {
		return (now / interval + 1) * interval;
	}

	@Nullable
	private static ServerPlayer pick(List<ServerPlayer> players) {
		return players.isEmpty() ? null : players.get(RANDOM.nextInt(players.size()));
	}

	/**
	 * A random standing spot between {@code min} and {@code max} blocks from {@code center} horizontally, near its height:
	 * sturdy ground below and room for {@code type} (or one air block when null). Null when 24 tries find nothing.
	 */
	@Nullable
	private static BlockPos findSpot(ServerLevel level, BlockPos center, int min, int max, @Nullable EntityType<?> type) {
		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = RANDOM.nextDouble() * Math.PI * 2;
			double distance = min + RANDOM.nextDouble() * (max - min);
			int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
			int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
			for (int dy = 3; dy >= -4; dy--) {
				BlockPos pos = new BlockPos(x, center.getY() + dy, z);
				if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
					continue;
				}
				BlockPos below = pos.below();
				if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
					continue;
				}
				boolean free = type == null
					? level.getBlockState(pos).isAir()
					: level.getBlockState(pos).isAir() && level.noCollision(type.getSpawnAABB(x + 0.5, pos.getY(), z + 0.5));
				if (free && level.getFluidState(pos).isEmpty()) {
					return pos;
				}
			}
		}
		return null;
	}
}
