tag @s remove tm_timer_pause
execute at @s run playsound minecraft:block.note_block.snare master @s ~ ~ ~ 0.3 1

execute if items entity @s weapon.mainhand *[minecraft:custom_data~{"tm:timer_stop":true}] run item modify entity @s weapon.mainhand trainingmode:glint/disable
execute if items entity @s weapon.offhand *[minecraft:custom_data~{"tm:timer_stop":true}] run item modify entity @s weapon.offhand trainingmode:glint/disable

return 1