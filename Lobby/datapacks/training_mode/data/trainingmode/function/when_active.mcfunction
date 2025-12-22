#Do the calculations for the timer once started
execute as @s[tag=tm_timer_running] run function trainingmode:timer/tick
execute as @s[tag=tm_wait_for_movement] run function trainingmode:movement/check