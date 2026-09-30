package com.eternalerkle.speedrun.config;

public enum Goal {
	DRAGON("dragon"),
	ALLBOSSES("allbosses");

	public final String id;

	Goal(String id) {
		this.id = id;
	}

	public static Goal byId(String id) {
		for (Goal goal : values()) {
			if (goal.id.equalsIgnoreCase(id)) {
				return goal;
			}
		}
		return null;
	}
}
