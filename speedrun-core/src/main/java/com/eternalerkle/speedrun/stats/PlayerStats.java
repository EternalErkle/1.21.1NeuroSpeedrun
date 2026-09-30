package com.eternalerkle.speedrun.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PlayerStats {
	public String name;
	public int lifetimeDeaths;
	public int deathsSinceWin;
	public int runsPlayed;
	public int wins;
	/** Death cause key (damage type id, plus attacker type when present) to count. */
	public Map<String, Integer> deathCauses = new LinkedHashMap<>();
	/** Attempt number of the last run this player was reset into. */
	public int lastAttempt;
}
