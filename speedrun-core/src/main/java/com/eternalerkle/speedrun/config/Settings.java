package com.eternalerkle.speedrun.config;

import com.eternalerkle.speedrun.util.JsonStore;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Persistent server settings. Changes apply when the next run starts. */
public final class Settings {
	public enum ModifierMode { OFF, RANDOM, VOTE }

	public Goal goal = Goal.DRAGON;
	public float tickRate = 20.0F;
	public boolean sharedHealth = false;
	public boolean sharedHunger = false;
	public ModifierMode modifierMode = ModifierMode.OFF;
	public int modifierCount = 1;
	/** Modifier ids admins have enabled. Empty means none are drawn. */
	public Set<String> modifierPool = new LinkedHashSet<>();
	public double deathRoomMinSeconds = 5.0;

	private transient Path file;

	public static Settings load(Path file) {
		Settings settings = JsonStore.load(file, Settings.class, Settings::new);
		settings.file = file;
		if (settings.goal == null) {
			settings.goal = Goal.DRAGON;
		}
		if (settings.modifierMode == null) {
			settings.modifierMode = ModifierMode.OFF;
		}
		if (settings.modifierPool == null) {
			settings.modifierPool = new LinkedHashSet<>();
		}
		return settings;
	}

	public void save() {
		JsonStore.save(file, this);
	}
}
