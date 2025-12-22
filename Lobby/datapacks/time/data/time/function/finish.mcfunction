advancement grant @s only main:finish

scoreboard players operation @s finaltime = @s time
function time:rank/calculate

tag @s remove ingame
tag @s add end_timer

tag @s add finished

#don't do on server because LAG
execute unless score #is_server temp matches 1 run team join finished @s

effect clear @s

#fireworks
execute as @e[type=marker,name=fireworks,limit=1] at @s run function time:firework/init