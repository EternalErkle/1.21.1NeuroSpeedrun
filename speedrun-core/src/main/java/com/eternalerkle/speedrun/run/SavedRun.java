package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.config.Goal;
import com.eternalerkle.speedrun.util.JsonStore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A live run written to disk so it survives a server restart when keep run is on. The world files hold blocks,
 * entities and players; this holds everything else about the run.
 */
public final class SavedRun {
	public String worldId;
	public long seed;
	public int spawnX;
	public int spawnY;
	public int spawnZ;
	public int attempt;
	public Goal goal;
	public float tickRate;
	public boolean sharedHealth;
	public boolean sharedHunger;
	public List<String> modifiers = new ArrayList<>();
	public long elapsedMillis;
	public long gameTicks;
	public Map<String, Long> splits = new LinkedHashMap<>();
	public List<ActiveRun.Boss> bossesKilled = new ArrayList<>();
	public boolean unranked;
	/** Fantasy keeps a run world's time and weather in memory only, so they are saved here. */
	public long dayTime;
	public int clearWeatherTime;
	public int rainTime;
	public boolean raining;
	public boolean thundering;
	@Nullable
	public JsonElement dragon;
	/** State saved by each {@link RunFeature}, under keys of its own. */
	public JsonObject features = new JsonObject();

	@Nullable
	static SavedRun load(Path file) {
		if (!Files.exists(file)) {
			return null;
		}
		SavedRun saved = JsonStore.load(file, SavedRun.class, () -> null);
		if (saved == null || saved.worldId == null || saved.goal == null) {
			return null;
		}
		if (saved.features == null) {
			saved.features = new JsonObject();
		}
		if (saved.modifiers == null) {
			saved.modifiers = new ArrayList<>();
		}
		return saved;
	}

	static void delete(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException ignored) {
		}
	}
}
