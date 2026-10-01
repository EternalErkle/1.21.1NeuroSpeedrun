# Run Modifiers

Modifiers change the rules of a single run. Admins choose which ones are in the pool, how many are active per run, and whether they are drawn at random or voted on.

In vote mode, the lobby or death room offers two options drawn from the pool as clickable chat messages. Players pick with a click or `/vote <number>`. In the death room, voting closes when the new worlds are ready. In the lobby, it closes when someone runs `/start`. Ties are broken at random.

Active modifiers are shown under the GO! title when a run starts and under the top-of-screen timer, and form part of the record category. Anyone can run `/modifiers` to see them, how the next run's are chosen, and the full list.

Modifiers are off by default. To turn them on, add some to the pool with `/speedrun modifiers enable all`, `/speedrun modifiers pool` (click to toggle) or `/speedrun modifiers enable <id>`, then set `/speedrun modifiers mode random` or `vote`.

Tags describe how each modifier affects difficulty:

- **Harder**: raises difficulty.
- **Chaos**: makes the run unpredictable.
- **Brutal**: may make the run close to unwinnable.
- **Helpful**: lowers difficulty.

## Health and damage

| ID | Name | Effect | Tag |
|---|---|---|---|
| `one_heart` | One Heart | Max health is 1 heart | Brutal |
| `half_health` | Half Health | Max health is 5 hearts | Harder |
| `uhc` | UHC | No natural regeneration | Harder |
| `glass_cannon` | Glass Cannon | Players deal 2x damage and take 2x damage | Chaos |
| `heavy_landing` | Heavy Landing | Fall damage is tripled | Harder |

## Hunger

| ID | Name | Effect | Tag |
|---|---|---|---|
| `snack_size` | Snack Size | Food restores half as much hunger and saturation | Harder |

## Body and movement

| ID | Name | Effect | Tag |
|---|---|---|---|
| `tiny` | Tiny | Players are half size, using the 1.21 scale attribute | Chaos |
| `giant` | Giant | Players are double size, with longer reach | Chaos |
| `moon_gravity` | Moon Gravity | Gravity is 30% of normal | Chaos |
| `heavy_gravity` | Heavy Gravity | Gravity is doubled, so jumps are lower | Harder |

## Mobs

| ID | Name | Effect | Tag |
|---|---|---|---|
| `charged_up` | Charged Up | Every creeper spawns charged | Brutal |
| `horde` | Horde | The hostile mob spawn cap is doubled | Harder |
| `hasty_mobs` | Hasty Mobs | Hostile mobs have permanent Speed | Harder |
| `mob_randomizer` | Mob Randomizer | Each mob type drops one random item, fixed for the whole run | Chaos |
| `blaze_boost` | Blaze Boost | Blazes drop twice as many rods | Helpful |

## Mob behavior

| ID | Name | Effect | Tag |
|---|---|---|---|
| `sharpshooters` | Sharpshooters | Skeletons fire twice as often | Harder |
| `short_fuse` | Short Fuse | Creepers explode in half the time | Harder |
| `enderman_rage` | Enderman Rage | Endermen attack on sight | Harder |
| `invisible_mobs` | Invisible Mobs | Hostile mobs are invisible | Brutal |
| `glowing_mobs` | Glowing Mobs | Hostile mobs glow through walls | Helpful |
| `silent_mobs` | Silent Mobs | Hostile mobs make no sound | Harder |
| `baby_zombies` | Baby Zombies | Every zombie is a baby | Harder |
| `spider_jockeys` | Spider Jockeys | Every spider carries a skeleton | Harder |
| `kamikaze` | Kamikaze | Hostile mobs explode when they die | Chaos |
| `spawn_randomizer` | Spawn Randomizer | Natural spawns become random mobs of the same kind | Chaos |
| `nether_invasion` | Nether Invasion | Blazes, piglins and ghasts spawn in the overworld at night | Chaos |
| `phantom_menace` | Phantom Menace | Phantoms come every night, even after sleeping | Harder |
| `patrol_season` | Patrol Season | Pillager patrols arrive every two minutes | Harder |
| `revenge` | Revenge | Killing a passive mob spawns a zombie | Chaos |
| `ambush` | Ambush | Every 5 minutes a hostile mob spawns next to each player | Chaos |
| `pet_wolves` | Pet Wolves | Everyone starts with a tamed wolf | Helpful |
| `piglin_friends` | Piglin Friends | Piglins never attack players | Helpful |
| `pearl_bonanza` | Pearl Bonanza | Endermen always drop at least 2 pearls | Helpful |
| `blaze_swarm` | Blaze Swarm | Blazes spawn anywhere in the Nether | Helpful |
| `armored_dragon` | Armored Dragon | The dragon has double health | Harder |
| `arrow_dragon` | Arrow Dragon | The dragon only takes damage from arrows | Harder |

Details worth knowing:

- `invisible_mobs` and `glowing_mobs` never appear together.
- `kamikaze` explosions break blocks only while the `mobGriefing` gamerule is on. The dragon is exempt.
- `spawn_randomizer` swaps each natural spawn for a random mob of the same category, so a monster becomes another monster and an animal another animal. Bosses, wardens, elder guardians, giants and illusioners are never picked. The replacement still has to pass its own spawn rules, so a fish rolled on dry land simply does not appear.
- `nether_invasion` piglins never turn into zombified piglins.
- `phantom_menace` treats every player as four days without sleep. Phantoms still need night and open sky.
- `patrol_season` starts two minutes into the run. Patrols still avoid villages and the biomes vanilla excludes.
- `revenge` counts animals, fish and villagers. The zombie targets the killer.
- `pet_wolves` gives one wolf per player per run, including late joiners. Reconnecting does not hand out another.
- `piglin_friends` also covers brutes. Hitting a piglin no longer makes it fight back.
- `arrow_dragon` counts arrows and tridents. End crystal blasts no longer hurt the dragon.
## Mob threats

| `creeper_rain` | Creeper Rain | Every minute, three creepers drop from 15 blocks up near each player who stands under open sky | Brutal |
| `sniper_skeletons` | Sniper Skeletons | Skeletons, strays and bogged shoot from twice as far and aim along the true arrow arc with no spread | Harder |
| `stalker` | Stalker | Every 2 minutes, a fast, fireproof zombie spawns behind a random player and targets only that player | Harder |
| `angry_neutrals` | Angry Neutrals | Wolves, bees, endermen, piglins, zombified piglins, iron golems and polar bears within 20 blocks attack. Tamed wolves and player-built golems are spared | Brutal |
| `juiced_mobs` | Juiced Mobs | Hostile mobs have permanent Strength and Speed | Harder |
| `armored_horde` | Armored Horde | Zombies and skeletons spawn in full armor, each piece iron or (1 in 4) diamond. The armor never drops | Harder |
| `splitters` | Splitters | A killed hostile mob splits into two smaller copies with half health and no loot. Copies never split again. Slimes, magma cubes, vexes and bosses don't split | Brutal |
| `elites` | Elites | One in ten hostile mobs is a named elite with triple health | Harder |
| `swarm` | Swarm | Every naturally spawned hostile mob brings two more of its kind | Harder |
| `relentless` | Relentless | Hostile mobs notice you from 64 blocks and keep chasing after losing sight of you | Harder |
| `vex_curse` | Vex Curse | Each hit from a mob has a 1 in 5 chance to summon a vex that lives 30 to 45 seconds | Brutal |
| `warden_alarm` | Warden Alarm | Once per run, between minute 10 and 50, an angry warden emerges near a random player | Brutal |
| `ghast_air_force` | Ghast Air Force | Every 30 seconds, ghasts spawn in the sky around overworld players, up to two per player | Brutal |
| `endless_raids` | Endless Raids | Everyone has Bad Omen for the whole run. It comes back after each raid starts | Brutal |
| `marked` | Marked | Every 5 minutes, a random player glows and every hostile mob within 48 blocks targets them for 60 seconds | Brutal |
| `thieves` | Thieves | A zombie or enderman that hits you takes a random item into its offhand. Kill it to get the item back | Chaos |

## Items and inventory

| ID | Name | Effect | Tag |
|---|---|---|---|
| `starter_kit` | Starter Kit | Everyone starts with stone tools, a bucket and bread | Helpful |
| `no_crafting_table` | No Crafting Table | Only the 2x2 inventory crafting grid works | Brutal |
| `hotbar_only` | Hotbar Only | Main inventory slots are locked | Harder |
| `shared_inventory` | Shared Inventory | All players share one inventory | Chaos |
| `shuffle` | Shuffle | Every inventory is shuffled every 5 minutes | Chaos |

## Items, crafting and loot

| ID | Name | Effect | Tag |
|---|---|---|---|
| `no_shields` | No Shields | Shields cannot be crafted, and any picked up vanish | Harder |
| `no_armor` | No Armor | Armor slips off into the inventory; elytra and pumpkins still work | Brutal |
| `no_diamond_gear` | No Diamond Gear | Diamond tools, weapons and armor cannot be crafted or kept | Harder |
| `stone_age` | Stone Age | Iron and chainmail tools, weapons and armor cannot be crafted or kept | Brutal |
| `bucketless` | Bucketless | Buckets cannot be crafted; found ones still work | Harder |
| `no_boats` | No Boats | Boats cannot be crafted or kept | Harder |
| `insomnia` | Insomnia | Nobody can sleep | Harder |
| `unbreakable` | Unbreakable | Player tools and armor never lose durability | Helpful |
| `fragile_tools` | Fragile Tools | Player tools and armor wear out 4x faster | Harder |
| `instant_smelting` | Instant Smelting | Furnaces, smokers and blast furnaces finish in one tick; fuel is still used | Helpful |
| `ore_rush` | Ore Rush | Ores drop double | Helpful |
| `vein_miner` | Vein Miner | Mining an ore with the right tool mines up to 64 connected blocks of it; sneak to mine one | Helpful |
| `timber` | Timber | Chopping a log fells up to 256 connected logs; sneak to chop one | Helpful |
| `orchard` | Orchard | Leaves have a 10% extra chance to drop an apple | Helpful |
| `treasure_hunter` | Treasure Hunter | Structure chests, barrels and chest minecarts roll their loot twice | Helpful |
| `looted` | Looted | Structure chests, barrels and chest minecarts are empty | Brutal |
| `pearl_start` | Pearl Start | Everyone starts with 4 ender pearls | Helpful |
| `wings` | Wings | Everyone starts with an elytra and 16 rockets | Helpful |
| `fragile_eyes` | Fragile Eyes | Thrown eyes of ender always break | Harder |
| `sturdy_eyes` | Sturdy Eyes | Thrown eyes of ender never break | Helpful |
| `bow_only` | Bow Only | Melee attacks on mobs and players are cancelled | Brutal |
| `melee_only` | Melee Only | Bows and crossbows cannot be drawn | Harder |
| `butterfingers` | Butterfingers | Taking damage throws the held item on the ground | Chaos |
| `cursed_armor` | Cursed Armor | Worn armor gains Curse of Binding | Chaos |
| `item_magnet` | Item Magnet | Dropped items within 8 blocks fly to you | Helpful |
| `gold_rush` | Gold Rush | Piglin bartering gives double | Helpful |
| `villager_strike` | Villager Strike | Villagers and wandering traders refuse to trade | Harder |
| `loot_chaos` | Loot Chaos | Every block and mob drop becomes a random item. Each block or mob type always gives the same item for a seed, and the mapping changes with every new seed | Chaos |
| `jackpot` | Jackpot | Each block a player breaks drops 1 to 640 times its drop. About 60% of breaks give x1, and x100 or more comes up about 1 in 160. Stacks with Loot Chaos and Ore Rush | Chaos |

## Team

| ID | Name | Effect | Tag |
|---|---|---|---|
| `tethered` | Tethered | Players more than 40 blocks apart are pulled together; over 100 blocks they are teleported | Chaos |
| `buddy_system` | Buddy System | Within 16 blocks of a teammate you regenerate; alone you get Weakness | Chaos |
| `hot_potato` | Hot Potato | One player glows and takes half a heart every 5 seconds until they hit a teammate to pass it | Chaos |
| `juggernaut` | Juggernaut | One random player gets double max health and attack damage | Chaos |
| `inventory_rotation` | Inventory Rotation | Every 5 minutes each player gets the previous player's whole inventory | Chaos |
| `no_nametags` | No Nametags | Player name tags are hidden | Harder |
| `radio_silence` | Radio Silence | Player chat is blocked for the run | Chaos |

## World

| ID | Name | Effect | Tag |
|---|---|---|---|
| `eternal_night` | Eternal Night | The daylight cycle is locked at midnight | Harder |
| `swap` | Swap | All players swap positions every 5 minutes | Chaos |

## Body, senses and survival

| ID | Name | Effect | Tag |
|---|---|---|---|
| `extra_hearts` | Extra Hearts | Max health is 15 hearts | Helpful |
| `vampire` | Vampire | No natural regeneration, but every kill heals 2 hearts | Chaos |
| `sudden_death` | Sudden Death | After 30 minutes, max health drops 1 heart every 5 minutes | Harder |
| `bleeding` | Bleeding | Any hit over 3 hearts also gives 3 seconds of Wither | Harder |
| `fire_weakness` | Fire Weakness | Fire and lava deal double damage | Harder |
| `fireproof` | Fireproof | Permanent Fire Resistance | Helpful |
| `featherweight` | Featherweight | No fall damage | Helpful |
| `blast_proof` | Blast Proof | Explosions deal no damage to players | Helpful |
| `big_booms` | Big Booms | Every explosion is twice as strong | Harder |
| `knockback_chaos` | Knockback Chaos | Players take triple knockback | Chaos |
| `second_wind` | Second Wind | The first death of the run is forgiven and that player respawns at spawn | Helpful |
| `last_one_standing` | Last One Standing | Dead players become spectators; the run fails only when everyone is dead | Helpful |
| `totem_start` | Totem Start | Everyone starts with a Totem of Undying | Helpful |
| `critical_only` | Critical Only | Only critical hits and critical arrows deal damage | Brutal |
| `night_vision` | Night Vision | Permanent Night Vision | Helpful |
| `haste` | Haste | Permanent Haste II | Helpful |
| `weak_hands` | Weak Hands | Permanent Mining Fatigue I | Harder |
| `gills` | Gills | Permanent Water Breathing | Helpful |
| `short_breath` | Short Breath | Air runs out three times faster underwater | Harder |
| `frost_walker` | Frost Walker | Water freezes under your feet | Helpful |
| `long_arms` | Long Arms | Reach is 3 blocks longer | Helpful |
| `short_arms` | Short Arms | Reach is 2 blocks | Harder |
| `no_jumping` | No Jumping | Players cannot jump | Brutal |
| `bouncy` | Bouncy | Falls bounce you back up instead of hurting | Chaos |
| `pumpkin_head` | Pumpkin Head | Everyone wears a carved pumpkin that cannot be removed | Harder |
| `darkness_pulse` | Darkness Pulse | Darkness falls on everyone every 30 seconds | Harder |
| `undead` | Undead | Players burn in daylight without a helmet, healing potions hurt, undead mobs ignore you | Chaos |
| `vertigo` | Vertigo | Nausea above Y 100 | Chaos |
| `hydrophobic` | Hydrophobic | Touching water hurts | Brutal |
| `venomous` | Venomous | Every mob hit poisons you for 3 seconds | Harder |
| `shellshock` | Shellshock | After taking damage, sprinting gives no speed for 5 seconds | Harder |
| `thorned_mobs` | Thorned Mobs | Hitting a mob reflects a quarter of the damage back to you | Harder |
| `explosive_arrows` | Explosive Arrows | Skeleton arrows explode on impact | Brutal |
| `hellfire_mobs` | Hellfire Mobs | Hostile mobs burn forever without dying and set you on fire when they hit | Brutal |

Totems of Undying now work in every run. Vanilla only checks them after the death event, which runs always cancel, so the run manager checks the totem first.
## World and time

| `pig_bomb` | Pig Bomb | Every 30 seconds to 3 minutes, a pig carrying lit TNT spawns near a random player and chases them. It flashes faster as the 4 second fuse runs down | Brutal |
| `eternal_day` | Eternal Day | The daylight cycle is locked at noon | Helpful |
| `fast_days` | Fast Days | The day cycle runs 4x faster | Chaos |
| `tight_border` | Tight Border | The world border sits 500 blocks from spawn, in the Overworld and the Nether | Harder |
| `shrinking_border` | Shrinking Border | The border starts 2000 blocks from spawn and closes to 50 over 60 minutes of run time | Harder |
| `short_sight` | Short Sight | View distance is 4 chunks | Harder |
| `time_bomb` | Time Bomb | The run fails after 60 minutes, with warnings as the end nears | Harder |
| `instant_portals` | Instant Portals | Nether portals teleport instantly | Helpful |
| `explosive_beds` | Explosive Beds | Beds explode in the Overworld too | Harder |
| `no_bed_bombs` | No Bed Bombs | Beds do not explode in the End | Harder |
| `silverfish_stone` | Silverfish Stone | Mining stone has a 2% chance to release a silverfish | Harder |
| `explosive_ores` | Explosive Ores | Mining an ore has a 5% chance to leave lit TNT behind | Chaos |
| `gravity_blocks` | Gravity Blocks | Every placed block falls like sand when nothing is under it | Chaos |
| `thunderstruck` | Thunderstruck | Lightning strikes near a random player every 1 to 4 minutes | Brutal |
| `meteor_shower` | Meteor Shower | Every 3 to 6 minutes, fireballs rain down around players | Brutal |
| `anvil_rain` | Anvil Rain | Every 2 to 5 minutes, an anvil drops on a random player | Brutal |
| `floor_is_lava` | Floor Is Lava | Every 10 minutes, standing on the ground hurts for 20 seconds, after a 3 second warning | Brutal |
| `blackouts` | Blackouts | Every 2 to 4 minutes, everyone is blinded for 5 seconds | Harder |
| `spawner_surprise` | Spawner Surprise | Every 10 minutes, a mob spawner appears near each player | Brutal |
| `cave_ins` | Cave-ins | Mining underground can bring the ceiling down as gravel | Chaos |
| `trapped_chests` | Trapped Chests | Opening an unopened loot chest has a 1 in 4 chance to release 2 or 3 hostile mobs or lit TNT | Chaos |
| `cursed_loot` | Cursed Loot | Armor in loot chests gets Curse of Binding or Vanishing; tools and weapons get Curse of Vanishing | Harder |
| `large_biomes` | Large Biomes | The run's Overworld generates with large biomes | Chaos |
| `amplified` | Amplified | The run's Overworld generates amplified | Chaos |
| `no_villages` | No Villages | No villages generate in the run's Overworld | Harder |
| `close_stronghold` | Close Stronghold | The first stronghold ring sits about 200 to 350 blocks from the world origin | Helpful |

Conflicts: `eternal_day` with `eternal_night` and `fast_days`; `eternal_night` with `fast_days`; `tight_border` with `shrinking_border`; `large_biomes` with `amplified`.

The four worldgen modifiers shape the Overworld when the next run's worlds are generated, so the next run's modifiers must be known before generation starts.

## Rules

- Modifiers apply when a run starts and are removed when it ends. Nothing carries over into the next run.
- Every modifier is removable mid-run by a reset, so a bugged modifier never locks the server.
- Timed modifiers (`shuffle`, `swap`) count real seconds of run time, so they fire at the same real interval at any tick rate and don't advance while a run is paused.
- `sudden_death` and `darkness_pulse` also follow run time.
- Timed modifiers (`shuffle`, `swap`, `ambush`) count real seconds of run time, so they fire at the same real interval at any tick rate and don't advance while a run is paused.
- `swap` and `shared_inventory` do nothing with one player online.
- `creeper_rain`, `stalker`, `ghast_air_force`, `warden_alarm` and `marked` also run on real run time. A restored run keeps the warden's time and the current mark.
- `swap` and `shared_inventory` do nothing with one player online. Neither do `hot_potato`, `inventory_rotation` and `tethered`. With one player, `buddy_system` only gives Weakness.
- `inventory_rotation` also runs on the 5-minute timer.
- `/speedrun modifiers force` overrides the pool for the next run only.
