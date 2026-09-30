package com.eternalerkle.speedrun.command;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandLogicTest {
	private static CommandDoc doc(String name, String category) {
		return new CommandDoc(name, category, "", "", List.of(), List.of(), null);
	}

	@Test
	void pageCountRoundsUpAndNeverZero() {
		assertEquals(1, CommandLogic.pageCount(0, 8));
		assertEquals(1, CommandLogic.pageCount(8, 8));
		assertEquals(2, CommandLogic.pageCount(9, 8));
		assertEquals(3, CommandLogic.pageCount(17, 8));
	}

	@Test
	void pageSlicesAndClamps() {
		List<Integer> items = IntStream.range(0, 17).boxed().toList();
		assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), CommandLogic.page(items, 1, 8));
		assertEquals(List.of(16), CommandLogic.page(items, 3, 8));
		assertEquals(List.of(16), CommandLogic.page(items, 99, 8));
		assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), CommandLogic.page(items, 0, 8));
		assertEquals(List.of(), CommandLogic.page(List.of(), 1, 8));
	}

	@Test
	void groupsKeepFirstSeenCategoryOrder() {
		List<CommandDoc> docs = List.of(doc("a", "Run"), doc("b", "Stats"), doc("c", "Run"), doc("d", "Help"));
		assertEquals(List.of("a", "c", "b", "d"), CommandLogic.groupByCategory(docs).stream().map(CommandDoc::name).toList());
	}

	@Test
	void findIgnoresCaseSlashAndSpacing() {
		List<CommandDoc> docs = List.of(doc("speedrun tickrate", "Settings"), doc("best", "Stats"));
		assertEquals("speedrun tickrate", CommandLogic.find(docs, "/Speedrun   TICKRATE ").name());
		assertEquals("best", CommandLogic.find(docs, "best").name());
		assertNull(CommandLogic.find(docs, "speedrun"));
	}

	@Test
	void parsePageAcceptsOnlyPositiveNumbers() {
		assertEquals(2, CommandLogic.parsePage(" 2 "));
		assertEquals(-1, CommandLogic.parsePage("0"));
		assertEquals(-1, CommandLogic.parsePage("best"));
		assertEquals(-1, CommandLogic.parsePage("-3"));
		assertEquals(-1, CommandLogic.parsePage("99999999999"));
	}

	@Test
	void measuredTpsCapsAtTarget() {
		assertEquals(20.0, CommandLogic.measuredTps(20.0F, 10_000_000L), 1e-9);
		assertEquals(50.0, CommandLogic.measuredTps(100.0F, 20_000_000L), 1e-9);
		assertEquals(100.0, CommandLogic.measuredTps(100.0F, 0L), 1e-9);
	}

	@Test
	void topSortsByCountThenKey() {
		Map<String, Integer> counts = new LinkedHashMap<>();
		counts.put("lava", 2);
		counts.put("fall", 5);
		counts.put("drown", 2);
		counts.put("mob", 1);
		List<Map.Entry<String, Integer>> top = CommandLogic.top(counts, 3);
		assertEquals(List.of("fall", "drown", "lava"), top.stream().map(Map.Entry::getKey).toList());
	}

	@Test
	void sumMergesCounts() {
		Map<String, Integer> total = CommandLogic.sum(List.of(Map.of("lava", 1, "fall", 2), Map.of("lava", 3)));
		assertEquals(4, total.get("lava"));
		assertEquals(2, total.get("fall"));
	}

	@Test
	void causeNameIsReadable() {
		assertEquals("Mob attack (zombie)", CommandLogic.causeName("mob_attack:zombie"));
		assertEquals("Lava", CommandLogic.causeName("lava"));
		assertEquals("Fall", CommandLogic.causeName("fall"));
	}

	@Test
	void splitRowsFollowDisplayOrderThenUnknown() {
		List<String> order = List.of("nether", "fortress", "blaze_rod", "end");
		Map<String, Long> run = new LinkedHashMap<>();
		run.put("fortress", 2L);
		run.put("nether", 1L);
		run.put("custom", 9L);
		Map<String, Long> record = new LinkedHashMap<>();
		record.put("nether", 1L);
		record.put("end", 5L);
		assertEquals(List.of("nether", "fortress", "end", "custom"), CommandLogic.splitRows(order, run, record));
		assertEquals(List.of(), CommandLogic.splitRows(order, Map.of(), Map.of()));
	}
}
