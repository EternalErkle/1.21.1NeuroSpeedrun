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

## Mob threats

| ID | Name | Effect | Tag |
|---|---|---|---|
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

## World

| ID | Name | Effect | Tag |
|---|---|---|---|
| `eternal_night` | Eternal Night | The daylight cycle is locked at midnight | Harder |
| `swap` | Swap | All players swap positions every 5 minutes | Chaos |

## Rules

- Modifiers apply when a run starts and are removed when it ends. Nothing carries over into the next run.
- Every modifier is removable mid-run by a reset, so a bugged modifier never locks the server.
- Timed modifiers (`shuffle`, `swap`) count real seconds of run time, so they fire at the same real interval at any tick rate and don't advance while a run is paused.
- `swap` and `shared_inventory` do nothing with one player online.
- `creeper_rain`, `stalker`, `ghast_air_force`, `warden_alarm` and `marked` also run on real run time. A restored run keeps the warden's time and the current mark.
- `/speedrun modifiers force` overrides the pool for the next run only.
