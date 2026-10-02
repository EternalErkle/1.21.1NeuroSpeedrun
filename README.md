# NeuroSpeedrun

A cooperative speedrun server for Minecraft 1.21.1, based on the neuro-sama hardcore speedrun from last subathon (Dec 2025). Everyone shares one life, so when any player dies, the run ends for everybody, a new world generates, and the next attempt starts on its own.

Players join with an unmodded 1.21.1 client. The server sends a small resource pack on join.

Design documents are in [docs/](docs/README.md).

## Hosting

### Requirements

- Java 21 or newer, from [Adoptium](https://adoptium.net).
- 4 GB of free memory for the server. More helps at high tick rates. (Recommending 6GB)
- Git, to clone this repository.

The first build downloads a newer JDK for Gradle on its own. You don't need to install it.

### Setup

Windows: double-click `scripts\setup.bat`.

Linux or macOS:

```sh
scripts/setup.sh
```

Setup builds the mod, downloads the Fabric server and Fabric API into `run/`, and copies `server-template/server.properties` there. It asks you to accept the Minecraft EULA. It never overwrites `run/server.properties` or any world, so run it again after pulling updates.

### Starting

Windows: double-click `scripts\start.bat`. Linux or macOS: `scripts/start.sh`.

The server uses 5.5 GB (`5632M`) by default. Set `MEMORY`, for example `MEMORY=6G`, to change it.

When the console prints `Done`, join at `localhost`. Players arrive in the lobby. Anyone there can type `/start`.

### Letting friends join

- **Same network:** friends join with your local IP address, such as `192.168.1.20`.
- **Over the internet:** forward TCP port 25565 on your router to your computer. Friends then join with your public IP.
- **Resource pack:** the pack is served from the Minecraft port itself, so it needs no extra port.
- **No port forwarding:** a tunnel such as [playit.gg](https://playit.gg) works without router changes.

`online-mode=true` is the default, so every player needs a real Minecraft account.

### Admins

Player commands such as `/help`, `/start`, `/vote` and `/stats` need no op.

Admin commands (`/speedrun ...`) need op level 2 or a place on the trusted list. Trusted players get every `/speedrun` command but none of op's vanilla powers, so they cannot use `/gamemode` or `/give`. Manage the list from the server console or as an op:

```
speedrun trust add <name>
speedrun trust remove <name>
speedrun trust list
```

The list is saved in `run/config/speedrun-core/settings.json`. Trusted players cannot change it. With a permissions mod such as LuckPerms installed, granting `speedrun.admin` works too.

Type `/help` in game for every command. The ones admins use most:

| Command | Effect |
|---|---|
| `/speedrun settings` | Show every setting and the record category it produces |
| `/speedrun goal dragon\|allbosses` | Set the win condition |
| `/speedrun tickrate <rate>` | Change game speed for the next run |
| `/speedrun sharedhealth on\|off` | Share one health bar between everyone |
| `/speedrun modifiers mode off\|random\|vote` | Turn run modifiers on (see [Modifiers](#modifiers)) |
| `/speedrun keeprun on\|off` | Keep the run when everyone leaves or the server restarts |
| `/speedrun village on\|off` | Place a village 100 to 500 blocks from spawn in every run world (on by default) |
| `/speedrun compass on\|off` | Give every player a compass that tracks a chosen player (on by default) |
| `/speedrun reset` | End the current run and start a new seed |

Settings and stats are saved in `run/config/speedrun-core/`.

Every run starts at sunrise with clear weather.

### Continuing a run across sessions

By default, a run ends as abandoned when everyone has been offline for 60 seconds, or when the server stops. For a run that takes more than one sitting, such as the all-bosses goal, turn on keep run:

```
/speedrun keeprun on
```

When the last player leaves, the run pauses instead of ending:

- The timer stops, so offline time doesn't count toward the run's time.
- The whole server freezes: time of day, weather, mobs, crops and furnaces stay exactly as they were.
- The first player to join lands back in the same world, where they logged out, with their inventory.

The run also survives stopping or restarting the server. The run worlds are saved to disk, and the run's state (timer, splits, bosses killed, modifiers, time of day, weather) is saved to `run/config/speedrun-core/current-run.json` every 30 seconds and on shutdown. On the next start the run is reopened, paused, until someone joins. After a crash, up to 30 seconds of the timer and up to 5 minutes of world changes (the autosave interval) can be lost.

With keep run off, leftover run worlds are deleted when the server starts.

## Modifiers

Modifiers change the rules of one run: for example one heart, moon gravity, a doubled mob cap or a shared inventory. There are 22, tagged Harder, Chaos, Brutal or Helpful. Each combination of modifiers keeps its own records.

Modifiers are **off by default**. To turn them on, as op:

1. Add modifiers to the pool. Only modifiers in the pool can be picked.
   - `/speedrun modifiers enable all` adds all 22 at once. `/speedrun modifiers disable all` empties the pool.
   - `/speedrun modifiers pool` lists every modifier. Click one to add or remove it, or use `/speedrun modifiers enable <id>` and `disable <id>`.
2. Choose how they're picked with `/speedrun modifiers mode`:
   - `random`: each run draws from the pool at random.
   - `vote`: while waiting in the lobby or death room, everyone votes between two options drawn from the pool, by clicking in chat or with `/vote <number>`.
3. Optionally set how many apply per run: `/speedrun modifiers count 2`. The default is 1.

Changes take effect from the next run. To try specific ones once, use `/speedrun modifiers force <id> [id...]`, which overrides the pool for the next run only.

Anyone can run `/modifiers` to see the active modifiers, how the next run's are picked, and every modifier with its effect. Active modifiers also show under the GO! title when a run starts and under the timer at the top of the screen.

The full list with every effect is in [docs/modifiers.md](docs/modifiers.md).

## Performance mods

Setup also installs five server-side performance mods from `server-template/mods.txt`. Players don't need any of them.

| Mod | What it does |
|---|---|
| Lithium | Faster game logic with vanilla behavior |
| C2ME | Generates chunks on several CPU cores (alpha) |
| ScalableLux | Faster lighting engine (alpha) |
| FerriteCore | Lower memory use |
| spark | Profiler. Run `/spark profiler start`, reproduce the lag, then `/spark profiler stop` for a report link. |

If C2ME or ScalableLux causes problems, delete its line from `mods.txt` and its jar from `run/mods/`.

The start scripts use Aikar's flags, a widely used set of Java garbage-collector settings that keep pauses short.

## Adding mods

To add a server-side Fabric mod, put a line in `server-template/mods.txt` with the jar name and a download URL, then run setup again. You can also drop the jar into `run/mods/` directly. Mods that clients need to install won't work for players on unmodded clients.

## Development

```sh
cd speedrun-core
./gradlew build          # compile and run unit tests
./gradlew runServer      # start a dev server in speedrun-core/run/
```

The mod uses Mojang mappings. CI builds and tests every push.
