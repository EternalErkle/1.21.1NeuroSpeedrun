package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsAModifiers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** nether_invasion and blaze_swarm add spawn entries; spawn_randomizer swaps the mob that gets created. */
@Mixin(NaturalSpawner.class)
public class ModifierMobsANaturalSpawnerMixin {
	@ModifyReturnValue(method = "mobsAt", at = @At("RETURN"))
	private static WeightedRandomList<MobSpawnSettings.SpawnerData> speedrun$extraSpawns(WeightedRandomList<MobSpawnSettings.SpawnerData> spawns,
		@Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) MobCategory category) {
		return MobsAModifiers.extraSpawns(level, category, spawns);
	}

	@ModifyArg(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getMobForSpawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/world/entity/Mob;"),
		index = 1)
	private static EntityType<?> speedrun$spawnRandomizer(ServerLevel level, EntityType<?> type) {
		return MobsAModifiers.randomizeSpawn(level, type);
	}
}
