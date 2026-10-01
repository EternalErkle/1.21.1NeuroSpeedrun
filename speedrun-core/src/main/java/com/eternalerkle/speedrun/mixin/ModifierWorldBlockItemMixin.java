package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.WorldModifiers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** gravity_blocks: lets a freshly placed block fall. */
@Mixin(BlockItem.class)
public abstract class ModifierWorldBlockItemMixin {
	@Inject(method = "place", at = @At("RETURN"))
	private void speedrun$gravityBlocks(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction()) {
			WorldModifiers.afterPlace(context.getLevel(), context.getClickedPos(), (BlockItem) (Object) this, context.getPlayer());
		}
	}
}
