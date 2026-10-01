package com.eternalerkle.speedrun.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPermissionTest {
	private static CommandSourceStack source(int level) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, level, "test", Component.literal("test"), null, null);
	}

	private static boolean parses(CommandDispatcher<CommandSourceStack> dispatcher, String command, CommandSourceStack source) {
		var parse = dispatcher.parse(command, source);
		return !parse.getReader().canRead() && parse.getExceptions().isEmpty() && parse.getContext().getCommand() != null;
	}

	@Test
	void playerCommandsNeedNoOp() {
		CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
		net.minecraft.server.commands.HelpCommand.register(dispatcher);
		Commands.register(dispatcher);
		assertTrue(dispatcher.getRoot().getChild("help").getChild("query") != null, "vanilla /help replaced");
		CommandSourceStack player = source(0);
		for (String command : List.of("help", "help 2", "help speedrun tickrate", "start", "voteskip", "best", "best all",
			"stats", "stats server", "splits", "modifiers", "vote 1")) {
			assertTrue(parses(dispatcher, command, player), command);
		}
		assertFalse(parses(dispatcher, "speedrun settings", player));
		assertFalse(parses(dispatcher, "speedrun trust list", player));
		assertTrue(parses(dispatcher, "speedrun settings", source(2)));
		assertTrue(parses(dispatcher, "speedrun trust list", source(2)));
	}
}
