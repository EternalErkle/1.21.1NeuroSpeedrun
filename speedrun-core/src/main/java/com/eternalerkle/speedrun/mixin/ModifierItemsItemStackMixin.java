package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** unbreakable and fragile_tools. Only items used by players; mob gear wears as usual. */
@Mixin(ItemStack.class)
public class ModifierItemsItemStackMixin {
	@Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"), cancellable = true)
	private void speedrun$unbreakable(int amount, ServerLevel level, @Nullable ServerPlayer player, Consumer<Item> onBreak, CallbackInfo ci) {
		if (player != null && Modifiers.isActive(ModifierCatalog.UNBREAKABLE, level)) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int speedrun$fragileTools(int amount, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) @Nullable ServerPlayer player) {
		return player != null && amount > 0 && Modifiers.isActive(ModifierCatalog.FRAGILE_TOOLS, level) ? amount * 4 : amount;
	}
}
