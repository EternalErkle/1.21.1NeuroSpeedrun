package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.BodyModifiers;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** explosive_arrows: skeleton arrows explode instead of hitting. */
@Mixin(Projectile.class)
public class ModifierBodyProjectileMixin {
	@Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
	private void speedrun$explosiveArrows(HitResult hit, CallbackInfo ci) {
		if (hit.getType() != HitResult.Type.MISS && BodyModifiers.explodeArrow((Projectile) (Object) this)) {
			ci.cancel();
		}
	}
}
