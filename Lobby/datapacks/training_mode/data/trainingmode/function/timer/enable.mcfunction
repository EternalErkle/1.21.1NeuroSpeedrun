advancement revoke @s only trainingmode:click/timer/start
execute as @s[tag=!training_mode] run return fail

tag @s remove tm_wait_for_movement

tag @s add tm_timer_running
tag @s remove tm_timer_enable

scoreboard players set @s tm_hours 0 
scoreboard players set @s tm_minutes 0 
scoreboard players set @s tm_seconds 0 
scoreboard players set @s tm_ticks 0 

execute at @s run playsound minecraft:block.lever.click master @s ~ ~ ~ 0.5 2

#replace item
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{"tm:timer_start":true}] run return run item replace entity @s weapon.mainhand with minecraft:magma_cream[minecraft:custom_data={"tm:timer":true,"tm:timer_stop":true,"tm:item":true,"hielkemaps:item":true,"tm:hand":"main"},minecraft:item_name=[{text:"Stop",color:"red",bold:true},{text:" Timer",color:"white",bold:false}],consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to stop the timer"}],{text:" "},{color:"gray",italic:false,text:"Swap hands to pause/resume"}]]
execute if items entity @s weapon.offhand *[minecraft:custom_data~{"tm:timer_start":true}] run return run item replace entity @s weapon.offhand with minecraft:magma_cream[minecraft:custom_data={"tm:timer":true,"tm:timer_stop":true,"tm:item":true,"hielkemaps:item":true,"tm:hand":"off"},minecraft:item_name=[{text:"Stop",color:"red",bold:true},{text:" Timer",color:"white",bold:false}],consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to stop the timer"}],{text:" "},{color:"gray",italic:false,text:"Swap hands to pause/resume"}]]