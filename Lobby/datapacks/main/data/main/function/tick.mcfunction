execute as @a at @s run function main:pressure_plate/set_score

#PLAYSOUNDS
execute as @a[tag=playsound_teleport] at @s run playsound minecraft:entity.enderman.teleport master @s
tag @a remove playsound_teleport

execute as @e[tag=playsound_firework] at @s run playsound minecraft:entity.firework_rocket.launch master @s ~ ~ ~ 0.2 1
tag @e remove playsound_firework

#water kill
execute as @a at @s positioned ~ 65 ~ as @s[distance=..5] run tag @s add water_damage
execute as @a[tag=ingame,tag=water_damage,gamemode=!spectator] at @s positioned ~ 51 ~ as @s[distance=..1] at @s if block ~ ~ ~ water run damage @s 1000 minecraft:drown

#ending
execute as @a[x=0,y=240,z=0,distance=..30,tag=ingame,gamemode=!spectator] at @s run function main:win/main

#timers
execute as @e[type=marker,name=console,limit=1] run function main:timer/tick

function main:easter_eggs/tick

#particle contest winners
execute as VDcharlie at @s unless entity @s[gamemode=spectator] run particle minecraft:sculk_soul ~ ~0.1 ~ 0.25 0 0.25 0.015 1 normal
execute as puyanpanman123 at @s unless entity @s[gamemode=spectator] run particle minecraft:falling_lava ~ ~ ~ 0.25 0 0.25 10 2 normal