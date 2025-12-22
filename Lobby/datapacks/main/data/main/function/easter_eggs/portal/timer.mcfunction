scoreboard players add @s portal_timer 1

execute if score @s portal_timer matches 1 run fill -1 192 -49 -2 194 -49 nether_portal[axis=x]
execute if score @s portal_timer matches 1 run playsound minecraft:entity.blaze.shoot block @a -1 193 -48.5
execute if score @s portal_timer matches 200.. run function main:easter_eggs/portal/reset