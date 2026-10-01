package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.config.Settings.ModifierMode;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Decides which modifiers the next run gets: a forced set first, then the mode's rule. Holds the open vote.
 * Free of Minecraft classes so it can be unit tested.
 */
public final class ModifierSelector {
	private final Random random;
	private List<String> forced;
	private ModifierVote vote;
	/** Random mode: the next run's set, drawn secretly when its worlds started generating. */
	private List<String> predrawn;

	public ModifierSelector(Random random) {
		this.random = random;
	}

	/** Forces these ids for the next run only, whatever the mode. */
	public void force(List<String> ids) {
		this.forced = List.copyOf(ids);
	}

	public List<String> forced() {
		return forced;
	}

	/** The open vote, or null when none is being offered. */
	public ModifierVote vote() {
		return vote;
	}

	/** Opens a fresh vote when the mode is VOTE and the pool has something to offer. Returns the vote or null. */
	/**
	 * Draws the next run's modifiers ahead of time in random mode and returns what its worlds must be generated with:
	 * the forced set, the predrawn set, or the always-on set (vote mode, whose options never include worldgen).
	 */
	public List<String> predraw(ModifierMode mode, Collection<String> pool, int count, Collection<String> always) {
		predrawn = null;
		if (forced != null) {
			return forced;
		}
		if (mode == ModifierMode.RANDOM) {
			predrawn = combine(always, ModifierPicker.pick(pool, count, always, random));
			return predrawn;
		}
		return List.copyOf(always);
	}

	/**
	 * Settings changed after the predraw: redraw the non-worldgen part from the new pool. Worldgen picks stay, since
	 * the worlds were already generated with them.
	 */
	public void redrawKeepingWorldgen(ModifierMode mode, Collection<String> pool, int count, Collection<String> always) {
		if (predrawn == null) {
			return;
		}
		java.util.Set<String> kept = new java.util.LinkedHashSet<>(always);
		for (String id : predrawn) {
			if (ModifierCatalog.WORLDGEN.contains(id)) {
				kept.add(id);
			}
		}
		if (mode != ModifierMode.RANDOM) {
			predrawn = null;
			return;
		}
		predrawn = combine(kept, ModifierPicker.pick(withoutWorldgen(pool), count, kept, random));
	}

	/** Pool for draws made after the worlds exist: worldgen modifiers would silently do nothing there. */
	public static java.util.Set<String> withoutWorldgen(Collection<String> pool) {
		java.util.Set<String> result = new java.util.LinkedHashSet<>(pool);
		result.removeAll(ModifierCatalog.WORLDGEN);
		return result;
	}

	private static List<String> combine(Collection<String> base, Collection<String> drawn) {
		java.util.Set<String> result = new java.util.LinkedHashSet<>(base);
		for (String id : drawn) {
			if (!ModifierCatalog.conflictsWithAny(id, result)) {
				result.add(id);
			}
		}
		return ModifierPicker.sortByCatalog(result);
	}

	public ModifierVote openVote(ModifierMode mode, Collection<String> pool, int count, Collection<String> always) {
		vote = null;
		if (mode != ModifierMode.VOTE || forced != null) {
			return null;
		}
		List<List<String>> options = ModifierPicker.voteOptions(withoutWorldgen(pool), count, always, random);
		if (options.get(0).isEmpty()) {
			return null;
		}
		vote = new ModifierVote(options);
		return vote;
	}

	public void closeVote() {
		vote = null;
	}

	/**
	 * Chooses the next run's modifiers and consumes the forced set and the vote. The always-on set is added to every
	 * run, except that a one-off forced set replaces everything. Random and voted picks never repeat or contradict it.
	 */
	public List<String> pickForNextRun(ModifierMode mode, Collection<String> pool, int count, Collection<String> always) {
		List<String> drawn;
		if (forced != null) {
			drawn = forced;
		} else if (mode == ModifierMode.RANDOM && predrawn != null) {
			drawn = predrawn;
		} else if (mode == ModifierMode.RANDOM) {
			drawn = ModifierPicker.pick(withoutWorldgen(pool), count, always, random);
		} else if (mode == ModifierMode.VOTE) {
			// With no vote open (for example voting started before the mode was switched), fall back to a random draw.
			drawn = vote != null ? vote.winner(random) : ModifierPicker.pick(withoutWorldgen(pool), count, always, random);
		} else {
			drawn = List.of();
		}
		java.util.Set<String> result = new java.util.LinkedHashSet<>();
		if (forced == null) {
			result.addAll(always);
		}
		for (String id : drawn) {
			if (!ModifierCatalog.conflictsWithAny(id, result)) {
				result.add(id);
			}
		}
		forced = null;
		vote = null;
		predrawn = null;
		return ModifierPicker.sortByCatalog(result);
	}
}
