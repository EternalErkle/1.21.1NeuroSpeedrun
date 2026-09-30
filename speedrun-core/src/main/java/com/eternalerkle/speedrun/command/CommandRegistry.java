package com.eternalerkle.speedrun.command;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.run.RunManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Collects every command's documentation for /help and holds shared helpers for command modules. */
public final class CommandRegistry {
	/** Op level used when no permissions mod is installed. */
	public static final int ADMIN_OP_LEVEL = 2;
	public static final String ADMIN = "speedrun.admin";

	private final CommandDispatcher<CommandSourceStack> dispatcher;
	private final List<CommandDoc> docs = new ArrayList<>();

	public CommandRegistry(CommandDispatcher<CommandSourceStack> dispatcher) {
		this.dispatcher = dispatcher;
	}

	public CommandDispatcher<CommandSourceStack> dispatcher() {
		return dispatcher;
	}

	public void document(CommandDoc doc) {
		docs.add(doc);
	}

	public List<CommandDoc> docs() {
		return Collections.unmodifiableList(docs);
	}

	/** Brigadier requirement for an admin permission node, falling back to op level 2. */
	public static Predicate<CommandSourceStack> requires(String permission) {
		return Permissions.require(permission, ADMIN_OP_LEVEL);
	}

	public static boolean canUse(CommandSourceStack source, CommandDoc doc) {
		return doc.permission() == null || Permissions.check(source, doc.permission(), ADMIN_OP_LEVEL);
	}

	/** The run manager, or throws a readable command error when the server has not finished starting. */
	public static RunManager runs(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		RunManager runs = SpeedrunCore.runs();
		if (runs == null) {
			throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("The server is still starting.")).create();
		}
		return runs;
	}
}
