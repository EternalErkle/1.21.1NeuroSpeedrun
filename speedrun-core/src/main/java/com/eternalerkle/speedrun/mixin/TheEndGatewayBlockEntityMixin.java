package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.world.RunWorldSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** End gateways only create their exit in the vanilla end. Treat the run end the same way. */
@Mixin(TheEndGatewayBlockEntity.class)
public class TheEndGatewayBlockEntityMixin {
	@Redirect(method = "getPortalPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
	private ResourceKey<Level> speedrun$treatRunEndAsEnd(ServerLevel level) {
		if (SpeedrunCore.runs() != null) {
			RunWorldSet set = SpeedrunCore.runs().worlds().current();
			if (set != null && level == set.end()) {
				return Level.END;
			}
		}
		return level.dimension();
	}
}
