# Commands

Every command is registered together with its description, usage line, examples and required permission. `/help` is generated from those registrations, so documentation cannot drift from the code.

Player commands need no op. Admin commands (`/speedrun ...`) pass for any of these:

- op level 2 or higher, or the server console
- a player on the trusted list, managed with `/speedrun trust`
- the `speedrun.admin` node from a permissions mod using fabric-permissions-api

Trusted players get no vanilla op powers. `/gamemode`, `/give` and other vanilla op commands stay locked for them. `/help` lists admin commands for them as it does for ops.

## Player commands

| Command | What it does |
|---|---|
| `/help` | Lists all commands you can use, grouped by category. Each entry is clickable. |
| `/help <command>` | Full documentation for one command, with examples. Multi-word names work, such as `/help speedrun tickrate`. |
| `/help <page>` | One page of the command list |
| `/start` | Starts the run from the lobby. Works only in the lobby. |
| `/best` | Record time and splits for the current category |
| `/best all` | Records for every category played so far |
| `/stats` | Your lifetime deaths, deaths by cause, runs played, wins |
| `/stats <player>` | Same, for another player |
| `/stats server` | Run count, win count, total deaths, most common death causes |
| `/splits` | Current run's splits against the best run |
| `/modifiers [page]` | Explains modifiers, shows the ones active in this run and how the next run's are chosen, and lists every modifier with its effect, 20 per page. |
| `/modifiers active` | Lists each modifier active in the current run with what it does. |
| `/voteskip` | Votes to abandon the current seed. Passes when more than half of the online players have voted, instantly with one player online, and triggers a reset without counting a death. Works only during a live run. Votes clear when a run starts or ends, and a player who leaves loses their vote. |
| `/vote <number>` | Votes for a modifier while the lobby or death room offers a choice |

## Admin commands

Permission node prefix: `speedrun.admin`.

| Command | What it does |
|---|---|
| `/speedrun reset` | Ends the current run and starts a new one. Counts as an attempt, not a death. In the lobby it rerolls the prepared seed. |
| `/speedrun tickrate <rate>` | Sets the game tick rate for future runs. Default 20. Accepts 1 to 100 (5x). |
| `/speedrun tickrate <rate> now` | Same, and also applies the rate immediately. The live run becomes unranked. |
| `/speedrun tickrate` | Shows the configured rate, the active rate and the actual measured TPS |
| `/speedrun sharedhealth <on\|off>` | Toggles shared health. Takes effect on the next run. |
| `/speedrun sharedhunger <on\|off>` | Toggles shared hunger. Takes effect on the next run. |
| `/speedrun goal <dragon\|allbosses>` | Sets what wins a run. `allbosses` requires the dragon, warden and wither. |
| `/speedrun settings` | Shows every current setting and the record category it produces |
| `/speedrun modifiers mode <off\|random\|vote>` | How modifiers are chosen each run |
| `/speedrun modifiers count <n>` | How many modifiers are active per run |
| `/speedrun modifiers pool [page]` | Lists every modifier and whether it is in the pool, 20 per page |
| `/speedrun modifiers enable <id\|all>` | Adds a modifier to the pool, or every modifier with `all` |
| `/speedrun modifiers disable <id\|all>` | Removes a modifier from the pool, or empties it with `all` |
| `/speedrun modifiers force <id...>` | Forces specific modifiers for the next run only |
| `/speedrun keeprun <on\|off>` | Keeps the run when everyone leaves or the server restarts. On: the run pauses, freezing the timer and the world, until someone joins, and is saved to disk to survive restarts. Off (default): the run ends 60 seconds after everyone leaves, or when the server stops. Applies immediately. |
| `/speedrun village <on\|off>` | Guarantees a village 100 to 500 blocks from spawn in every run world, matching the biome there. Skipped only when there is no dry land in range. On by default. Applies to worlds generated after the change. |
| `/speedrun compass <on\|off>` | Gives every player a Player Tracker compass at the start of each run. Right-click it to switch to the next player; it points at that player. On by default. |
| `/speedrun deathroom mintime <seconds>` | Minimum time the death room stays up. Default 5. Accepts 0 to 600. |
| `/speedrun stats reset runcount` | Resets the attempt counter |
| `/speedrun stats reset best [category]` | Clears one category's record, or all of them |
| `/speedrun stats reset deaths` | Clears the death sidebar |
| `/speedrun trust add <player>` | Adds a player to the trusted list. Needs real op level 2 or the console. |
| `/speedrun trust remove <player>` | Removes a player from the trusted list. Needs real op level 2 or the console. |
| `/speedrun trust list`, `/speedrun trust auto on|off` (default on: everyone who joins is trusted) | Lists trusted players. Needs real op level 2 or the console. |
| `/speedrun stats reset all` | Clears every stat. Asks for confirmation with a clickable message that runs `/speedrun stats reset all confirm`. |

Settings changes take effect at the start of the next run, so a run's category never changes mid-run. `/speedrun tickrate` is the exception when used with `now` as a trailing argument. In that case the current run is marked unranked.

## `/help` behavior

- `/help` replaces the vanilla command of the same name.
- Output is paged at 8 entries per page with clickable previous and next arrows. `/help <page>` jumps to a page.
- Commands a player lacks permission for are hidden from them.
- Hovering an entry shows its usage line. Clicking it opens `/help <command>`.
- `/help <command>` shows the description, every argument with its accepted values, and at least one example, such as `/speedrun tickrate 100`. Clicking an example puts it in the chat box.
