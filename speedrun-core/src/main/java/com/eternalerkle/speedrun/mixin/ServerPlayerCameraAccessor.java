package com.eternalerkle.speedrun.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets the death room release a camera lock without {@link ServerPlayer#setCamera}'s built-in teleport. */
@Mixin(ServerPlayer.class)
public interface ServerPlayerCameraAccessor {
	@Accessor("camera")
	void speedrun$setCameraField(Entity camera);
}
