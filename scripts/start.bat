@echo off
rem Starts the server in run\. Set MEMORY to change the heap size, e.g. set MEMORY=6G
if not exist "%~dp0..\run\fabric-server.jar" (
	echo run\ is not set up. Run scripts\setup.bat first.
	pause
	exit /b 1
)
if "%MEMORY%"=="" set MEMORY=5632M
cd /d "%~dp0..\run"
rem Aikar's flags: G1 tuned for Minecraft's short-lived allocations, to keep GC pauses short. Min and max heap are
rem equal so the heap never resizes mid-game.
java -Xms%MEMORY% -Xmx%MEMORY% -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=30 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -jar fabric-server.jar nogui
pause
