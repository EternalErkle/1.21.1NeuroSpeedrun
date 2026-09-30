package com.eternalerkle.speedrun.pack;

import com.eternalerkle.speedrun.SpeedrunCore;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds and serves the server resource pack through Polymer. Everything under {@code assets/} in this mod's jar goes
 * into the pack, which Polymer builds at server start and autohost serves over the game port.
 */
public final class ServerPack {
	/** Autohost is off by default outside a dev environment, so a fresh server gets it switched on. */
	private static final String DEFAULT_AUTOHOST_CONFIG = """
		{
		  "enabled": true,
		  "required": true
		}
		""";

	private ServerPack() {
	}

	public static void init() {
		PolymerResourcePackUtils.getInstance().addAssetSource(SpeedrunCore.MOD_ID);
		PolymerResourcePackUtils.markAsRequired();
		writeDefaultAutoHostConfig();
	}

	private static void writeDefaultAutoHostConfig() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("polymer").resolve("auto-host.json");
		if (Files.exists(file)) {
			return;
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, DEFAULT_AUTOHOST_CONFIG);
			SpeedrunCore.LOGGER.info("Wrote default Polymer autohost config to {}", file);
		} catch (IOException e) {
			SpeedrunCore.LOGGER.warn("Could not write Polymer autohost config", e);
		}
	}
}
