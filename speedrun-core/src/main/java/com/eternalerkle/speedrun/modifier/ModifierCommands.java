package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.command.CommandDoc;
import com.eternalerkle.speedrun.command.CommandRegistry;
import com.eternalerkle.speedrun.config.Settings;
import com.eternalerkle.speedrun.config.Settings.ModifierMode;
import com.eternalerkle.speedrun.run.RunManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /vote and /speedrun modifiers. */
public final class ModifierCommands {
	private static final String CATEGORY = "Modifiers";

	private ModifierCommands() {
	}

	public static void register(CommandRegistry registry) {
		registry.dispatcher().register(literal("vote")
			.then(argument("number", IntegerArgumentType.integer(1)).executes(context -> {
				Component error = modifiers(context).castVote(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "number"));
				if (error != null) {
					context.getSource().sendFailure(error);
					return 0;
				}
				return Command.SINGLE_SUCCESS;
			})));
		registry.document(new CommandDoc("vote", "Run", "<number>", "Votes for the next run's modifiers",
			List.of("Works while the lobby or death room offers a modifier vote. You can also click an option in chat.",
				"You get one vote and can change it until voting closes. Ties are broken at random."),
			List.of("vote 1", "vote 2"), null));

		LiteralArgumentBuilder<CommandSourceStack> mode = literal("mode");
		for (ModifierMode value : ModifierMode.values()) {
			mode.then(literal(value.name().toLowerCase(Locale.ROOT)).executes(context -> setMode(context, value)));
		}

		registry.dispatcher().register(literal("speedrun").then(literal("modifiers")
			.requires(CommandRegistry.requires(CommandRegistry.ADMIN))
			.then(mode)
			.then(literal("count").then(argument("n", IntegerArgumentType.integer(1, ModifierCatalog.ALL.size())).executes(ModifierCommands::setCount)))
			.then(literal("pool").executes(ModifierCommands::showPool))
			.then(literal("enable").then(argument("id", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder))
				.executes(context -> setEnabled(context, true))))
			.then(literal("disable").then(argument("id", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder))
				.executes(context -> setEnabled(context, false))))
			.then(literal("force").then(argument("ids", StringArgumentType.greedyString()).suggests((context, builder) -> {
				// Suggest ids for the word being typed after the last space.
				String typed = builder.getRemaining();
				int lastSpace = typed.lastIndexOf(' ');
				return SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder.createOffset(builder.getStart() + lastSpace + 1));
			}).executes(ModifierCommands::force)))));

		registry.document(new CommandDoc("speedrun modifiers mode", CATEGORY, "<off|random|vote>", "Sets how modifiers are chosen each run",
			List.of("off: no modifiers. random: drawn from the pool each run. vote: players pick between two options while waiting."),
			List.of("speedrun modifiers mode vote"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers count", CATEGORY, "<n>", "Sets how many modifiers are active per run",
			List.of("Fewer are used when the pool is too small or its modifiers conflict."),
			List.of("speedrun modifiers count 2"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers pool", CATEGORY, "", "Lists every modifier and whether it is in the pool",
			List.of("Click a modifier in the list to add it to or remove it from the pool."),
			List.of("speedrun modifiers pool"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers enable", CATEGORY, "<id>", "Adds a modifier to the pool",
			List.of("Use /speedrun modifiers pool to see every id."),
			List.of("speedrun modifiers enable one_heart"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers disable", CATEGORY, "<id>", "Removes a modifier from the pool",
			List.of("Use /speedrun modifiers pool to see every id."),
			List.of("speedrun modifiers disable one_heart"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers force", CATEGORY, "<id...>", "Forces specific modifiers for the next run only",
			List.of("Takes one or more ids separated by spaces. Overrides the mode and pool once, then normal selection resumes.",
				"Conflicting pairs such as tiny and giant are refused."),
			List.of("speedrun modifiers force tiny moon_gravity"), CommandRegistry.ADMIN));
	}

	private static Modifiers modifiers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandRegistry.runs(context);
		Modifiers modifiers = Modifiers.instance();
		if (modifiers == null) {
			throw new SimpleCommandExceptionType(Component.literal("The server is still starting.")).create();
		}
		return modifiers;
	}

	private static int setMode(CommandContext<CommandSourceStack> context, ModifierMode mode) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		Settings settings = runs.settings();
		settings.modifierMode = mode;
		settings.save();
		modifiers(context).refreshVote();
		context.getSource().sendSuccess(() -> Component.literal("Modifier mode set to " + mode.name().toLowerCase(Locale.ROOT) + ". Takes effect on the next run."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setCount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		int count = IntegerArgumentType.getInteger(context, "n");
		runs.settings().modifierCount = count;
		runs.settings().save();
		modifiers(context).refreshVote();
		context.getSource().sendSuccess(() -> Component.literal("Modifiers per run set to " + count + "."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setEnabled(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		String id = StringArgumentType.getString(context, "id").toLowerCase(Locale.ROOT);
		if (!ModifierCatalog.isKnown(id)) {
			context.getSource().sendFailure(Component.literal("Unknown modifier: " + id + ". See /speedrun modifiers pool."));
			return 0;
		}
		boolean changed = enabled ? runs.settings().modifierPool.add(id) : runs.settings().modifierPool.remove(id);
		if (!changed) {
			context.getSource().sendFailure(Component.literal(id + " is already " + (enabled ? "in" : "out of") + " the pool."));
			return 0;
		}
		runs.settings().save();
		modifiers(context).refreshVote();
		context.getSource().sendSuccess(() -> Component.literal((enabled ? "Added " : "Removed ") + ModifierCatalog.displayName(id) + (enabled ? " to" : " from") + " the pool."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int showPool(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Settings settings = CommandRegistry.runs(context).settings();
		MutableComponent message = Component.literal("Modifiers (mode " + settings.modifierMode.name().toLowerCase(Locale.ROOT) + ", " + settings.modifierCount + " per run):")
			.withStyle(ChatFormatting.GOLD);
		for (ModifierInfo info : ModifierCatalog.ALL) {
			boolean enabled = settings.modifierPool.contains(info.id());
			String command = "/speedrun modifiers " + (enabled ? "disable " : "enable ") + info.id();
			message.append(Component.literal("\n " + (enabled ? "[on] " : "[off] ")).withStyle(Style.EMPTY.withColor(enabled ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)
					.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(enabled ? "Click to remove from the pool" : "Click to add to the pool")))))
				.append(Component.literal(info.name()).withStyle(ChatFormatting.WHITE))
				.append(Component.literal(" " + info.id() + " · " + info.tag().label).withStyle(ChatFormatting.GRAY)
					.withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(info.effect())))));
		}
		List<String> forced = modifiers(context).forced();
		if (forced != null) {
			message.append(Component.literal("\nForced for the next run: " + String.join(", ", forced.stream().map(ModifierCatalog::displayName).toList()))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		context.getSource().sendSystemMessage(message);
		return Command.SINGLE_SUCCESS;
	}

	private static int force(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Modifiers modifiers = modifiers(context);
		ModifierPicker.Parsed parsed = ModifierPicker.parse(StringArgumentType.getString(context, "ids"));
		if (!parsed.ok()) {
			context.getSource().sendFailure(Component.literal(parsed.error()));
			return 0;
		}
		modifiers.force(parsed.ids());
		context.getSource().sendSuccess(() -> Component.literal("Next run forced to: " + String.join(", ", parsed.ids().stream().map(ModifierCatalog::displayName).toList())), true);
		return Command.SINGLE_SUCCESS;
	}
}
