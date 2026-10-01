package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsAModifiers;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** sharpshooters: halves the bow cooldown every time the skeleton picks its weapon goal. */
@Mixin(AbstractSkeleton.class)
public class ModifierMobsASkeletonMixin {
	@ModifyArg(method = "reassessWeaponGoal", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/ai/goal/RangedBowAttackGoal;setMinAttackInterval(I)V"))
	private int speedrun$sharpshooters(int interval) {
		return MobsAModifiers.bowInterval((Mob) (Object) this, interval);
	}
}
