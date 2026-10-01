package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsAModifiers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * piglin_friends: a piglin never picks a player as attack target. The same check decides whether an attack continues,
 * so a piglin already chasing a player stops too.
 */
@Mixin(PiglinAi.class)
public class ModifierMobsAPiglinAiMixin {
	@ModifyReturnValue(method = "findNearestValidAttackTarget", at = @At("RETURN"))
	private static Optional<? extends LivingEntity> speedrun$piglinFriends(Optional<? extends LivingEntity> target, @Local(argsOnly = true) Piglin piglin) {
		return target.isPresent() && MobsAModifiers.blocksPiglinTarget(piglin, target.get()) ? Optional.empty() : target;
	}
}
