# Commands

Every command is registered together with its description, usage line, examples and required permission. `/help` is generated from those registrations, so documentation cannot drift from the code.

Permissions come from fabric-permissions-api. With no permissions mod installed, admin commands fall back to op level 2.

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
| `/voteskip` | Votes to abandon the current seed. Passes when more than half of the online players have voted, instantly with one player online, and triggers a reset without counting a death. Works only during a live run. Votes clear when a run starts or ends, and a player who leaves loses their vote. |
| `/vote <number>` | Votes for a modifier while the lobby or death room offers a choice |

## Admin commands

Permission node prefix: `speedrun.admin`.

| Command | What it does |
|---|---|
| `/speedrun reset` | Ends the current run and starts a new one. Counts as an attempt, not a death. In the lobby it rerolls the prepared seed. |
| `/speedrun tickrate <rate>` | Sets the game tick rate for future runs. Default 20. Accepts 1 to 1000. |
| `/speedrun tickrate <rate> now` | Same, and also applies the rate immediately. The live run becomes unranked. |
| `/speedrun tickrate` | Shows the configured rate, the active rate and the actual measured TPS |
| `/speedrun sharedhealth <on\|off>` | Toggles shared health. Takes effect on the next run. |
| `/speedrun sharedhunger <on\|off>` | Toggles shared hunger. Takes effect on the next run. |
| `/speedrun goal <dragon\|allbosses>` | Sets what wins a run. `allbosses` requires the dragon, warden and wither. |
| `/speedrun settings` | Shows every current setting and the record category it produces |
| `/speedrun modifiers mode <off\|random\|vote>` | How modifiers are chosen each run |
| `/speedrun modifiers count <n>` | How many modifiers are active per run |
| `/speedrun modifiers pool` | Lists every modifier and whether it is in the pool |
| `/speedrun modifiers enable <id>` | Adds a modifier to the pool |
| `/speedrun modifiers disable <id>` | Removes a modifier from the pool |
| `/speedrun modifiers force <id...>` | Forces specific modifiers for the next run only |
| `/speedrun deathroom mintime <seconds>` | Minimum time the death room stays up. Default 5. Accepts 0 to 600. |
| `/speedrun stats reset runcount` | Resets the attempt counter |
| `/speedrun stats reset best [category]` | Clears one category's record, or all of them |
| `/speedrun stats reset deaths` | Clears the death sidebar |
| `/speedrun stats reset all` | Clears every stat. Asks for confirmation with a clickable message that runs `/speedrun stats reset all confirm`. |

Settings changes take effect at the start of the next run, so a run's category never changes mid-run. `/speedrun tickrate` is the exception when used with `now` as a trailing argument. In that case the current run is marked unranked.

## `/help` behavior

- `/help` replaces the vanilla command of the same name.
- Output is paged at 8 entries per page with clickable previous and next arrows. `/help <page>` jumps to a page.
- Commands a player lacks permission for are hidden from them.
- Hovering an entry shows its usage line. Clicking it opens `/help <command>`.
- `/help <command>` shows the description, every argument with its accepted values, and at least one example, such as `/speedrun tickrate 100`. Clicking an example puts it in the chat box.
