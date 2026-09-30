package com.eternalerkle.speedrun.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

/** The overworld, nether and end created for one run. */
public final class RunWorldSet {
	public final long seed;
	final RuntimeWorldHandle overworldHandle;
	final RuntimeWorldHandle netherHandle;
	final RuntimeWorldHandle endHandle;
	BlockPos spawn;
	boolean ready;
	boolean deleted;

	RunWorldSet(long seed, RuntimeWorldHandle overworld, RuntimeWorldHandle nether, RuntimeWorldHandle end) {
		this.seed = seed;
		this.overworldHandle = overworld;
		this.netherHandle = nether;
		this.endHandle = end;
	}

	public ServerLevel overworld() {
		return overworldHandle.asWorld();
	}

	public ServerLevel nether() {
		return netherHandle.asWorld();
	}

	public ServerLevel end() {
		return endHandle.asWorld();
	}

	/** Player spawn in the overworld. Only valid once {@link #isReady()}. */
	public BlockPos spawn() {
		return spawn;
	}

	public boolean isReady() {
		return ready;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public boolean contains(ServerLevel level) {
		return !deleted && (level == overworld() || level == nether() || level == end());
	}
}
