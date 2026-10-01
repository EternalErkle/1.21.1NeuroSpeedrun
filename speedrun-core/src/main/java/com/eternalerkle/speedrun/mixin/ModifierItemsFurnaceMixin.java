package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** instant_smelting: every recipe takes one tick. Fuel is still needed. */
@Mixin(AbstractFurnaceBlockEntity.class)
public class ModifierItemsFurnaceMixin {
	@Inject(method = "getTotalCookTime", at = @At("RETURN"), cancellable = true)
	private static void speedrun$instantSmelting(Level level, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Integer> cir) {
		if (Modifiers.isActive(ModifierCatalog.INSTANT_SMELTING, level)) {
			cir.setReturnValue(1);
		}
	}
}
