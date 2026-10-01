package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.config.Settings;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.ModifierHooks;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.run.RunState;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Owns the modifier catalog, picks modifiers for each run, and applies them. */
public final class Modifiers implements ModifierHooks, RunFeature {
	/** Interval of the timed modifiers, counted in real time so tick rate changes do not affect it. */
	static final long TIMED_INTERVAL_NANOS = 5L * 60L * 1_000_000_000L;
	private static final long ETERNAL_NIGHT_TIME = 18000L;

	@Nullable
	private static Modifiers instance;
	/** The live run, read by mixins and event handlers. Null outside a run. */
	@Nullable
	private static ActiveRun current;
	private static boolean eventsRegistered;

	private final RunManager runs;
	private final Random random = new Random();
	private final ModifierSelector selector = new ModifierSelector(random);
	private final InventoryModifiers inventories = new InventoryModifiers(random);
	/** Gamerule values replaced at run start, restored at run end. */
	private final Map<ServerLevel, Map<GameRules.Key<GameRules.BooleanValue>, Boolean>> savedRules = new HashMap<>();
	private long nextSwapNanos;

	private Modifiers(RunManager runs) {
		this.runs = runs;
	}

	public static void register(RunManager runs) {
		instance = new Modifiers(runs);
		current = null;
		runs.setModifierHooks(instance);
		runs.addFeature(instance);
		BodyModifiers.register(runs);
		runs.addFeature(new MobsAModifiers());
		MobsBModifiers.register(runs);
		if (!eventsRegistered) {
			eventsRegistered = true;
			registerEvents();
		}
	}

	/** The modifier system for the running server, or null before it has started. */
	@Nullable
	public static Modifiers instance() {
		return instance;
	}

	/** Whether a modifier is active in the live run and the level belongs to that run. Safe to call from mixins. */
	public static boolean isActive(String id, Level level) {
		ActiveRun run = current;
		return run != null && level instanceof ServerLevel serverLevel && run.modifiers.contains(id) && run.worlds.contains(serverLevel);
	}

	private static void registerEvents() {
		MobModifiers.registerEvents();
		// no_crafting_table: refuse to open the table. Sneaking with an item still places blocks against it.
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (isActive(ModifierCatalog.NO_CRAFTING_TABLE, world)
				&& world.getBlockState(hit.getBlockPos()).is(Blocks.CRAFTING_TABLE)
				&& !(player.isSecondaryUseActive() && !player.getItemInHand(hand).isEmpty())) {
				player.displayClientMessage(Component.literal("Crafting tables are disabled this run.").withStyle(ChatFormatting.RED), true);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		// Players who join while a vote is open still get the clickable options.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Modifiers modifiers = instance;
			if (modifiers != null && modifiers.selector.vote() != null) {
				handler.player.sendSystemMessage(modifiers.voteMessage(modifiers.selector.vote()));
			}
		});
	}

	// ---- ModifierHooks ----

	@Override
	public List<String> pickForNextRun(Settings settings) {
		return selector.pickForNextRun(settings.modifierMode, knownPool(settings), settings.modifierCount, knownAlways(settings));
	}

	@Override
	public String displayName(String id) {
		return ModifierCatalog.displayName(id);
	}

	@Override
	public void onWaitingStarted() {
		offerVote();
	}

	// ---- admin and vote API used by ModifierCommands ----

	/** Opens a new vote when the mode is VOTE and nobody is in a run, or closes it otherwise. Call after settings change. */
	public void refreshVote() {
		if (runs.state() == RunState.RUNNING) {
			selector.closeVote();
			return;
		}
		offerVote();
	}

	private void offerVote() {
		Settings settings = runs.settings();
		ModifierVote vote = selector.openVote(settings.modifierMode, knownPool(settings), settings.modifierCount, knownAlways(settings));
		if (vote != null) {
			runs.broadcast(voteMessage(vote));
		}
	}

	public void force(List<String> ids) {
		selector.force(ids);
		selector.closeVote();
	}

	@Nullable
	public List<String> forced() {
		return selector.forced();
	}

	/** Casts a vote. Returns an error message, or null on success. */
	@Nullable
	public Component castVote(ServerPlayer player, int option) {
		ModifierVote vote = selector.vote();
		if (vote == null) {
			return Component.literal("No modifier vote is open.");
		}
		if (!vote.cast(player.getUUID(), option)) {
			return Component.literal("Pick option 1 to " + vote.options().size() + ".");
		}
		int[] counts = vote.counts();
		StringBuilder tally = new StringBuilder();
		for (int i = 0; i < counts.length; i++) {
			tally.append(i > 0 ? " - " : "").append(counts[i]);
		}
		runs.broadcast(Component.literal(player.getGameProfile().getName() + " voted for option " + option + " (" + tally + ")").withStyle(ChatFormatting.GRAY));
		return null;
	}

	private MutableComponent voteMessage(ModifierVote vote) {
		MutableComponent message = Component.literal("Vote for the next run's modifiers:").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
		List<List<String>> options = vote.options();
		for (int i = 0; i < options.size(); i++) {
			int number = i + 1;
			MutableComponent hover = Component.empty();
			List<String> ids = options.get(i);
			for (int j = 0; j < ids.size(); j++) {
				ModifierInfo info = ModifierCatalog.get(ids.get(j));
				hover.append(Component.literal((j > 0 ? "\n" : "") + info.name() + " (" + info.tag().label + "): ").withStyle(ChatFormatting.GOLD))
					.append(Component.literal(info.effect()).withStyle(ChatFormatting.WHITE));
			}
			hover.append(Component.literal("\nClick to vote").withStyle(ChatFormatting.GRAY));
			message.append(Component.literal("\n  [" + number + "] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
					.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote " + number))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))))
				.append(Component.literal(String.join(", ", ids.stream().map(ModifierCatalog::displayName).toList())).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false)
					.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/vote " + number))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
		}
		return message;
	}

	private static Set<String> knownAlways(Settings settings) {
		Set<String> known = new java.util.LinkedHashSet<>();
		for (String id : settings.alwaysModifiers) {
			if (ModifierCatalog.isKnown(id)) {
				known.add(id);
			}
		}
		return known;
	}

	private static Set<String> knownPool(Settings settings) {
		Set<String> pool = new LinkedHashSet<>();
		for (String id : settings.modifierPool) {
			if (ModifierCatalog.isKnown(id)) {
				pool.add(id);
			}
		}
		return pool;
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		current = run;
		if (run.modifiers.contains(ModifierCatalog.UHC)) {
			for (ServerLevel level : List.of(run.worlds.overworld(), run.worlds.nether(), run.worlds.end())) {
				setRule(level, GameRules.RULE_NATURAL_REGENERATION, false);
			}
		}
		if (run.modifiers.contains(ModifierCatalog.ETERNAL_NIGHT)) {
			setRule(run.worlds.overworld(), GameRules.RULE_DAYLIGHT, false);
			run.worlds.overworld().setDayTime(ETERNAL_NIGHT_TIME);
		}
		MobModifiers.onRunStart(run, random.nextLong());
		inventories.onRunStart(run);
		nextSwapNanos = nextTimedNanos(run);
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		inventories.save(out);
		MobModifiers.save(out);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		inventories.restore(in);
		MobModifiers.restore(in);
	}

	/** The next 5 minute mark of run time. A restored run continues the schedule instead of catching up. */
	static long nextTimedNanos(ActiveRun run) {
		long elapsed = Math.max(0, run.clockNanos() - run.startNanos);
		return run.startNanos + (elapsed / TIMED_INTERVAL_NANOS + 1) * TIMED_INTERVAL_NANOS;
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		current = null;
		savedRules.forEach((level, rules) -> rules.forEach((rule, value) -> level.getGameRules().getRule(rule).set(value, runs.server())));
		savedRules.clear();
		for (ServerPlayer player : runs.server().getPlayerList().getPlayers()) {
			PlayerModifiers.remove(player);
		}
		MobModifiers.onRunEnd();
		inventories.onRunEnd();
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		PlayerModifiers.apply(run.modifiers, player);
		inventories.onPlayerEnterRun(run, player);
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		// Attribute modifiers are per player; remove them so they never follow the player into the lobby.
		PlayerModifiers.remove(player);
	}

	@Override
	public void tick(ActiveRun run) {
		if (run.modifiers.isEmpty()) {
			return;
		}
		List<ServerPlayer> players = playersIn(run);
		inventories.tick(run, players);
		if (run.modifiers.contains(ModifierCatalog.SWAP) && run.clockNanos() >= nextSwapNanos) {
			nextSwapNanos += TIMED_INTERVAL_NANOS;
			swap(players);
		}
	}

	/** Online players standing in one of the run's worlds. */
	static List<ServerPlayer> playersIn(ActiveRun run) {
		List<ServerPlayer> players = new ArrayList<>();
		for (ServerPlayer player : run.worlds.overworld().getServer().getPlayerList().getPlayers()) {
			if (run.worlds.contains(player.serverLevel()) && !player.isSpectator()) {
				players.add(player);
			}
		}
		return players;
	}

	private void setRule(ServerLevel level, GameRules.Key<GameRules.BooleanValue> rule, boolean value) {
		GameRules.BooleanValue current = level.getGameRules().getRule(rule);
		savedRules.computeIfAbsent(level, key -> new HashMap<>()).putIfAbsent(rule, current.get());
		current.set(value, runs.server());
	}

	/** Moves every player to another player's position. Does nothing with fewer than two players. */
	private void swap(List<ServerPlayer> players) {
		if (players.size() < 2) {
			return;
		}
		List<ServerPlayer> order = new ArrayList<>(players);
		Collections.shuffle(order, random);
		record Spot(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
		}
		List<Spot> spots = new ArrayList<>();
		for (ServerPlayer player : order) {
			spots.add(new Spot(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
		}
		// Rotating a shuffled order by one guarantees nobody stays in place.
		for (int i = 0; i < order.size(); i++) {
			ServerPlayer player = order.get(i);
			Spot spot = spots.get((i + 1) % spots.size());
			player.stopRiding();
			player.teleportTo(spot.level(), spot.x(), spot.y(), spot.z(), spot.yRot(), spot.xRot());
			player.resetFallDistance();
		}
		runs.broadcast(Component.literal("Swap! Everyone traded places.").withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
