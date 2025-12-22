playsound minecraft:entity.ender_dragon.flap master @s ~ ~ ~ 0.25 0.75
tag @s remove flight

execute unless score #is_server temp matches 1 run gamemode adventure @s
execute if score #is_server temp matches 1 run tag @s add SERVER_DISABLE_FLIGHT

effect clear @s invisibility
title @s actionbar [{text:"Disabled",color:"dark_red",bold:true},{text:" flight!",color:"white",bold:false}]

execute if items entity @s weapon.mainhand *[minecraft:custom_data~{"tm:flight_toggle":true}] run return run item modify entity @s weapon.mainhand trainingmode:glint/disable

execute if items entity @s weapon.offhand *[minecraft:custom_data~{"tm:flight_toggle":true}] run item modify entity @s weapon.offhand trainingmode:glint/disable