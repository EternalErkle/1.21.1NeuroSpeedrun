package com.eternalerkle.speedrun.run;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** /voteskip tally for the live run. Votes clear when a run starts or ends, and a voter who leaves loses their vote. */
public final class VoteSkip implements RunFeature {
	private final Set<UUID> votes = new HashSet<>();

	/** Votes needed to pass: strictly more than half of the online players. */
	public static int needed(int online) {
		return online / 2 + 1;
	}

	/** Adds a vote. Returns false when this player already voted. */
	public boolean vote(UUID player) {
		return votes.add(player);
	}

	public int count() {
		return votes.size();
	}

	public boolean passes(int online) {
		return online > 0 && votes.size() >= needed(online);
	}

	public void clear() {
		votes.clear();
	}

	@Override
	public void onRunStart(ActiveRun run) {
		clear();
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		clear();
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		votes.remove(player.getUUID());
	}
}
