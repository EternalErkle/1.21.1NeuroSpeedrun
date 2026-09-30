package com.eternalerkle.speedrun.vitals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageAnnouncementTest {
	@Test
	void formatsHealthAsHearts() {
		assertEquals("1.5", SharedVitals.formatHearts(3.0F));
		assertEquals("2", SharedVitals.formatHearts(4.0F));
		assertEquals("0.5", SharedVitals.formatHearts(1.0F));
		assertEquals("0.2", SharedVitals.formatHearts(0.4F));
	}

	@Test
	void spellsOutDamageTypes() {
		assertEquals("in fire", SharedVitals.spellOut("inFire"));
		assertEquals("fall", SharedVitals.spellOut("fall"));
		assertEquals("hot floor", SharedVitals.spellOut("hotFloor"));
		assertEquals("fell out of world", SharedVitals.spellOut("outOfWorld".replace("outOf", "fellOutOf")));
	}
}
