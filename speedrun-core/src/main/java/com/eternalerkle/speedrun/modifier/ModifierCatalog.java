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
	// ---- mobs-a ----
	public static final String SHARPSHOOTERS = "sharpshooters";
	public static final String SHORT_FUSE = "short_fuse";
	public static final String ENDERMAN_RAGE = "enderman_rage";
	public static final String INVISIBLE_MOBS = "invisible_mobs";
	public static final String GLOWING_MOBS = "glowing_mobs";
	public static final String SILENT_MOBS = "silent_mobs";
	public static final String BABY_ZOMBIES = "baby_zombies";
	public static final String SPIDER_JOCKEYS = "spider_jockeys";
	public static final String KAMIKAZE = "kamikaze";
	public static final String SPAWN_RANDOMIZER = "spawn_randomizer";
	public static final String NETHER_INVASION = "nether_invasion";
	public static final String PHANTOM_MENACE = "phantom_menace";
	public static final String PATROL_SEASON = "patrol_season";
	public static final String REVENGE = "revenge";
	public static final String AMBUSH = "ambush";
	public static final String PET_WOLVES = "pet_wolves";
	public static final String PIGLIN_FRIENDS = "piglin_friends";
	public static final String PEARL_BONANZA = "pearl_bonanza";
	public static final String BLAZE_SWARM = "blaze_swarm";
	public static final String ARMORED_DRAGON = "armored_dragon";
	public static final String ARROW_DRAGON = "arrow_dragon";
	// ---- end mobs-a ----

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
		// ---- mobs-a ----
		new ModifierInfo(SHARPSHOOTERS, "Sharpshooters", "Skeletons fire twice as often", Tag.HARDER),
		new ModifierInfo(SHORT_FUSE, "Short Fuse", "Creepers explode in half the time", Tag.HARDER),
		new ModifierInfo(ENDERMAN_RAGE, "Enderman Rage", "Endermen attack on sight", Tag.HARDER),
		new ModifierInfo(INVISIBLE_MOBS, "Invisible Mobs", "Hostile mobs are invisible", Tag.BRUTAL),
		new ModifierInfo(GLOWING_MOBS, "Glowing Mobs", "Hostile mobs glow through walls", Tag.HELPFUL),
		new ModifierInfo(SILENT_MOBS, "Silent Mobs", "Hostile mobs make no sound", Tag.HARDER),
		new ModifierInfo(BABY_ZOMBIES, "Baby Zombies", "Every zombie is a baby", Tag.HARDER),
		new ModifierInfo(SPIDER_JOCKEYS, "Spider Jockeys", "Every spider carries a skeleton", Tag.HARDER),
		new ModifierInfo(KAMIKAZE, "Kamikaze", "Hostile mobs explode when they die", Tag.CHAOS),
		new ModifierInfo(SPAWN_RANDOMIZER, "Spawn Randomizer", "Natural spawns become random mobs of the same kind", Tag.CHAOS),
		new ModifierInfo(NETHER_INVASION, "Nether Invasion", "Blazes, piglins and ghasts spawn in the overworld at night", Tag.CHAOS),
		new ModifierInfo(PHANTOM_MENACE, "Phantom Menace", "Phantoms come every night, even after sleeping", Tag.HARDER),
		new ModifierInfo(PATROL_SEASON, "Patrol Season", "Pillager patrols arrive every two minutes", Tag.HARDER),
		new ModifierInfo(REVENGE, "Revenge", "Killing a passive mob spawns a zombie", Tag.CHAOS),
		new ModifierInfo(AMBUSH, "Ambush", "Every 5 minutes a hostile mob spawns next to each player", Tag.CHAOS),
		new ModifierInfo(PET_WOLVES, "Pet Wolves", "Everyone starts with a tamed wolf", Tag.HELPFUL),
		new ModifierInfo(PIGLIN_FRIENDS, "Piglin Friends", "Piglins never attack players", Tag.HELPFUL),
		new ModifierInfo(PEARL_BONANZA, "Pearl Bonanza", "Endermen always drop at least 2 pearls", Tag.HELPFUL),
		new ModifierInfo(BLAZE_SWARM, "Blaze Swarm", "Blazes spawn anywhere in the Nether", Tag.HELPFUL),
		new ModifierInfo(ARMORED_DRAGON, "Armored Dragon", "The dragon has double health", Tag.HARDER),
		new ModifierInfo(ARROW_DRAGON, "Arrow Dragon", "The dragon only takes damage from arrows", Tag.HARDER)
		// ---- end mobs-a ----
	);

	/** Pairs that contradict each other. The picker never draws both halves of a pair. */
	public static final List<List<String>> CONFLICTS = List.of(
		List.of(ONE_HEART, HALF_HEALTH),
		List.of(TINY, GIANT),
		List.of(MOON_GRAVITY, HEAVY_GRAVITY),
		// ---- mobs-a ----
		List.of(INVISIBLE_MOBS, GLOWING_MOBS)
		// ---- end mobs-a ----
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
