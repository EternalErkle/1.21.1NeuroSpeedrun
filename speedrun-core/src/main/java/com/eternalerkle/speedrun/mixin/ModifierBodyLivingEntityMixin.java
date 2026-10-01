package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.BodyModifiers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** fire_weakness, knockback_chaos, short_breath and undead's inverted healing. */
@Mixin(LivingEntity.class)
public class ModifierBodyLivingEntityMixin {
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float speedrun$fireWeakness(float amount, @Local(argsOnly = true) DamageSource source) {
		return BodyModifiers.scaleIncomingDamage((LivingEntity) (Object) this, source, amount);
	}

	@ModifyVariable(method = "knockback", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private double speedrun$knockbackChaos(double strength) {
		return BodyModifiers.scaleKnockback((LivingEntity) (Object) this, strength);
	}

	@ModifyReturnValue(method = "decreaseAirSupply", at = @At("RETURN"))
	private int speedrun$shortBreath(int after, @Local(argsOnly = true) int before) {
		return BodyModifiers.scaleAirLoss((LivingEntity) (Object) this, before, after);
	}

	@ModifyReturnValue(method = "isInvertedHealAndHarm", at = @At("RETURN"))
	private boolean speedrun$undead(boolean inverted) {
		return inverted || BodyModifiers.invertsHealing((LivingEntity) (Object) this);
	}
}
