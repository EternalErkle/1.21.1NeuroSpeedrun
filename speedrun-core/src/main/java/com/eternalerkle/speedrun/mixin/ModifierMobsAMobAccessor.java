package com.eternalerkle.speedrun.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** enderman_rage adds a player target goal. */
@Mixin(Mob.class)
public interface ModifierMobsAMobAccessor {
	@Accessor("targetSelector")
	GoalSelector speedrun$targetSelector();
}
