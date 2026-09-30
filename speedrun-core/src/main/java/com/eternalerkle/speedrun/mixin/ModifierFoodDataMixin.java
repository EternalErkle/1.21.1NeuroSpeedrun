package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.eternalerkle.speedrun.vitals.SharedVitals;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shared health: players other than the regen carrier skip natural regeneration. Both regen branches of tick require
 * player.isHurt() and the starvation branch does not, so reporting "not hurt" turns off regen and nothing else.
 * <p>
 * snack_size: halves hunger and saturation gained. Every food source (items, cake, the Saturation effect) goes through
 * the private add(int, float), which has no player, so the owner is remembered from tick(Player), called every tick.
 */
@Mixin(FoodData.class)
public class ModifierFoodDataMixin {
	@Unique
	private Player speedrun$owner;

	@Inject(method = "tick", at = @At("HEAD"))
	private void speedrun$rememberOwner(Player player, CallbackInfo ci) {
		speedrun$owner = player;
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isHurt()Z"))
	private boolean speedrun$skipSharedRegen(Player player) {
		return player.isHurt() && !SharedVitals.skipsNaturalRegen(player);
	}

	@ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int speedrun$halveFood(int food) {
		return speedrun$snackSize() && food > 0 ? (food + 1) / 2 : food;
	}

	@ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float speedrun$halveSaturation(float saturation) {
		return speedrun$snackSize() ? saturation * 0.5F : saturation;
	}

	@Unique
	private boolean speedrun$snackSize() {
		return speedrun$owner != null && Modifiers.isActive(ModifierCatalog.SNACK_SIZE, speedrun$owner.level());
	}
}
