# NeuroSpeedrun Revamp

A full rewrite of the NeuroSpeedrun server. The old build was a Paper server where all game logic lived in ConditionalEvents YAML and PlaceholderAPI JavaScript. It had race conditions, broken commands, and roughly 900MB of generated server data committed to git.

The new build is a single Fabric mod, `speedrun-core`, running on a Fabric 1.21.1 server. Third-party Fabric mods can be dropped in alongside it.

## Documents

| File | Covers |
|---|---|
| [architecture.md](architecture.md) | Platform, dependencies, repo layout, code structure |
| [run-lifecycle.md](run-lifecycle.md) | Run states, death screen, world regeneration, wins, joins |
| [hud-and-resource-pack.md](hud-and-resource-pack.md) | Top-of-screen text, death sidebar, face rendering, resource pack |
| [commands.md](commands.md) | Every command, permissions, `/help` |
| [settings-and-mods.md](settings-and-mods.md) | Tick rate, shared health toggle, third-party mod support, stats |
| [modifiers.md](modifiers.md) | The run modifier pool |
| [roadmap.md](roadmap.md) | Build phases, testing, open questions |

## Fixed decisions

- Minecraft 1.21.1 on Fabric, Java 21.
- Run worlds are created and deleted at runtime with the Fantasy library. The server never restarts to reset.
- After a death or win, players wait in a tiny black room while new worlds generate, then go straight into the next run.
- A lobby exists only for starting fresh: on server startup, and after a run is lost because everyone left. Players start the run with `/start`.
- Every client-visible feature works on an unmodded client plus the server resource pack.
- Shared health and shared hunger are separate toggles, not random modifiers.
- The win condition is a setting: the ender dragon alone, or the dragon, warden and wither.
- Out of scope: Discord webhooks, Twitch integration, a web stats page.
