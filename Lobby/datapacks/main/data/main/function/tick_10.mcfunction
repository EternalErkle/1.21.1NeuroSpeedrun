#runs twice a second
schedule function main:tick_10 10t

effect give @a minecraft:saturation infinite 10 true

#Reset scores at spawn
scoreboard players reset @a[x=0,y=52,z=-97,distance=..5,tag=!inRace] time
scoreboard players reset @a[x=0,y=52,z=-97,distance=..5,tag=!inRace] time_tick
tag @a[x=0,y=52,z=-97,distance=..5,tag=!inRace] remove ingame

# thanks for player reset
execute as @e[type=marker,name=console,tag=finish] unless entity @a[x=-60,y=228,z=-60,dx=120,dy=30,dz=120,gamemode=!spectator] run function main:win/reset

#kill items
execute as @e[type=item] unless items entity @s contents *[minecraft:custom_data~{"hielkemaps:stay":true}] run kill