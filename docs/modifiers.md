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

## World and time

| ID | Name | Effect | Tag |
|---|---|---|---|
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
- `swap` and `shared_inventory` do nothing with one player online.
- `/speedrun modifiers force` overrides the pool for the next run only.
