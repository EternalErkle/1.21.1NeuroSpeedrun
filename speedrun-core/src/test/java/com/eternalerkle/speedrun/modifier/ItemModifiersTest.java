package com.eternalerkle.speedrun.modifier;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModifiersTest {
	@Test
	void lootChaosIsFixedPerSeedAndChangesWithIt() {
		int size = 1300;
		assertEquals(ItemModifiers.chaosIndex(42L, "block/minecraft:stone", size), ItemModifiers.chaosIndex(42L, "block/minecraft:stone", size));
		int changed = 0;
		Set<Integer> spread = new HashSet<>();
		for (long seed = 0; seed < 200; seed++) {
			int a = ItemModifiers.chaosIndex(seed, "block/minecraft:stone", size);
			if (a != ItemModifiers.chaosIndex(seed + 1, "block/minecraft:stone", size)) {
				changed++;
			}
			spread.add(a);
			assertTrue(a >= 0 && a < size);
		}
		assertTrue(changed > 190);
		assertTrue(spread.size() > 150);
		assertTrue(ItemModifiers.chaosIndex(7L, "block/minecraft:stone", size) != ItemModifiers.chaosIndex(7L, "entity/minecraft:zombie", size)
			|| ItemModifiers.chaosIndex(8L, "block/minecraft:stone", size) != ItemModifiers.chaosIndex(8L, "entity/minecraft:zombie", size));
	}

	@Test
	void jackpotOddsMatchTheSpec() {
		Random random = new Random(1);
		int trials = 2_000_000;
		int ones = 0;
		int hundreds = 0;
		for (int i = 0; i < trials; i++) {
			int n = ItemModifiers.jackpotMultiplier(random.nextDouble(), random.nextDouble());
			assertTrue(n >= 1 && n <= 640);
			if (n == 1) {
				ones++;
			}
			if (n >= 100) {
				hundreds++;
			}
		}
		assertEquals(0.6, ones / (double) trials, 0.01);
		assertEquals(1.0 / 160, hundreds / (double) trials, 0.0005);
		assertEquals(640, ItemModifiers.jackpotMultiplier(0.99, 0.999999999));
	}
}
