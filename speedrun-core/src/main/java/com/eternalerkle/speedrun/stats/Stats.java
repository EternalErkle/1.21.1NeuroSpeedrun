package com.eternalerkle.speedrun.stats;

import com.eternalerkle.speedrun.util.JsonStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent run statistics. Owned and written by the server thread only. */
public final class Stats {
	/** Number of attempts started so far. The current run's attempt number equals this value. */
	public int attempts;
	public int wins;
	/** Category key to best winning run. */
	public Map<String, RunRecord> records = new LinkedHashMap<>();
	public List<RunRecord> history = new ArrayList<>();
	public Map<UUID, PlayerStats> players = new LinkedHashMap<>();

	private transient Path file;

	public static Stats load(Path file) {
		Stats stats = JsonStore.load(file, Stats.class, Stats::new);
		stats.file = file;
		if (stats.records == null) {
			stats.records = new LinkedHashMap<>();
		}
		if (stats.history == null) {
			stats.history = new ArrayList<>();
		}
		if (stats.players == null) {
			stats.players = new LinkedHashMap<>();
		}
		return stats;
	}

	public PlayerStats player(UUID id, String name) {
		PlayerStats stats = players.computeIfAbsent(id, key -> new PlayerStats());
		if (name != null) {
			stats.name = name;
		}
		return stats;
	}

	/** Stores a finished run and returns true when it set a new category record. */
	public boolean addRun(RunRecord run) {
		history.add(run);
		if (run.result != RunRecord.Result.WON) {
			return false;
		}
		RunRecord best = records.get(run.category);
		if (best == null || run.realMillis < best.realMillis) {
			records.put(run.category, run);
			return true;
		}
		return false;
	}

	public void save() {
		JsonStore.save(file, this);
	}
}
