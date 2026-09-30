package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /help, generated from every registered {@link CommandDoc}. Replaces vanilla /help. */
public final class HelpCommand {
	public static final int PER_PAGE = 8;

	private HelpCommand() {
	}

	public static void register(CommandRegistry registry) {
		removeRootLiteral(registry.dispatcher().getRoot(), "help");
		registry.document(new CommandDoc("help", "Help", "[<page>|<command>]", "Lists commands, or explains one",
			List.of("No argument: every command you can use, grouped by category, " + PER_PAGE + " per page.",
				"<page>: a page number of the list.",
				"<command>: full documentation for one command, such as speedrun tickrate."),
			List.of("help", "help 2", "help speedrun tickrate"), null));
		registry.dispatcher().register(literal("help")
			.executes(context -> list(registry, context, 1))
			.then(argument("query", StringArgumentType.greedyString())
				.suggests((context, builder) -> SharedSuggestionProvider.suggest(
					registry.docs().stream().filter(doc -> CommandRegistry.canUse(context.getSource(), doc)).map(CommandDoc::name), builder))
				.executes(context -> query(registry, context, StringArgumentType.getString(context, "query")))));
	}

	/** Removes a top-level command so it can be replaced instead of merged. Brigadier has no public removal API. */
	private static void removeRootLiteral(CommandNode<CommandSourceStack> root, String name) {
		if (root.getChild(name) == null) {
			return;
		}
		for (Field field : CommandNode.class.getDeclaredFields()) {
			if (!Map.class.isAssignableFrom(field.getType())) {
				continue;
			}
			try {
				field.setAccessible(true);
				((Map<?, ?>) field.get(root)).remove(name);
			} catch (ReflectiveOperationException | RuntimeException e) {
				SpeedrunCore.LOGGER.warn("Could not remove vanilla /{} from {}", name, field.getName(), e);
			}
		}
		if (root.getChild(name) != null) {
			SpeedrunCore.LOGGER.warn("Vanilla /{} is still registered; /{} will merge with it", name, name);
		}
	}

	private static int query(CommandRegistry registry, CommandContext<CommandSourceStack> context, String query) {
		int page = CommandLogic.parsePage(query);
		if (page > 0) {
			return list(registry, context, page);
		}
		CommandDoc doc = CommandLogic.find(registry.docs(), query);
		if (doc == null || !CommandRegistry.canUse(context.getSource(), doc)) {
			context.getSource().sendFailure(Component.literal("Unknown command: " + query + ". Use /help to list commands."));
			return 0;
		}
		context.getSource().sendSuccess(() -> detail(doc), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int list(CommandRegistry registry, CommandContext<CommandSourceStack> context, int requested) {
		CommandSourceStack source = context.getSource();
		List<CommandDoc> visible = CommandLogic.groupByCategory(registry.docs().stream().filter(doc -> CommandRegistry.canUse(source, doc)).toList());
		int pages = CommandLogic.pageCount(visible.size(), PER_PAGE);
		int page = CommandLogic.clampPage(requested, visible.size(), PER_PAGE);
		MutableComponent message = Component.empty().append(Component.literal("Commands").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		String category = null;
		for (CommandDoc doc : CommandLogic.page(visible, page, PER_PAGE)) {
			if (!doc.category().equals(category)) {
				category = doc.category();
				message.append(Component.literal("\n" + category).withStyle(ChatFormatting.YELLOW));
			}
			String usage = usageLine(doc);
			Style style = Style.EMPTY
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + doc.name()))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(usage).withStyle(ChatFormatting.AQUA)
					.append(Component.literal("\nClick for details").withStyle(ChatFormatting.GRAY))));
			message.append(Component.literal("\n  /" + doc.name()).withStyle(style.withColor(ChatFormatting.AQUA)))
				.append(Component.literal(" - " + doc.summary()).withStyle(style.withColor(ChatFormatting.GRAY)));
		}
		message.append(Component.literal("\n"))
			.append(arrow("<<", page > 1, page - 1))
			.append(Component.literal(" Page " + page + "/" + pages + " ").withStyle(ChatFormatting.GRAY))
			.append(arrow(">>", page < pages, page + 1));
		source.sendSuccess(() -> message, false);
		return Command.SINGLE_SUCCESS;
	}

	private static Component arrow(String text, boolean enabled, int target) {
		if (!enabled) {
			return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
		}
		return Component.literal(text).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
			.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/help " + target))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Page " + target))));
	}

	private static String usageLine(CommandDoc doc) {
		return "/" + doc.name() + (doc.usage().isEmpty() ? "" : " " + doc.usage());
	}

	private static Component detail(CommandDoc doc) {
		MutableComponent message = Component.empty()
			.append(Component.literal("/" + doc.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(doc.isAdmin() ? "  [admin]" : "").withStyle(ChatFormatting.RED))
			.append(Component.literal("\n" + doc.summary()).withStyle(ChatFormatting.WHITE));
		for (String line : doc.details()) {
			message.append(Component.literal("\n  " + line).withStyle(ChatFormatting.GRAY));
		}
		message.append(Component.literal("\nUsage: ").withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(usageLine(doc)).withStyle(ChatFormatting.AQUA));
		if (!doc.examples().isEmpty()) {
			message.append(Component.literal("\nExamples:").withStyle(ChatFormatting.YELLOW));
			for (String example : doc.examples()) {
				message.append(Component.literal("\n  /" + example).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
					.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + example))
					.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to put in chat")))));
			}
		}
		return message;
	}
}
