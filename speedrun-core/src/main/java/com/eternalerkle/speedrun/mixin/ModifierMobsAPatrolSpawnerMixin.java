package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** patrol_season: a patrol every 2 minutes, day or night, from the first day, without the 1 in 5 roll. */
@Mixin(PatrolSpawner.class)
public class ModifierMobsAPatrolSpawnerMixin {
	@ModifyConstant(method = "tick", constant = @Constant(intValue = 12000))
	private int speedrun$interval(int interval, @Local(argsOnly = true) ServerLevel level) {
		return Modifiers.isActive(ModifierCatalog.PATROL_SEASON, level) ? 2400 : interval;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getDayTime()J"))
	private long speedrun$anyDay(long dayTime, @Local(argsOnly = true) ServerLevel level) {
		return Modifiers.isActive(ModifierCatalog.PATROL_SEASON, level) ? Math.max(dayTime, 5L * 24000L) : dayTime;
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isDay()Z"))
	private boolean speedrun$anyTime(boolean day, @Local(argsOnly = true) ServerLevel level) {
		return day || Modifiers.isActive(ModifierCatalog.PATROL_SEASON, level);
	}

	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 1))
	private int speedrun$always(int roll, @Local(argsOnly = true) ServerLevel level) {
		return Modifiers.isActive(ModifierCatalog.PATROL_SEASON, level) ? 0 : roll;
	}
}
