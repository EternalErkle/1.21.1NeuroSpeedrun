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

## Rules

- Modifiers apply when a run starts and are removed when it ends. Nothing carries over into the next run.
- Every modifier is removable mid-run by a reset, so a bugged modifier never locks the server.
- Timed modifiers (`shuffle`, `swap`) count real seconds of run time, so they fire at the same real interval at any tick rate and don't advance while a run is paused.
- `swap` and `shared_inventory` do nothing with one player online. Neither do `hot_potato`, `inventory_rotation` and `tethered`. With one player, `buddy_system` only gives Weakness.
- `inventory_rotation` also runs on the 5-minute timer.
- `/speedrun modifiers force` overrides the pool for the next run only.
