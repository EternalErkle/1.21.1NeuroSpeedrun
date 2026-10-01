package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ItemModifiers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** loot_chaos, jackpot, ore_rush and orchard. Every block drop, from mining or explosions, goes through here. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class ModifierItemsBlockStateMixin {
	@Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
	private void speedrun$blockDrops(LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
		cir.setReturnValue(ItemModifiers.blockDrops((BlockState) (Object) this, params, cir.getReturnValue()));
	}
}
