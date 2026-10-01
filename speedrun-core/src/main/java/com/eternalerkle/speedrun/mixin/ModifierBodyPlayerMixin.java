package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.BodyModifiers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** critical_only: records whether the swing in progress is a critical hit, before attack resets the cooldown. */
@Mixin(Player.class)
public class ModifierBodyPlayerMixin {
	@Inject(method = "attack", at = @At("HEAD"))
	private void speedrun$startAttack(Entity target, CallbackInfo ci) {
		BodyModifiers.startAttack((Player) (Object) this, target);
	}

	@Inject(method = "attack", at = @At("RETURN"))
	private void speedrun$endAttack(Entity target, CallbackInfo ci) {
		BodyModifiers.endAttack();
	}
}
