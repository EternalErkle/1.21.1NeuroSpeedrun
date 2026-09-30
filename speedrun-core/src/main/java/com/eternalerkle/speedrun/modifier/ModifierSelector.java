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
	public ModifierVote openVote(ModifierMode mode, Collection<String> pool, int count) {
		vote = null;
		if (mode != ModifierMode.VOTE || forced != null) {
			return null;
		}
		List<List<String>> options = ModifierPicker.voteOptions(pool, count, random);
		if (options.get(0).isEmpty()) {
			return null;
		}
		vote = new ModifierVote(options);
		return vote;
	}

	public void closeVote() {
		vote = null;
	}

	/** Chooses the next run's modifiers and consumes the forced set and the vote. */
	public List<String> pickForNextRun(ModifierMode mode, Collection<String> pool, int count) {
		List<String> result;
		if (forced != null) {
			result = forced;
		} else if (mode == ModifierMode.RANDOM) {
			result = ModifierPicker.pick(pool, count, random);
		} else if (mode == ModifierMode.VOTE) {
			// With no vote open (for example voting started before the mode was switched), fall back to a random draw.
			result = vote != null ? vote.winner(random) : ModifierPicker.pick(pool, count, random);
		} else {
			result = List.of();
		}
		forced = null;
		vote = null;
		return result;
	}
}
