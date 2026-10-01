package com.eternalerkle.speedrun;

import com.eternalerkle.speedrun.command.Commands;
import com.eternalerkle.speedrun.run.RunManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedrunCore implements ModInitializer {
	public static final String MOD_ID = "speedrun-core";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Nullable
	private static RunManager runs;

	/** The run manager, or null before the server has started. */
	@Nullable
	public static RunManager runs() {
		return runs;
	}

	@Override
	public void onInitialize() {
		Integrations.init();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			runs = new RunManager(server, FabricLoader.getInstance().getConfigDir().resolve(MOD_ID));
			Integrations.register(runs);
			runs.onServerStarted();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (runs != null) {
				runs.onServerStopping();
				runs = null;
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (runs != null) {
				runs.tick();
			}
		});
		// Joins reach the run manager through PlayerListMixin, after the player is placed in their level.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> runs == null || runs.allowDeath(entity, source));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (runs != null) {
				runs.afterDeath(entity, source);
			}
		});
		// Player chat is blocked while the loading screen is up, so nothing spoils it or gets lost in the held summary.
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> runs == null || !runs.isChatHeld());
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			if (runs != null) {
				runs.onChat(sender, message.signedContent());
			}
		});
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Commands.register(dispatcher));
		LOGGER.info("Speedrun Core loaded");
	}
}
