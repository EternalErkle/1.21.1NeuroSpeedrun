package com.eternalerkle.speedrun.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One finished run, as stored in the history and as a category record. */
public final class RunRecord {
	public enum Result { WON, DIED, ABANDONED, SKIPPED, RESET }

	public int attempt;
	public String category;
	public Result result;
	public long realMillis;
	public long gameTicks;
	public long seed;
	public long endedAtEpochMillis;
	public List<String> players = new ArrayList<>();
	/** Death summary text, or the reason the run ended when nobody died. */
	public String cause;
	/** Split id to real milliseconds since run start, in the order reached. */
	public Map<String, Long> splits = new LinkedHashMap<>();
}
