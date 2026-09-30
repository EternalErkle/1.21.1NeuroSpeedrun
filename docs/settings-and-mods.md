# Settings and Mods

## Tick rate

The tick rate controls how fast the whole game runs. At 100 TPS everything happens five times faster: mob movement and attacks, crop growth, smelting, the day and night cycle. This uses the vanilla `/tick rate` system from 1.20.3 onward, which clients follow automatically.

`speedrun-core` keeps its own features correct at any rate:

- **Timer.** The on-screen timer measures real elapsed time. In-game time, ticks divided by 20, is recorded alongside it in run results.
- **Durations.** Countdowns, death room timing and face motion are defined in seconds. `Time` converts them to ticks at the current rate, so a 3 second countdown lasts 3 seconds at any rate.
- **Records.** Records are grouped by category. The category key is the goal, the tick rate, the shared health and shared hunger toggles, and the set of active modifiers. A 100 TPS run never competes with a 20 TPS run.
- **Server load.** At 100 TPS each tick has 10ms instead of 50ms. When the server falls behind, `/speedrun tickrate` reports the actual TPS and a warning appears in chat for admins. Real time stays accurate even when the server is behind; in-game time does not.

The rate is applied when a run starts. It affects every world, including the death room, which is harmless because all death room timing is in seconds.

## Shared health and shared hunger

Shared health and shared hunger are two independent settings, not random modifiers:

- `/speedrun sharedhealth on|off` makes every player share one health bar.
- `/speedrun sharedhunger on|off` makes every player share one hunger bar, including saturation.

Either can be on without the other. Changes apply from the next run.

Both are built into `speedrun-core` (`SharedVitals`). No extra mod is needed.

How it works:

- The mod keeps one shared value each for health, food level, saturation and exhaustion.
- Every tick it reads how much each player's value changed since the last tick and adds all those changes to the shared value. Two players who each take 3 damage in the same tick cost the shared bar 6.
- The result is clamped and written back to every player in the run.
- Absorption and max-health changes from modifiers still work. Each player's health is capped at their own max health.
- Natural regeneration and starvation happen per player, so with shared health they scale with the number of players.
- A player who joins mid-run takes the current shared values.
- If the shared health reaches 0, one player dies through normal damage. That triggers exactly one reset.

Every reset restores full health and hunger to everyone.

Both toggles are part of the record category.

## Goals

The goal setting decides what wins a run: `dragon` (default) or `allbosses`, which also requires the warden and the wither. `/speedrun goal <dragon|allbosses>` changes it from the next run. Full rules are in [run-lifecycle.md](run-lifecycle.md#goals). The goal is part of the record category.

## Third-party mods

Any Fabric mod for 1.21.1 can run beside `speedrun-core`:

- The server's mod list is kept in `pack/` with packwiz, which pins exact versions from CurseForge or Modrinth.
- Mods that are server-side only need nothing from players.
- If a mod must also be on the client, packwiz exports the same list as a CurseForge modpack that players import in the CurseForge app.

Mods that change health, hunger, death or dimensions can conflict with the run lifecycle. Each new one is tested against a full reset and a full win before it is added to the pack.

## Stats storage

Stats are stored in `config/speedrun-core/stats.json`:

- Attempt counter and win counter.
- Records per category, each with real time, in-game time, splits, seed, date and players.
- Run history: every run's category, result (won, died, abandoned, skipped), duration, seed, and cause of failure.
- Per player: lifetime deaths, deaths by cause, deaths since the last win.

Settings are stored separately in `config/speedrun-core/settings.json`.

## Splits

Splits record when the team first reaches each milestone:

1. Enter the nether
2. Enter a fortress
3. First blaze rod
4. Enter a bastion
5. Enter the stronghold
6. Enter the end
7. Dragon killed

With the `allbosses` goal, two more splits are added: warden killed and wither killed. Boss splits are recorded in the order they happen.

Each split is announced in chat with the time difference against the category record's split, in green when ahead and red when behind. The top-of-screen timer uses the same comparison for its color.
