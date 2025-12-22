schedule function time:tick_20 20t
execute as @e[type=marker,name=console,tag=!sidebar_level,limit=1] unless score @s player_count matches 1 run function time:display/tick