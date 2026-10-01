package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.MobsBModifiers;
import com.eternalerkle.speedrun.modifier.Modifiers;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * sniper_skeletons: vanilla aims 0.2 blocks up per block of distance and adds spread. This aims along the arrow's real
 * ballistic arc with no spread, so a standing target is always hit.
 */
@Mixin(AbstractSkeleton.class)
public class ModifierMobsBSkeletonMixin {
	@Redirect(method = "performRangedAttack", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shoot(DDDFF)V"))
	private void speedrun$perfectAim(AbstractArrow arrow, double x, double y, double z, float speed, float inaccuracy) {
		AbstractSkeleton self = (AbstractSkeleton) (Object) this;
		if (!Modifiers.isActive(ModifierCatalog.SNIPER_SKELETONS, self.level()) || self.getTarget() == null) {
			arrow.shoot(x, y, z, speed, inaccuracy);
			return;
		}
		double horizontal = Math.sqrt(x * x + z * z);
		double vertical = self.getTarget().getY(0.5) - arrow.getY();
		arrow.shoot(x, MobsBModifiers.aimSlope(horizontal, vertical, speed) * horizontal, z, speed, 0.0F);
	}
}
