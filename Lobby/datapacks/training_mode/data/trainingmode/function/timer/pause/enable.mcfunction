tag @s add tm_timer_pause
execute at @s run playsound minecraft:block.note_block.snare master @s ~ ~ ~ 0.3 0.8

execute if items entity @s weapon.mainhand *[minecraft:custom_data~{"tm:timer_stop":true}] run item modify entity @s weapon.mainhand trainingmode:glint/enable
execute if items entity @s weapon.offhand *[minecraft:custom_data~{"tm:timer_stop":true}] run item modify entity @s weapon.offhand trainingmode:glint/enable

return 1