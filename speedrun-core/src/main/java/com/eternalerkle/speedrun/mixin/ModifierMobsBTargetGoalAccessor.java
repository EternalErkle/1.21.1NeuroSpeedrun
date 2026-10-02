package com.eternalerkle.speedrun.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** relentless and sniper_skeletons: the goal copies the follow range into its conditions when built, so a later bonus is pushed in. */
@Mixin(NearestAttackableTargetGoal.class)
public interface ModifierMobsBTargetGoalAccessor {
	@Accessor("targetConditions")
	TargetingConditions speedrun$targetConditions();
}
