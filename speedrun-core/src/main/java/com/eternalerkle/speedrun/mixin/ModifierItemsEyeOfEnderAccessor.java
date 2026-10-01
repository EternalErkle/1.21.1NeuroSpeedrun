package com.eternalerkle.speedrun.mixin;

import net.minecraft.world.entity.projectile.EyeOfEnder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** fragile_eyes and sturdy_eyes overwrite the eye's break roll. */
@Mixin(EyeOfEnder.class)
public interface ModifierItemsEyeOfEnderAccessor {
	@Accessor("surviveAfterDeath")
	void speedrun$setSurviveAfterDeath(boolean survive);
}
