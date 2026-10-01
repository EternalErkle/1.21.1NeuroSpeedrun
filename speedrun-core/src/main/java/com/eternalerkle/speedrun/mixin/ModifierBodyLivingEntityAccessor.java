package com.eternalerkle.speedrun.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the run manager run vanilla's totem check, which Fabric's death event skips once a death is cancelled. */
@Mixin(LivingEntity.class)
public interface ModifierBodyLivingEntityAccessor {
	@Invoker("checkTotemDeathProtection")
	boolean speedrun$checkTotemDeathProtection(DamageSource source);
}
