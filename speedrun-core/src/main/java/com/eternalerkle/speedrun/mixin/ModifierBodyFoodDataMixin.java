package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.BodyModifiers;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** vampire: no natural regeneration, as if the naturalRegeneration rule were off for that player only. */
@Mixin(FoodData.class)
public class ModifierBodyFoodDataMixin {
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
	private boolean speedrun$vampire(boolean regen, @Local(argsOnly = true) Player player) {
		return regen && !BodyModifiers.blocksNaturalRegen(player);
	}
}
