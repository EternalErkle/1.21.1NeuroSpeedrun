package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.config.Goal;
import com.eternalerkle.speedrun.config.Settings;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.Category;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.run.RunState;
import com.eternalerkle.speedrun.run.VoteSkip;
import com.eternalerkle.speedrun.stats.RunRecord;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.function.BiConsumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /start, /voteskip and the /speedrun admin tree for run control and settings. */
public final class RunCommands {
	public static final float MIN_TICK_RATE = 1.0F;
	public static final float MAX_TICK_RATE = 1000.0F;

	private RunCommands() {
	}

	public static void register(CommandRegistry registry) {
		registry.dispatcher().register(literal("start").executes(context -> {
			Component error = CommandRegistry.runs(context).requestStart();
			if (error != null) {
				context.getSource().sendFailure(error);
				return 0;
			}
			return Command.SINGLE_SUCCESS;
		}));
		registry.document(new CommandDoc("start", "Run", "", "Starts the run from the lobby",
			List.of("Works only while waiting in the lobby. Anyone in the lobby can start the run.",
				"If the next world is still generating, the run starts as soon as it is ready."),
			List.of("start"), null));

		registry.dispatcher().register(literal("voteskip").executes(RunCommands::voteSkip));
		registry.document(new CommandDoc("voteskip", "Run", "", "Votes to abandon the current seed",
			List.of("Passes when more than half of the online players have voted. With one player online it passes at once.",
				"A skipped seed counts as an attempt but not as a death. Votes clear when a run starts or ends."),
			List.of("voteskip"), null));

		LiteralArgumentBuilder<CommandSourceStack> root = literal("speedrun").requires(CommandRegistry.requires(CommandRegistry.ADMIN));

		root.then(literal("reset").executes(RunCommands::reset));
		registry.document(new CommandDoc("speedrun reset", "Run", "", "Ends the current run and starts a new one",
			List.of("Counts as an attempt, not a death. Everyone goes to the death room while the next seed generates.",
				"In the lobby, rerolls the prepared seed instead."),
			List.of("speedrun reset"), CommandRegistry.ADMIN));

		root.then(literal("tickrate")
			.executes(RunCommands::showTickRate)
			.then(argument("rate", FloatArgumentType.floatArg(MIN_TICK_RATE, MAX_TICK_RATE))
				.executes(context -> setTickRate(context, false))
				.then(literal("now").executes(context -> setTickRate(context, true)))));
		registry.document(new CommandDoc("speedrun tickrate", "Settings", "[<rate>] [now]", "Shows or sets the game tick rate",
			List.of("Without arguments, shows the configured rate and the TPS the server actually reaches.",
				"<rate>: ticks per second from " + (int) MIN_TICK_RATE + " to " + (int) MAX_TICK_RATE + ". Default 20. Applies from the next run.",
				"now: also applies the rate immediately. The current run becomes unranked."),
			List.of("speedrun tickrate", "speedrun tickrate 100", "speedrun tickrate 40 now"), CommandRegistry.ADMIN));

		root.then(toggle("sharedhealth", (settings, on) -> settings.sharedHealth = on, "Shared health"));
		registry.document(new CommandDoc("speedrun sharedhealth", "Settings", "<on|off>", "Toggles shared health",
			List.of("on: every player shares one health bar. off: normal health.", "Takes effect on the next run."),
			List.of("speedrun sharedhealth on", "speedrun sharedhealth off"), CommandRegistry.ADMIN));

		root.then(toggle("sharedhunger", (settings, on) -> settings.sharedHunger = on, "Shared hunger"));
		registry.document(new CommandDoc("speedrun sharedhunger", "Settings", "<on|off>", "Toggles shared hunger",
			List.of("on: every player shares one hunger bar, including saturation. off: normal hunger.", "Takes effect on the next run."),
			List.of("speedrun sharedhunger on", "speedrun sharedhunger off"), CommandRegistry.ADMIN));

		LiteralArgumentBuilder<CommandSourceStack> goal = literal("goal");
		for (Goal value : Goal.values()) {
			goal.then(literal(value.id).executes(context -> setGoal(context, value)));
		}
		root.then(goal);
		registry.document(new CommandDoc("speedrun goal", "Settings", "<dragon|allbosses>", "Sets what wins a run",
			List.of("dragon: kill the ender dragon. The default.",
				"allbosses: kill the ender dragon, the warden and the wither, in any order.",
				"Takes effect on the next run."),
			List.of("speedrun goal allbosses", "speedrun goal dragon"), CommandRegistry.ADMIN));

		root.then(literal("settings").executes(RunCommands::showSettings));
		registry.document(new CommandDoc("speedrun settings", "Settings", "", "Shows every setting and its record category",
			List.of("Lists the goal, tick rate, shared toggles, modifier settings and death room time,",
				"then the record category the next run will use."),
			List.of("speedrun settings"), CommandRegistry.ADMIN));

		root.then(literal("deathroom").then(literal("mintime")
			.then(argument("seconds", IntegerArgumentType.integer(0, 600)).executes(RunCommands::setDeathRoomMinTime))));
		registry.document(new CommandDoc("speedrun deathroom mintime", "Settings", "<seconds>", "Sets the minimum death room time",
			List.of("<seconds>: 0 to 600. Default 5. The room stays up at least this long and until the next world is ready."),
			List.of("speedrun deathroom mintime 10"), CommandRegistry.ADMIN));

		registry.dispatcher().register(root);
	}

	private static int voteSkip(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (runs.state() != RunState.RUNNING) {
			context.getSource().sendFailure(Component.literal("There is no live run to skip."));
			return 0;
		}
		VoteSkip votes = runs.voteSkip();
		int online = runs.server().getPlayerList().getPlayerCount();
		if (votes.vote(player.getUUID())) {
			runs.broadcast(Component.literal(player.getGameProfile().getName() + " voted to skip this seed ").withStyle(ChatFormatting.YELLOW)
				.append(Component.literal("(" + votes.count() + "/" + VoteSkip.needed(online) + ")").withStyle(ChatFormatting.GRAY)));
		} else {
			context.getSource().sendFailure(Component.literal("You already voted to skip."));
		}
		if (votes.passes(online)) {
			runs.abortRun(RunRecord.Result.SKIPPED, "Seed skipped by vote.", null);
		}
		return Command.SINGLE_SUCCESS;
	}

	private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		if (runs.state() == RunState.RESETTING || runs.state() == RunState.VICTORY) {
			context.getSource().sendFailure(Component.literal("The next run is already being prepared."));
			return 0;
		}
		runs.abortRun(RunRecord.Result.RESET, "Run reset by " + context.getSource().getTextName() + ".", null);
		return Command.SINGLE_SUCCESS;
	}

	private static int showTickRate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		MinecraftServer server = runs.server();
		float current = server.tickRateManager().tickrate();
		double measured = CommandLogic.measuredTps(current, server.getAverageTickTimeNanos());
		double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
		MutableComponent message = Component.literal("Tick rate: ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(Category.formatRate(runs.settings().tickRate)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal(" configured, ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(Category.formatRate(current)).withStyle(ChatFormatting.WHITE))
			.append(Component.literal(" active\nMeasured: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.format("%.1f TPS", measured)).withStyle(measured < current * 0.95 ? ChatFormatting.RED : ChatFormatting.GREEN))
			.append(Component.literal(String.format(" (%.1f ms per tick, budget %.1f ms)", mspt, 1000.0 / current)).withStyle(ChatFormatting.GRAY));
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	private static int setTickRate(CommandContext<CommandSourceStack> context, boolean now) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		float rate = FloatArgumentType.getFloat(context, "rate");
		runs.settings().tickRate = rate;
		runs.settings().save();
		String text = Category.formatRate(rate);
		if (!now) {
			context.getSource().sendSuccess(() -> Component.literal("Tick rate set to " + text + ". Takes effect on the next run.").withStyle(ChatFormatting.GREEN), true);
			return Command.SINGLE_SUCCESS;
		}
		runs.server().tickRateManager().setTickRate(rate);
		boolean live = runs.state() == RunState.RUNNING && runs.run() != null;
		runs.markUnranked();
		runs.broadcast(Component.literal("Tick rate changed to " + text + " TPS.").withStyle(ChatFormatting.YELLOW)
			.append(live ? Component.literal(" This run is now unranked.").withStyle(ChatFormatting.RED) : Component.empty()));
		return Command.SINGLE_SUCCESS;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> toggle(String name, BiConsumer<Settings, Boolean> setter, String label) {
		LiteralArgumentBuilder<CommandSourceStack> node = literal(name);
		for (boolean on : new boolean[] {true, false}) {
			node.then(literal(on ? "on" : "off").executes(context -> {
				RunManager runs = CommandRegistry.runs(context);
				setter.accept(runs.settings(), on);
				runs.settings().save();
				context.getSource().sendSuccess(() -> Component.literal(label + " " + (on ? "on" : "off") + ". Takes effect on the next run.").withStyle(ChatFormatting.GREEN), true);
				return Command.SINGLE_SUCCESS;
			}));
		}
		return node;
	}

	private static int setGoal(CommandContext<CommandSourceStack> context, Goal goal) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		runs.settings().goal = goal;
		runs.settings().save();
		context.getSource().sendSuccess(() -> Component.literal("Goal set to " + goal.id + ". Takes effect on the next run.").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setDeathRoomMinTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		int seconds = IntegerArgumentType.getInteger(context, "seconds");
		runs.settings().deathRoomMinSeconds = seconds;
		runs.settings().save();
		context.getSource().sendSuccess(() -> Component.literal("Death room minimum time set to " + seconds + "s.").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int showSettings(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		Settings settings = runs.settings();
		MutableComponent message = Component.empty().append(Component.literal("Speedrun settings").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		line(message, "Goal", settings.goal.id);
		line(message, "Tick rate", Category.formatRate(settings.tickRate));
		line(message, "Shared health", settings.sharedHealth ? "on" : "off");
		line(message, "Shared hunger", settings.sharedHunger ? "on" : "off");
		line(message, "Modifiers", settings.modifierMode.name().toLowerCase(java.util.Locale.ROOT) + ", " + settings.modifierCount + " per run, "
			+ settings.modifierPool.size() + " in pool");
		line(message, "Death room minimum", Category.formatRate((float) settings.deathRoomMinSeconds) + "s");
		line(message, "Next run category", runs.pendingCategory());
		ActiveRun run = runs.run();
		if (run != null) {
			line(message, "Current run category", run.category + (run.unranked ? " (unranked)" : ""));
		}
		context.getSource().sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	private static void line(MutableComponent message, String label, String value) {
		message.append(Component.literal("\n  " + label + ": ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
	}
}
