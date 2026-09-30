package com.eternalerkle.speedrun.run;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

/**
 * A system that changes gameplay only while a run is live, such as modifiers or shared health.
 * {@link RunManager} calls these hooks on the server thread; implementations never change run state themselves.
 */
public interface RunFeature {
	/** The run just started. Players are already reset and teleported to spawn. */
	default void onRunStart(ActiveRun run) {
	}

	/** The run ended for any reason. Undo every change so nothing leaks into the lobby or next run. */
	default void onRunEnd(ActiveRun run) {
	}

	/** A player entered the live run, either at start, on first join mid-run, or on reconnect. */
	default void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
	}

	/** A player left the server or the run. */
	default void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
	}

	/** Called every server tick while the run is RUNNING. */
	default void tick(ActiveRun run) {
	}

	/** Writes state that must survive a server restart into {@code out}, under keys of this feature's own. */
	default void save(ActiveRun run, JsonObject out) {
	}

	/** Reads state written by {@link #save}. Called on startup right after {@link #onRunStart} for a restored run. */
	default void restore(ActiveRun run, JsonObject in) {
	}
}
