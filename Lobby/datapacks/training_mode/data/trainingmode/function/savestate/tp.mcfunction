advancement revoke @s only trainingmode:click/savestate/tp
execute as @s[tag=!training_mode] run return fail

execute if score @s tm_delay matches 0.. run return fail
scoreboard players set @s tm_delay 0

#Find player's marker
execute as @e[type=marker,tag=tm_marker] if score @s tm_uuid = @p tm_uuid if function trainingmode:savestate/set_tped run tp @p @s
execute as @s[tag=!tm_tped] run tellraw @s {text:"You need to add a Savestate first!",color:"gray"}
tag @s remove tm_tped

effect give @s resistance 1 100 true
effect give @s instant_health 1 100 true

execute as @s[tag=!tm_timer_pause,tag=tm_timer_running] run function trainingmode:savestate/after_tp

execute at @s run playsound minecraft:entity.enderman.teleport master @s ~ ~ ~ 0.5 2