package com.eternalerkle.speedrun;

import com.eternalerkle.speedrun.modifier.Modifiers;
import com.eternalerkle.speedrun.pack.ServerPack;
import com.eternalerkle.speedrun.vitals.SharedVitals;
import com.eternalerkle.speedrun.run.RunManager;

/** Wires optional systems (modifiers, shared vitals) into the run manager. */
final class Integrations {
	private Integrations() {
	}

	static void register(RunManager runs) {
		Modifiers.register(runs);
		SharedVitals.register(runs);
	}

	/** Called once from mod init, before any server exists. */
	static void init() {
		ServerPack.init();
	}
}
