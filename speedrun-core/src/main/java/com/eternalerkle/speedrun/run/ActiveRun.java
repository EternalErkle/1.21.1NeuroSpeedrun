package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.config.Goal;
import com.eternalerkle.speedrun.world.RunWorldSet;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything about the run in progress. Settings are copied in at start so mid-run changes cannot alter the category. */
public final class ActiveRun {
	public enum Boss { DRAGON, WARDEN, WITHER }

	public final int attempt;
	public final RunWorldSet worlds;
	public final Goal goal;
	public final float tickRate;
	public final boolean sharedHealth;
	public final boolean sharedHunger;
	public final List<String> modifiers;
	public final String category;
	public final long startNanos;
	public long gameTicks;
	public long endNanos = -1;
	/** Total time spent paused, and when the current pause began (-1 when not paused). */
	private long pausedNanos;
	private long pausedSince = -1;
	/** Split id to real milliseconds since start. */
	public final Map<String, Long> splits = new LinkedHashMap<>();
	public final Set<Boss> bossesKilled = EnumSet.noneOf(Boss.class);
	/** Set when a mid-run settings change made this run ineligible for records. */
	public boolean unranked;

	public ActiveRun(int attempt, RunWorldSet worlds, Goal goal, float tickRate, boolean sharedHealth, boolean sharedHunger, List<String> modifiers) {
		this(attempt, worlds, goal, tickRate, sharedHealth, sharedHunger, modifiers, 0);
	}

	/** A run that has already been going for {@code elapsedMillis}, restored after a server restart. */
	public ActiveRun(int attempt, RunWorldSet worlds, Goal goal, float tickRate, boolean sharedHealth, boolean sharedHunger, List<String> modifiers, long elapsedMillis) {
		this.attempt = attempt;
		this.worlds = worlds;
		this.goal = goal;
		this.tickRate = tickRate;
		this.sharedHealth = sharedHealth;
		this.sharedHunger = sharedHunger;
		this.modifiers = List.copyOf(modifiers);
		this.category = Category.key(goal, tickRate, sharedHealth, sharedHunger, modifiers);
		this.startNanos = System.nanoTime() - elapsedMillis * 1_000_000L;
	}

	public long realMillis() {
		return (clockNanos() - startNanos) / 1_000_000L;
	}

	/**
	 * The run's own clock, comparable with {@link #startNanos}. It stands still while the run is paused and after it
	 * ends, so timers based on it never count time nobody was playing.
	 */
	public long clockNanos() {
		long now = endNanos >= 0 ? endNanos : pausedSince >= 0 ? pausedSince : System.nanoTime();
		return now - pausedNanos;
	}

	public boolean isPaused() {
		return pausedSince >= 0;
	}

	public void pause() {
		if (pausedSince < 0 && endNanos < 0) {
			pausedSince = System.nanoTime();
		}
	}

	public void resume() {
		if (pausedSince >= 0) {
			pausedNanos += System.nanoTime() - pausedSince;
			pausedSince = -1;
		}
	}

	public boolean isFinished() {
		return endNanos >= 0;
	}

	public boolean goalComplete() {
		return switch (goal) {
			case DRAGON -> bossesKilled.contains(Boss.DRAGON);
			case ALLBOSSES -> bossesKilled.containsAll(EnumSet.allOf(Boss.class));
		};
	}

	/** Records a split the first time it is reached. Returns true when it was new. */
	public boolean split(String id) {
		if (splits.containsKey(id) || isFinished()) {
			return false;
		}
		splits.put(id, realMillis());
		return true;
	}
}
