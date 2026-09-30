# Architecture

## Platform

Paper cannot load CurseForge mods, and hybrid servers such as Mohist or Arclight break too often to build on. The server therefore runs Fabric Loader on Minecraft 1.21.1 with Java 21.

All game logic lives in one server-side mod, `speedrun-core`. Players need no client mod. The server resource pack is the only thing they receive.

## Dependencies

| Dependency | Purpose | Verify in Phase 0 |
|---|---|---|
| Fabric Loader + Fabric API | Mod loading, events, command registration | No |
| Fantasy (NucleoidMC) | Create and delete run dimensions at runtime | Yes: a 1.21.1 build exists |
| Polymer autohost (Patbox) | Serve the resource pack over the game port | Yes: module available for 1.21.1 |
| fabric-permissions-api (lucko) | Permission nodes, falling back to op level | No |
| Shared Life (CurseForge) | Shared health and hunger, toggled by command | Yes: how it switches on and off at runtime |
| packwiz | Pinned mod list, including CurseForge mods | No |

The original Shared Health and Hunger mod by neddslayer stops at 1.20.4, so Shared Life is the 1.21.1 replacement. If Shared Life cannot be switched off while the server runs, `speedrun-core` implements shared health and hunger itself behind the same command.

## Repo layout

```
speedrun-core/            the Fabric mod (Gradle + Fabric Loom)
  src/main/java/...       mod source
  src/main/resources/     fabric.mod.json, resource pack sources
  src/test/java/...       unit tests
  src/gametest/java/...   Fabric GameTest integration tests
pack/                     packwiz manifest for the server's mod list
server-template/          server.properties and other files copied on setup
scripts/                  setup and launch scripts for Windows and Linux
docs/                     these documents
.gitignore                excludes worlds, jars, logs, libraries, player data
```

Nothing the server generates is committed. `scripts/setup` downloads the Fabric server, builds `speedrun-core`, pulls the mod list through packwiz, and copies `server-template/` into a local `run/` folder that git ignores.

The rewrite lives in the same repository. The last commit of the old server is tagged `legacy`, and every old file was deleted from the tree. The old world and jar data stays in git history, so fresh clones still download it.

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
| `Commands` | Command registration, each with the metadata `/help` reads |
| `Time` | Converts seconds to ticks at the current tick rate |

## Persistence

Settings and stats live in `config/speedrun-core/`, separate from world data, so deleting a world never touches them. Files are written to a temporary file and then renamed, so a crash mid-write cannot corrupt them.
