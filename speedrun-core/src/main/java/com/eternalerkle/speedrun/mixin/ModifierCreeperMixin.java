package com.eternalerkle.speedrun.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the creeper's powered flag so charged_up can charge creepers without a lightning strike. */
@Mixin(Creeper.class)
public interface ModifierCreeperMixin {
	@Accessor("DATA_IS_POWERED")
	static EntityDataAccessor<Boolean> speedrun$poweredData() {
		throw new AssertionError();
	}
}
