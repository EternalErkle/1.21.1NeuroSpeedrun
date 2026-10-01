package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ItemModifiers;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** gold_rush doubles barter results. */
@Mixin(PiglinAi.class)
public class ModifierItemsPiglinAiMixin {
	@Inject(method = "getBarterResponseItems", at = @At("RETURN"), cancellable = true)
	private static void speedrun$goldRush(Piglin piglin, CallbackInfoReturnable<List<ItemStack>> cir) {
		cir.setReturnValue(ItemModifiers.barter(piglin, cir.getReturnValue()));
	}
}
