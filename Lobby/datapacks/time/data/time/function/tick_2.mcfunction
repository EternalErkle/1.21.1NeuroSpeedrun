schedule function time:tick_2 2t
execute as @a[tag=end_timer] at @s run function time:end/timer

execute as @e[type=marker,name=console,tag=sidebar_level,limit=1] unless score @s player_count matches 1 run function time:display/tick