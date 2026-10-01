package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.WorldModifiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** tight_border and shrinking_border: send the run level's own border instead of the main overworld's. */
@Mixin(PlayerList.class)
public abstract class ModifierWorldPlayerListMixin {
	@Inject(method = "sendLevelInfo", at = @At("TAIL"))
	private void speedrun$runBorder(ServerPlayer player, ServerLevel level, CallbackInfo ci) {
		WorldModifiers.afterSendLevelInfo(player, level);
	}
}
