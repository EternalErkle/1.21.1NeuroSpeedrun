advancement revoke @s only trainingmode:click/savestate/set
execute as @s[tag=!training_mode] run return fail

execute if score @s tm_delay matches 0.. run return fail
scoreboard players set @s tm_delay 0

# Find player's marker
execute as @e[type=marker,tag=tm_marker] if score @s tm_uuid = @p tm_uuid run tag @s add tm_found

# If no marker, create one
execute unless entity @e[type=marker,tag=tm_marker,tag=tm_found] run function trainingmode:savestate/summon

# As found marker
execute as @e[type=marker,tag=tm_marker,tag=tm_found] run function trainingmode:savestate/set_marker

# Tellraw message
execute store result score @s tm_x run data get entity @s Pos[0] 1000
execute store result score @s tm_y run data get entity @s Pos[1] 1000
execute store result score @s tm_z run data get entity @s Pos[2] 1000
execute store result score @s tm_dx run data get entity @s Rotation[0] 1000
execute store result score @s tm_dy run data get entity @s Rotation[1] 1000

# Store in storage to get rid of too many numbers
execute store result storage main tempx float 0.001 run scoreboard players get @s tm_x
execute store result storage main tempy float 0.001 run scoreboard players get @s tm_y
execute store result storage main tempz float 0.001 run scoreboard players get @s tm_z
execute store result storage main tempdx float 0.001 run scoreboard players get @s tm_dx
execute store result storage main tempdy float 0.001 run scoreboard players get @s tm_dy

execute at @s run playsound minecraft:block.beacon.power_select master @s ~ ~ ~ 0.5 2

tellraw @s [{text:""}]
tellraw @s [{text:""}]
tellraw @s [{text:"Savestate set to:",color:"yellow",bold:true}]
tellraw @s [{text:"X: ",color:"gray"}, {"storage":"main","nbt":"tempx",color:"white"}]
tellraw @s [{text:"Y: ",color:"gray"}, {"storage":"main","nbt":"tempy",color:"white"}]
tellraw @s [{text:"Z: ",color:"gray"}, {"storage":"main","nbt":"tempz",color:"white"}]
tellraw @s [{text:"RX: ",color:"gray"}, {"storage":"main","nbt":"tempdx",color:"white"}]
tellraw @s [{text:"RY: ",color:"gray"}, {"storage":"main","nbt":"tempdy",color:"white"}]
tellraw @s [{text:""}]