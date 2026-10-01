package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.WorldModifiers;
import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** cursed_loot: curses gear right after a loot chest is filled. */
@Mixin(LootTable.class)
public abstract class ModifierWorldLootTableMixin {
	@Inject(method = "fill", at = @At("TAIL"))
	private void speedrun$cursedLoot(Container container, LootParams params, long seed, CallbackInfo ci) {
		WorldModifiers.afterLootFill(container, params.getLevel());
	}
}
