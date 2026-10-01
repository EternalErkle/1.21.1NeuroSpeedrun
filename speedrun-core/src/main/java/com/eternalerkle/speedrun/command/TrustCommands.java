package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.config.Settings;
import com.eternalerkle.speedrun.run.RunManager;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** /speedrun trust: lets non-op players use /speedrun admin commands without vanilla op powers. Changing it needs real op. */
public final class TrustCommands {
	private TrustCommands() {
	}

	public static void register(CommandRegistry registry) {
		registry.dispatcher().register(literal("speedrun").requires(CommandRegistry.requires(CommandRegistry.ADMIN))
			.then(literal("trust").requires(CommandRegistry.requires(CommandRegistry.OP))
				.then(literal("add").then(argument("player", GameProfileArgument.gameProfile())
					.executes(context -> add(context, GameProfileArgument.getGameProfiles(context, "player")))))
				.then(literal("remove").then(argument("player", StringArgumentType.word())
					.suggests((context, builder) -> {
						RunManager runs = SpeedrunCore.runs();
						return SharedSuggestionProvider.suggest(runs == null ? List.of()
							: runs.settings().trusted.stream().map(player -> player.name).toList(), builder);
					})
					.executes(context -> remove(context, StringArgumentType.getString(context, "player")))))
				.then(literal("list").executes(TrustCommands::list))));
		registry.document(new CommandDoc("speedrun trust", "Admin", "<add|remove> <player> | list", "Lets a player use admin commands without op",
			List.of("add: the player can use every /speedrun command. They get no vanilla op powers such as /gamemode.",
				"remove: takes that back. list: shows every trusted player.",
				"Needs op level 2 or the server console. Trusted players cannot change the list."),
			List.of("speedrun trust add Steve", "speedrun trust remove Steve", "speedrun trust list"), CommandRegistry.OP));
	}

	private static int add(CommandContext<CommandSourceStack> context, Collection<GameProfile> profiles) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		Settings settings = runs.settings();
		int added = 0;
		for (GameProfile profile : profiles) {
			if (settings.isTrusted(profile.getId())) {
				context.getSource().sendFailure(Component.literal(profile.getName() + " is already trusted."));
				continue;
			}
			settings.trusted.add(new Settings.TrustedPlayer(profile.getId(), profile.getName()));
			added++;
			refresh(runs.server(), profile.getId());
			context.getSource().sendSuccess(() -> Component.literal(profile.getName() + " is now trusted.").withStyle(ChatFormatting.GREEN), true);
		}
		settings.save();
		return added;
	}

	private static int remove(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		RunManager runs = CommandRegistry.runs(context);
		Settings settings = runs.settings();
		Settings.TrustedPlayer match = settings.trusted.stream()
			.filter(player -> name.equalsIgnoreCase(player.name) || name.equalsIgnoreCase(player.uuid))
			.findFirst().orElse(null);
		if (match == null) {
			context.getSource().sendFailure(Component.literal(name + " is not trusted."));
			return 0;
		}
		settings.trusted.remove(match);
		settings.save();
		refresh(runs.server(), UUID.fromString(match.uuid));
		context.getSource().sendSuccess(() -> Component.literal(match.name + " is no longer trusted.").withStyle(ChatFormatting.GREEN), true);
		return Command.SINGLE_SUCCESS;
	}

	private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		List<Settings.TrustedPlayer> trusted = CommandRegistry.runs(context).settings().trusted;
		String names = trusted.isEmpty() ? "none" : String.join(", ", trusted.stream().map(player -> player.name).toList());
		context.getSource().sendSuccess(() -> Component.literal("Trusted players: " + names), false);
		return trusted.size();
	}

	/** Resends the command tree so the player's client shows or hides the admin commands right away. */
	private static void refresh(MinecraftServer server, UUID uuid) {
		ServerPlayer player = server.getPlayerList().getPlayer(uuid);
		if (player != null) {
			server.getCommands().sendCommands(player);
		}
	}
}
