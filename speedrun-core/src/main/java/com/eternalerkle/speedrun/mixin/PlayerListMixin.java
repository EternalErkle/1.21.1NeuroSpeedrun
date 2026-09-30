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
 * Tells the run manager about joins and leaves on the server thread.
 * <p>
 * Join: hands a joining player to the run manager once they are fully placed. Fabric's JOIN event fires midway through
 * placeNewPlayer, before the player is added to their level, so a teleport to another dimension from there left the
 * player tracked by both the run world and the level they were loaded into.
 * <p>
 * Leave: Fabric's DISCONNECT event can fire on a Netty IO thread, and touching game state from there corrupts it.
 * PlayerList.remove runs on the server thread while the player is still in their level.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void speedrun$afterJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		if (SpeedrunCore.runs() != null) {
			SpeedrunCore.runs().onJoin(player);
		}
	}

	@Inject(method = "remove", at = @At("HEAD"))
	private void speedrun$beforeLeave(ServerPlayer player, CallbackInfo ci) {
		if (SpeedrunCore.runs() != null) {
			SpeedrunCore.runs().onLeave(player);
		}
	}
}
