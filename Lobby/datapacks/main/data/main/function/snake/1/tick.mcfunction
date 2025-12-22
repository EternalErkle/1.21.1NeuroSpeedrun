scoreboard players add @s snake_1 1
execute if score @s snake_1 matches 1 run setblock ~ ~ ~ minecraft:lime_concrete
execute if score @s snake_1 matches 5 run setblock ~ ~ ~ minecraft:green_concrete
execute if score @s snake_1 matches 9 run setblock ~ ~ ~ minecraft:lime_concrete
execute if score @s snake_1 matches 14 run setblock ~ ~ ~ minecraft:green_concrete
execute if score @s snake_1 matches 18 run setblock ~ ~ ~ minecraft:lime_concrete
execute if score @s snake_1 matches 22 run setblock ~ ~ ~ minecraft:green_concrete
execute if score @s snake_1 matches 26 run setblock ~ ~ ~ air
scoreboard players set @s[scores={snake_1=26}] snake_1 -20
