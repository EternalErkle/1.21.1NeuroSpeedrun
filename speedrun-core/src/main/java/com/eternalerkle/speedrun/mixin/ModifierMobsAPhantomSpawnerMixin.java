package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** phantom_menace: every player counts as four days without sleep, so each night check has a 3 in 4 chance. */
@Mixin(PhantomSpawner.class)
public class ModifierMobsAPhantomSpawnerMixin {
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
	private int speedrun$phantomMenace(int ticksSinceRest, @Local(argsOnly = true) ServerLevel level) {
		return Modifiers.isActive(ModifierCatalog.PHANTOM_MENACE, level) ? Math.max(ticksSinceRest, 4 * 72000) : ticksSinceRest;
	}
}
