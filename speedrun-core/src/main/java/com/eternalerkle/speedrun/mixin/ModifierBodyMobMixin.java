package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.BodyModifiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** undead: undead mobs refuse to target undead players. */
@Mixin(Mob.class)
public class ModifierBodyMobMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void speedrun$undeadIgnore(LivingEntity target, CallbackInfo ci) {
		if (BodyModifiers.undeadIgnores((Mob) (Object) this, target)) {
			ci.cancel();
		}
	}
}
