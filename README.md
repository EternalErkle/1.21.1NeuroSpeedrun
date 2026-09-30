# NeuroSpeedrun

A cooperative hardcore speedrun server for Minecraft 1.21.1. Everyone shares one life: when any player dies, the run ends for everybody, a new world generates, and the next attempt starts on its own. The server never restarts to reset.

Players join with an unmodded 1.21.1 client. The server sends a small resource pack on join.

Everything runs from one Fabric mod, `speedrun-core`. Design documents are in [docs/](docs/README.md).

## Hosting

### Requirements

- Java 21 or newer, from [Adoptium](https://adoptium.net).
- 4 GB of free memory for the server. More helps at high tick rates.
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

The server uses 4 GB by default. Set `MEMORY`, for example `MEMORY=6G`, to change it.

When the console prints `Done`, join at `localhost`. Players arrive in the lobby. Anyone there can type `/start`.

### Letting friends join

- **Same network:** friends join with your local IP address, such as `192.168.1.20`.
- **Over the internet:** forward TCP port 25565 on your router to your computer. Friends then join with your public IP.
- **Resource pack:** the pack is served from the Minecraft port itself, so it needs no extra port.
- **No port forwarding:** a tunnel such as [playit.gg](https://playit.gg) works without router changes.

`online-mode=true` is the default, so every player needs a real Minecraft account.

### Admins

Admin commands need op level 2. Give it from the server console:

```
op <name>
```

With a permissions mod such as LuckPerms installed, grant `speedrun.admin` instead.

Type `/help` in game for every command. The ones admins use most:

| Command | Effect |
|---|---|
| `/speedrun settings` | Show every setting and the record category it produces |
| `/speedrun goal dragon\|allbosses` | Set the win condition |
| `/speedrun tickrate <rate>` | Change game speed for the next run |
| `/speedrun sharedhealth on\|off` | Share one health bar between everyone |
| `/speedrun modifiers mode off\|random\|vote` | Turn run modifiers on |
| `/speedrun reset` | End the current run and start a new seed |

Settings and stats are saved in `run/config/speedrun-core/`.

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
