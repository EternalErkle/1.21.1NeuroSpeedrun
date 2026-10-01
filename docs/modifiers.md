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

## Items and inventory

| ID | Name | Effect | Tag |
|---|---|---|---|
| `starter_kit` | Starter Kit | Everyone starts with stone tools, a bucket and bread | Helpful |
| `no_crafting_table` | No Crafting Table | Only the 2x2 inventory crafting grid works | Brutal |
| `hotbar_only` | Hotbar Only | Main inventory slots are locked | Harder |
| `shared_inventory` | Shared Inventory | All players share one inventory | Chaos |
| `shuffle` | Shuffle | Every inventory is shuffled every 5 minutes | Chaos |

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

## Rules

- Modifiers apply when a run starts and are removed when it ends. Nothing carries over into the next run.
- Every modifier is removable mid-run by a reset, so a bugged modifier never locks the server.
- Timed modifiers (`shuffle`, `swap`) count real seconds of run time, so they fire at the same real interval at any tick rate and don't advance while a run is paused.
- `sudden_death` and `darkness_pulse` also follow run time.
- Timed modifiers (`shuffle`, `swap`, `ambush`) count real seconds of run time, so they fire at the same real interval at any tick rate and don't advance while a run is paused.
- `swap` and `shared_inventory` do nothing with one player online.
- `/speedrun modifiers force` overrides the pool for the next run only.
