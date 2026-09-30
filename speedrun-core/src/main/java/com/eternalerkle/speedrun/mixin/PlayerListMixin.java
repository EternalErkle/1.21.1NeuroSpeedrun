package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.SpeedrunCore;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands a joining player to the run manager once they are fully placed. Fabric's JOIN event fires midway through
 * placeNewPlayer, before the player is added to their level, so a teleport to another dimension from there left the
 * player tracked by both the run world and the level they were loaded into.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void speedrun$afterJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		if (SpeedrunCore.runs() != null) {
			SpeedrunCore.runs().onJoin(player);
		}
	}
}
