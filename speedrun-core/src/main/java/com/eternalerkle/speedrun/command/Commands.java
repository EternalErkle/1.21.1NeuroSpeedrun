package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.modifier.ModifierCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

/** Registers every command module. /help is registered last so it sees every other command's documentation. */
public final class Commands {
	private Commands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		CommandRegistry registry = new CommandRegistry(dispatcher);
		RunCommands.register(registry);
		StatsCommands.register(registry);
		ModifierCommands.register(registry);
		TrustCommands.register(registry);
		HelpCommand.register(registry);
	}
}
