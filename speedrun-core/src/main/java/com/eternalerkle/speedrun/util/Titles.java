package com.eternalerkle.speedrun.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class Titles {
	private Titles() {
	}

	/** Shows a title to every online player. Durations are real seconds. */
	public static void show(MinecraftServer server, Component title, Component subtitle, double fadeIn, double stay, double fadeOut) {
		var animation = new ClientboundSetTitlesAnimationPacket(
			fadeIn <= 0 ? 0 : Time.ticks(server, fadeIn), Time.ticks(server, stay), fadeOut <= 0 ? 0 : Time.ticks(server, fadeOut));
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.connection.send(animation);
			player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
			player.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}
}
