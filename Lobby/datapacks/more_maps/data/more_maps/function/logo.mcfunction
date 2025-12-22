# Sets frame from 1000 - 1039
scoreboard players add @s mm_value 1
execute as @s store result entity @s Item.components."minecraft:map_id" int 1 run scoreboard players get @s mm_value
scoreboard players set @s[scores={mm_value=1039}] mm_value 999