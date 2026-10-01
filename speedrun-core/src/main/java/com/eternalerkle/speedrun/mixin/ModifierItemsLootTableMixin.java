package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ItemModifiers;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * looted and treasure_hunter. LootTable.fill is what structure chests, barrels and chest minecarts use when first opened.
 * Treasure hunter rolls the table a second time into the slots that are still empty.
 */
@Mixin(LootTable.class)
public class ModifierItemsLootTableMixin {
	@Unique
	private static boolean speedrun$refilling;

	@Inject(method = "fill", at = @At("HEAD"), cancellable = true)
	private void speedrun$looted(Container container, LootParams params, long seed, CallbackInfo ci) {
		if (ItemModifiers.chestLooted(params)) {
			ci.cancel();
		}
	}

	@Inject(method = "fill", at = @At("TAIL"))
	private void speedrun$treasureHunter(Container container, LootParams params, long seed, CallbackInfo ci) {
		if (speedrun$refilling || !ItemModifiers.chestDoubled(params)) {
			return;
		}
		speedrun$refilling = true;
		try {
			// Seed 0 means random; any other seed is shifted so the second roll differs from the first.
			((LootTable) (Object) this).fill(container, params, seed == 0 ? 0 : seed + 1);
		} finally {
			speedrun$refilling = false;
		}
	}
}
