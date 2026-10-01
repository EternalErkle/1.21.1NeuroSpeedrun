package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsAModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.EnderDragonPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** arrow_dragon: all dragon damage, to the body or any part, passes through this method. */
@Mixin(EnderDragon.class)
public class ModifierMobsAEnderDragonMixin {
	@Inject(method = "hurt(Lnet/minecraft/world/entity/boss/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
		at = @At("HEAD"), cancellable = true)
	private void speedrun$arrowDragon(EnderDragonPart part, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (MobsAModifiers.dragonIgnores((EnderDragon) (Object) this, source)) {
			cir.setReturnValue(false);
		}
	}
}
