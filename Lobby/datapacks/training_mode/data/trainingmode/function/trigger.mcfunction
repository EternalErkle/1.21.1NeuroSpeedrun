scoreboard players set @s training_mode 0
execute as @s[tag=training_mode] run return run function trainingmode:leave
function trainingmode:join