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

/**
 * Top-of-screen text and the death sidebar.
 *
 * <p>The text lines are boss bars in the WHITE color. The resource pack makes the white bar textures transparent,
 * so only the title shows. The dragon (pink) and wither (purple) bars are unaffected.
 */
public final class Hud {
	private static final String OBJECTIVE = "speedrun_deaths";

	private final MinecraftServer server;
	private final ServerBossEvent line1 = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private final ServerBossEvent line2 = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
	private Objective deaths;

	public Hud(MinecraftServer server) {
		this.server = server;
		line1.setProgress(0);
		line2.setProgress(0);
		line2.setVisible(false);
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
		line2.addPlayer(player);
		setDeaths(player.getScoreboardName(), deathCount);
	}

	public void removePlayer(ServerPlayer player) {
		line1.removePlayer(player);
		line2.removePlayer(player);
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

	/** Sets the second line, hiding it when {@code text} is null. */
	public void setLine2(Component text) {
		if (text == null) {
			line2.setVisible(false);
			return;
		}
		if (!text.equals(line2.getName())) {
			line2.setName(text);
		}
		line2.setVisible(true);
	}

	public void shutdown() {
		line1.removeAllPlayers();
		line2.removeAllPlayers();
	}
}
