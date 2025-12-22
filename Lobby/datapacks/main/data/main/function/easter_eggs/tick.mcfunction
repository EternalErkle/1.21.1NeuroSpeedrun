#tnt house
execute as @e[type=tnt,x=44,y=140,z=13,distance=..3,nbt={fuse:1s}] run function main:easter_eggs/house/explode


#play music disc
execute if entity @a[x=20,y=144,z=4,distance=..16] run setblock 27 142 -15 air
execute unless entity @a[x=20,y=144,z=4,distance=..16] run setblock 27 142 -15 redstone_block

#nether portal
execute if block -1 198 -40 minecraft:crimson_pressure_plate[powered=true] run tag @e[type=marker,name=console,limit=1] add portal_timer


execute if block -1 192 -49 minecraft:nether_portal as @a[x=-1,y=192,z=-49,dx=-1,dy=2,dz=0] run function main:easter_eggs/portal/player

#end
execute as @a[x=-43,y=160,z=-13,dx=42,dy=-15,dz=44,gamemode=!spectator] at @s if block ~ ~ ~ air if block ~ ~-1 ~ black_concrete run kill @s
execute as @e[type=enderman,tag=end,x=-43,y=160,z=-13,dx=42,dy=-15,dz=44] at @s if block ~ ~ ~ air if block ~ ~-1 ~ black_concrete run tp @s -20 161 5
execute as @e[type=enderman,tag=end] unless entity @s[x=-21,y=162,z=7,distance=..20] run tp @s -20 161 5

#backdoor
tag @a[x=-3,y=55,z=46,dx=5,dy=-3,dz=-3] remove ingame
advancement grant @a[x=-3,y=55,z=46,dx=5,dy=-3,dz=-3,gamemode=!spectator] only main:easter_eggs/ee10

advancement grant @a[x=5,y=59,z=-41,dx=8,dy=-3,dz=6,gamemode=!spectator] only main:easter_eggs/ee9
advancement grant @a[x=33,y=226,z=-5,dx=-4,dy=-5,dz=-4,gamemode=!spectator] only main:easter_eggs/ee8
advancement grant @a[x=-1,y=210,z=18,dx=6,dy=-6,dz=8,gamemode=!spectator] only main:easter_eggs/ee7
advancement grant @a[x=-31,y=161,z=1,dx=-6,dy=5,dz=5,gamemode=!spectator] only main:easter_eggs/ee6

advancement grant @a[x=20,y=144,z=4,distance=..16,gamemode=!spectator] only main:easter_eggs/ee4
advancement grant @a[x=29,y=116,z=-30,dx=4,dy=-3,dz=-4,gamemode=!spectator] only main:easter_eggs/ee3
advancement grant @a[x=-1,y=120,z=-2,distance=..5,gamemode=!spectator] only main:easter_eggs/ee2
tp @e[type=magma_cube,tag=larry] 33 114 -35