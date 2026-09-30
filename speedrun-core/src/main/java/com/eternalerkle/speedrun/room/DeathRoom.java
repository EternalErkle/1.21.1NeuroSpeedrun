package com.eternalerkle.speedrun.room;

import com.eternalerkle.speedrun.mixin.ServerPlayerCameraAccessor;
import com.mojang.math.Transformation;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The black room players wait in between runs. Locks each player's camera onto a fixed anchor and bounces a flat
 * image of a player's face across the view like the old DVD screensaver.
 */
public final class DeathRoom {
	/** Font glyphs defined in the resource pack: a 10x10 solid pixel and a -1 advance space. */
	public static final ResourceLocation FACE_FONT = ResourceLocation.fromNamespaceAndPath("speedrun", "face");
	private static final String PIXEL = "";
	private static final String BACK = "";

	/** Distance from the camera to the face plane, in blocks. */
	private static final double DISTANCE = 4.0;
	/** Half of the vertical field of view at the default 70 degree FOV. */
	private static final double HALF_FOV = Math.toRadians(35);
	private static final double ASPECT = 16.0 / 9.0;
	/** 80 font pixels at 0.025 blocks each gives 2 blocks, scaled to 1.1. */
	private static final float SCALE = 0.55F;
	private static final double FACE_SIZE = 80 * 0.025 * SCALE;
	private static final double SPEED = 1.6;

	private final MinecraftServer server;
	private final FaceCache faces;
	private final Set<UUID> occupants = new HashSet<>();
	private final Random random = new Random();
	private Display.ItemDisplay anchor;
	private Display.TextDisplay face;
	private DvdBounce bounce;
	private long lastNanos;
	private Runnable onCornerHit = () -> { };

	public DeathRoom(MinecraftServer server, FaceCache faces) {
		this.server = server;
		this.faces = faces;
	}

	public void onCornerHit(Runnable listener) {
		this.onCornerHit = listener;
	}

	public boolean isActive() {
		return face != null;
	}

	public boolean contains(ServerPlayer player) {
		return occupants.contains(player.getUUID());
	}

	/** Shows {@code faceOwner}'s face and starts it bouncing. Replaces any face already shown. */
	public void open(UUID faceOwner) {
		ServerLevel level = server.overworld();
		clearEntities();
		anchor = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
		anchor.moveTo(Hub.CAMERA.x, Hub.CAMERA.y, Hub.CAMERA.z, 0, 0);
		anchor.setInvisible(true);
		level.addFreshEntity(anchor);

		double halfHeight = DISTANCE * Math.tan(HALF_FOV);
		double halfWidth = halfHeight * ASPECT;
		bounce = new DvdBounce(halfWidth, halfHeight, FACE_SIZE, SPEED,
			(random.nextDouble() - 0.5) * halfWidth, (random.nextDouble() - 0.5) * halfHeight, random.nextBoolean(), random.nextBoolean());

		face = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
		face.setText(faceText(faces.face(faceOwner)));
		face.setBackgroundColor(0);
		face.setFlags((byte) 0);
		face.setLineWidth(1000);
		face.setBrightnessOverride(Brightness.FULL_BRIGHT);
		face.setBillboardConstraints(Display.BillboardConstraints.FIXED);
		face.setViewRange(4.0F);
		face.setPosRotInterpolationDuration(2);
		// Text displays face -Z by default; turn them to face the camera, which looks toward +Z.
		face.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateY((float) Math.PI), new Vector3f(SCALE, SCALE, SCALE), new Quaternionf()));
		placeFace();
		level.addFreshEntity(face);
		lastNanos = System.nanoTime();
	}

	/** Moves a player into the room and locks their view. */
	/**
	 * Adds a player to the room. The move and camera lock happen in {@link #tick()}: setCamera teleports internally,
	 * and doing that in the same tick as another teleport, or during the join event before the player is registered
	 * with the chunk map, corrupts the level's chunk tracking.
	 */
	public void enter(ServerPlayer player) {
		occupants.add(player.getUUID());
		player.setGameMode(GameType.SPECTATOR);
	}

	/** Releases a player from the camera lock. The caller moves them somewhere else. */
	public void leave(ServerPlayer player) {
		occupants.remove(player.getUUID());
		if (player.getCamera() != player) {
			// Not setCamera: its teleport, followed by the caller's own teleport in the same tick, corrupts chunk tracking.
			((ServerPlayerCameraAccessor) player).speedrun$setCameraField(player);
			player.connection.send(new ClientboundSetCameraPacket(player));
		}
	}

	/**
	 * Drops a disconnecting player without touching their camera. Resetting the camera teleports the player and
	 * re-registers them for chunk tracking, which corrupts the level's chunk tickets while they are being removed.
	 */
	public void forget(ServerPlayer player) {
		occupants.remove(player.getUUID());
	}

	public void close() {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			leave(player);
		}
		occupants.clear();
		clearEntities();
	}

	public boolean isLocked(ServerPlayer player) {
		return anchor != null && occupants.contains(player.getUUID());
	}

	public void tick() {
		if (face == null) {
			return;
		}
		long now = System.nanoTime();
		double seconds = Math.min(0.25, (now - lastNanos) / 1e9);
		lastNanos = now;
		DvdBounce.Step step = bounce.step(seconds);
		placeFace();
		if (step.corner()) {
			cornerHit();
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!occupants.contains(player.getUUID())) {
				continue;
			}
			if (player.level() != server.overworld() || player.distanceToSqr(Hub.CAMERA) > 4) {
				// Move first and lock the camera on a later tick, once the player has settled in the new position.
				player.teleportTo(server.overworld(), Hub.CAMERA.x, Hub.CAMERA.y, Hub.CAMERA.z, 0, 0);
				continue;
			}
			if (player.getCamera() != anchor) {
				player.setCamera(anchor);
			}
		}
	}

	private void placeFace() {
		// Camera looks toward +Z, so screen-right is -X.
		double worldX = Hub.CAMERA.x - bounce.x;
		double worldY = Hub.CAMERA.y + bounce.y - FACE_SIZE / 2;
		double worldZ = Hub.CAMERA.z + DISTANCE;
		face.moveTo(worldX, worldY, worldZ, 0, 0);
	}

	private void cornerHit() {
		ServerLevel level = server.overworld();
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, face.getX(), face.getY() + FACE_SIZE / 2, face.getZ() - 0.2, 40, 0.3, 0.3, 0.1, 0.4);
		level.playSound(null, Hub.CAMERA.x, Hub.CAMERA.y, Hub.CAMERA.z, SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0F, 1.2F);
		onCornerHit.run();
	}

	private void clearEntities() {
		if (anchor != null) {
			anchor.discard();
			anchor = null;
		}
		if (face != null) {
			face.discard();
			face = null;
		}
	}

	/** Anchor used by the camera mixin to keep sneaking from breaking the lock. */
	public Entity anchor() {
		return anchor;
	}

	static Component faceText(int[] pixels) {
		MutableComponent text = Component.empty().withStyle(Style.EMPTY.withFont(FACE_FONT));
		for (int row = 0; row < 8; row++) {
			for (int col = 0; col < 8; col++) {
				text.append(Component.literal(PIXEL).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(pixels[row * 8 + col]))));
				if (col < 7) {
					text.append(Component.literal(BACK));
				}
			}
			if (row < 7) {
				text.append(Component.literal("\n"));
			}
		}
		return text;
	}
}
