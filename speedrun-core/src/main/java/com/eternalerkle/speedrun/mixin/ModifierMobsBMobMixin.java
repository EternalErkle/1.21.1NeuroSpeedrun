package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsBModifiers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** swarm: natural spawns are the only ones finalized with MobSpawnType.NATURAL, so this sees each one exactly once. */
@Mixin(Mob.class)
public class ModifierMobsBMobMixin {
	@Inject(method = "finalizeSpawn", at = @At("RETURN"))
	private void speedrun$swarm(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
		CallbackInfoReturnable<SpawnGroupData> cir) {
		MobsBModifiers.onFinalizeSpawn((Mob) (Object) this, type);
	}
}
