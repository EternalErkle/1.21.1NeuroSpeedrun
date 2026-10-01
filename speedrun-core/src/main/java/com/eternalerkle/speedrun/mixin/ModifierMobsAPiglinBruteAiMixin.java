package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsAModifiers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/** piglin_friends for piglin brutes, which have their own brain. */
@Mixin(PiglinBruteAi.class)
public class ModifierMobsAPiglinBruteAiMixin {
	@ModifyReturnValue(method = "findNearestValidAttackTarget", at = @At("RETURN"))
	private static Optional<? extends LivingEntity> speedrun$piglinFriends(Optional<? extends LivingEntity> target, @Local(argsOnly = true) AbstractPiglin brute) {
		return target.isPresent() && MobsAModifiers.blocksPiglinTarget(brute, target.get()) ? Optional.empty() : target;
	}
}
