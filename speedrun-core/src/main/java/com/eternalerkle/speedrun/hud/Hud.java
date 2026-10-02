package com.eternalerkle.speedrun.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-of-screen text and the death sidebar.
 *
 * <p>The text lines are boss bars in the WHITE color. The resource pack makes the white bar textures transparent,
 * so only the title shows. The dragon (pink) and wither (purple) bars are unaffected.
 *
 * <p>Boss bar titles never wrap, so text longer than the screen is split over several stacked lower lines.
 */
public final class Hud {
	private static final String OBJECTIVE = "speedrun_deaths";

	private final MinecraftServer server;
	private final ServerBossEvent line1 = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private static final int MAX_LOWER_LINES = 8;
	private final List<ServerBossEvent> lower = new ArrayList<>();
	private int shownLower;
	private Objective deaths;

	public Hud(MinecraftServer server) {
		this.server = server;
		line1.setProgress(0);
		for (int i = 0; i < MAX_LOWER_LINES; i++) {
			ServerBossEvent line = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
			line.setProgress(0);
			line.setVisible(false);
			lower.add(line);
		}
	}

	public void init() {
		Scoreboard scoreboard = server.getScoreboard();
		Objective existing = scoreboard.getObjective(OBJECTIVE);
		if (existing != null) {
			scoreboard.removeObjective(existing);
		}
		deaths = scoreboard.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal("Deaths"), ObjectiveCriteria.RenderType.INTEGER, true, null);
		scoreboard.setDisplayObjective(DisplaySlot.SIDEBAR, deaths);
	}

	public void addPlayer(ServerPlayer player, int deathCount) {
		line1.addPlayer(player);
		lower.forEach(line -> line.addPlayer(player));
		setDeaths(player.getScoreboardName(), deathCount);
	}

	public void removePlayer(ServerPlayer player) {
		line1.removePlayer(player);
		lower.forEach(line -> line.removePlayer(player));
		if (deaths != null) {
			server.getScoreboard().resetSinglePlayerScore(ScoreHolder.forNameOnly(player.getScoreboardName()), deaths);
		}
	}

	public void setDeaths(String name, int count) {
		if (deaths != null) {
			server.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), deaths).set(count);
		}
	}

	public void setLine1(Component text) {
		if (!text.equals(line1.getName())) {
			line1.setName(text);
		}
	}

	/** Sets the lines under line 1. An empty list hides them. Lines past {@link #MAX_LOWER_LINES} are dropped. */
	public void setLowerLines(List<Component> texts) {
		int count = Math.min(texts.size(), MAX_LOWER_LINES);
		if (count != shownLower) {
			// The client stacks bars in the order they were shown, so re-show them all to keep the order.
			lower.forEach(line -> line.setVisible(false));
			for (int i = 0; i < count; i++) {
				lower.get(i).setVisible(true);
			}
			shownLower = count;
		}
		for (int i = 0; i < count; i++) {
			if (!texts.get(i).equals(lower.get(i).getName())) {
				lower.get(i).setName(texts.get(i));
			}
		}
	}

	public void shutdown() {
		line1.removeAllPlayers();
		lower.forEach(ServerBossEvent::removeAllPlayers);
	}
}
