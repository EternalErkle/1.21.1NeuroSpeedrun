package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.DamageScaling;
import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.MobModifiers;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** glass_cannon damage scaling, plus mob_randomizer and blaze_boost loot changes. */
@Mixin(LivingEntity.class)
public class ModifierLivingEntityMixin {
	/** Vanilla's own cutoff for "this is a kill, not damage" in its damage stats. */
	@Unique
	private static final float LETHAL_LIMIT = 3.4028235E37F;

	/** Players take double damage and deal double damage, including through their projectiles. */
	@ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
	private float speedrun$glassCannon(float amount, @Local(argsOnly = true) DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!Modifiers.isActive(ModifierCatalog.GLASS_CANNON, self.level())) {
			return amount;
		}
		if (self instanceof ServerPlayer) {
			amount = DamageScaling.scale(amount, 2.0F, LETHAL_LIMIT);
		}
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != self) {
			amount = DamageScaling.scale(amount, 2.0F, LETHAL_LIMIT);
		}
		return amount;
	}

	@Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
	private void speedrun$mobRandomizer(DamageSource source, boolean playerKill, CallbackInfo ci) {
		if (MobModifiers.replaceLoot((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}

	@ModifyArg(method = "dropFromLootTable", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
		index = 2)
	private Consumer<ItemStack> speedrun$blazeBoost(Consumer<ItemStack> drop) {
		return MobModifiers.boostLoot((LivingEntity) (Object) this, drop);
	}
}
