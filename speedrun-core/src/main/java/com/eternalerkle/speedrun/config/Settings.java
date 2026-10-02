package com.eternalerkle.speedrun.config;

import com.eternalerkle.speedrun.util.JsonStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persistent server settings. Changes apply when the next run starts. */
public final class Settings {
	public enum ModifierMode { OFF, RANDOM, VOTE }

	/** A player allowed to use /speedrun admin commands without being op. */
	public static final class TrustedPlayer {
		public String uuid;
		public String name;

		public TrustedPlayer(UUID uuid, String name) {
			this.uuid = uuid.toString();
			this.name = name;
		}
	}

	public Goal goal = Goal.DRAGON;
	public float tickRate = 20.0F;
	public boolean sharedHealth = false;
	public boolean sharedHunger = false;
	public ModifierMode modifierMode = ModifierMode.OFF;
	public int modifierCount = 1;
	/** Modifier ids admins have enabled. Empty means none are drawn. */
	public Set<String> modifierPool = new LinkedHashSet<>();
	/** Modifier ids applied to every run, on top of the ones the mode draws. */
	public Set<String> alwaysModifiers = new LinkedHashSet<>();
	/** When on, any death ends the run and a new seed starts. Off: the dead player respawns and the run goes on. */
	public boolean resetOnDeath = true;
	public double deathRoomMinSeconds = 5.0;
	/** When on, a run survives an empty server: it pauses until someone rejoins instead of being abandoned. */
	public boolean keepRunWhenEmpty = false;
	/** When on, every run world gets a village placed 100 to 500 blocks from spawn, unless there is no dry land in range. */
	public boolean guaranteedVillage = true;
	/** When on, every player gets a player tracker compass at the start of each run. */
	public boolean trackerCompass = true;
	/** Players who may use /speedrun admin commands without op. They get no vanilla op powers. */
	public List<TrustedPlayer> trusted = new ArrayList<>();
	/** When on, every player who joins is added to {@link #trusted}. */
	public boolean autoTrust = true;

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
		// The server cannot keep up above 5x; older settings files may hold more.
		settings.tickRate = Math.max(1.0F, Math.min(100.0F, settings.tickRate));
		if (settings.alwaysModifiers == null) {
			settings.alwaysModifiers = new LinkedHashSet<>();
		}
		if (settings.modifierPool == null) {
			settings.modifierPool = new LinkedHashSet<>();
		}
		if (settings.trusted == null) {
			settings.trusted = new ArrayList<>();
		}
		settings.trusted.removeIf(player -> player == null || player.uuid == null);
		return settings;
	}

	public boolean isTrusted(UUID uuid) {
		String id = uuid.toString();
		return trusted.stream().anyMatch(player -> player.uuid.equalsIgnoreCase(id));
	}

	public void save() {
		JsonStore.save(file, this);
	}
}
