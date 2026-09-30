package com.eternalerkle.speedrun.pack;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.util.JsonStore;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

	/**
	 * Polymer writes its own default with autohost disabled the first time it runs, and reads the file at server start,
	 * after every mod has initialized. So the file is patched here on every start, keeping every other field.
	 */
	private static void writeDefaultAutoHostConfig() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("polymer").resolve("auto-host.json");
		try {
			JsonObject config = Files.exists(file)
				? JsonParser.parseString(Files.readString(file)).getAsJsonObject()
				: JsonParser.parseString(DEFAULT_AUTOHOST_CONFIG).getAsJsonObject();
			if (config.has("enabled") && config.get("enabled").getAsBoolean() && config.has("required") && config.get("required").getAsBoolean()) {
				return;
			}
			config.addProperty("enabled", true);
			config.addProperty("required", true);
			Files.createDirectories(file.getParent());
			Files.writeString(file, JsonStore.GSON.toJson(config));
			SpeedrunCore.LOGGER.info("Enabled Polymer resource pack autohost in {}", file);
		} catch (IOException | RuntimeException e) {
			SpeedrunCore.LOGGER.warn("Could not update Polymer autohost config at {}; enable it there by hand", file, e);
		}
	}
}
