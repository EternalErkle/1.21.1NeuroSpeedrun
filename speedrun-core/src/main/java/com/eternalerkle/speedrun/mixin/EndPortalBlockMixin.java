package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.world.RunWorldSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes end portals from the run overworld to the run end, and the exit portal back to the run spawn. */
@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	@Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
	private void speedrun$routeRunPortal(ServerLevel from, Entity entity, BlockPos portalPos, CallbackInfoReturnable<DimensionTransition> cir) {
		if (SpeedrunCore.runs() == null) {
			return;
		}
		RunWorldSet set = SpeedrunCore.runs().worlds().current();
		if (set == null || !set.contains(from)) {
			return;
		}
		if (from == set.end()) {
			BlockPos spawn = set.spawn();
			Vec3 target = entity instanceof ServerPlayer ? spawn.getBottomCenter() : entity.adjustSpawnLocation(set.overworld(), spawn).getBottomCenter();
			cir.setReturnValue(new DimensionTransition(set.overworld(), target, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(),
				DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)));
			return;
		}
		ServerLevel end = set.end();
		Vec3 target = ServerLevel.END_SPAWN_POINT.getBottomCenter();
		EndPlatformFeature.createEndPlatform(end, BlockPos.containing(target).below(), true);
		if (entity instanceof ServerPlayer) {
			target = target.subtract(0.0, 1.0, 0.0);
		}
		cir.setReturnValue(new DimensionTransition(end, target, entity.getDeltaMovement(), Direction.WEST.toYRot(), entity.getXRot(),
			DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)));
	}
}
