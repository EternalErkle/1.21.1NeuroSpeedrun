package com.eternalerkle.speedrun.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Places one village near spawn, the way /place structure does. The village type matches the biome at the chosen spot.
 * Planning reads only noise, so it loads no chunks; placing needs every chunk under the village loaded.
 */
final class VillagePlacer {
	private static final int MIN_DISTANCE = 100;
	private static final int MAX_DISTANCE = 500;
	/** Tries for a spot in a biome with its own village type, then for any dry land, which gets a plains village. */
	private static final int BIOME_TRIES = 256;
	private static final int LAND_TRIES = 128;

	private static final List<Map.Entry<TagKey<Biome>, ResourceKey<Structure>>> VILLAGES = List.of(
		Map.entry(BiomeTags.HAS_VILLAGE_PLAINS, BuiltinStructures.VILLAGE_PLAINS),
		Map.entry(BiomeTags.HAS_VILLAGE_DESERT, BuiltinStructures.VILLAGE_DESERT),
		Map.entry(BiomeTags.HAS_VILLAGE_SAVANNA, BuiltinStructures.VILLAGE_SAVANNA),
		Map.entry(BiomeTags.HAS_VILLAGE_SNOWY, BuiltinStructures.VILLAGE_SNOWY),
		Map.entry(BiomeTags.HAS_VILLAGE_TAIGA, BuiltinStructures.VILLAGE_TAIGA));

	private VillagePlacer() {
	}

	/** A village 100 to 500 blocks from spawn, the same for the same seed, or null when no dry land is in range. */
	@Nullable
	static StructureStart plan(ServerLevel level, BlockPos spawn, long seed) {
		RandomSource random = RandomSource.create(seed ^ 0x5649_4C4C_4147_45L);
		for (int i = 0; i < BIOME_TRIES + LAND_TRIES; i++) {
			float angle = random.nextFloat() * Mth.TWO_PI;
			int distance = Mth.randomBetweenInclusive(random, MIN_DISTANCE, MAX_DISTANCE);
			BlockPos pos = spawn.offset((int) (Mth.cos(angle) * distance), 0, (int) (Mth.sin(angle) * distance));
			ResourceKey<Structure> type = villageAt(level, pos, i >= BIOME_TRIES);
			if (type == null) {
				continue;
			}
			StructureStart start = generate(level, type, new ChunkPos(pos));
			if (start.isValid()) {
				return start;
			}
		}
		return null;
	}

	/** The village type for this spot, or null when it is underwater or, unless {@code anyLand}, not a village biome. */
	@Nullable
	private static ResourceKey<Structure> villageAt(ServerLevel level, BlockPos pos, boolean anyLand) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		RandomState randomState = level.getChunkSource().randomState();
		int ground = generator.getBaseHeight(pos.getX(), pos.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, level, randomState);
		if (ground < generator.getSeaLevel()) {
			return null;
		}
		Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(
			QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(ground), QuartPos.fromBlock(pos.getZ()), randomState.sampler());
		for (Map.Entry<TagKey<Biome>, ResourceKey<Structure>> village : VILLAGES) {
			if (biome.is(village.getKey())) {
				return village.getValue();
			}
		}
		boolean wet = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH);
		return anyLand && !wet ? BuiltinStructures.VILLAGE_PLAINS : null;
	}

	private static StructureStart generate(ServerLevel level, ResourceKey<Structure> type, ChunkPos chunk) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(type);
		return structure.generate(level.registryAccess(), generator, generator.getBiomeSource(), level.getChunkSource().randomState(),
			level.getStructureManager(), level.getSeed(), chunk, 0, level, holder -> true);
	}

	static ChunkPos minChunk(StructureStart start) {
		BoundingBox box = start.getBoundingBox();
		return new ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()));
	}

	static ChunkPos maxChunk(StructureStart start) {
		BoundingBox box = start.getBoundingBox();
		return new ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()));
	}

	/** Places the village. Every chunk from {@link #minChunk} to {@link #maxChunk} must be loaded. */
	static void place(ServerLevel level, StructureStart start) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		ChunkPos.rangeClosed(minChunk(start), maxChunk(start)).forEach(chunk -> start.placeInChunk(level, level.structureManager(), generator,
			level.getRandom(), new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
				chunk.getMaxBlockX(), level.getMaxBuildHeight(), chunk.getMaxBlockZ()), chunk));
	}
}
