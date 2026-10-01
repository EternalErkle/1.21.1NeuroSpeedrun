package com.eternalerkle.speedrun.modifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Draws modifier sets from the pool and validates typed id lists. Free of Minecraft classes. */
public final class ModifierPicker {
	/** Draw attempts made to give the second vote option a different set from the first. */
	private static final int DISTINCT_OPTION_ATTEMPTS = 16;

	private ModifierPicker() {
	}

	/**
	 * Picks up to {@code count} distinct known ids from the pool, never both halves of a conflicting pair.
	 * Returns fewer when the pool is too small. The result is in catalog order.
	 */
	public static List<String> pick(Collection<String> pool, int count, Random random) {
		return pick(pool, count, List.of(), random);
	}

	/** Like {@link #pick(Collection, int, Random)}, but never draws an id in {@code taken} or one conflicting with it. */
	public static List<String> pick(Collection<String> pool, int count, Collection<String> taken, Random random) {
		List<String> candidates = new ArrayList<>();
		for (String id : ModifierCatalog.ids()) {
			if (pool.contains(id) && !taken.contains(id) && !ModifierCatalog.conflictsWithAny(id, taken)) {
				candidates.add(id);
			}
		}
		Collections.shuffle(candidates, random);
		List<String> chosen = new ArrayList<>();
		for (String id : candidates) {
			if (chosen.size() >= count) {
				break;
			}
			if (!ModifierCatalog.conflictsWithAny(id, chosen)) {
				chosen.add(id);
			}
		}
		return sortByCatalog(chosen);
	}

	/** Two vote options drawn from the pool, made different from each other when the pool allows it. */
	public static List<List<String>> voteOptions(Collection<String> pool, int count, Collection<String> taken, Random random) {
		List<String> first = pick(pool, count, taken, random);
		List<String> second = pick(pool, count, taken, random);
		for (int i = 0; i < DISTINCT_OPTION_ATTEMPTS && second.equals(first); i++) {
			second = pick(pool, count, taken, random);
		}
		return List.of(first, second);
	}

	public static List<String> sortByCatalog(Collection<String> ids) {
		List<String> order = new ArrayList<>(ModifierCatalog.ids());
		List<String> sorted = new ArrayList<>(ids);
		sorted.sort(Comparator.comparingInt(order::indexOf));
		return sorted;
	}

	/** Result of {@link #parse}: either ids or an error message. */
	public record Parsed(List<String> ids, String error) {
		public boolean ok() {
			return error == null;
		}
	}

	/** Parses a space separated id list as typed in /speedrun modifiers force. */
	public static Parsed parse(String input) {
		Set<String> ids = new LinkedHashSet<>();
		List<String> unknown = new ArrayList<>();
		for (String raw : input.trim().split("\\s+")) {
			if (raw.isEmpty()) {
				continue;
			}
			String id = raw.toLowerCase(java.util.Locale.ROOT);
			if (!ModifierCatalog.isKnown(id)) {
				unknown.add(raw);
			} else if (!ids.add(id)) {
				return new Parsed(List.of(), "Modifier listed twice: " + id);
			}
		}
		if (!unknown.isEmpty()) {
			return new Parsed(List.of(), "Unknown modifier" + (unknown.size() > 1 ? "s: " : ": ") + String.join(", ", unknown));
		}
		if (ids.isEmpty()) {
			return new Parsed(List.of(), "List at least one modifier id.");
		}
		List<String> checked = new ArrayList<>();
		for (String id : ids) {
			for (String other : checked) {
				if (ModifierCatalog.conflicts(id, other)) {
					return new Parsed(List.of(), other + " and " + id + " cannot be active together.");
				}
			}
			checked.add(id);
		}
		return new Parsed(sortByCatalog(ids), null);
	}
}
