# Architecture

## Platform

Paper cannot load CurseForge mods, and hybrid servers such as Mohist or Arclight break too often to build on. The server therefore runs Fabric Loader on Minecraft 1.21.1 with Java 21.

All game logic lives in one server-side mod, `speedrun-core`. Players need no client mod. The server resource pack is the only thing they receive.

## Dependencies

| Dependency | Purpose | Verify in Phase 0 |
|---|---|---|
| Fabric Loader + Fabric API | Mod loading, events, command registration | No |
| Fantasy (NucleoidMC) | Create and delete run dimensions at runtime | Yes: a 1.21.1 build exists |
| Polymer resource pack + autohost (Patbox) | Build the resource pack from the mod's assets and serve it over the game port | No |
| fabric-permissions-api (lucko) | Permission nodes, falling back to op level | No |

Shared health and shared hunger are built into `speedrun-core`. No third-party mod is involved.

## Repo layout

```
speedrun-core/            the Fabric mod (Gradle + Fabric Loom)
  src/main/java/...       mod source
  src/main/resources/     fabric.mod.json, resource pack sources
  src/test/java/...       unit tests
  src/gametest/java/...   Fabric GameTest integration tests
server-template/          server.properties and mods.txt, the pinned extra mod list
scripts/                  setup and launch scripts for Windows and Linux
docs/                     these documents
.gitignore                excludes worlds, jars, logs, libraries, player data
```

Nothing the server generates is committed. `scripts/setup` downloads the Fabric server, builds `speedrun-core`, downloads the mods in `server-template/mods.txt`, and copies `server-template/` into a local `run/` folder that git ignores.

The rewrite lives in the same repository. Every file from the old Paper server was deleted from the tree.

## Code structure

`RunManager` is the only class that changes run state. Every other component either reads that state or asks `RunManager` to act, which is what makes duplicate resets impossible.

| Component | Responsibility |
|---|---|
| `RunManager` | State machine, reacts to deaths, boss kills, joins, leaves |
| `Lobby` | The lobby platform, `/start`, the empty server grace period |
| `RunWorlds` | Creates and deletes the overworld, nether and end for each run through Fantasy; routes portals between them |
| `DeathRoom` | The permanent black room, camera lock, bouncing face |
| `FaceCache` | Downloads skins on join and stores each player's 8x8 face with the hat layer applied |
| `Hud` | Top-of-screen text and the death sidebar |
| `Splits` | Milestone timestamps and comparison against the best run |
| `Stats` | Records, run history, death counts and causes, saved as JSON |
| `Settings` | Goal, tick rate, shared health, shared hunger, modifier pool, saved as JSON |
| `Modifiers` | Applies and removes the active run modifiers |
| `SharedVitals` | Shared health and shared hunger for the players in a run |
| `ServerPack` | Registers the mod's assets with Polymer and enables autohost |
| `Commands` | Command registration, each with the metadata `/help` reads |
| `Time` | Converts seconds to ticks at the current tick rate |

## Persistence

Settings and stats live in `config/speedrun-core/`, separate from world data, so deleting a world never touches them. Files are written to a temporary file and then renamed, so a crash mid-write cannot corrupt them.
