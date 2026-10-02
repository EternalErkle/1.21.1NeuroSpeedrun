#!/usr/bin/env bash
# Starts the server in run/. Set MEMORY to change the heap size, e.g. MEMORY=6G scripts/start.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/run" 2>/dev/null || { echo "run/ does not exist. Run scripts/setup.sh first." >&2; exit 1; }
# Aikar's flags: G1 tuned for Minecraft's short-lived allocations, to keep GC pauses short. Min and max heap are
# equal so the heap never resizes mid-game.
exec java -Xms"${MEMORY:-5632M}" -Xmx"${MEMORY:-5632M}" -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar fabric-server.jar nogui
