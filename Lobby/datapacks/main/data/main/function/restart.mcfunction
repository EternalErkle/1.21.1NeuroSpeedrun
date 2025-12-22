spawnpoint @s 0 52 -51
tp @s 0 52 -51 0 10
gamemode adventure @s
tag @s remove ingame
function time:reset
clear @s *[minecraft:custom_data~{"hielkemaps:item":true},!minecraft:custom_data~{"tm:item":true}]
tag @s remove water_damage