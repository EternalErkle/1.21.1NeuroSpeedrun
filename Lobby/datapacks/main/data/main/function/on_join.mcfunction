attribute @s minecraft:burning_time base set 0.001

tp @s 0 53.5 -97 0 0
spawnpoint @s 0 54 -97
gamemode adventure @s
tag @s remove ingame
function time:reset
tag @s add joined
clear @s *[minecraft:custom_data~{"hielkemaps:item":true}]
tag @s remove water_damage

execute as @s[tag=training_mode] run function trainingmode:leave