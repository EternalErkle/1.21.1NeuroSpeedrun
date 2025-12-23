advancement revoke @s only trainingmode:click/flight/toggle
execute as @s[tag=!training_mode] run return fail

execute if score @s tm_delay matches 0.. run return fail
scoreboard players set @s tm_delay 0


execute as @s[tag=flight] run return run function trainingmode:flight/disable
function trainingmode:flight/enable