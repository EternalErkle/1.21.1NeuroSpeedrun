tag @s add training_mode

title @s times 5 40 5
title @s title {text:""}
title @s subtitle {text:"Enabled training mode",color:"green"}

#start/stop
item replace entity @s hotbar.0 with minecraft:slime_ball[minecraft:custom_data={"tm:timer":true,"tm:timer_start":true,"tm:item":true,"hielkemaps:item":true,"tm:hand":"main"},minecraft:item_name=[{text:"Start",color:"green",bold:true},{text:" Timer",color:"white",bold:false}],consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to start the timer"}],{text:" "},{color:"gray",italic:false,text:"Swap hands to pause/resume"},{color:"gray",italic:false,text:"the timer while it\'s running"}]]

#flight
item replace entity @s hotbar.4 with minecraft:feather[minecraft:custom_data={"tm:flight_toggle":true,"tm:item":true,"hielkemaps:item":true},minecraft:item_name={text:"Toggle Flight",color:"white",bold:true},consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to toggle flight"}]]]

#save states
item replace entity @s hotbar.7 with minecraft:nether_star[minecraft:custom_data={"tm:savestate_tp":true,"tm:item":true,"hielkemaps:item":true},minecraft:item_name={text:"Tp to Savestate",color:"white",bold:true},consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to teleport to"}],{color:"gray",italic:false,text:"your saved location"}]]

item replace entity @s hotbar.8 with minecraft:heart_of_the_sea[minecraft:custom_data={"tm:savestate_set":true,"tm:item":true,"hielkemaps:item":true},minecraft:item_name={text:"Set Savestate",color:"white",bold:true},consumable={has_consume_particles:false,animation:"none",consume_seconds:2147483647},lore=[[{color:"gold",italic:false,text:"Right click"},{color:"gray",italic:false,text:" to set your Savestate"}],{text:" "},{color:"gray",italic:false,text:"Saves your current location"},{color:"gray",italic:false,text:"and rotation as a Savestate"},{text:" "},[{bold:true,color:"red",italic:false,text:"Note: "},{bold:false,color:"gray",italic:false,text:"You can only have one"}],{color:"gray",italic:false,text:"Savestate at a time"}]]