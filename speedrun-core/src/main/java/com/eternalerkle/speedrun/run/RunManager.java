package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.config.Goal;
import com.eternalerkle.speedrun.config.Settings;
import com.eternalerkle.speedrun.hud.Hud;
import com.eternalerkle.speedrun.room.DeathRoom;
import com.eternalerkle.speedrun.room.FaceCache;
import com.eternalerkle.speedrun.room.Hub;
import com.eternalerkle.speedrun.stats.PlayerStats;
import com.eternalerkle.speedrun.stats.RunRecord;
import com.eternalerkle.speedrun.stats.Stats;
import com.eternalerkle.speedrun.util.JsonStore;
import com.eternalerkle.speedrun.util.Scheduler;
import com.eternalerkle.speedrun.util.Time;
import com.eternalerkle.speedrun.util.Titles;
import com.eternalerkle.speedrun.world.RunWorldSet;
import com.eternalerkle.speedrun.world.RunWorlds;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The only class that changes run state. Every event handler checks {@link #state} first, so a second death while
 * resetting, or a boss kill after a death, is ignored without any timing windows.
 */
public final class RunManager {
	private static final double VICTORY_SHOW_SECONDS = 5.0;
	private static final double COUNTDOWN_SECONDS = 3.0;
	/** Morning on a fresh world: the sunrise every vanilla world starts at. */
	private static final long RUN_START_DAY_TIME = 0L;
	/** Real seconds a run survives an empty server. The GameTest run shortens it through the system property. */
	private static final double EMPTY_GRACE_SECONDS = Double.parseDouble(System.getProperty("speedrun.emptyGraceSeconds", "60"));
	/** Delay after a run starts before generating the next run's worlds, so the start is not slowed down. */
	private static final double PREGENERATE_DELAY_SECONDS = 20.0;
	private static final long HUD_INTERVAL_NANOS = 200_000_000L;
	/** How often a kept run is written to disk, so a crash loses little. The worlds autosave every 5 minutes. */
	private static final long RUN_SAVE_INTERVAL_NANOS = 30_000_000_000L;

	private final MinecraftServer server;
	private final Settings settings;
	private final Stats stats;
	private final Path savedRunFile;
	private final RunWorlds worlds;
	private final FaceCache faces = new FaceCache();
	private final DeathRoom deathRoom;
	private final Hud hud;
	private final Splits splits;
	private final VoteSkip voteSkip = new VoteSkip();
	private final Scheduler scheduler = new Scheduler();
	private final List<RunFeature> features = new ArrayList<>();
	private final Map<UUID, String> lastChat = new HashMap<>();
	/** Lines a full client chat window shows; sending this many blanks scrolls everything out of view. */
	private static final int CHAT_CLEAR_LINES = 100;
	/** Broadcasts held while the loading screen is up, or null when chat flows normally. */
	@Nullable
	private List<Component> heldChat;
	private final Random random = new Random();
	/** Every run uses this seed when set with -Dspeedrun.fixedSeed. For benchmarks only: records stay per seed. */
	@Nullable
	private static final Long FIXED_SEED = Long.getLong("speedrun.fixedSeed");

	private long nextSeed() {
		return FIXED_SEED != null ? FIXED_SEED : random.nextLong();
	}
	private ModifierHooks modifiers = ModifierHooks.NONE;

	private RunState state = RunState.LOBBY;
	@Nullable
	private ActiveRun run;
	/** The most recently finished run, shown on the HUD while waiting. */
	@Nullable
	private ActiveRun lastRun;
	private boolean startRequested;
	private boolean countdownActive;
	private boolean roomMinElapsed;
	/** Incremented whenever pending scheduled work tied to an older phase must be ignored. */
	private int phase;
	private int graceToken;
	private long lastHudNanos;
	private long lastRunSaveNanos;

	public RunManager(MinecraftServer server, Path configDir) {
		this.server = server;
		this.settings = Settings.load(configDir.resolve("settings.json"));
		this.stats = Stats.load(configDir.resolve("stats.json"));
		this.savedRunFile = configDir.resolve("current-run.json");
		this.worlds = new RunWorlds(server);
		this.deathRoom = new DeathRoom(server, faces);
		this.hud = new Hud(server);
		this.splits = new Splits(this);
		this.features.add(splits);
		this.features.add(voteSkip);
		this.deathRoom.onCornerHit(() -> server.getPlayerList().broadcastSystemMessage(
			Component.literal("The face hit the corner!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false));
	}

	public void addFeature(RunFeature feature) {
		features.add(feature);
	}

	public void setModifierHooks(ModifierHooks hooks) {
		this.modifiers = hooks;
	}

	public MinecraftServer server() {
		return server;
	}

	public Settings settings() {
		return settings;
	}

	public Stats stats() {
		return stats;
	}

	public RunState state() {
		return state;
	}

	@Nullable
	public ActiveRun run() {
		return run;
	}

	public RunWorlds worlds() {
		return worlds;
	}

	public DeathRoom deathRoom() {
		return deathRoom;
	}

	public ModifierHooks modifiers() {
		return modifiers;
	}

	public VoteSkip voteSkip() {
		return voteSkip;
	}

	/** Category key the next run would use if it started now, without modifiers. */
	public String pendingCategory() {
		return Category.key(settings.goal, settings.tickRate, settings.sharedHealth, settings.sharedHunger, List.of());
	}

	// ---- lifecycle ----

	public void onServerStarted() {
		Hub.build(server);
		hud.init();
		for (PlayerStats player : stats.players.values()) {
			player.name = player.name == null ? "" : player.name;
		}
		SavedRun saved = settings.keepRunWhenEmpty ? SavedRun.load(savedRunFile) : null;
		if (saved != null && !worlds.isSaved(saved.worldId)) {
			SpeedrunCore.LOGGER.warn("Saved run #{} has no worlds on disk, starting fresh", saved.attempt);
			saved = null;
		}
		worlds.sweep(saved == null ? null : saved.worldId);
		if (saved != null) {
			restoreRun(saved);
			return;
		}
		SavedRun.delete(savedRunFile);
		enterLobby();
	}

	public void onServerStopping() {
		if (state == RunState.RUNNING && run != null) {
			if (settings.keepRunWhenEmpty) {
				saveRun();
				SpeedrunCore.LOGGER.info("Run #{} saved, it continues when the server starts again", run.attempt);
			} else {
				finishRun(RunRecord.Result.ABANDONED, "Server stopped", null);
			}
		}
		deathRoom.close();
		releaseChat();
		hud.shutdown();
		stats.save();
		settings.save();
	}

	public void tick() {
		scheduler.tick();
		worlds.tick();
		deathRoom.tick();
		if (state == RunState.RUNNING && run != null && !run.isPaused()) {
			run.gameTicks++;
			ActiveRun current = run;
			for (RunFeature feature : features) {
				// A feature's tick can end the run (a shared-health death), so stop once it is over.
				if (run != current || state != RunState.RUNNING) {
					break;
				}
				feature.tick(current);
			}
		}
		long now = System.nanoTime();
		if (settings.keepRunWhenEmpty && state == RunState.RUNNING && run != null && !run.isPaused()
			&& now - lastRunSaveNanos >= RUN_SAVE_INTERVAL_NANOS) {
			lastRunSaveNanos = now;
			saveRun();
		}
		if (now - lastHudNanos >= HUD_INTERVAL_NANOS) {
			lastHudNanos = now;
			updateHud();
		}
	}

	// ---- starting runs ----

	/** Handles /start. Returns an error message, or null on success. */
	@Nullable
	public Component requestStart() {
		if (state != RunState.LOBBY) {
			return Component.literal("A run is already in progress.");
		}
		if (startRequested) {
			return Component.literal("The run is already starting.");
		}
		startRequested = true;
		broadcast(Component.literal("Starting the run...").withStyle(ChatFormatting.GREEN));
		tryBeginFromLobby();
		return null;
	}

	private void tryBeginFromLobby() {
		RunWorldSet next = worlds.next();
		if (state == RunState.LOBBY && startRequested && !countdownActive && next != null && next.isReady()) {
			countdown(this::startRun);
		}
	}

	private void tryContinueFromRoom() {
		if (state != RunState.RESETTING && state != RunState.VICTORY) {
			return;
		}
		RunWorldSet next = worlds.next();
		if (!roomMinElapsed || countdownActive || next == null || !next.isReady()) {
			return;
		}
		if (server.getPlayerList().getPlayers().isEmpty()) {
			enterLobby();
			return;
		}
		countdown(this::startRun);
	}

	private void onNextReady(RunWorldSet set) {
		if (worlds.next() != set) {
			return;
		}
		if (state == RunState.LOBBY) {
			tryBeginFromLobby();
		} else {
			tryContinueFromRoom();
		}
	}

	private void ensureNextPreparing() {
		RunWorldSet next = worlds.next();
		if (next == null || next.isDeleted()) {
			worlds.prepareNext(nextSeed(), this::onNextReady);
		}
	}

	private void countdown(Runnable then) {
		countdownActive = true;
		int token = phase;
		for (int i = 0; i < 3; i++) {
			int number = 3 - i;
			scheduler.after(i, () -> {
				if (token != phase) {
					return;
				}
				Titles.show(server, Component.literal(Integer.toString(number)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), Component.empty(), 0, 0.9, 0.1);
				playToAll(SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F);
			});
		}
		scheduler.after(COUNTDOWN_SECONDS, () -> {
			if (token != phase) {
				return;
			}
			countdownActive = false;
			then.run();
		});
	}

	private void startRun() {
		RunWorldSet next = worlds.next();
		if (next == null || !next.isReady()) {
			// The prepared worlds were rerolled during the countdown. onNextReady starts the run once the new ones are ready.
			ensureNextPreparing();
			return;
		}
		startRequested = false;
		List<ServerPlayer> players = List.copyOf(server.getPlayerList().getPlayers());
		if (players.isEmpty()) {
			enterLobby();
			return;
		}
		phase++;
		RunWorldSet set = worlds.promoteNext();
		// The worlds were prepared during the previous run and their clock and weather kept running since. Start fresh.
		set.overworld().setDayTime(RUN_START_DAY_TIME);
		set.overworld().setWeatherParameters(0, 0, false, false);
		deathRoom.close();
		releaseChat();
		server.tickRateManager().setTickRate(settings.tickRate);
		stats.attempts++;
		List<String> chosen = modifiers.pickForNextRun(settings);
		run = new ActiveRun(stats.attempts, set, settings.goal, settings.tickRate, settings.sharedHealth, settings.sharedHunger, chosen);
		state = RunState.RUNNING;
		for (ServerPlayer player : players) {
			putIntoRun(player, true);
		}
		for (RunFeature feature : features) {
			feature.onRunStart(run);
		}
		for (ServerPlayer player : players) {
			for (RunFeature feature : features) {
				feature.onPlayerEnterRun(run, player);
			}
		}
		MutableComponent subtitle = Component.literal("Attempt #" + run.attempt);
		if (!chosen.isEmpty()) {
			subtitle.append(" · ").append(String.join(", ", chosen.stream().map(modifiers::displayName).toList()));
		}
		Titles.show(server, Component.literal("GO!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), subtitle, 0, 1.5, 0.5);
		stats.save();
		int token = phase;
		scheduler.after(PREGENERATE_DELAY_SECONDS, () -> {
			if (token == phase && state == RunState.RUNNING) {
				ensureNextPreparing();
			}
		});
		SpeedrunCore.LOGGER.info("Run #{} started, category {}, seed {}", run.attempt, run.category, set.seed);
	}

	/** Puts a player into the current run, resetting them if they are new to this run. */
	private void putIntoRun(ServerPlayer player, boolean fresh) {
		ActiveRun current = run;
		if (current == null) {
			return;
		}
		PlayerStats playerStats = stats.player(player.getUUID(), player.getGameProfile().getName());
		deathRoom.leave(player);
		if (fresh || playerStats.lastAttempt != current.attempt || !worlds.isRunLevel(player.serverLevel())) {
			resetPlayer(player);
			BlockPos spawn = current.worlds.spawn();
			player.teleportTo(current.worlds.overworld(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
			player.setRespawnPosition(current.worlds.overworld().dimension(), spawn, 0, true, false);
			if (playerStats.lastAttempt != current.attempt) {
				playerStats.runsPlayed++;
				playerStats.lastAttempt = current.attempt;
			}
		}
		player.setGameMode(GameType.SURVIVAL);
	}

	private void resetPlayer(ServerPlayer player) {
		player.removeAllEffects();
		player.getInventory().clearContent();
		player.getEnderChestInventory().clearContent();
		player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);
		player.getFoodData().setExhaustion(0);
		player.setExperienceLevels(0);
		player.setExperiencePoints(0);
		player.totalExperience = 0;
		player.setRemainingFireTicks(0);
		player.resetFallDistance();
		player.setAirSupply(player.getMaxAirSupply());
		player.stopSleeping();
		for (AdvancementHolder advancement : server.getAdvancements().getAllAdvancements()) {
			AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
			for (String criterion : progress.getCompletedCriteria()) {
				player.getAdvancements().revoke(advancement, criterion);
			}
		}
	}

	// ---- ending runs ----

	/** Records the current run and tells features it ended. Leaves the state for the caller to set. */
	private RunRecord finishRun(RunRecord.Result result, String cause, @Nullable ServerPlayer culprit) {
		ActiveRun ended = run;
		unpause(ended);
		SavedRun.delete(savedRunFile);
		ended.endNanos = System.nanoTime();
		RunRecord record = new RunRecord();
		record.attempt = ended.attempt;
		record.category = ended.unranked ? ended.category + "/unranked" : ended.category;
		record.result = result;
		record.realMillis = ended.realMillis();
		record.gameTicks = ended.gameTicks;
		record.seed = ended.worlds.seed;
		record.endedAtEpochMillis = System.currentTimeMillis();
		record.cause = cause;
		record.splits.putAll(ended.splits);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			record.players.add(player.getGameProfile().getName());
		}
		for (RunFeature feature : features) {
			feature.onRunEnd(ended);
		}
		boolean newRecord = !ended.unranked && stats.addRun(record);
		stats.save();
		lastRun = ended;
		run = null;
		phase++;
		if (newRecord) {
			scheduler.after(0.5, () -> {
				Titles.show(server, Component.literal("NEW RECORD!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(Time.format(record.realMillis)), 0.2, 3, 0.5);
				playToAll(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
			});
		}
		return record;
	}

	/**
	 * Called for every fatal damage to a living entity. Returns false to cancel the death.
	 * Player deaths never go through vanilla death handling: the run resets instead.
	 */
	public boolean allowDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}
		boolean inRun = state == RunState.RUNNING && run != null && run.worlds.contains(player.serverLevel());
		player.setHealth(player.getMaxHealth());
		if (!inRun) {
			// The hub is never dangerous, and nobody dies outside a live run.
			return false;
		}
		if (!settings.resetOnDeath) {
			// The run goes on: count the death and let vanilla kill and respawn the player at the run spawn.
			countDeath(player, source);
			return true;
		}
		fail(player, source);
		return false;
	}

	/** Lifetime and since-win death counts, cause stats and the sidebar. */
	private void countDeath(ServerPlayer dead, DamageSource source) {
		String causeKey = source.typeHolder().unwrapKey().map(key -> key.location().getPath()).orElse("unknown");
		if (source.getEntity() != null && source.getEntity() != dead) {
			causeKey += ":" + net.minecraft.world.entity.EntityType.getKey(source.getEntity().getType()).getPath();
		}
		PlayerStats deadStats = stats.player(dead.getUUID(), dead.getGameProfile().getName());
		deadStats.lifetimeDeaths++;
		deadStats.deathsSinceWin++;
		deadStats.deathCauses.merge(causeKey, 1, Integer::sum);
		hud.setDeaths(dead.getScoreboardName(), deadStats.deathsSinceWin);
	}

	/** Called after any living entity died. Used for boss tracking. */
	public void afterDeath(LivingEntity entity, DamageSource source) {
		if (state != RunState.RUNNING || run == null || !(entity.level() instanceof ServerLevel level) || !run.worlds.contains(level)) {
			return;
		}
		ActiveRun.Boss boss = entity instanceof EnderDragon ? ActiveRun.Boss.DRAGON
			: entity instanceof Warden ? ActiveRun.Boss.WARDEN
			: entity instanceof WitherBoss ? ActiveRun.Boss.WITHER
			: null;
		if (boss == null || !run.bossesKilled.add(boss)) {
			return;
		}
		splits.onBossKilled(run, boss);
		if (run.goalComplete()) {
			win(killerOf(entity, source));
		} else if (run.goal == Goal.ALLBOSSES) {
			broadcast(Component.literal(bossName(boss) + " defeated! ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.literal(run.bossesKilled.size() + "/3 bosses").withStyle(ChatFormatting.GRAY)));
		}
	}

	@Nullable
	private ServerPlayer killerOf(LivingEntity entity, DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker instanceof ServerPlayer player) {
			return player;
		}
		if (entity.getKillCredit() instanceof ServerPlayer player) {
			return player;
		}
		return null;
	}

	private void fail(ServerPlayer dead, DamageSource source) {
		ActiveRun ended = run;
		Component deathMessage = source.getLocalizedDeathMessage(dead);
		countDeath(dead, source);
		holdChat();

		BlockPos pos = dead.blockPosition();
		String dimension = dead.serverLevel() == ended.worlds.nether() ? "Nether" : dead.serverLevel() == ended.worlds.end() ? "End" : "Overworld";
		RunRecord record = finishRun(RunRecord.Result.DIED, deathMessage.getString(), dead);
		state = RunState.RESETTING;

		MutableComponent summary = Component.empty()
			// The loading screen already shows who died, so the summary leaves the death message out.
			.append(Component.literal("☠ Run lost").withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n  Run time: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(Time.format(record.realMillis)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal("  at ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pos.getX() + " " + pos.getY() + " " + pos.getZ() + " (" + dimension + ")").withStyle(ChatFormatting.WHITE));
		String last = lastChat.get(dead.getUUID());
		if (last != null) {
			summary.append(Component.literal("\n  Last words: ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("\"" + last + "\"").withStyle(ChatFormatting.ITALIC, ChatFormatting.WHITE));
		}
		broadcast(summary);
		broadcast(seedMessage(record.seed));
		playToAll(SoundEvents.WITHER_DEATH, 0.8F);
		openRoom(dead.getUUID());
	}

	private void win(@Nullable ServerPlayer killer) {
		ActiveRun ended = run;
		RunRecord record = finishRun(RunRecord.Result.WON, "Won", killer);
		holdChat();
		state = RunState.VICTORY;
		stats.wins++;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			stats.player(player.getUUID(), player.getGameProfile().getName()).wins++;
		}
		for (PlayerStats player : stats.players.values()) {
			player.deathsSinceWin = 0;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			hud.setDeaths(player.getScoreboardName(), 0);
		}
		stats.save();
		Titles.show(server, Component.literal("VICTORY").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
			Component.literal(Time.format(record.realMillis) + " · Attempt #" + ended.attempt), 0.3, VICTORY_SHOW_SECONDS - 0.5, 0.5);
		broadcast(Component.literal("Run won in ").withStyle(ChatFormatting.GOLD)
			.append(Component.literal(Time.format(record.realMillis)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
			.append(Component.literal(" real time, " + Time.format(record.gameTicks * 50) + " in-game.").withStyle(ChatFormatting.GOLD)));
		broadcast(seedMessage(record.seed));
		UUID faceOwner = killer != null ? killer.getUUID() : randomPlayerId();
		int token = phase;
		scheduler.after(VICTORY_SHOW_SECONDS, () -> {
			if (token == phase && state == RunState.VICTORY) {
				openRoom(faceOwner);
			}
		});
		ensureNextPreparing();
	}

	/** Ends the current run without a death: /speedrun reset and /voteskip. */
	public void abortRun(RunRecord.Result result, String reason, @Nullable UUID faceOwner) {
		if (state == RunState.LOBBY) {
			// Reroll the prepared worlds. Cancel a running countdown; a pending /start begins once the new worlds are ready.
			phase++;
			countdownActive = false;
			worlds.prepareNext(nextSeed(), this::onNextReady);
			broadcast(Component.literal(reason + " New seed is generating.").withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (state != RunState.RUNNING || run == null) {
			return;
		}
		RunRecord record = finishRun(result, reason, null);
		state = RunState.RESETTING;
		holdChat();
		broadcast(Component.literal(reason).withStyle(ChatFormatting.YELLOW));
		broadcast(seedMessage(record.seed));
		openRoom(faceOwner != null ? faceOwner : randomPlayerId());
	}

	private void openRoom(@Nullable UUID faceOwner) {
		roomMinElapsed = false;
		deathRoom.open(faceOwner != null ? faceOwner : new UUID(0, 0));
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			deathRoom.enter(player);
		}
		ensureNextPreparing();
		modifiers.onWaitingStarted();
		int token = phase;
		scheduler.after(settings.deathRoomMinSeconds, () -> {
			if (token == phase) {
				roomMinElapsed = true;
				tryContinueFromRoom();
			}
		});
	}

	private void enterLobby() {
		phase++;
		state = RunState.LOBBY;
		startRequested = false;
		countdownActive = false;
		deathRoom.close();
		releaseChat();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sendToLobby(player);
		}
		ensureNextPreparing();
		modifiers.onWaitingStarted();
	}

	private void sendToLobby(ServerPlayer player) {
		deathRoom.leave(player);
		resetPlayer(player);
		player.setGameMode(GameType.ADVENTURE);
		player.teleportTo(server.overworld(), Hub.LOBBY_SPAWN.x, Hub.LOBBY_SPAWN.y, Hub.LOBBY_SPAWN.z, 0, 0);
		player.setRespawnPosition(server.overworld().dimension(), BlockPos.containing(Hub.LOBBY_SPAWN), 0, true, false);
		player.sendSystemMessage(Component.literal("Waiting in the lobby. ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal("[Start run]").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/start"))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Start the run for everyone"))))));
	}

	// ---- players ----

	public void onJoin(ServerPlayer player) {
		faces.fetch(player.getGameProfile());
		graceToken++;
		if (state == RunState.RUNNING && run != null && run.isPaused()) {
			unpause(run);
			SpeedrunCore.LOGGER.info("Run #{} resumed by {}", run.attempt, player.getGameProfile().getName());
			player.sendSystemMessage(Component.literal("Welcome back. The run and its timer resume from where everyone left.").withStyle(ChatFormatting.GREEN));
		}
		PlayerStats playerStats = stats.player(player.getUUID(), player.getGameProfile().getName());
		hud.addPlayer(player, playerStats.deathsSinceWin);
		switch (state) {
			case LOBBY -> sendToLobby(player);
			case RUNNING -> {
				putIntoRun(player, false);
				for (RunFeature feature : features) {
					feature.onPlayerEnterRun(run, player);
				}
			}
			case RESETTING, VICTORY -> {
				if (deathRoom.isActive()) {
					deathRoom.enter(player);
				} else {
					player.setGameMode(GameType.SPECTATOR);
					player.teleportTo(server.overworld(), Hub.LOBBY_SPAWN.x, Hub.LOBBY_SPAWN.y, Hub.LOBBY_SPAWN.z, 0, 0);
				}
			}
		}
	}

	public void onLeave(ServerPlayer player) {
		hud.removePlayer(player);
		deathRoom.forget(player);
		if (state == RunState.RUNNING && run != null) {
			for (RunFeature feature : features) {
				feature.onPlayerLeaveRun(run, player);
			}
		}
		// Called before the player is removed from the player list, so they still count here.
		boolean empty = server.getPlayerList().getPlayers().stream().allMatch(other -> other == player);
		if (empty) {
			onServerEmpty();
		}
	}

	/** The last player left during a run: pause it when the setting keeps runs, otherwise start the grace period. */
	private void onServerEmpty() {
		if (state != RunState.RUNNING || run == null) {
			return;
		}
		int token = ++graceToken;
		if (settings.keepRunWhenEmpty) {
			pause(run);
			return;
		}
		unpause(run);
		int runPhase = phase;
		scheduler.after(EMPTY_GRACE_SECONDS, () -> {
			if (token == graceToken && runPhase == phase && state == RunState.RUNNING && server.getPlayerList().getPlayers().isEmpty()) {
				abandon();
			}
		});
	}

	/** Applies a changed keep-run setting to the live run. */
	public void onKeepRunChanged() {
		if (settings.keepRunWhenEmpty) {
			saveRun();
		} else {
			SavedRun.delete(savedRunFile);
		}
		if (server.getPlayerList().getPlayers().isEmpty()) {
			onServerEmpty();
		}
	}

	// ---- surviving restarts ----

	/** Writes the live run to disk. The world files are saved separately by the server's autosave and shutdown. */
	private void saveRun() {
		ActiveRun current = run;
		if (current == null || state != RunState.RUNNING || !settings.keepRunWhenEmpty) {
			return;
		}
		SavedRun saved = new SavedRun();
		saved.worldId = current.worlds.id;
		saved.seed = current.worlds.seed;
		BlockPos spawn = current.worlds.spawn();
		saved.spawnX = spawn.getX();
		saved.spawnY = spawn.getY();
		saved.spawnZ = spawn.getZ();
		saved.attempt = current.attempt;
		saved.goal = current.goal;
		saved.tickRate = current.tickRate;
		saved.sharedHealth = current.sharedHealth;
		saved.sharedHunger = current.sharedHunger;
		saved.modifiers.addAll(current.modifiers);
		saved.elapsedMillis = current.realMillis();
		saved.gameTicks = current.gameTicks;
		saved.splits.putAll(current.splits);
		saved.bossesKilled.addAll(current.bossesKilled);
		saved.unranked = current.unranked;
		ServerLevel overworld = current.worlds.overworld();
		ServerLevelData data = (ServerLevelData) overworld.getLevelData();
		saved.dayTime = data.getDayTime();
		saved.clearWeatherTime = data.getClearWeatherTime();
		saved.rainTime = data.getRainTime();
		saved.raining = data.isRaining();
		saved.thundering = data.isThundering();
		saved.dragon = RunWorlds.saveDragon(current.worlds);
		for (RunFeature feature : features) {
			feature.save(current, saved.features);
		}
		JsonStore.save(savedRunFile, saved);
	}

	/** Reopens a run saved before the last shutdown. It stays paused until someone joins. */
	private void restoreRun(SavedRun saved) {
		RunWorldSet set = worlds.restore(saved.worldId, saved.seed, new BlockPos(saved.spawnX, saved.spawnY, saved.spawnZ), saved.dragon);
		phase++;
		server.tickRateManager().setTickRate(saved.tickRate);
		run = new ActiveRun(saved.attempt, set, saved.goal, saved.tickRate, saved.sharedHealth, saved.sharedHunger, saved.modifiers, saved.elapsedMillis);
		run.gameTicks = saved.gameTicks;
		run.splits.putAll(saved.splits);
		run.bossesKilled.addAll(saved.bossesKilled);
		run.unranked = saved.unranked;
		state = RunState.RUNNING;
		for (RunFeature feature : features) {
			feature.onRunStart(run);
			feature.restore(run, saved.features);
		}
		set.overworld().setDayTime(saved.dayTime);
		set.overworld().setWeatherParameters(saved.clearWeatherTime, saved.rainTime, saved.raining, saved.thundering);
		pause(run);
		int token = phase;
		scheduler.after(PREGENERATE_DELAY_SECONDS, () -> {
			if (token == phase && state == RunState.RUNNING) {
				ensureNextPreparing();
			}
		});
		SpeedrunCore.LOGGER.info("Run #{} restored at {}, waiting for a player to join", run.attempt, Time.format(run.realMillis()));
	}

	/**
	 * Freezes the whole server and stops the run clock, so the run is exactly as everyone left it: no time of day,
	 * weather, furnaces or mobs move on. Only happens with nobody online.
	 */
	private void pause(ActiveRun current) {
		if (current.isPaused()) {
			return;
		}
		current.pause();
		server.tickRateManager().setFrozen(true);
		saveRun();
		SpeedrunCore.LOGGER.info("Run #{} paused because the server is empty", current.attempt);
	}

	private void unpause(ActiveRun current) {
		if (!current.isPaused()) {
			return;
		}
		current.resume();
		server.tickRateManager().setFrozen(false);
	}

	private void abandon() {
		finishRun(RunRecord.Result.ABANDONED, "Abandoned", null);
		worlds.deleteCurrent();
		SpeedrunCore.LOGGER.info("Run abandoned because the server was empty");
		enterLobby();
	}

	public void onChat(ServerPlayer sender, String message) {
		lastChat.put(sender.getUUID(), message);
	}

	/** Whether the camera lock should ignore sneaking for this player. */
	public boolean isCameraLocked(ServerPlayer player) {
		return deathRoom.isLocked(player);
	}

	// ---- HUD ----

	private void updateHud() {
		int nextAttempt = stats.attempts + 1;
		switch (state) {
			case LOBBY -> {
				hud.setLine1(Component.literal("Attempt #" + nextAttempt + " · Lobby").withStyle(ChatFormatting.WHITE)
					.append(bestSuffix(pendingCategory())));
				RunWorldSet next = worlds.next();
				boolean ready = next != null && next.isReady();
				hud.setLine2(Component.literal(!ready ? "Generating..." : startRequested ? "Starting..." : "/start to begin").withStyle(ChatFormatting.GRAY));
			}
			case RUNNING -> {
				ActiveRun current = run;
				hud.setLine1(Component.literal("Attempt #" + current.attempt + " · ").withStyle(ChatFormatting.WHITE)
					.append(Component.literal(Time.format(current.realMillis())).withStyle(splits.paceColor(current)))
					.append(bestSuffix(current.category)));
				hud.setLine2(runLine2(current));
			}
			case RESETTING, VICTORY -> {
				ActiveRun ended = lastRun;
				MutableComponent line = Component.literal(ended != null ? "Attempt #" + ended.attempt + " · " + Time.format(ended.realMillis()) : "").withStyle(ChatFormatting.WHITE);
				hud.setLine1(line);
				RunWorldSet next = worlds.next();
				boolean ready = next != null && next.isReady();
				hud.setLine2(Component.literal(ready ? "Next run starting..." : "Generating next world...").withStyle(ChatFormatting.GRAY));
			}
		}
	}

	@Nullable
	private Component runLine2(ActiveRun current) {
		List<Component> parts = new ArrayList<>();
		if (current.goal == Goal.ALLBOSSES) {
			MutableComponent checklist = Component.empty();
			ActiveRun.Boss[] bosses = ActiveRun.Boss.values();
			for (int i = 0; i < bosses.length; i++) {
				boolean done = current.bossesKilled.contains(bosses[i]);
				if (i > 0) {
					checklist.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
				}
				checklist.append(Component.literal(bossName(bosses[i]) + (done ? " ✔" : " ✘")).withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY));
			}
			parts.add(checklist);
		}
		if (!current.modifiers.isEmpty()) {
			parts.add(Component.literal(String.join(", ", current.modifiers.stream().map(modifiers::displayName).toList())).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		if (current.unranked) {
			parts.add(Component.literal("Unranked").withStyle(ChatFormatting.RED));
		}
		if (parts.isEmpty()) {
			return null;
		}
		MutableComponent line = Component.empty();
		for (int i = 0; i < parts.size(); i++) {
			if (i > 0) {
				line.append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY));
			}
			line.append(parts.get(i));
		}
		return line;
	}

	private Component bestSuffix(String category) {
		RunRecord best = stats.records.get(category);
		return Component.literal(" · Best " + (best == null ? "--:--" : Time.format(best.realMillis))).withStyle(ChatFormatting.GRAY);
	}

	// ---- helpers ----

	public static String bossName(ActiveRun.Boss boss) {
		return switch (boss) {
			case DRAGON -> "Dragon";
			case WARDEN -> "Warden";
			case WITHER -> "Wither";
		};
	}

	private Component seedMessage(long seed) {
		String text = Long.toString(seed);
		return Component.literal("Seed: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(text).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))));
	}

	@Nullable
	private UUID randomPlayerId() {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		return players.isEmpty() ? null : players.get(random.nextInt(players.size())).getUUID();
	}

	public void broadcast(Component message) {
		if (heldChat != null) {
			heldChat.add(message);
			return;
		}
		server.getPlayerList().broadcastSystemMessage(message, false);
	}

	/**
	 * Clears everyone's chat and holds every broadcast until the loading screen ends, so the end-of-run summary does
	 * not spoil the screen and is read afterwards. Player chat is blocked meanwhile. Released by {@link #releaseChat}.
	 */
	private void holdChat() {
		if (heldChat != null) {
			return;
		}
		heldChat = new ArrayList<>();
		Component blank = Component.literal(" ");
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (int i = 0; i < CHAT_CLEAR_LINES; i++) {
				player.sendSystemMessage(blank);
			}
		}
	}

	private void releaseChat() {
		List<Component> held = heldChat;
		heldChat = null;
		if (held != null) {
			for (Component message : held) {
				broadcast(message);
			}
		}
	}

	/** Whether player chat is blocked because the loading screen is up. */
	public boolean isChatHeld() {
		return heldChat != null;
	}

	private void playToAll(net.minecraft.sounds.SoundEvent sound, float pitch) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			player.playNotifySound(sound, SoundSource.MASTER, 1.0F, pitch);
		}
	}

	/** Marks the live run unranked. Used when a setting is changed mid-run. */
	public void markUnranked() {
		if (run != null) {
			run.unranked = true;
		}
	}

	/** Deletes every player's since-win death count and refreshes the sidebar. */
	public void clearDeathCounts() {
		for (PlayerStats player : stats.players.values()) {
			player.deathsSinceWin = 0;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			hud.setDeaths(player.getScoreboardName(), 0);
		}
		stats.save();
	}
}
