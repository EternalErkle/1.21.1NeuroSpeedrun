package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.MobModifiers;
import com.eternalerkle.speedrun.modifier.Modifiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * horde, part 1. Both spawn cap checks (the level-wide cap in SpawnState and the per-player cap in LocalMobCapCalculator)
 * read MobCategory.getMaxInstancesPerChunk() and run only inside spawnForChunk, but neither knows the level. This marks
 * when spawnForChunk is running for a horde level, and {@link ModifierMobCategoryMixin} doubles the monster cap then.
 * This keeps vanilla's own counting and needs no copy of the cap formula.
 */
@Mixin(NaturalSpawner.class)
public class ModifierNaturalSpawnerMixin {
	@Inject(method = "spawnForChunk", at = @At("HEAD"))
	private static void speedrun$hordeStart(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state, boolean friendlies, boolean enemies, boolean persistent, CallbackInfo ci) {
		MobModifiers.doublingMonsterCap = Modifiers.isActive(ModifierCatalog.HORDE, level);
	}

	@Inject(method = "spawnForChunk", at = @At("RETURN"))
	private static void speedrun$hordeEnd(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state, boolean friendlies, boolean enemies, boolean persistent, CallbackInfo ci) {
		MobModifiers.doublingMonsterCap = false;
	}
}
