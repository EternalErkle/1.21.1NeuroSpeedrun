package com.eternalerkle.speedrun.modifier;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModifierPickerTest {
	@Test
	void catalogHasEveryModifierOnce() {
		// Counts change as modifiers are added; what matters is that no id appears twice.
		assertEquals(ModifierCatalog.ALL.size(), new HashSet<>(ModifierCatalog.ids()).size());
		assertEquals("One Heart", ModifierCatalog.displayName("one_heart"));
		assertEquals("unknown_id", ModifierCatalog.displayName("unknown_id"));
	}

	@Test
	void picksDistinctIdsFromThePoolOnly() {
		Set<String> pool = Set.of("uhc", "horde", "swap", "shuffle");
		Random random = new Random(1);
		for (int i = 0; i < 200; i++) {
			List<String> picked = ModifierPicker.pick(pool, 3, random);
			assertEquals(3, picked.size());
			assertEquals(3, new HashSet<>(picked).size());
			assertTrue(pool.containsAll(picked));
		}
	}

	@Test
	void neverPicksBothHalvesOfAConflict() {
		Set<String> pool = Set.of("one_heart", "half_health", "tiny", "giant", "moon_gravity", "heavy_gravity");
		Random random = new Random(2);
		for (int i = 0; i < 500; i++) {
			List<String> picked = ModifierPicker.pick(pool, 6, random);
			assertEquals(3, picked.size());
			for (List<String> pair : ModifierCatalog.CONFLICTS) {
				assertFalse(picked.containsAll(pair), () -> "picked conflicting pair " + pair);
			}
		}
	}

	@Test
	void smallOrEmptyPoolGivesFewer() {
		Random random = new Random(3);
		assertEquals(List.of(), ModifierPicker.pick(Set.of(), 2, random));
		assertEquals(List.of("uhc"), ModifierPicker.pick(Set.of("uhc"), 2, random));
		assertEquals(List.of(), ModifierPicker.pick(Set.of("not_a_modifier"), 2, random));
	}

	@Test
	void voteOptionsDifferWhenPossible() {
		Random random = new Random(4);
		for (int i = 0; i < 100; i++) {
			List<List<String>> options = ModifierPicker.voteOptions(Set.of("uhc", "horde", "swap"), 1, Set.of(), random);
			assertEquals(2, options.size());
			assertNotEquals(options.get(0), options.get(1));
		}
	}

	@Test
	void parseAcceptsKnownIds() {
		ModifierPicker.Parsed parsed = ModifierPicker.parse("  Swap uhc ");
		assertTrue(parsed.ok());
		assertEquals(List.of("uhc", "swap"), parsed.ids());
	}

	@Test
	void parseRejectsBadInput() {
		assertFalse(ModifierPicker.parse("uhc bogus").ok());
		assertFalse(ModifierPicker.parse("uhc uhc").ok());
		assertFalse(ModifierPicker.parse("tiny giant").ok());
		assertFalse(ModifierPicker.parse("   ").ok());
	}
}
