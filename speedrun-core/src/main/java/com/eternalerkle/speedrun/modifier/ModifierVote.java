package com.eternalerkle.speedrun.modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** An open vote between modifier sets. One vote per player, which they may change. Free of Minecraft classes. */
public final class ModifierVote {
	private final List<List<String>> options;
	private final Map<UUID, Integer> votes = new HashMap<>();

	public ModifierVote(List<List<String>> options) {
		if (options.isEmpty()) {
			throw new IllegalArgumentException("A vote needs at least one option");
		}
		this.options = List.copyOf(options);
	}

	public List<List<String>> options() {
		return options;
	}

	/**
	 * Records a player's vote for a 1-based option number, replacing any earlier vote.
	 * Returns false when the number is out of range.
	 */
	public boolean cast(UUID player, int option) {
		if (option < 1 || option > options.size()) {
			return false;
		}
		votes.put(player, option);
		return true;
	}

	/** Votes per option, indexed from 0. */
	public int[] counts() {
		int[] counts = new int[options.size()];
		for (int option : votes.values()) {
			counts[option - 1]++;
		}
		return counts;
	}

	/** The winning option's modifiers. Ties, including no votes at all, are broken at random. */
	public List<String> winner(Random random) {
		int[] counts = counts();
		int best = -1;
		List<Integer> leaders = new ArrayList<>();
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] > best) {
				best = counts[i];
				leaders.clear();
			}
			if (counts[i] == best) {
				leaders.add(i);
			}
		}
		return options.get(leaders.get(random.nextInt(leaders.size())));
	}
}
