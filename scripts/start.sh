#!/usr/bin/env bash
# Starts the server in run/. Set MEMORY to change the heap size, e.g. MEMORY=6G scripts/start.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/run" 2>/dev/null || { echo "run/ does not exist. Run scripts/setup.sh first." >&2; exit 1; }
exec java -Xms2G -Xmx"${MEMORY:-4G}" -XX:+UseG1GC -jar fabric-server.jar nogui
