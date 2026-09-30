package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.config.Goal;

import java.util.Collection;
import java.util.TreeSet;

/** Builds record category keys. Runs only compete with runs that share every setting in the key. */
public final class Category {
	private Category() {
	}

	public static String key(Goal goal, float tickRate, boolean sharedHealth, boolean sharedHunger, Collection<String> modifiers) {
		StringBuilder key = new StringBuilder(goal.id).append('/').append(formatRate(tickRate)).append("tps");
		if (sharedHealth) {
			key.append("/sharedhealth");
		}
		if (sharedHunger) {
			key.append("/sharedhunger");
		}
		for (String modifier : new TreeSet<>(modifiers)) {
			key.append('/').append(modifier);
		}
		return key.toString();
	}

	public static String formatRate(float tickRate) {
		return tickRate == Math.rint(tickRate) ? Integer.toString((int) tickRate) : Float.toString(tickRate);
	}
}
