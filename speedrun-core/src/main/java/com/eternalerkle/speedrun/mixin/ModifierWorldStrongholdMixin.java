package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.WorldModifiers;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** close_stronghold: pulls the stronghold rings in for worlds generated with it. */
@Mixin(ChunkGeneratorStructureState.class)
public abstract class ModifierWorldStrongholdMixin {
	@Redirect(method = "generateRingPositions", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/level/levelgen/structure/placement/ConcentricRingsStructurePlacement;distance()I"))
	private int speedrun$closeStronghold(ConcentricRingsStructurePlacement placement) {
		return WorldModifiers.strongholdDistance((ChunkGeneratorStructureState) (Object) this, placement.distance());
	}
}
