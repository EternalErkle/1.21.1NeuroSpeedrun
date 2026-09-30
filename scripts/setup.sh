#!/usr/bin/env bash
# Builds speedrun-core and assembles a runnable server in run/.
# Safe to re-run: it rebuilds the mod and updates jars, but never overwrites run/server.properties or worlds.
set -euo pipefail

MC_VERSION=1.21.1
LOADER_VERSION=0.19.5
INSTALLER_VERSION=1.1.2

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RUN="$ROOT/run"

need_java() {
	if ! command -v java >/dev/null 2>&1; then
		echo "Java 21 or newer is required. Install it from https://adoptium.net and run this again." >&2
		exit 1
	fi
	local major
	major="$(java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1)"
	if [ -z "$major" ] || [ "$major" -lt 21 ]; then
		echo "Java $major found, but Java 21 or newer is required. Install it from https://adoptium.net." >&2
		exit 1
	fi
}

need_java
mkdir -p "$RUN/mods"

echo "Building speedrun-core..."
(cd "$ROOT/speedrun-core" && chmod +x gradlew && ./gradlew --quiet build)
rm -f "$RUN"/mods/speedrun-core-*.jar
cp "$(ls "$ROOT"/speedrun-core/build/libs/speedrun-core-*.jar | grep -v -- '-sources' | head -1)" "$RUN/mods/"

echo "Downloading the Fabric server launcher..."
curl -fsSL -o "$RUN/fabric-server.jar" \
	"https://meta.fabricmc.net/v2/versions/loader/$MC_VERSION/$LOADER_VERSION/$INSTALLER_VERSION/server/jar"

echo "Downloading mods..."
while read -r name url; do
	case "$name" in ''|'#'*) continue ;; esac
	if [ ! -f "$RUN/mods/$name" ]; then
		echo "  $name"
		curl -fsSL -o "$RUN/mods/$name" "$url"
	fi
done < "$ROOT/server-template/mods.txt"

if [ ! -f "$RUN/server.properties" ]; then
	cp "$ROOT/server-template/server.properties" "$RUN/server.properties"
fi

if ! grep -qs '^eula=true' "$RUN/eula.txt"; then
	echo
	echo "Minecraft's EULA must be accepted to run a server: https://aka.ms/MinecraftEULA"
	read -r -p "Do you accept it? [y/N] " answer
	if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
		echo "eula=true" > "$RUN/eula.txt"
	else
		echo "Not accepted. Setup finished, but the server will not start until eula.txt says eula=true."
	fi
fi

echo
echo "Setup done. Start the server with scripts/start.sh"
