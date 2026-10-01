package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ItemModifiers;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Item bans: empties the crafting result where it is stored, so the result slot and the packet sent to the client
 * agree. Covers the 2x2 inventory grid too, which calls the same method.
 */
@Mixin(CraftingMenu.class)
public class ModifierItemsCraftingMenuMixin {
	@ModifyVariable(method = "slotChangedCraftingGrid", at = @At("STORE"), ordinal = 0)
	private static ItemStack speedrun$bannedCraft(ItemStack result, @Local(argsOnly = true) Player player) {
		return ItemModifiers.filterCraft(result, player);
	}
}
