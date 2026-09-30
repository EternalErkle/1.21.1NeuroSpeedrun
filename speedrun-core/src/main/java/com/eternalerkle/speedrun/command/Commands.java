package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public final class Commands {
	private Commands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("start").executes(context -> {
			if (SpeedrunCore.runs() == null) {
				return 0;
			}
			Component error = SpeedrunCore.runs().requestStart();
			if (error != null) {
				context.getSource().sendFailure(error);
				return 0;
			}
			return 1;
		}));
	}
}
