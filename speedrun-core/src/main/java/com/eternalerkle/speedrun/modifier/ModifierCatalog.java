package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.modifier.ModifierInfo.Tag;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Every modifier id, its display name and which pairs can never be active together. Free of Minecraft classes. */
public final class ModifierCatalog {
	public static final String ONE_HEART = "one_heart";
	public static final String HALF_HEALTH = "half_health";
	public static final String UHC = "uhc";
	public static final String GLASS_CANNON = "glass_cannon";
	public static final String HEAVY_LANDING = "heavy_landing";
	public static final String SNACK_SIZE = "snack_size";
	public static final String TINY = "tiny";
	public static final String GIANT = "giant";
	public static final String MOON_GRAVITY = "moon_gravity";
	public static final String HEAVY_GRAVITY = "heavy_gravity";
	public static final String CHARGED_UP = "charged_up";
	public static final String HORDE = "horde";
	public static final String HASTY_MOBS = "hasty_mobs";
	public static final String MOB_RANDOMIZER = "mob_randomizer";
	public static final String BLAZE_BOOST = "blaze_boost";
	public static final String STARTER_KIT = "starter_kit";
	public static final String NO_CRAFTING_TABLE = "no_crafting_table";
	public static final String HOTBAR_ONLY = "hotbar_only";
	public static final String SHARED_INVENTORY = "shared_inventory";
	public static final String SHUFFLE = "shuffle";
	public static final String ETERNAL_NIGHT = "eternal_night";
	public static final String SWAP = "swap";
	// ---- mobs-b ----
	public static final String CREEPER_RAIN = "creeper_rain";
	public static final String SNIPER_SKELETONS = "sniper_skeletons";
	public static final String STALKER = "stalker";
	public static final String ANGRY_NEUTRALS = "angry_neutrals";
	public static final String JUICED_MOBS = "juiced_mobs";
	public static final String ARMORED_HORDE = "armored_horde";
	public static final String SPLITTERS = "splitters";
	public static final String ELITES = "elites";
	public static final String SWARM = "swarm";
	public static final String RELENTLESS = "relentless";
	public static final String VEX_CURSE = "vex_curse";
	public static final String WARDEN_ALARM = "warden_alarm";
	public static final String GHAST_AIR_FORCE = "ghast_air_force";
	public static final String ENDLESS_RAIDS = "endless_raids";
	public static final String MARKED = "marked";
	public static final String THIEVES = "thieves";

	public static final List<ModifierInfo> ALL = List.of(
		new ModifierInfo(ONE_HEART, "One Heart", "Max health is 1 heart", Tag.BRUTAL),
		new ModifierInfo(HALF_HEALTH, "Half Health", "Max health is 5 hearts", Tag.HARDER),
		new ModifierInfo(UHC, "UHC", "No natural regeneration", Tag.HARDER),
		new ModifierInfo(GLASS_CANNON, "Glass Cannon", "Players deal 2x damage and take 2x damage", Tag.CHAOS),
		new ModifierInfo(HEAVY_LANDING, "Heavy Landing", "Fall damage is tripled", Tag.HARDER),
		new ModifierInfo(SNACK_SIZE, "Snack Size", "Food restores half as much hunger and saturation", Tag.HARDER),
		new ModifierInfo(TINY, "Tiny", "Players are half size", Tag.CHAOS),
		new ModifierInfo(GIANT, "Giant", "Players are double size, with longer reach", Tag.CHAOS),
		new ModifierInfo(MOON_GRAVITY, "Moon Gravity", "Gravity is 30% of normal", Tag.CHAOS),
		new ModifierInfo(HEAVY_GRAVITY, "Heavy Gravity", "Gravity is doubled, so jumps are lower", Tag.HARDER),
		new ModifierInfo(CHARGED_UP, "Charged Up", "Every creeper spawns charged", Tag.BRUTAL),
		new ModifierInfo(HORDE, "Horde", "The hostile mob spawn cap is doubled", Tag.HARDER),
		new ModifierInfo(HASTY_MOBS, "Hasty Mobs", "Hostile mobs have permanent Speed", Tag.HARDER),
		new ModifierInfo(MOB_RANDOMIZER, "Mob Randomizer", "Each mob type drops one random item, fixed for the whole run", Tag.CHAOS),
		new ModifierInfo(BLAZE_BOOST, "Blaze Boost", "Blazes drop twice as many rods", Tag.HELPFUL),
		new ModifierInfo(STARTER_KIT, "Starter Kit", "Everyone starts with stone tools, a bucket and bread", Tag.HELPFUL),
		new ModifierInfo(NO_CRAFTING_TABLE, "No Crafting Table", "Only the 2x2 inventory crafting grid works", Tag.BRUTAL),
		new ModifierInfo(HOTBAR_ONLY, "Hotbar Only", "Main inventory slots are locked", Tag.HARDER),
		new ModifierInfo(SHARED_INVENTORY, "Shared Inventory", "All players share one inventory", Tag.CHAOS),
		new ModifierInfo(SHUFFLE, "Shuffle", "Every inventory is shuffled every 5 minutes", Tag.CHAOS),
		new ModifierInfo(ETERNAL_NIGHT, "Eternal Night", "The daylight cycle is locked at midnight", Tag.HARDER),
		new ModifierInfo(SWAP, "Swap", "All players swap positions every 5 minutes", Tag.CHAOS),
		// ---- mobs-b ----
		new ModifierInfo(CREEPER_RAIN, "Creeper Rain", "Every minute, creepers drop from the sky near each player under open sky", Tag.BRUTAL),
		new ModifierInfo(SNIPER_SKELETONS, "Sniper Skeletons", "Skeletons shoot from twice as far, with perfect aim", Tag.HARDER),
		new ModifierInfo(STALKER, "Stalker", "Every 2 minutes, a fast zombie spawns behind a random player and hunts only them", Tag.HARDER),
		new ModifierInfo(ANGRY_NEUTRALS, "Angry Neutrals", "Wolves, bees, endermen, piglins, iron golems and polar bears always attack", Tag.BRUTAL),
		new ModifierInfo(JUICED_MOBS, "Juiced Mobs", "Hostile mobs have permanent Strength and Speed", Tag.HARDER),
		new ModifierInfo(ARMORED_HORDE, "Armored Horde", "Zombies and skeletons spawn in full iron or diamond armor", Tag.HARDER),
		new ModifierInfo(SPLITTERS, "Splitters", "A killed hostile mob splits into two smaller, weaker copies", Tag.BRUTAL),
		new ModifierInfo(ELITES, "Elites", "One in ten hostile mobs is a named elite with triple health", Tag.HARDER),
		new ModifierInfo(SWARM, "Swarm", "Hostile mobs spawn in groups three times larger", Tag.HARDER),
		new ModifierInfo(RELENTLESS, "Relentless", "Hostile mobs notice you from 64 blocks and never lose track of you", Tag.HARDER),
		new ModifierInfo(VEX_CURSE, "Vex Curse", "A hit from a mob can summon a vex", Tag.BRUTAL),
		new ModifierInfo(WARDEN_ALARM, "Warden Alarm", "Once per run, at a random time, a warden emerges near a player", Tag.BRUTAL),
		new ModifierInfo(GHAST_AIR_FORCE, "Ghast Air Force", "Ghasts patrol the overworld sky", Tag.BRUTAL),
		new ModifierInfo(ENDLESS_RAIDS, "Endless Raids", "Everyone has Bad Omen for the whole run", Tag.BRUTAL),
		new ModifierInfo(MARKED, "Marked", "Every 5 minutes, one player is marked and all mobs hunt them for 60 seconds", Tag.BRUTAL),
		new ModifierInfo(THIEVES, "Thieves", "Zombies and endermen steal a random item when they hit you", Tag.CHAOS)
	);

	/** Pairs that contradict each other. The picker never draws both halves of a pair. */
	public static final List<List<String>> CONFLICTS = List.of(
		List.of(ONE_HEART, HALF_HEALTH),
		List.of(TINY, GIANT),
		List.of(MOON_GRAVITY, HEAVY_GRAVITY)
	);

	private static final Map<String, ModifierInfo> BY_ID = new LinkedHashMap<>();

	static {
		for (ModifierInfo info : ALL) {
			BY_ID.put(info.id(), info);
		}
	}

	private ModifierCatalog() {
	}

	public static boolean isKnown(String id) {
		return BY_ID.containsKey(id);
	}

	/** The catalog entry, or null for an unknown id. */
	public static ModifierInfo get(String id) {
		return BY_ID.get(id);
	}

	public static String displayName(String id) {
		ModifierInfo info = BY_ID.get(id);
		return info == null ? id : info.name();
	}

	public static Collection<String> ids() {
		return BY_ID.keySet();
	}

	public static boolean conflicts(String a, String b) {
		for (List<String> pair : CONFLICTS) {
			if (pair.contains(a) && pair.contains(b) && !a.equals(b)) {
				return true;
			}
		}
		return false;
	}

	/** Whether the id conflicts with any id in the collection. */
	public static boolean conflictsWithAny(String id, Collection<String> chosen) {
		for (String other : chosen) {
			if (conflicts(id, other)) {
				return true;
			}
		}
		return false;
	}
}
