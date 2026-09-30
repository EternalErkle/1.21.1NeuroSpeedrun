package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.run.Splits;
import com.eternalerkle.speedrun.stats.PlayerStats;
import com.eternalerkle.speedrun.stats.RunRecord;
import com.eternalerkle.speedrun.stats.Stats;
import com.eternalerkle.speedrun.util.Time;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /best, /stats, /splits and /speedrun stats reset. */
public final class StatsCommands {
	private static final int TOP_CAUSES = 5;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

	private StatsCommands() {
	}

	public static void register(CommandRegistry registry) {
		registry.dispatcher().register(literal("best")
			.executes(StatsCommands::best)
			.then(literal("all").executes(StatsCommands::bestAll)));
		registry.document(new CommandDoc("best", "Stats", "[all]", "Record time and splits for the current category",
			List.of("Shows the record for the live run's category, or for the category the next run will use.",
				"all: lists the record of every category played so far. Hover a line to see its splits."),
			List.of("best", "best all"), null));

		registry.dispatcher().register(literal("stats")
			.executes(context -> playerStats(context, null))
			.then(literal("server").executes(StatsCommands::serverStats))
			.then(argument("player", StringArgumentType.word())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(playerNames(), builder))
				.executes(context -> playerStats(context, StringArgumentType.getString(context, "player")))));
		registry.document(new CommandDoc("stats", "Stats", "[<player>|server]", "Lifetime deaths, causes, runs and wins",
			List.of("No argument: your own stats.",
				"<player>: any player who has played here, online or not.",
				"server: run count, win count, total deaths and the most common causes of death."),
			List.of("stats", "stats Steve", "stats server"), null));

		registry.dispatcher().register(literal("splits").executes(StatsCommands::splits));
		registry.document(new CommandDoc("splits", "Stats", "", "Current run's splits against the record",
			List.of("Lists every split reached so far with the difference to the category record.",
				"Green means ahead of the record, red means behind."),
			List.of("splits"), null));

		registry.dispatcher().register(literal("speedrun").requires(CommandRegistry.requires(CommandRegistry.ADMIN))
			.then(literal("stats").then(literal("reset")
				.then(literal("runcount").executes(StatsCommands::resetRunCount))
				.then(literal("best")
					.executes(context -> resetBest(context, null))
					.then(argument("category", StringArgumentType.greedyString())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(recordCategories(), builder))
						.executes(context -> resetBest(context, StringArgumentType.getString(context, "category")))))
				.then(literal("deaths").executes(StatsCommands::resetDeaths))
				.then(literal("all")
					.executes(StatsCommands::askResetAll)
					.then(literal("confirm").executes(StatsCommands::resetAll))))));
		registry.document(new CommandDoc("speedrun stats reset runcount", "Stats", "", "Resets the attempt counter",
			List.of("The next run becomes attempt #1. Records, history and player stats are kept."),
			List.of("speedrun stats reset runcount"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun stats reset best", "Stats", "[<category>]", "Clears one category's record, or all of them",
			List.of("<category>: a record category key as shown by /best all, e.g. dragon/20tps. Omit it to clear every record."),
			List.of("speedrun stats reset best dragon/20tps", "speedrun stats reset best"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun stats reset deaths", "Stats", "", "Clears the death sidebar",
			List.of("Sets every player's deaths since the last win to zero. Lifetime deaths are kept."),
			List.of("speedrun stats reset deaths"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun stats reset all", "Stats", "[confirm]", "Clears every stat",
			List.of("Clears attempts, wins, records, run history and every player's stats.",
				"Without confirm, prints a clickable confirmation message instead."),
			List.of("speedrun stats reset all"), CommandRegistry.ADMIN));
	}

	// ---- /best ----

	private static int best(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		ActiveRun run = runs.run();
		String category = run != null ? run.category : runs.pendingCategory();
		RunRecord record = runs.stats().records.get(category);
		if (record == null) {
			context.getSource().sendSuccess(() -> Component.literal("No record yet for " + category + ".").withStyle(ChatFormatting.GRAY), false);
			return Command.SINGLE_SUCCESS;
		}
		MutableComponent message = Component.empty()
			.append(Component.literal("Record for " + category).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(recordDetails(record));
		for (String id : CommandLogic.splitRows(Splits.NAMES.keySet(), Map.of(), record.splits)) {
			message.append(Component.literal("\n  " + Splits.NAMES.getOrDefault(id, id) + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(Time.format(record.splits.get(id))).withStyle(ChatFormatting.WHITE));
		}
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	private static MutableComponent recordDetails(RunRecord record) {
		String players = record.players.isEmpty() ? "nobody" : String.join(", ", record.players);
		return Component.literal("\n  ").append(Component.literal(Time.format(record.realMillis)).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD))
			.append(Component.literal(" real, " + Time.format(record.gameTicks * 50) + " in-game · attempt #" + record.attempt
				+ " · " + DATE.format(Instant.ofEpochMilli(record.endedAtEpochMillis))).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n  Players: " + players).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n  Seed: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(Long.toString(record.seed)).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, Long.toString(record.seed)))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))));
	}

	private static int bestAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Map<String, RunRecord> records = new TreeMap<>(CommandRegistry.runs(context).stats().records);
		if (records.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal("No records yet.").withStyle(ChatFormatting.GRAY), false);
			return Command.SINGLE_SUCCESS;
		}
		MutableComponent message = Component.empty().append(Component.literal("Records").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		records.forEach((category, record) -> {
			MutableComponent hover = Component.literal(category).withStyle(ChatFormatting.GOLD).append(recordDetails(record));
			for (String id : CommandLogic.splitRows(Splits.NAMES.keySet(), Map.of(), record.splits)) {
				hover.append(Component.literal("\n  " + Splits.NAMES.getOrDefault(id, id) + ": " + Time.format(record.splits.get(id))).withStyle(ChatFormatting.GRAY));
			}
			message.append(Component.literal("\n  " + category + ": ").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))))
				.append(Component.literal(Time.format(record.realMillis)).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))));
		});
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	// ---- /stats ----

	private static List<String> playerNames() {
		RunManager runs = com.eternalerkle.speedrun.SpeedrunCore.runs();
		if (runs == null) {
			return List.of();
		}
		return runs.stats().players.values().stream().map(player -> player.name).filter(name -> name != null && !name.isEmpty()).sorted(String.CASE_INSENSITIVE_ORDER).toList();
	}

	private static List<String> recordCategories() {
		RunManager runs = com.eternalerkle.speedrun.SpeedrunCore.runs();
		return runs == null ? List.of() : runs.stats().records.keySet().stream().sorted().toList();
	}

	private static int playerStats(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		Stats stats = CommandRegistry.runs(context).stats();
		PlayerStats player;
		String shownName;
		if (name == null) {
			ServerPlayer self = context.getSource().getPlayerOrException();
			shownName = self.getGameProfile().getName();
			player = stats.players.get(self.getUUID());
		} else {
			player = null;
			shownName = name;
			for (Map.Entry<UUID, PlayerStats> entry : stats.players.entrySet()) {
				if (name.equalsIgnoreCase(entry.getValue().name)) {
					player = entry.getValue();
					shownName = entry.getValue().name;
					break;
				}
			}
			if (player == null) {
				context.getSource().sendFailure(Component.literal("No stats for " + name + "."));
				return 0;
			}
		}
		if (player == null) {
			player = new PlayerStats();
		}
		MutableComponent message = Component.empty().append(Component.literal("Stats for " + shownName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		line(message, "Runs played", player.runsPlayed);
		line(message, "Wins", player.wins);
		line(message, "Lifetime deaths", player.lifetimeDeaths);
		line(message, "Deaths since last win", player.deathsSinceWin);
		causes(message, player.deathCauses);
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	private static int serverStats(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Stats stats = CommandRegistry.runs(context).stats();
		int deaths = stats.players.values().stream().mapToInt(player -> player.lifetimeDeaths).sum();
		MutableComponent message = Component.empty().append(Component.literal("Server stats").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		line(message, "Attempts", stats.attempts);
		line(message, "Wins", stats.wins);
		line(message, "Total deaths", deaths);
		line(message, "Categories with a record", stats.records.size());
		line(message, "Players", stats.players.size());
		causes(message, CommandLogic.sum(stats.players.values().stream().map(player -> player.deathCauses).filter(Objects::nonNull).toList()));
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	private static void line(MutableComponent message, String label, Object value) {
		message.append(Component.literal("\n  " + label + ": ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.WHITE));
	}

	private static void causes(MutableComponent message, Map<String, Integer> causes) {
		if (causes == null || causes.isEmpty()) {
			return;
		}
		message.append(Component.literal("\n  Most common deaths:").withStyle(ChatFormatting.GRAY));
		for (Map.Entry<String, Integer> entry : CommandLogic.top(causes, TOP_CAUSES)) {
			message.append(Component.literal("\n    " + CommandLogic.causeName(entry.getKey()) + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(Integer.toString(entry.getValue())).withStyle(ChatFormatting.RED));
		}
	}

	// ---- /splits ----

	private static int splits(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		ActiveRun run = runs.run();
		if (run == null) {
			context.getSource().sendFailure(Component.literal("There is no live run."));
			return 0;
		}
		RunRecord record = runs.stats().records.get(run.category);
		Map<String, Long> best = record == null ? Map.of() : record.splits;
		MutableComponent message = Component.empty()
			.append(Component.literal("Splits · attempt #" + run.attempt + " · " + Time.format(run.realMillis())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		if (record == null) {
			message.append(Component.literal("\n  No record yet for " + run.category + ".").withStyle(ChatFormatting.GRAY));
		}
		List<String> rows = CommandLogic.splitRows(Splits.NAMES.keySet(), run.splits, best);
		if (rows.isEmpty()) {
			message.append(Component.literal("\n  No splits reached yet.").withStyle(ChatFormatting.GRAY));
		}
		for (String id : rows) {
			Long mine = run.splits.get(id);
			Long theirs = best.get(id);
			message.append(Component.literal("\n  " + Splits.NAMES.getOrDefault(id, id) + ": ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(mine == null ? "--:--" : Time.format(mine)).withStyle(ChatFormatting.WHITE));
			if (theirs != null) {
				message.append(Component.literal(" / " + Time.format(theirs)).withStyle(ChatFormatting.DARK_GRAY));
			}
			if (mine != null && theirs != null) {
				long delta = mine - theirs;
				message.append(Component.literal(" (" + Time.formatDelta(delta) + ")").withStyle(delta <= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
			}
		}
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	// ---- /speedrun stats reset ----

	private static int resetRunCount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Stats stats = CommandRegistry.runs(context).stats();
		stats.attempts = 0;
		stats.save();
		context.getSource().sendSuccess(() -> Component.literal("Attempt counter reset. The next run is attempt #1.").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int resetBest(CommandContext<CommandSourceStack> context, String category) throws CommandSyntaxException {
		Stats stats = CommandRegistry.runs(context).stats();
		if (category == null) {
			int count = stats.records.size();
			stats.records.clear();
			stats.save();
			context.getSource().sendSuccess(() -> Component.literal("Cleared " + count + " record" + (count == 1 ? "" : "s") + ".").withStyle(ChatFormatting.GREEN), true);
			return Command.SINGLE_SUCCESS;
		}
		String key = category.trim();
		if (stats.records.remove(key) == null) {
			context.getSource().sendFailure(Component.literal("No record for category " + key + ". See /best all."));
			return 0;
		}
		stats.save();
		context.getSource().sendSuccess(() -> Component.literal("Cleared the record for " + key + ".").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int resetDeaths(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandRegistry.runs(context).clearDeathCounts();
		context.getSource().sendSuccess(() -> Component.literal("Death sidebar cleared.").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int askResetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandRegistry.runs(context);
		String command = "/speedrun stats reset all confirm";
		context.getSource().sendSuccess(() -> Component.literal("This clears attempts, wins, records, history and every player's stats. ").withStyle(ChatFormatting.RED)
			.append(Component.literal("[Confirm]").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))))), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		Stats stats = runs.stats();
		stats.attempts = 0;
		stats.wins = 0;
		stats.records.clear();
		stats.history.clear();
		// Keep name and last attempt so a player reconnecting mid-run is not reset to spawn.
		stats.players.replaceAll((id, old) -> {
			PlayerStats fresh = new PlayerStats();
			fresh.name = old.name;
			fresh.lastAttempt = old.lastAttempt;
			return fresh;
		});
		runs.clearDeathCounts();
		context.getSource().sendSuccess(() -> Component.literal("Every stat has been cleared.").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}
}
