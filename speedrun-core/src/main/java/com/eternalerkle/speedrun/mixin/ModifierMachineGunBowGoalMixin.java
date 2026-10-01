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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * machine_gun_skeletons: a skeleton fires after drawing its bow for 2 ticks instead of 20, at full power, with no
 * cooldown between shots. Roughly one arrow every 3 ticks.
 */
@Mixin(RangedBowAttackGoal.class)
public class ModifierMachineGunBowGoalMixin {
	private static final int DRAW_TICKS = 2;

	@Shadow
	@Final
	private Monster mob;

	@ModifyConstant(method = "tick", constant = @Constant(intValue = 20))
	private int speedrun$drawTime(int ticks) {
		return active() ? DRAW_TICKS : ticks;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/item/BowItem;getPowerForTime(I)F"))
	private float speedrun$fullPower(float power) {
		return active() ? 1.0F : power;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD",
		target = "Lnet/minecraft/world/entity/ai/goal/RangedBowAttackGoal;attackIntervalMin:I"))
	private int speedrun$noCooldown(int interval) {
		return active() ? 1 : interval;
	}

	private boolean active() {
		return Modifiers.isActive(ModifierCatalog.MACHINE_GUN_SKELETONS, mob.level());
	}
}
