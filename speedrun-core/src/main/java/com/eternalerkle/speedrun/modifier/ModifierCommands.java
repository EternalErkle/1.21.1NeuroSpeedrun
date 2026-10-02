package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.command.CommandDoc;
import com.eternalerkle.speedrun.command.CommandRegistry;
import com.eternalerkle.speedrun.config.Settings;
import com.eternalerkle.speedrun.config.Settings.ModifierMode;
import com.eternalerkle.speedrun.run.ActiveRun;
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

		registry.dispatcher().register(literal("modifiers").executes(context -> showModifiers(context, 1))
			.then(argument("page", IntegerArgumentType.integer(1)).executes(context -> showModifiers(context, IntegerArgumentType.getInteger(context, "page")))));
		registry.document(new CommandDoc("modifiers", CATEGORY, "[page]", "Explains modifiers and lists every one",
			List.of("Modifiers change the rules of one run, such as one heart, moon gravity or a doubled mob cap.",
				"Shows the modifiers active in the current run, how the next run's are chosen, and every modifier",
				"with its effect, " + PAGE_SIZE + " per page. Click Next or run /modifiers <page>. Hover a modifier for details.",
				"Active modifiers also show under the GO! title and under the timer at the top of the screen.",
				"Each combination of modifiers keeps its own records."),
			List.of("modifiers"), null));

		LiteralArgumentBuilder<CommandSourceStack> mode = literal("mode");
		for (ModifierMode value : ModifierMode.values()) {
			mode.then(literal(value.name().toLowerCase(Locale.ROOT)).executes(context -> setMode(context, value)));
		}

		registry.dispatcher().register(literal("speedrun").then(literal("modifiers")
			.requires(CommandRegistry.requires(CommandRegistry.ADMIN))
			.then(mode)
			.then(literal("count").then(argument("n", IntegerArgumentType.integer(1, ModifierCatalog.ALL.size())).executes(ModifierCommands::setCount)))
			.then(literal("pool").executes(context -> showPool(context, 1))
				.then(argument("page", IntegerArgumentType.integer(1)).executes(context -> showPool(context, IntegerArgumentType.getInteger(context, "page")))))
			.then(literal("enable")
				.then(literal("all").executes(context -> setAll(context, true)))
				.then(argument("id", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder))
					.executes(context -> setEnabled(context, true))))
			.then(literal("disable")
				.then(literal("all").executes(context -> setAll(context, false)))
				.then(argument("id", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder))
					.executes(context -> setEnabled(context, false))))
			.then(literal("always")
				.then(literal("add").then(argument("id", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder))
					.executes(context -> setAlways(context, true))))
				.then(literal("remove").then(argument("id", StringArgumentType.word()).suggests((context, builder) -> SharedSuggestionProvider.suggest(CommandRegistry.runs(context).settings().alwaysModifiers, builder))
					.executes(context -> setAlways(context, false))))
				.then(literal("clear").executes(ModifierCommands::clearAlways)))
			.then(literal("force").then(argument("ids", StringArgumentType.greedyString()).suggests((context, builder) -> {
				// Suggest ids for the word being typed after the last space.
				String typed = builder.getRemaining();
				int lastSpace = typed.lastIndexOf(' ');
				return SharedSuggestionProvider.suggest(ModifierCatalog.ids(), builder.createOffset(builder.getStart() + lastSpace + 1));
			}).executes(ModifierCommands::force)))));

		registry.document(new CommandDoc("speedrun modifiers mode", CATEGORY, "<off|random|vote>", "Sets how modifiers are chosen each run",
			List.of("off: no modifiers. The default.",
				"random: each run draws modifiers from the pool at random.",
				"vote: while waiting in the lobby or death room, players vote between two options drawn from the pool.",
				"random and vote need modifiers in the pool first: see /speedrun modifiers pool.",
				"Setup: /speedrun modifiers enable all (or pick some in /speedrun modifiers pool), then set the mode. Takes effect on the next run."),
			List.of("speedrun modifiers mode vote", "speedrun modifiers mode random", "speedrun modifiers mode off"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers count", CATEGORY, "<n>", "Sets how many modifiers are active per run",
			List.of("Fewer are used when the pool is too small or its modifiers conflict."),
			List.of("speedrun modifiers count 2"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers pool", CATEGORY, "[page]", "Lists every modifier and whether it is in the pool",
			List.of("Click a modifier in the list to add it to or remove it from the pool.", PAGE_SIZE + " modifiers per page. Click Next or add a page number."),
			List.of("speedrun modifiers pool"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers enable", CATEGORY, "<id|all>", "Adds a modifier, or every modifier, to the pool",
			List.of("<id>: one modifier. Use /speedrun modifiers pool to see every id.", "all: adds all " + ModifierCatalog.ALL.size() + " modifiers at once."),
			List.of("speedrun modifiers enable all", "speedrun modifiers enable one_heart"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers disable", CATEGORY, "<id|all>", "Removes a modifier, or every modifier, from the pool",
			List.of("<id>: one modifier. Use /speedrun modifiers pool to see every id.", "all: empties the pool."),
			List.of("speedrun modifiers disable all", "speedrun modifiers disable one_heart"), CommandRegistry.ADMIN));
		registry.document(new CommandDoc("speedrun modifiers always", CATEGORY, "<add|remove> <id> | clear", "Sets modifiers that are on in every run",
			List.of("Always-on modifiers apply to every run, even with the mode off.",
				"The mode then adds its random or voted modifiers on top, never repeating or contradicting these.",
				"A modifier can be always-on without being in the pool. Takes effect on the next run."),
			List.of("speedrun modifiers always add one_heart", "speedrun modifiers always remove one_heart", "speedrun modifiers always clear"), CommandRegistry.ADMIN));
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

	/** Modifiers per page. The client keeps only 100 chat lines, so the full catalog cannot fit in one message. */
	private static final int PAGE_SIZE = 20;

	private static int pageCount() {
		return (ModifierCatalog.ALL.size() + PAGE_SIZE - 1) / PAGE_SIZE;
	}

	private static List<ModifierInfo> page(int page) {
		int from = (page - 1) * PAGE_SIZE;
		return ModifierCatalog.ALL.subList(from, Math.min(from + PAGE_SIZE, ModifierCatalog.ALL.size()));
	}

	/** "< Prev  Page 2/8  Next >", with the arrows running the same command for the neighbouring page. */
	private static Component pageFooter(String command, int page) {
		int pages = pageCount();
		MutableComponent footer = Component.literal("\n");
		footer.append(pageLink("< Prev", command, page - 1, page > 1));
		footer.append(Component.literal("  Page " + page + "/" + pages + "  ").withStyle(ChatFormatting.YELLOW));
		footer.append(pageLink("Next >", command, page + 1, page < pages));
		return footer;
	}

	private static Component pageLink(String label, String command, int page, boolean available) {
		if (!available) {
			return Component.literal(label).withStyle(ChatFormatting.DARK_GRAY);
		}
		return Component.literal(label).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)
			.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command + " " + page))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command + " " + page))));
	}

	private static int showModifiers(CommandContext<CommandSourceStack> context, int page) throws CommandSyntaxException {
		page = Math.min(page, pageCount());
		RunManager runs = CommandRegistry.runs(context);
		Settings settings = runs.settings();
		ActiveRun run = runs.run();
		String active = run == null ? "no run in progress" : run.modifiers.isEmpty() ? "none"
			: String.join(", ", run.modifiers.stream().map(ModifierCatalog::displayName).toList());
		String always = settings.alwaysModifiers.isEmpty() ? "" : "always " + String.join(", ", settings.alwaysModifiers.stream().map(ModifierCatalog::displayName).toList()) + "; plus ";
		String next = always + switch (settings.modifierMode) {
			case OFF -> always.isEmpty() ? "none (modifiers are off)" : "nothing random (mode is off)";
			case RANDOM -> settings.modifierCount + " drawn at random from the " + settings.modifierPool.size() + " in the pool";
			case VOTE -> settings.modifierCount + " chosen by /vote from the " + settings.modifierPool.size() + " in the pool";
		};
		MutableComponent message = Component.empty()
			.append(Component.literal("Modifiers").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal("\nModifiers change the rules of one run. Each combination keeps its own records.").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\nThis run: ").withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(active).withStyle(ChatFormatting.LIGHT_PURPLE))
			.append(Component.literal("\nNext run: ").withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(next).withStyle(ChatFormatting.WHITE));
		for (ModifierInfo info : page(page)) {
			boolean inPool = settings.modifierPool.contains(info.id());
			Component hover = Component.literal(info.effect() + "\nTag: " + info.tag().label + "\nID: " + info.id());
			boolean isAlways = settings.alwaysModifiers.contains(info.id());
			message.append(Component.literal("\n " + (isAlways ? "★ " : inPool ? "● " : "○ ")).withStyle(isAlways ? ChatFormatting.GOLD : inPool ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY))
				.append(Component.literal(info.name()).withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE)
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover))))
				.append(Component.literal(" - " + info.effect()).withStyle(ChatFormatting.GRAY));
		}
		message.append(Component.literal("\n● in the pool   ○ not in the pool   ★ always on").withStyle(ChatFormatting.DARK_GRAY));
		message.append(pageFooter("/modifiers", page));
		context.getSource().sendSystemMessage(message);
		return Command.SINGLE_SUCCESS;
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

	private static int setAll(CommandContext<CommandSourceStack> context, boolean enabled) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		if (enabled) {
			runs.settings().modifierPool.addAll(ModifierCatalog.ids());
		} else {
			runs.settings().modifierPool.clear();
		}
		runs.settings().save();
		modifiers(context).refreshVote();
		context.getSource().sendSuccess(() -> Component.literal(enabled
			? "Added all " + ModifierCatalog.ALL.size() + " modifiers to the pool."
			: "Removed every modifier from the pool."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int showPool(CommandContext<CommandSourceStack> context, int page) throws CommandSyntaxException {
		page = Math.min(page, pageCount());
		Settings settings = CommandRegistry.runs(context).settings();
		MutableComponent message = Component.literal("Modifiers (mode " + settings.modifierMode.name().toLowerCase(Locale.ROOT) + ", " + settings.modifierCount + " per run):")
			.withStyle(ChatFormatting.GOLD);
		for (ModifierInfo info : page(page)) {
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
		message.append(pageFooter("/speedrun modifiers pool", page));
		context.getSource().sendSystemMessage(message);
		return Command.SINGLE_SUCCESS;
	}

	private static int setAlways(CommandContext<CommandSourceStack> context, boolean add) throws CommandSyntaxException {
		Settings settings = CommandRegistry.runs(context).settings();
		String id = StringArgumentType.getString(context, "id").toLowerCase(Locale.ROOT);
		if (!ModifierCatalog.isKnown(id)) {
			context.getSource().sendFailure(Component.literal("Unknown modifier: " + id + ". See /speedrun modifiers pool."));
			return 0;
		}
		if (add) {
			for (String other : settings.alwaysModifiers) {
				if (ModifierCatalog.conflicts(id, other)) {
					context.getSource().sendFailure(Component.literal(id + " conflicts with always-on " + other + "."));
					return 0;
				}
			}
		}
		boolean changed = add ? settings.alwaysModifiers.add(id) : settings.alwaysModifiers.remove(id);
		if (!changed) {
			context.getSource().sendFailure(Component.literal(id + " is already " + (add ? "always on." : "not always on.")));
			return 0;
		}
		settings.save();
		modifiers(context).refreshVote();
		context.getSource().sendSuccess(() -> Component.literal(ModifierCatalog.displayName(id) + (add ? " is now on in every run." : " is no longer always on.") + " Takes effect on the next run."), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int clearAlways(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Settings settings = CommandRegistry.runs(context).settings();
		settings.alwaysModifiers.clear();
		settings.save();
		modifiers(context).refreshVote();
		context.getSource().sendSuccess(() -> Component.literal("No modifiers are always on now. Takes effect on the next run."), true);
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
