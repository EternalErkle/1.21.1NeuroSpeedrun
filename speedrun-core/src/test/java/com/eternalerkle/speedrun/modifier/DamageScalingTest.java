package com.eternalerkle.speedrun.modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageScalingTest {
	private static final float LIMIT = 3.4028235E37F;

	@Test
	void scalesNormalDamage() {
		assertEquals(6.0F, DamageScaling.scale(3.0F, 2.0F, LIMIT));
	}

	@Test
	void killDamageStaysFinite() {
		float scaled = DamageScaling.scale(Float.MAX_VALUE, 2.0F, LIMIT);
		assertTrue(Float.isFinite(scaled));
		assertTrue(Float.isFinite(DamageScaling.scale(DamageScaling.scale(Float.MAX_VALUE, 2.0F, LIMIT), 2.0F, LIMIT)));
		assertEquals(LIMIT, DamageScaling.scale(LIMIT / 1.5F, 2.0F, LIMIT));
	}
}
