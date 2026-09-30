# Builds speedrun-core and assembles a runnable server in run\.
# Safe to re-run: it rebuilds the mod and updates jars, but never overwrites run\server.properties or worlds.
$ErrorActionPreference = 'Stop'

$McVersion = '1.21.1'
$LoaderVersion = '0.19.5'
$InstallerVersion = '1.1.2'

$Root = Split-Path -Parent $PSScriptRoot
$Run = Join-Path $Root 'run'

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
	Write-Error 'Java 21 or newer is required. Install it from https://adoptium.net and run this again.'
}
$versionLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($versionLine -notmatch 'version "(\d+)') { Write-Error "Could not read the Java version from: $versionLine" }
if ([int]$Matches[1] -lt 21) {
	Write-Error "Java $($Matches[1]) found, but Java 21 or newer is required. Install it from https://adoptium.net."
}

New-Item -ItemType Directory -Force (Join-Path $Run 'mods') | Out-Null

Write-Host 'Building speedrun-core...'
Push-Location (Join-Path $Root 'speedrun-core')
try {
	& .\gradlew.bat --quiet build
	if ($LASTEXITCODE -ne 0) { Write-Error 'The speedrun-core build failed.' }
} finally {
	Pop-Location
}
Get-ChildItem (Join-Path $Run 'mods') -Filter 'speedrun-core-*.jar' | Remove-Item -Confirm:$false
Get-ChildItem (Join-Path $Root 'speedrun-core\build\libs') -Filter 'speedrun-core-*.jar' |
	Where-Object { $_.Name -notlike '*-sources*' } | Select-Object -First 1 |
	Copy-Item -Destination (Join-Path $Run 'mods')

Write-Host 'Downloading the Fabric server launcher...'
Invoke-WebRequest -UseBasicParsing -OutFile (Join-Path $Run 'fabric-server.jar') `
	"https://meta.fabricmc.net/v2/versions/loader/$McVersion/$LoaderVersion/$InstallerVersion/server/jar"

Write-Host 'Downloading mods...'
foreach ($line in Get-Content (Join-Path $Root 'server-template\mods.txt')) {
	$line = $line.Trim()
	if ($line -eq '' -or $line.StartsWith('#')) { continue }
	$name, $url = $line -split '\s+', 2
	$target = Join-Path $Run "mods\$name"
	if (-not (Test-Path $target)) {
		Write-Host "  $name"
		Invoke-WebRequest -UseBasicParsing -OutFile $target $url
	}
}

$properties = Join-Path $Run 'server.properties'
if (-not (Test-Path $properties)) {
	Copy-Item (Join-Path $Root 'server-template\server.properties') $properties
}

$eula = Join-Path $Run 'eula.txt'
if (-not ((Test-Path $eula) -and (Select-String -Quiet -Path $eula -Pattern '^eula=true'))) {
	Write-Host ''
	Write-Host "Minecraft's EULA must be accepted to run a server: https://aka.ms/MinecraftEULA"
	$answer = Read-Host 'Do you accept it? [y/N]'
	if ($answer -match '^[yY]$') {
		Set-Content -Path $eula -Value 'eula=true'
	} else {
		Write-Host 'Not accepted. Setup finished, but the server will not start until eula.txt says eula=true.'
	}
}

Write-Host ''
Write-Host 'Setup done. Start the server with scripts\start.bat'
