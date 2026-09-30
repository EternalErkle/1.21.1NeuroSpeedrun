package com.eternalerkle.speedrun.util;

import net.minecraft.server.MinecraftServer;

public final class Time {
	private Time() {
	}

	/** Converts real seconds to game ticks at the server's current tick rate. */
	public static int ticks(MinecraftServer server, double seconds) {
		return Math.max(1, (int) Math.round(seconds * server.tickRateManager().tickrate()));
	}

	/** Formats milliseconds as H:MM:SS, or M:SS under an hour. */
	public static String format(long millis) {
		long totalSeconds = Math.max(0, millis) / 1000;
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;
		return hours > 0
			? String.format("%d:%02d:%02d", hours, minutes, seconds)
			: String.format("%02d:%02d", minutes, seconds);
	}

	/** Formats a signed difference as +M:SS or -M:SS. */
	public static String formatDelta(long millis) {
		return (millis < 0 ? "-" : "+") + format(Math.abs(millis));
	}
}
