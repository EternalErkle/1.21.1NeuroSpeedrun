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
	// ---- items ----
	public static final String NO_SHIELDS = "no_shields";
	public static final String NO_ARMOR = "no_armor";
	public static final String NO_DIAMOND_GEAR = "no_diamond_gear";
	public static final String STONE_AGE = "stone_age";
	public static final String BUCKETLESS = "bucketless";
	public static final String NO_BOATS = "no_boats";
	public static final String INSOMNIA = "insomnia";
	public static final String UNBREAKABLE = "unbreakable";
	public static final String FRAGILE_TOOLS = "fragile_tools";
	public static final String INSTANT_SMELTING = "instant_smelting";
	public static final String ORE_RUSH = "ore_rush";
	public static final String VEIN_MINER = "vein_miner";
	public static final String TIMBER = "timber";
	public static final String ORCHARD = "orchard";
	public static final String TREASURE_HUNTER = "treasure_hunter";
	public static final String LOOTED = "looted";
	public static final String PEARL_START = "pearl_start";
	public static final String WINGS = "wings";
	public static final String FRAGILE_EYES = "fragile_eyes";
	public static final String STURDY_EYES = "sturdy_eyes";
	public static final String BOW_ONLY = "bow_only";
	public static final String MELEE_ONLY = "melee_only";
	public static final String BUTTERFINGERS = "butterfingers";
	public static final String CURSED_ARMOR = "cursed_armor";
	public static final String ITEM_MAGNET = "item_magnet";
	public static final String GOLD_RUSH = "gold_rush";
	public static final String VILLAGER_STRIKE = "villager_strike";
	public static final String TETHERED = "tethered";
	public static final String BUDDY_SYSTEM = "buddy_system";
	public static final String HOT_POTATO = "hot_potato";
	public static final String JUGGERNAUT = "juggernaut";
	public static final String INVENTORY_ROTATION = "inventory_rotation";
	public static final String NO_NAMETAGS = "no_nametags";
	public static final String RADIO_SILENCE = "radio_silence";
	public static final String LOOT_CHAOS = "loot_chaos";
	public static final String JACKPOT = "jackpot";
	// ---- end items ----

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
		// ---- items ----
		new ModifierInfo(NO_SHIELDS, "No Shields", "Shields cannot be crafted or kept", Tag.HARDER),
		new ModifierInfo(NO_ARMOR, "No Armor", "Armor cannot be worn", Tag.BRUTAL),
		new ModifierInfo(NO_DIAMOND_GEAR, "No Diamond Gear", "Diamond tools, weapons and armor cannot be crafted or kept", Tag.HARDER),
		new ModifierInfo(STONE_AGE, "Stone Age", "Iron and chainmail tools, weapons and armor cannot be crafted or kept", Tag.BRUTAL),
		new ModifierInfo(BUCKETLESS, "Bucketless", "Buckets cannot be crafted", Tag.HARDER),
		new ModifierInfo(NO_BOATS, "No Boats", "Boats cannot be crafted or kept", Tag.HARDER),
		new ModifierInfo(INSOMNIA, "Insomnia", "Nobody can sleep", Tag.HARDER),
		new ModifierInfo(UNBREAKABLE, "Unbreakable", "Tools and armor never lose durability", Tag.HELPFUL),
		new ModifierInfo(FRAGILE_TOOLS, "Fragile Tools", "Tools and armor wear out 4x faster", Tag.HARDER),
		new ModifierInfo(INSTANT_SMELTING, "Instant Smelting", "Furnaces, smokers and blast furnaces finish instantly", Tag.HELPFUL),
		new ModifierInfo(ORE_RUSH, "Ore Rush", "Ores drop double", Tag.HELPFUL),
		new ModifierInfo(VEIN_MINER, "Vein Miner", "Mining one ore mines the whole vein", Tag.HELPFUL),
		new ModifierInfo(TIMBER, "Timber", "Chopping one log fells the whole tree", Tag.HELPFUL),
		new ModifierInfo(ORCHARD, "Orchard", "Leaves often drop apples", Tag.HELPFUL),
		new ModifierInfo(TREASURE_HUNTER, "Treasure Hunter", "Structure chests hold double loot", Tag.HELPFUL),
		new ModifierInfo(LOOTED, "Looted", "Structure chests are empty", Tag.BRUTAL),
		new ModifierInfo(PEARL_START, "Pearl Start", "Everyone starts with 4 ender pearls", Tag.HELPFUL),
		new ModifierInfo(WINGS, "Wings", "Everyone starts with an elytra and 16 rockets", Tag.HELPFUL),
		new ModifierInfo(FRAGILE_EYES, "Fragile Eyes", "Eyes of ender always break", Tag.HARDER),
		new ModifierInfo(STURDY_EYES, "Sturdy Eyes", "Eyes of ender never break", Tag.HELPFUL),
		new ModifierInfo(BOW_ONLY, "Bow Only", "Melee attacks do no damage", Tag.BRUTAL),
		new ModifierInfo(MELEE_ONLY, "Melee Only", "Bows and crossbows do not work", Tag.HARDER),
		new ModifierInfo(BUTTERFINGERS, "Butterfingers", "Taking damage drops the held item", Tag.CHAOS),
		new ModifierInfo(CURSED_ARMOR, "Cursed Armor", "All worn armor gets Curse of Binding", Tag.CHAOS),
		new ModifierInfo(ITEM_MAGNET, "Item Magnet", "Items within 8 blocks fly to you", Tag.HELPFUL),
		new ModifierInfo(GOLD_RUSH, "Gold Rush", "Piglin bartering gives double", Tag.HELPFUL),
		new ModifierInfo(VILLAGER_STRIKE, "Villager Strike", "Villagers refuse to trade", Tag.HARDER),
		new ModifierInfo(TETHERED, "Tethered", "Players cannot get more than 40 blocks apart", Tag.CHAOS),
		new ModifierInfo(BUDDY_SYSTEM, "Buddy System", "Near a teammate you regenerate, alone you are weak", Tag.CHAOS),
		new ModifierInfo(HOT_POTATO, "Hot Potato", "The potato holder takes damage until they hit a teammate", Tag.CHAOS),
		new ModifierInfo(JUGGERNAUT, "Juggernaut", "One random player gets double health and damage", Tag.CHAOS),
		new ModifierInfo(INVENTORY_ROTATION, "Inventory Rotation", "Every 5 minutes inventories pass to the next player", Tag.CHAOS),
		new ModifierInfo(NO_NAMETAGS, "No Nametags", "Player names are hidden", Tag.HARDER),
		new ModifierInfo(RADIO_SILENCE, "Radio Silence", "Chat is disabled for the run", Tag.CHAOS),
		new ModifierInfo(LOOT_CHAOS, "Loot Chaos", "Every block and mob drops a random item, fixed per seed", Tag.CHAOS),
		new ModifierInfo(JACKPOT, "Jackpot", "Broken blocks drop up to 640 times their drop", Tag.CHAOS),
		// ---- end items ----
		new ModifierInfo(SWAP, "Swap", "All players swap positions every 5 minutes", Tag.CHAOS)
	);

	/** Pairs that contradict each other. The picker never draws both halves of a pair. */
	public static final List<List<String>> CONFLICTS = List.of(
		List.of(ONE_HEART, HALF_HEALTH),
		List.of(TINY, GIANT),
		// ---- items ----
		List.of(UNBREAKABLE, FRAGILE_TOOLS),
		List.of(TREASURE_HUNTER, LOOTED),
		List.of(FRAGILE_EYES, STURDY_EYES),
		List.of(BOW_ONLY, MELEE_ONLY),
		List.of(NO_ARMOR, CURSED_ARMOR),
		List.of(BUCKETLESS, STARTER_KIT),
		List.of(INVENTORY_ROTATION, SHARED_INVENTORY),
		List.of(LOOT_CHAOS, MOB_RANDOMIZER),
		// ---- end items ----
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
