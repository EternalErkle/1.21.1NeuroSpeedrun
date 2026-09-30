package com.eternalerkle.speedrun.run;

public enum RunState {
	/** Waiting for someone to run /start. The next run's worlds generate in the background. */
	LOBBY,
	/** A live run with the timer counting. */
	RUNNING,
	/** A player died. Everyone waits in the death room until the next run starts. */
	RESETTING,
	/** The goal was completed. Shows the result, then continues like a reset. */
	VICTORY
}
