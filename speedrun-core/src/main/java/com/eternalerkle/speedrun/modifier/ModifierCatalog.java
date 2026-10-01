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

	// ---- body ----
	public static final String EXTRA_HEARTS = "extra_hearts";
	public static final String VAMPIRE = "vampire";
	public static final String SUDDEN_DEATH = "sudden_death";
	public static final String BLEEDING = "bleeding";
	public static final String FIRE_WEAKNESS = "fire_weakness";
	public static final String FIREPROOF = "fireproof";
	public static final String FEATHERWEIGHT = "featherweight";
	public static final String BLAST_PROOF = "blast_proof";
	public static final String BIG_BOOMS = "big_booms";
	public static final String KNOCKBACK_CHAOS = "knockback_chaos";
	public static final String SECOND_WIND = "second_wind";
	public static final String LAST_ONE_STANDING = "last_one_standing";
	public static final String TOTEM_START = "totem_start";
	public static final String CRITICAL_ONLY = "critical_only";
	public static final String NIGHT_VISION = "night_vision";
	public static final String HASTE = "haste";
	public static final String WEAK_HANDS = "weak_hands";
	public static final String GILLS = "gills";
	public static final String SHORT_BREATH = "short_breath";
	public static final String FROST_WALKER = "frost_walker";
	public static final String LONG_ARMS = "long_arms";
	public static final String SHORT_ARMS = "short_arms";
	public static final String NO_JUMPING = "no_jumping";
	public static final String BOUNCY = "bouncy";
	public static final String PUMPKIN_HEAD = "pumpkin_head";
	public static final String DARKNESS_PULSE = "darkness_pulse";
	public static final String UNDEAD = "undead";
	public static final String VERTIGO = "vertigo";
	public static final String HYDROPHOBIC = "hydrophobic";
	public static final String VENOMOUS = "venomous";
	public static final String SHELLSHOCK = "shellshock";
	public static final String THORNED_MOBS = "thorned_mobs";
	public static final String EXPLOSIVE_ARROWS = "explosive_arrows";
	public static final String HELLFIRE_MOBS = "hellfire_mobs";
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
		// ---- body ----
		new ModifierInfo(EXTRA_HEARTS, "Extra Hearts", "Max health is 15 hearts", Tag.HELPFUL),
		new ModifierInfo(VAMPIRE, "Vampire", "No natural regeneration, but every kill heals 2 hearts", Tag.CHAOS),
		new ModifierInfo(SUDDEN_DEATH, "Sudden Death", "After 30 minutes, max health drops 1 heart every 5 minutes", Tag.HARDER),
		new ModifierInfo(BLEEDING, "Bleeding", "Any hit over 3 hearts also gives 3 seconds of Wither", Tag.HARDER),
		new ModifierInfo(FIRE_WEAKNESS, "Fire Weakness", "Fire and lava deal double damage", Tag.HARDER),
		new ModifierInfo(FIREPROOF, "Fireproof", "Permanent Fire Resistance", Tag.HELPFUL),
		new ModifierInfo(FEATHERWEIGHT, "Featherweight", "No fall damage", Tag.HELPFUL),
		new ModifierInfo(BLAST_PROOF, "Blast Proof", "Explosions deal no damage to players", Tag.HELPFUL),
		new ModifierInfo(BIG_BOOMS, "Big Booms", "Every explosion is twice as strong", Tag.HARDER),
		new ModifierInfo(KNOCKBACK_CHAOS, "Knockback Chaos", "Players take triple knockback", Tag.CHAOS),
		new ModifierInfo(SECOND_WIND, "Second Wind", "The first death of the run is forgiven and that player respawns at spawn", Tag.HELPFUL),
		new ModifierInfo(LAST_ONE_STANDING, "Last One Standing", "Dead players become spectators; the run fails only when everyone is dead", Tag.HELPFUL),
		new ModifierInfo(TOTEM_START, "Totem Start", "Everyone starts with a Totem of Undying", Tag.HELPFUL),
		new ModifierInfo(CRITICAL_ONLY, "Critical Only", "Only critical hits and critical arrows deal damage", Tag.BRUTAL),
		new ModifierInfo(NIGHT_VISION, "Night Vision", "Permanent Night Vision", Tag.HELPFUL),
		new ModifierInfo(HASTE, "Haste", "Permanent Haste II", Tag.HELPFUL),
		new ModifierInfo(WEAK_HANDS, "Weak Hands", "Permanent Mining Fatigue I", Tag.HARDER),
		new ModifierInfo(GILLS, "Gills", "Permanent Water Breathing", Tag.HELPFUL),
		new ModifierInfo(SHORT_BREATH, "Short Breath", "Air runs out three times faster underwater", Tag.HARDER),
		new ModifierInfo(FROST_WALKER, "Frost Walker", "Water freezes under your feet", Tag.HELPFUL),
		new ModifierInfo(LONG_ARMS, "Long Arms", "Reach is 3 blocks longer", Tag.HELPFUL),
		new ModifierInfo(SHORT_ARMS, "Short Arms", "Reach is 2 blocks", Tag.HARDER),
		new ModifierInfo(NO_JUMPING, "No Jumping", "Players cannot jump", Tag.BRUTAL),
		new ModifierInfo(BOUNCY, "Bouncy", "Falls bounce you back up instead of hurting", Tag.CHAOS),
		new ModifierInfo(PUMPKIN_HEAD, "Pumpkin Head", "Everyone wears a carved pumpkin that cannot be removed", Tag.HARDER),
		new ModifierInfo(DARKNESS_PULSE, "Darkness Pulse", "Darkness falls on everyone every 30 seconds", Tag.HARDER),
		new ModifierInfo(UNDEAD, "Undead", "Players burn in daylight without a helmet, healing potions hurt, undead mobs ignore you", Tag.CHAOS),
		new ModifierInfo(VERTIGO, "Vertigo", "Nausea above Y 100", Tag.CHAOS),
		new ModifierInfo(HYDROPHOBIC, "Hydrophobic", "Touching water hurts", Tag.BRUTAL),
		new ModifierInfo(VENOMOUS, "Venomous", "Every mob hit poisons you for 3 seconds", Tag.HARDER),
		new ModifierInfo(SHELLSHOCK, "Shellshock", "After taking damage, sprinting gives no speed for 5 seconds", Tag.HARDER),
		new ModifierInfo(THORNED_MOBS, "Thorned Mobs", "Hitting a mob reflects a quarter of the damage back to you", Tag.HARDER),
		new ModifierInfo(EXPLOSIVE_ARROWS, "Explosive Arrows", "Skeleton arrows explode on impact", Tag.BRUTAL),
		new ModifierInfo(HELLFIRE_MOBS, "Hellfire Mobs", "Hostile mobs burn forever without dying and set you on fire when they hit", Tag.BRUTAL)
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
		new ModifierInfo(CAVE_INS, "Cave-ins", "Mining underground can bring the ceiling down as gravel", Tag.CHAOS),
		new ModifierInfo(TRAPPED_CHESTS, "Trapped Chests", "Opening a loot chest can release mobs or lit TNT", Tag.CHAOS),
		new ModifierInfo(CURSED_LOOT, "Cursed Loot", "Gear in loot chests carries Curse of Binding or Vanishing", Tag.HARDER),
		new ModifierInfo(LARGE_BIOMES, "Large Biomes", "The world generates with large biomes", Tag.CHAOS),
		new ModifierInfo(AMPLIFIED, "Amplified", "The world generates amplified", Tag.CHAOS),
		new ModifierInfo(NO_VILLAGES, "No Villages", "No villages generate", Tag.HARDER),
		new ModifierInfo(CLOSE_STRONGHOLD, "Close Stronghold", "The first stronghold ring sits a few hundred blocks from the world origin", Tag.HELPFUL)
		// ---- end world ----
	);

	/** Pairs that contradict each other. The picker never draws both halves of a pair. */
	public static final List<List<String>> CONFLICTS = List.of(
		List.of(ONE_HEART, HALF_HEALTH),
		List.of(TINY, GIANT),
		List.of(MOON_GRAVITY, HEAVY_GRAVITY),
		// ---- body ----
		List.of(EXTRA_HEARTS, ONE_HEART),
		List.of(EXTRA_HEARTS, HALF_HEALTH),
		List.of(SUDDEN_DEATH, ONE_HEART),
		List.of(SUDDEN_DEATH, HALF_HEALTH),
		List.of(FIRE_WEAKNESS, FIREPROOF),
		List.of(FEATHERWEIGHT, HEAVY_LANDING),
		List.of(FEATHERWEIGHT, BOUNCY),
		List.of(BOUNCY, HEAVY_LANDING),
		List.of(HASTE, WEAK_HANDS),
		List.of(GILLS, SHORT_BREATH),
		List.of(LONG_ARMS, SHORT_ARMS),
		List.of(SHORT_ARMS, GIANT),
		List.of(UNDEAD, FIREPROOF),
		List.of(UNDEAD, PUMPKIN_HEAD)
		// ---- mobs-a ----
		List.of(INVISIBLE_MOBS, GLOWING_MOBS)
		// ---- end mobs-a ----
		// ---- world ----
		List.of(ETERNAL_DAY, ETERNAL_NIGHT),
		List.of(ETERNAL_DAY, FAST_DAYS),
		List.of(ETERNAL_NIGHT, FAST_DAYS),
		List.of(TIGHT_BORDER, SHRINKING_BORDER),
		List.of(LARGE_BIOMES, AMPLIFIED)
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
