package com.eternalerkle.speedrun.modifier;

/** Damage multipliers for modifiers. Free of Minecraft classes. */
public final class DamageScaling {
	private DamageScaling() {
	}

	/**
	 * Multiplies a damage amount, leaving huge amounts alone. Vanilla kills with Float.MAX_VALUE (/kill, the void);
	 * doubling that gives Infinity, and vanilla's absorption math turns Infinity into NaN health and absorption, which
	 * leaves a player unkillable with grey hearts. Damage at or above {@code limit} is already lethal, so it passes
	 * through unscaled, and the result never exceeds {@code limit}.
	 */
	public static float scale(float amount, float factor, float limit) {
		if (!(amount < limit)) {
			return amount;
		}
		return Math.min(amount * factor, limit);
	}
}
