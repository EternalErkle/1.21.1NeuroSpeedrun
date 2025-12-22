scoreboard players add @s firework_timer 1

execute if score @s firework_timer matches 1 run summon firework_rocket ~ ~5 ~ {LifeTime:20,FireworksItem:{id:"minecraft:firework_rocket",count:1,components:{"minecraft:fireworks":{explosions:[{shape:"star",has_trail:true,colors:[I;16766208],fade_colors:[I;16777207]}]}}}}

execute if score @s firework_timer matches 5.. run function time:firework/position/x_offset
execute if score @s firework_timer matches 100.. run function time:firework/reset