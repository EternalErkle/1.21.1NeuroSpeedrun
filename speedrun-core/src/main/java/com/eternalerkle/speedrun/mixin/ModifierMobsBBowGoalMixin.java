package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** sniper_skeletons: the bow goal's squared attack radius is quadrupled, doubling the range skeletons shoot and strafe at. */
@Mixin(RangedBowAttackGoal.class)
public class ModifierMobsBBowGoalMixin {
	@Shadow
	@Final
	private Monster mob;

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/entity/ai/goal/RangedBowAttackGoal;attackRadiusSqr:F"))
	private float speedrun$sniperRange(float radiusSqr) {
		return Modifiers.isActive(ModifierCatalog.SNIPER_SKELETONS, mob.level()) ? radiusSqr * 4.0F : radiusSqr;
	}
}
