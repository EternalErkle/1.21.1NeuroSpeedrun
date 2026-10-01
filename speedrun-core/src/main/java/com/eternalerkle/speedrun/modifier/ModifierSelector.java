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
	public ModifierVote openVote(ModifierMode mode, Collection<String> pool, int count, Collection<String> always) {
		vote = null;
		if (mode != ModifierMode.VOTE || forced != null) {
			return null;
		}
		List<List<String>> options = ModifierPicker.voteOptions(pool, count, always, random);
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
		} else if (mode == ModifierMode.RANDOM) {
			drawn = ModifierPicker.pick(pool, count, always, random);
		} else if (mode == ModifierMode.VOTE) {
			// With no vote open (for example voting started before the mode was switched), fall back to a random draw.
			drawn = vote != null ? vote.winner(random) : ModifierPicker.pick(pool, count, always, random);
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
		return ModifierPicker.sortByCatalog(result);
	}
}
