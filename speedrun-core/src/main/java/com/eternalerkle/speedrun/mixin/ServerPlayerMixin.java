package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.SpeedrunCore;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.injection.At;

/** Stops sneaking from releasing the death room camera lock, which would otherwise flicker every time. */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;wantsToStopRiding()Z"))
	private boolean speedrun$keepCameraLock(boolean wantsToStop) {
		if (wantsToStop && SpeedrunCore.runs() != null && SpeedrunCore.runs().isCameraLocked((ServerPlayer) (Object) this)) {
			return false;
		}
		return wantsToStop;
	}
}
