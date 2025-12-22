scoreboard players add @a[scores={tm_delay=0..}] tm_delay 1
scoreboard players reset @a[scores={tm_delay=5..}] tm_delay

#toggle training mode
scoreboard players enable @a training_mode
scoreboard players set @a[tag=inRace] training_mode 0
execute as @a[scores={training_mode=1..}] run function trainingmode:trigger
execute as @a[tag=training_mode] run function trainingmode:when_active
execute as @a[tag=inRace] as @s[tag=training_mode] run function trainingmode:leave

#hide flying players
effect give @a[tag=flight] invisibility 1 10 true