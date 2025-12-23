scoreboard players add @a[tag=ingame,tag=!afk] time_tick 1
scoreboard players add @a[scores={time_tick=20..}] time 1
scoreboard players reset @a[scores={time_tick=20..}] time_tick

execute as @a run function time:triggers

execute as @e[type=marker,name=console,limit=1,scores={player_count=1}] run function time:display/tick