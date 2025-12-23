schedule function main:tick_2 2t

#reset trigger
scoreboard players enable @a reset
scoreboard players set @a[tag=inRace] reset 0
tag @a[scores={reset=1}] remove joined
scoreboard players set @a reset 0

#restart trigger
scoreboard players enable @a restart
scoreboard players set @a[tag=inRace] restart 0
execute as @a[scores={restart=1}] run function main:restart
scoreboard players set @a restart 0

#deep dark
effect give @a[x=56,y=12,z=-24,dx=-58,dy=-10,dz=70] resistance 2 100 true
execute as @a[x=-16,y=-46,z=-20,dx=-2,dy=0,dz=-2] run function main:tp/dark_exit

#On join
execute as @a[tag=!joined] run function main:on_join

#elytra
function main:util/elytra/tick


particle minecraft:snowflake 20 160 -4 7 2 5 0 5 normal
particle minecraft:snowflake 15 152 6 7 2 4 0 5 normal

#snake
execute if block 29 222 -8 #minecraft:wooden_pressure_plates[powered=true] run function main:snake/1/init
function main:snake/tick_2