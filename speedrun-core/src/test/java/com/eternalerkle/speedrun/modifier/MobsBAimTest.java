package com.eternalerkle.speedrun.modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobsBAimTest {
	@Test
	void aimedArrowPassesThroughTheTarget() {
		double[][] targets = {{5, 0}, {15, 0}, {30, 0}, {30, 8}, {25, -10}, {30, 3}};
		for (double[] target : targets) {
			double slope = MobsBModifiers.aimSlope(target[0], target[1], 1.6);
			double height = MobsBModifiers.heightAt(Math.atan(slope), target[0], 1.6);
			assertEquals(target[1], height, 0.05, "target at " + target[0] + ", " + target[1]);
		}
	}
}
