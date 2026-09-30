package com.eternalerkle.speedrun.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import xyz.nucleoid.fantasy.RuntimeWorld;

/**
 * Vanilla builds the structure check with the server world seed. Runtime worlds have their own seed, so without
 * this, structure lookups (eyes of ender, locate, maps) would search with the wrong seed.
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructureCheck;<init>(Lnet/minecraft/world/level/chunk/storage/ChunkScanAccess;Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/biome/BiomeSource;JLcom/mojang/datafixers/DataFixer;)V"), index = 8)
	private long speedrun$useRuntimeSeed(long seed) {
		if ((Object) this instanceof RuntimeWorld runtime) {
			return runtime.getSeed();
		}
		return seed;
	}
}
