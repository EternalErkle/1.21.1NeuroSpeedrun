package com.eternalerkle.speedrun.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pure helpers behind the commands, kept free of Minecraft classes so they can be unit tested. */
public final class CommandLogic {
	private CommandLogic() {
	}

	/** Number of pages needed for the given entries, never less than one. */
	public static int pageCount(int entries, int perPage) {
		return Math.max(1, (entries + perPage - 1) / perPage);
	}

	/** Clamps a 1-based page number into range. */
	public static int clampPage(int page, int entries, int perPage) {
		return Math.max(1, Math.min(page, pageCount(entries, perPage)));
	}

	/** The entries on a 1-based page, after clamping the page number. */
	public static <T> List<T> page(List<T> entries, int page, int perPage) {
		int clamped = clampPage(page, entries.size(), perPage);
		int from = (clamped - 1) * perPage;
		return entries.subList(from, Math.min(entries.size(), from + perPage));
	}

	/** Orders docs by category, categories in first-registered order, keeping registration order inside each. */
	public static List<CommandDoc> groupByCategory(List<CommandDoc> docs) {
		Map<String, List<CommandDoc>> groups = new LinkedHashMap<>();
		for (CommandDoc doc : docs) {
			groups.computeIfAbsent(doc.category(), key -> new ArrayList<>()).add(doc);
		}
		List<CommandDoc> ordered = new ArrayList<>();
		groups.values().forEach(ordered::addAll);
		return ordered;
	}

	/** Finds a doc by command name, ignoring case, a leading slash and repeated spaces. */
	public static CommandDoc find(List<CommandDoc> docs, String name) {
		String wanted = normalize(name);
		for (CommandDoc doc : docs) {
			if (doc.name().equalsIgnoreCase(wanted)) {
				return doc;
			}
		}
		return null;
	}

	static String normalize(String name) {
		String trimmed = name.trim().replaceAll("\\s+", " ");
		return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
	}

	/** Parses a positive page number, or returns -1 when the text is not one. */
	public static int parsePage(String text) {
		String trimmed = text.trim();
		if (trimmed.isEmpty() || trimmed.length() > 6 || !trimmed.chars().allMatch(Character::isDigit)) {
			return -1;
		}
		int page = Integer.parseInt(trimmed);
		return page > 0 ? page : -1;
	}

	/** Actual ticks per second: the target rate, or less when the average tick takes longer than its budget. */
	public static double measuredTps(float targetRate, long averageTickNanos) {
		if (averageTickNanos <= 0) {
			return targetRate;
		}
		return Math.min(targetRate, 1_000_000_000.0 / averageTickNanos);
	}

	/** The highest counts first, ties by key, at most {@code limit} entries. */
	public static List<Map.Entry<String, Integer>> top(Map<String, Integer> counts, int limit) {
		return counts.entrySet().stream()
			.sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
			.limit(limit)
			.map(entry -> Map.entry(entry.getKey(), entry.getValue()))
			.toList();
	}

	/** Adds every map's counts together. */
	public static Map<String, Integer> sum(Iterable<Map<String, Integer>> maps) {
		Map<String, Integer> total = new LinkedHashMap<>();
		for (Map<String, Integer> map : maps) {
			map.forEach((key, value) -> total.merge(key, value, Integer::sum));
		}
		return total;
	}

	/**
	 * Split ids to show when comparing a run with a record: every id either one reached, in {@code order},
	 * then any unknown ids in the order the run and then the record reached them.
	 */
	public static List<String> splitRows(Collection<String> order, Map<String, Long> run, Map<String, Long> record) {
		Set<String> rows = new LinkedHashSet<>();
		for (String id : order) {
			if (run.containsKey(id) || record.containsKey(id)) {
				rows.add(id);
			}
		}
		rows.addAll(run.keySet());
		rows.addAll(record.keySet());
		return List.copyOf(rows);
	}

	/** Turns a death cause key like "mob_attack:zombie" into "Mob attack (zombie)". */
	public static String causeName(String key) {
		int colon = key.indexOf(':');
		String type = colon < 0 ? key : key.substring(0, colon);
		String readable = type.replace('_', ' ');
		readable = readable.isEmpty() ? readable : Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
		return colon < 0 ? readable : readable + " (" + key.substring(colon + 1).replace('_', ' ') + ")";
	}
}
