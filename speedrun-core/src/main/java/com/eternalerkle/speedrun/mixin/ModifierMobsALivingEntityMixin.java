package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobsAModifiers;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** pearl_bonanza: counts the pearls an enderman's loot table drops and tops them up to 2. */
@Mixin(LivingEntity.class)
public class ModifierMobsALivingEntityMixin {
	@Inject(method = "dropFromLootTable", at = @At("HEAD"))
	private void speedrun$pearlStart(DamageSource source, boolean playerKill, CallbackInfo ci) {
		MobsAModifiers.startLoot();
	}

	@ModifyArg(method = "dropFromLootTable", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
		index = 2)
	private Consumer<ItemStack> speedrun$pearlCount(Consumer<ItemStack> drop) {
		return MobsAModifiers.pearlLoot((LivingEntity) (Object) this, drop);
	}

	@Inject(method = "dropFromLootTable", at = @At("TAIL"))
	private void speedrun$pearlFinish(DamageSource source, boolean playerKill, CallbackInfo ci) {
		MobsAModifiers.finishLoot((LivingEntity) (Object) this);
	}
}
