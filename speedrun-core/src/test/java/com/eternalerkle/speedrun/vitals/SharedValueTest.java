package com.eternalerkle.speedrun.vitals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedValueTest {
	private static final float MAX = 20.0F;
	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();
	private final UUID c = UUID.randomUUID();
	private SharedValue value;

	@BeforeEach
	void setUp() {
		value = new SharedValue();
		value.set(20);
		value.writeFor(a, MAX);
		value.writeFor(b, MAX);
		value.writeFor(c, MAX);
	}

	@Test
	void noChangesKeepsValue() {
		value.observe(a, 20, MAX);
		value.observe(b, 20, MAX);
		assertEquals(20, value.apply(0, MAX));
	}

	@Test
	void damageFromSeveralPlayersAddsUp() {
		value.observe(a, 17, MAX);
		value.observe(b, 17, MAX);
		value.observe(c, 20, MAX);
		assertEquals(14, value.apply(0, MAX));
		assertEquals(14, value.writeFor(a, MAX));
	}

	@Test
	void damageAndHealingInOneTickNetOut() {
		value.set(10);
		value.writeFor(a, MAX);
		value.writeFor(b, MAX);
		value.observe(a, 6, MAX);
		value.observe(b, 12, MAX);
		assertEquals(8, value.apply(0, MAX));
	}

	@Test
	void healingClampsAtMax() {
		value.observe(a, 25, 30);
		assertEquals(20, value.apply(0, MAX));
	}

	@Test
	void combinedDamageClampsAtZero() {
		value.set(4);
		value.writeFor(a, MAX);
		value.writeFor(b, MAX);
		value.observe(a, 1, MAX);
		value.observe(b, 1, MAX);
		assertEquals(0, value.apply(0, MAX));
	}

	@Test
	void observeReturnsThePlayersOwnChange() {
		assertEquals(-5, value.observe(a, 15, MAX));
		assertEquals(0, value.observe(b, 20, MAX));
	}

	@Test
	void unknownPlayerContributesNothing() {
		UUID stranger = UUID.randomUUID();
		assertFalse(value.isTracked(stranger));
		assertEquals(0, value.observe(stranger, 3, MAX));
		assertEquals(20, value.apply(0, MAX));
	}

	@Test
	void forgottenPlayerStopsContributing() {
		value.forget(a);
		assertFalse(value.isTracked(a));
		value.observe(a, 2, MAX);
		assertEquals(20, value.apply(0, MAX));
	}

	@Test
	void writeCapsAtPlayersOwnMax() {
		assertEquals(10, value.writeFor(a, 10));
		// The player sits at their own cap, so nothing changed.
		assertEquals(0, value.observe(a, 10, 10));
		assertEquals(20, value.apply(0, 40));
	}

	@Test
	void droppedMaxIsNotCountedAsDamage() {
		// A max-health modifier lowered everyone's cap from 20 to 10; vanilla clamped their health.
		value.observe(a, 10, 10);
		value.observe(b, 10, 10);
		value.observe(c, 10, 10);
		assertEquals(10, value.apply(0, 10));
	}

	@Test
	void damageBelowLoweredMaxStillCounts() {
		value.observe(a, 7, 10);
		assertEquals(17, value.apply(0, MAX));
	}

	@Test
	void pendingChangesResetAfterApply() {
		value.observe(a, 18, MAX);
		assertEquals(18, value.apply(0, MAX));
		value.writeFor(a, MAX);
		assertEquals(18, value.apply(0, MAX));
	}

	@Test
	void clearResetsEverything() {
		value.clear();
		assertFalse(value.isInitialized());
		assertFalse(value.isTracked(a));
		value.set(5);
		assertTrue(value.isInitialized());
		assertEquals(5, value.value());
	}

	@Test
	void clampBounds() {
		assertEquals(0, SharedValue.clamp(-3, 0, 20));
		assertEquals(20, SharedValue.clamp(30, 0, 20));
		assertEquals(7.5F, SharedValue.clamp(7.5F, 0, 20));
	}
}
