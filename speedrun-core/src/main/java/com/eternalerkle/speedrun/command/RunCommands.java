package com.eternalerkle.speedrun.command;

import com.mojang.brigadier.Command;
import net.minecraft.network.chat.Component;

import java.util.List;

import static net.minecraft.commands.Commands.literal;

/** /start, /voteskip and the /speedrun admin tree for run control and settings. */
public final class RunCommands {
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
	}
}
