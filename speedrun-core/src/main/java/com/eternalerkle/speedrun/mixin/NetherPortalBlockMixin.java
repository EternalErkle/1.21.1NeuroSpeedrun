package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.SpeedrunCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes nether portals between the current run overworld and nether, with vanilla 8:1 scaling. */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
	@Shadow
	protected abstract DimensionTransition getExitPortal(ServerLevel serverLevel, Entity entity, BlockPos blockPos, BlockPos blockPos2, boolean bl, WorldBorder worldBorder);

	@Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
	private void speedrun$routeRunPortal(ServerLevel from, Entity entity, BlockPos portalPos, CallbackInfoReturnable<DimensionTransition> cir) {
		if (SpeedrunCore.runs() == null) {
			return;
		}
		ServerLevel target = SpeedrunCore.runs().worlds().netherPortalTarget(from);
		if (target == null) {
			return;
		}
		boolean toNether = target.dimensionType().ultraWarm();
		WorldBorder border = target.getWorldBorder();
		double scale = DimensionType.getTeleportationScale(from.dimensionType(), target.dimensionType());
		BlockPos exit = border.clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);
		cir.setReturnValue(this.getExitPortal(target, entity, portalPos, exit, toNether, border));
	}
}
