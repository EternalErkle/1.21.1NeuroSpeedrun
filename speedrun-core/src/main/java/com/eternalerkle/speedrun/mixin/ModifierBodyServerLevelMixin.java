package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.BodyModifiers;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** big_booms: every explosion in a run world has double the power. All server explosions go through this override. */
@Mixin(ServerLevel.class)
public class ModifierBodyServerLevelMixin {
	@ModifyVariable(method = "explode", at = @At("HEAD"), argsOnly = true)
	private float speedrun$bigBooms(float radius) {
		return BodyModifiers.scaleExplosion((ServerLevel) (Object) this, radius);
	}
}
