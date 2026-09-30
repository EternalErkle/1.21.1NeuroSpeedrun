# Roadmap

## Phase 0: spikes

Each spike answers a question the design depends on. A failed spike changes the plan before any real code is written.

| Question | If the answer is no |
|---|---|
| Does Fantasy have a 1.21.1 build that creates and deletes a full overworld, nether and end set? | Fall back to restart-based resets through the launch script |
| Can the dragon fight be initialized in a Fantasy End? | Start the fight manually through the End's dragon fight API |
| Does Polymer autohost work on 1.21.1? | Host the pack on a small separate HTTP port |
| Can the resource pack make the dimension-change screen black? | Accept a brief flash on entering the death room |
| Does an 8x8 text display face render gap-free at the death room distance? | Adjust glyph spacing, or render one text display per row |

## Phase 1: scaffold

- Gradle project with Fabric Loom, Fabric API, Fantasy and the permissions API.
- `.gitignore` excluding everything the server generates.
- packwiz manifest, `server-template/`, and `scripts/setup` plus `scripts/start` for Windows and Linux.
- CI builds the mod and runs tests on every push.

## Phase 2: core loop

- `RunManager` state machine.
- `RunWorlds` with portal routing.
- The void world with the lobby, `/start`, and the empty server rules.
- The death room with camera lock.
- `FaceCache` and the bouncing face.
- The full reset and victory sequences.

## Phase 3: HUD, stats, commands

- Resource pack with Polymer autohost.
- Top-of-screen text and the death sidebar.
- `Stats`, splits, death summaries, the clickable seed.
- Every command in [commands.md](commands.md), with `/help` generated from the registrations.

## Phase 4: settings and mods

- Tick rate setting, the `Time` helper, and record categories.
- Shared health and shared hunger toggles.
- The `allbosses` goal, boss checklist and boss splits.
- Modifier system with the pool from [modifiers.md](modifiers.md), random and vote modes.
- The corner hit.

## Phase 5: polish

- Sounds.
- `/voteskip`.
- README with hosting instructions and port forwarding notes.

## Testing

- **Unit tests** cover state transitions, time conversion at several tick rates, category keys, split comparison, bounce math, and stats serialization.
- **Fabric GameTest** covers:
  - a death during RUNNING triggering exactly one reset
  - a second death during RESETTING being ignored
  - inventories and ender chests being cleared
  - a new seed being used
  - an empty server losing the run after the grace period
  - the `allbosses` goal winning only after all three boss kills
  - a rejoin during the grace period keeping the run
  - a player joining mid-run landing only in the run overworld
  - nether portals routing between the run's own overworld and nether

  The suite lives in `speedrun-core/src/gametest` and runs with `./gradlew runGametest`, which takes about three minutes because world generation and the lifecycle timers run in real time. It is not part of `check`. The run sets `-Dspeedrun.emptyGraceSeconds=10` so the grace test does not wait a full minute.
- **Manual checks with two clients** cover the death room visuals, shared health, portal routing, and a full win.

## Open questions

- **Restarts mid-run.** When the server restarts during a run, should it resume the same worlds (Fantasy persistent worlds, timer restored) or treat the restart as a reset? Resuming is more work but protects long runs.
- **Solo voteskip.** With one player online, `/voteskip` passes instantly. Confirm that is wanted.
