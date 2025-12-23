execute unless score #is_server temp matches 1 run team join main @s
tag @s remove finished
tag @s remove afk

scoreboard players reset @s time
scoreboard players reset @s time_tick
scoreboard players reset @s time_display

#if player has no PB
execute unless score @s time_pb = @s time_pb run scoreboard players set @s time_pb 2147483647