tag @s remove training_mode

tag @s remove tm_timer_running
tag @s remove tm_timer_pause
tag @s remove tm_wait_for_movement

clear @s *[minecraft:custom_data~{"tm:item":true}]

tag @s remove flight
execute unless score #is_server temp matches 1 run gamemode adventure @s
execute if score #is_server temp matches 1 run tag @s add SERVER_DISABLE_FLIGHT
execute if score #is_server temp matches 1 run tag @s[tag=!inRace] remove joined

title @s times 5 40 5
title @s actionbar {text:""}
title @s title {text:""}
title @s subtitle {text:"Disabled training mode",color:"red"}