function main:timer/text/tick

execute as @s[tag=house_timer] run function main:easter_eggs/house/timer

execute as @s[tag=portal_timer] at @s run function main:easter_eggs/portal/timer

#win
execute as @a[tag=win_timer] at @s run function main:timer/win

