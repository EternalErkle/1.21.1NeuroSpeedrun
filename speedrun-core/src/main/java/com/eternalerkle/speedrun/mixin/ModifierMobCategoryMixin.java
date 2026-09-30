package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.MobModifiers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** horde, part 2: doubles the monster cap while {@link ModifierNaturalSpawnerMixin} marks a horde level's spawn pass. */
@Mixin(MobCategory.class)
public class ModifierMobCategoryMixin {
	@ModifyReturnValue(method = "getMaxInstancesPerChunk", at = @At("RETURN"))
	private int speedrun$hordeCap(int cap) {
		return MobModifiers.doublingMonsterCap && (Object) this == MobCategory.MONSTER ? cap * 2 : cap;
	}
}
