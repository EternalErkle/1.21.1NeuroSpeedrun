package com.eternalerkle.speedrun.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** short_fuse sets the creeper fuse length. */
@Mixin(Creeper.class)
public interface ModifierMobsACreeperAccessor {
	@Accessor("maxSwell")
	void speedrun$setMaxSwell(int maxSwell);
}
