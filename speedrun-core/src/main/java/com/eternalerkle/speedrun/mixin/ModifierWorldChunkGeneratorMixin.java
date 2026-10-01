package com.eternalerkle.speedrun.mixin;

import com.eternalerkle.speedrun.modifier.WorldModifiers;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** no_villages and close_stronghold: adjust the structure placement of run overworlds generated with them. */
@Mixin(ChunkGenerator.class)
public abstract class ModifierWorldChunkGeneratorMixin {
	@ModifyVariable(method = "createState", at = @At("HEAD"), argsOnly = true)
	private HolderLookup<StructureSet> speedrun$noVillages(HolderLookup<StructureSet> sets) {
		return WorldModifiers.structureSets((ChunkGenerator) (Object) this, sets);
	}

	@Inject(method = "createState", at = @At("RETURN"))
	private void speedrun$closeStronghold(HolderLookup<StructureSet> sets, RandomState randomState, long seed, CallbackInfoReturnable<ChunkGeneratorStructureState> cir) {
		WorldModifiers.onStructureState((ChunkGenerator) (Object) this, cir.getReturnValue());
	}
}
