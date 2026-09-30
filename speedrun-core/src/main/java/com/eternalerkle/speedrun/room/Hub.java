package com.eternalerkle.speedrun.room;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The server's default world: a void holding the lobby platform and the death room.
 * Both are rebuilt idempotently on every start and sit inside the permanently loaded spawn chunks.
 */
public final class Hub {
	/** Lobby floor is at y=100, centered on 0,0. */
	public static final Vec3 LOBBY_SPAWN = new Vec3(0.5, 101, 0.5);
	private static final int LOBBY_RADIUS = 6;

	/** Death room shell spans these block coordinates inclusive. */
	static final int ROOM_MIN_X = 16, ROOM_MAX_X = 31;
	static final int ROOM_MIN_Y = 96, ROOM_MAX_Y = 111;
	static final int ROOM_MIN_Z = 16, ROOM_MAX_Z = 31;
	/** Camera position inside the room, looking toward +Z. */
	public static final Vec3 CAMERA = new Vec3(24.0, 103.5, 18.0);

	private Hub() {
	}

	public static void build(MinecraftServer server) {
		ServerLevel level = server.overworld();
		GameRules rules = level.getGameRules();
		rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(2, server);
		rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
		rules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
		rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
		rules.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
		rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(false, server);
		rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, server);
		// Must match the run worlds (default true). Spectators crossing between levels with different values corrupt chunk tracking.
		rules.getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(true, server);
		level.setDayTime(6000);
		level.setDefaultSpawnPos(BlockPos.containing(LOBBY_SPAWN), 0);

		buildLobby(level);
		buildDeathRoom(level);
	}

	private static void buildLobby(ServerLevel level) {
		BlockState floor = Blocks.SMOOTH_QUARTZ.defaultBlockState();
		BlockState light = Blocks.SEA_LANTERN.defaultBlockState();
		BlockState wall = Blocks.BARRIER.defaultBlockState();
		int r = LOBBY_RADIUS;
		for (int x = -r; x <= r; x++) {
			for (int z = -r; z <= r; z++) {
				boolean corner = Math.abs(x) == r - 1 && Math.abs(z) == r - 1;
				boolean center = x == 0 && z == 0;
				set(level, x, 100, z, corner || center ? light : floor);
				boolean edge = Math.abs(x) == r || Math.abs(z) == r;
				for (int y = 101; y <= 103; y++) {
					set(level, x, y, z, edge ? wall : Blocks.AIR.defaultBlockState());
				}
			}
		}
	}

	private static void buildDeathRoom(ServerLevel level) {
		BlockState shell = Blocks.BLACK_CONCRETE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = ROOM_MIN_X; x <= ROOM_MAX_X; x++) {
			for (int y = ROOM_MIN_Y; y <= ROOM_MAX_Y; y++) {
				for (int z = ROOM_MIN_Z; z <= ROOM_MAX_Z; z++) {
					boolean edge = x == ROOM_MIN_X || x == ROOM_MAX_X || y == ROOM_MIN_Y || y == ROOM_MAX_Y || z == ROOM_MIN_Z || z == ROOM_MAX_Z;
					set(level, x, y, z, edge ? shell : air);
				}
			}
		}
	}

	private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (level.getBlockState(pos) != state) {
			level.setBlock(pos, state, 2);
		}
	}
}
