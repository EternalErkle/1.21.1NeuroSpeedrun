@echo off
rem Starts the server in run\. Set MEMORY to change the heap size, e.g. set MEMORY=6G
if not exist "%~dp0..\run\fabric-server.jar" (
	echo run\ is not set up. Run scripts\setup.bat first.
	pause
	exit /b 1
)
if "%MEMORY%"=="" set MEMORY=4G
cd /d "%~dp0..\run"
java -Xms2G -Xmx%MEMORY% -XX:+UseG1GC -jar fabric-server.jar nogui
pause
