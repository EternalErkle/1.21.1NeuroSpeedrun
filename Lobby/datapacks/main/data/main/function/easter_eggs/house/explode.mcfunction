kill

particle minecraft:explosion_emitter 44 142 14 2 2 2 1 5

playsound minecraft:entity.generic.explode block @a 45 140 13 2 1
place template minecraft:house_destroyed 38 132 5

execute positioned 44 140 14 as @a[distance=..8] run damage @s 15 minecraft:explosion
execute positioned 44 140 14 as @a[distance=..8,gamemode=!spectator] run advancement grant @s only main:easter_eggs/ee5

tag @e[type=marker,name=console,limit=1] add house_timer