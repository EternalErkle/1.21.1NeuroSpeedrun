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
	// ---- world ----
	public static final String ETERNAL_DAY = "eternal_day";
	public static final String FAST_DAYS = "fast_days";
	public static final String TIGHT_BORDER = "tight_border";
	public static final String SHRINKING_BORDER = "shrinking_border";
	public static final String SHORT_SIGHT = "short_sight";
	public static final String TIME_BOMB = "time_bomb";
	public static final String INSTANT_PORTALS = "instant_portals";
	public static final String EXPLOSIVE_BEDS = "explosive_beds";
	public static final String NO_BED_BOMBS = "no_bed_bombs";
	public static final String PIG_BOMB = "pig_bomb";
	public static final String SILVERFISH_STONE = "silverfish_stone";
	public static final String EXPLOSIVE_ORES = "explosive_ores";
	public static final String GRAVITY_BLOCKS = "gravity_blocks";
	public static final String THUNDERSTRUCK = "thunderstruck";
	public static final String METEOR_SHOWER = "meteor_shower";
	public static final String ANVIL_RAIN = "anvil_rain";
	public static final String FLOOR_IS_LAVA = "floor_is_lava";
	public static final String BLACKOUTS = "blackouts";
	public static final String SPAWNER_SURPRISE = "spawner_surprise";
	public static final String CAVE_INS = "cave_ins";
	/** Implemented in later batches, not in ALL yet. */
	public static final String TRAPPED_CHESTS = "trapped_chests";
	public static final String CURSED_LOOT = "cursed_loot";
	public static final String LARGE_BIOMES = "large_biomes";
	public static final String AMPLIFIED = "amplified";
	public static final String NO_VILLAGES = "no_villages";
	public static final String CLOSE_STRONGHOLD = "close_stronghold";
	// ---- end world ----

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
		// ---- world ----
		new ModifierInfo(ETERNAL_DAY, "Eternal Day", "The daylight cycle is locked at noon", Tag.HELPFUL),
		new ModifierInfo(FAST_DAYS, "Fast Days", "The day cycle runs 4x faster", Tag.CHAOS),
		new ModifierInfo(TIGHT_BORDER, "Tight Border", "The world border sits 500 blocks from spawn", Tag.HARDER),
		new ModifierInfo(SHRINKING_BORDER, "Shrinking Border", "The world border starts 2000 blocks from spawn and closes to 50 over 60 minutes", Tag.HARDER),
		new ModifierInfo(SHORT_SIGHT, "Short Sight", "View distance is 4 chunks", Tag.HARDER),
		new ModifierInfo(TIME_BOMB, "Time Bomb", "The run fails after 60 minutes", Tag.HARDER),
		new ModifierInfo(INSTANT_PORTALS, "Instant Portals", "Nether portals teleport instantly", Tag.HELPFUL),
		new ModifierInfo(EXPLOSIVE_BEDS, "Explosive Beds", "Beds explode in the Overworld too", Tag.HARDER),
		new ModifierInfo(NO_BED_BOMBS, "No Bed Bombs", "Beds do not explode in the End", Tag.HARDER),
		new ModifierInfo(PIG_BOMB, "Pig Bomb", "Every 30 seconds to 3 minutes, a pig carrying lit TNT chases a random player", Tag.BRUTAL),
		new ModifierInfo(SILVERFISH_STONE, "Silverfish Stone", "Mining stone has a 2% chance to release a silverfish", Tag.HARDER),
		new ModifierInfo(EXPLOSIVE_ORES, "Explosive Ores", "Mining an ore has a 5% chance to leave lit TNT behind", Tag.CHAOS),
		new ModifierInfo(GRAVITY_BLOCKS, "Gravity Blocks", "Every placed block falls like sand", Tag.CHAOS),
		new ModifierInfo(THUNDERSTRUCK, "Thunderstruck", "Lightning strikes near a random player every 1 to 4 minutes", Tag.BRUTAL),
		new ModifierInfo(METEOR_SHOWER, "Meteor Shower", "Every 3 to 6 minutes, fireballs rain down around players", Tag.BRUTAL),
		new ModifierInfo(ANVIL_RAIN, "Anvil Rain", "Every 2 to 5 minutes, an anvil drops on a random player", Tag.BRUTAL),
		new ModifierInfo(FLOOR_IS_LAVA, "Floor Is Lava", "Every 10 minutes, standing on the ground hurts for 20 seconds", Tag.BRUTAL),
		new ModifierInfo(BLACKOUTS, "Blackouts", "Every 2 to 4 minutes, everyone is blinded for 5 seconds", Tag.HARDER),
		new ModifierInfo(SPAWNER_SURPRISE, "Spawner Surprise", "Every 10 minutes, a mob spawner appears near each player", Tag.BRUTAL),
		new ModifierInfo(CAVE_INS, "Cave-ins", "Mining underground can bring the ceiling down as gravel", Tag.CHAOS)
		// ---- end world ----
	);

	/** Pairs that contradict each other. The picker never draws both halves of a pair. */
	public static final List<List<String>> CONFLICTS = List.of(
		List.of(ONE_HEART, HALF_HEALTH),
		List.of(TINY, GIANT),
		List.of(MOON_GRAVITY, HEAVY_GRAVITY),
		// ---- world ----
		List.of(ETERNAL_DAY, ETERNAL_NIGHT),
		List.of(ETERNAL_DAY, FAST_DAYS),
		List.of(ETERNAL_NIGHT, FAST_DAYS),
		List.of(TIGHT_BORDER, SHRINKING_BORDER)
		// ---- end world ----
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
