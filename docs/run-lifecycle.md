# Run Lifecycle

## States

```
   server start / run lost
              |
              v
          +-------+   /start, worlds ready   +---------+
          | LOBBY | -----------------------> | RUNNING |
          +-------+                          +---------+
              ^                               |   |   |
              |   everyone left (after grace) |   |   |  goal complete
              +-------------------------------+   |   +-----> VICTORY
                                                  |              |
                               player death       v              |
                                           RESETTING <-----------+
                                                  |
                                                  +----> RUNNING
```

- **LOBBY** is where players wait before a run starts. The server enters it on startup and after a run is lost to an empty server. The next run's worlds generate in the background while players wait.
- **RUNNING** is a live run with the timer counting.
- **RESETTING** starts on the first death. Every later death, damage event, or boss kill is ignored until the next run begins. No time windows are used. It ends by starting the next run directly, not by returning to the lobby.
- **VICTORY** starts when the run's goal is complete (see [Goals](#goals)). It records the result, then moves into the same regeneration flow as a reset.

Only `RunManager` changes state. Each transition runs its steps in order and moves on when the previous step finishes, never after a fixed delay.

## The void world

The server's default world is a void holding two small structures: the death room and the lobby. It is never deleted, and its few chunks stay loaded permanently through the `spawnChunkRadius` gamerule. Mob spawning, weather and the daylight cycle are off, so it costs almost nothing per tick. Keeping both structures in this one preloaded world means moving players into either is near-instant.

## The lobby

The lobby is a small lit platform where players wait for friends before starting.

- Players are in adventure mode. They take no damage and hunger does not drain.
- A clickable `[Start run]` message and the `/start` command begin the run. Any player in the lobby can start it.
- If the next run's worlds are still generating when someone starts, the top-of-screen text shows `Generating...` and the run begins as soon as they are ready.
- Settings changed in the lobby apply to the run that starts next. Worlds are regenerated if a setting affects them.
- In vote mode, the modifier vote runs in the lobby and closes when the run starts.

## Empty server

What happens when every player leaves depends on the keep run setting (`/speedrun keeprun`).

With keep run on, the run pauses. The run timer stops and the server's tick rate manager is frozen, so the time of day, weather, mobs, crops and furnaces do not move. The first player to join unfreezes the server and continues the run in the same world, at their logout position with their inventory.

## Surviving a restart

Run worlds are Fantasy persistent worlds, saved under `world/dimensions/speedrun/`. With keep run on, the rest of the run is written to `config/speedrun-core/current-run.json` every 30 seconds, when the run pauses, and on shutdown: world id, seed, spawn, attempt, category settings, elapsed time, splits, bosses killed, the overworld's time and weather (Fantasy keeps those in memory only), the dragon fight, and feature state such as `mob_randomizer` drops and who already got the starter kit.

On startup, if keep run is on and that file points at worlds still on disk, the worlds are reopened and the run resumes in RUNNING, paused and frozen until someone joins. Players load straight into the run world from their own player data. Every other folder under `dimensions/speedrun/` is deleted, including the prepared next world, which is generated again.

With keep run off, the file is deleted and a run in progress ends as `Server stopped`; its worlds are deleted on the next start.

With keep run off, the default, a run in progress is lost when every player has left.

1. The last player leaves. A 60 second grace period starts, so a crash and quick reconnect does not end the run. The timer keeps running through it.
2. If anyone rejoins within the grace period, the run continues as normal.
3. If nobody does, the run is recorded as lost with the cause `Abandoned`. The attempt counter goes up, and no player's death count changes.
4. The run worlds are deleted and the next set starts generating.
5. The server enters LOBBY. The next player to join lands there.

If everyone leaves during RESETTING or VICTORY, the regeneration finishes and the server enters LOBBY instead of starting a run.

## The death room

The death room is a small sealed room of black blocks in the void world. Players are there only while a new run is being prepared after a death or win.

When players arrive:

1. They switch to spectator mode.
2. Their camera is locked onto a fixed invisible entity with `/spectate`. The lock is reapplied every tick, so sneaking cannot break it.
3. Spectator mode hides the hotbar, so the screen shows only black and the face.

Moving between dimensions always shows the client a short loading screen. The room is tiny and preloaded, so that screen lasts a fraction of a second, and the resource pack recolors it black so it blends in.

## The bouncing face

The face is a flat 2D image of the front of the player's skin, with the hat layer applied. It moves in straight lines and bounces off the edges of the screen, like the old DVD logo. It never changes color.

- `FaceCache` downloads each player's skin when they join, so the face is ready before anyone dies.
- The face is drawn by a text display entity: 8 rows of 8 square pixel characters, each colored to match one skin pixel. The background and text shadow are off. Rendering details are in [hud-and-resource-pack.md](hud-and-resource-pack.md).
- The display moves every tick with interpolation, so motion is smooth.
- Bounce bounds are tuned for the default 70 FOV. At other FOVs the turning point lands slightly inside or outside the visible edge.
- When the face lands exactly in a corner, a sound plays, confetti particles burst, and chat announces the corner hit.

## Reset sequence

1. A player dies. `RunManager` enters RESETTING.
2. That player's death count goes up by one on the sidebar.
3. Everyone is moved into the death room, and the dead player's face starts bouncing.
4. Chat shows the death summary:
   - who died
   - cause and killer
   - run time
   - coordinates
   - the player's last chat message
5. Chat shows the finished run's seed as a clickable message that copies it.
6. The old overworld, nether and end are deleted.
7. New worlds are created with a fresh random seed and the current settings.
8. The spawn area is generated and loaded, so the new run starts without lag.
9. The room stays up for a minimum time (a setting, default 5 seconds) and until step 8 finishes, whichever takes longer.
10. A 3-2-1 countdown plays as a title.
11. Everyone is reset:
    - teleported to the new overworld spawn
    - survival mode
    - full health and hunger
    - empty inventory and ender chest
    - no XP, effects or advancements
12. The overworld's time is set to sunrise and its weather cleared. The worlds were generated during the previous run, so their clock has already moved on by then.
13. Active modifiers are applied, the timer starts, and state becomes RUNNING.

All durations are set in seconds and converted to ticks at the current tick rate, so the sequence looks the same at 20 TPS or 100 TPS.

## Goals

The goal decides what wins a run. It is a setting, changed with `/speedrun goal`, and part of the record category.

| Goal | Win condition |
|---|---|
| `dragon` | Kill the ender dragon. The default. |
| `allbosses` | Kill the ender dragon, the warden and the wither, in any order |

In `allbosses`:

- Each boss counts once, the first time one dies in a run world. Deaths from any cause count, including lava or fall damage. A warden that digs back underground has not died and does not count.
- Killing the dragon does not end the run. Players leave the End through the exit portal and continue.
- A boss checklist appears on the second line of the top-of-screen text, for example `Dragon ✔ · Warden ✘ · Wither ✘`.
- Each boss kill is a split.
- The run is won the moment the last of the three dies.

## Victory sequence

1. The goal is complete. `RunManager` enters VICTORY.
2. The timer stops. Real time, in-game time and splits are recorded.
3. If the run beats the record for its category, a new-record sound and title play for everyone.
4. A victory title shows the final time for a few seconds, where the final boss died.
5. Death counts on the sidebar reset to zero.
6. The flow continues from step 3 of the reset sequence. The face in the room belongs to the player who dealt the final blow to the last boss.

## Joining and leaving

- A player who joins during RUNNING goes straight to the current run's overworld spawn in survival.
- A player who joins during LOBBY lands in the lobby.
- A player who joins during RESETTING or VICTORY waits in the death room with everyone else.
- Leaving hides a player's sidebar row. Their death count is kept and shown again when they rejoin.
- When the last player leaves during RUNNING, the empty server rules apply.

## Portals

Fantasy worlds are not linked by vanilla portal logic. `RunWorlds` routes travel itself:

- A nether portal in the run overworld leads to the run nether, and back, with the vanilla 8:1 coordinate scaling.
- An end portal leads to the run end.
- The end exit portal returns the player to the run overworld spawn.

The dragon fight must be initialized in the runtime End. This is checked in Phase 0.
