advancement revoke @s only trainingmode:click/timer/stop
execute as @s[tag=!training_mode] run return fail

tag @s remove tm_timer_running
tag @s remove tm_timer_disable
tag @s remove tm_timer_pause
tag @s remove tm_wait_for_movement

function trainingmode:timer/title/show

execute at @s run playsound minecraft:block.lever.click master @s ~ ~ ~ 0.5 1.8

#replace item
execute if items entity @s weapon.mainhand *[minecraft:custom_data~{"tm:timer_stop":true}] run return run item replace entity @s weapon.mainhand with minecraft:slime_ball[minecraft:custom_data={"tm:timer":true,"tm:timer_start":true,"tm:item":true,"hielkemaps:item":true,"tm:hand":"main"},minecraft:item_name=[{text:"Start",color:"green",bold:true},{text:" Timer",color:"white",bold:false}],consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to start the timer"}],{text:" "},{color:"gray",italic:false,text:"Swap hands to pause/resume"},{color:"gray",italic:false,text:"the timer while it\'s running"}]]
execute if items entity @s weapon.offhand *[minecraft:custom_data~{"tm:timer_stop":true}] run return run item replace entity @s weapon.offhand with minecraft:slime_ball[minecraft:custom_data={"tm:timer":true,"tm:timer_start":true,"tm:item":true,"hielkemaps:item":true,"tm:hand":"off"},minecraft:item_name=[{text:"Start",color:"green",bold:true},{text:" Timer",color:"white",bold:false}],consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to start the timer"}],{text:" "},{color:"gray",italic:false,text:"Swap hands to pause/resume"},{color:"gray",italic:false,text:"the timer while it\'s running"}]]