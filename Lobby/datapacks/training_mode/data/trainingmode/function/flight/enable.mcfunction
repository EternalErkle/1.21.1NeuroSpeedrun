playsound minecraft:entity.ender_dragon.flap master @s ~ ~ ~ 0.5 1.5
tag @s add flight

execute unless score #is_server temp matches 1 run gamemode creative @s
execute if score #is_server temp matches 1 run tag @s add SERVER_ENABLE_FLIGHT

title @s actionbar [{text:"Enabled",color:"green",bold:true},{text:" flight!",color:"white",bold:false}]

execute if items entity @s weapon.mainhand *[minecraft:custom_data~{"tm:flight_toggle":true}] run return run item modify entity @s weapon.mainhand trainingmode:glint/enable

execute if items entity @s weapon.offhand *[minecraft:custom_data~{"tm:flight_toggle":true}] run item modify entity @s weapon.offhand trainingmode:glint/enable