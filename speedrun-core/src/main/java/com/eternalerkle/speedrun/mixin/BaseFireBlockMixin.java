package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.SpeedrunCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla only lights nether portals in the vanilla overworld and nether. Allow the run copies too. */
@Mixin(BaseFireBlock.class)
public class BaseFireBlockMixin {
	@Inject(method = "inPortalDimension", at = @At("HEAD"), cancellable = true)
	private static void speedrun$allowRunPortals(Level level, CallbackInfoReturnable<Boolean> cir) {
		if (level instanceof ServerLevel serverLevel && SpeedrunCore.runs() != null
			&& SpeedrunCore.runs().worlds().netherPortalTarget(serverLevel) != null) {
			cir.setReturnValue(true);
		}
	}
}
