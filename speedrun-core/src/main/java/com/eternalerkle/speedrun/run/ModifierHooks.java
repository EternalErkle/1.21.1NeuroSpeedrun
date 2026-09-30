package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.config.Settings;

import java.util.List;

/** How {@link RunManager} asks the modifier system which modifiers the next run uses and what they are called. */
public interface ModifierHooks {
	ModifierHooks NONE = new ModifierHooks() {
		@Override
		public List<String> pickForNextRun(Settings settings) {
			return List.of();
		}

		@Override
		public String displayName(String id) {
			return id;
		}
	};

	/** Chooses the modifier ids for the run about to start. Consumes any forced or voted choice. */
	List<String> pickForNextRun(Settings settings);

	String displayName(String id);

	/** Called when the lobby or death room opens, so a vote can be offered. */
	default void onWaitingStarted() {
	}
}
