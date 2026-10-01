package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ItemModifiers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

/** loot_chaos for mob drops. */
@Mixin(LivingEntity.class)
public class ModifierItemsLivingEntityMixin {
	@ModifyArg(method = "dropFromLootTable", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"),
		index = 2)
	private Consumer<ItemStack> speedrun$lootChaos(Consumer<ItemStack> drop) {
		return ItemModifiers.mobDrops((LivingEntity) (Object) this, drop);
	}
}
