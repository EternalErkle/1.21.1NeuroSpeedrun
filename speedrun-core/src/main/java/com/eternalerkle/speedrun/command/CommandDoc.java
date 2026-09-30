package com.eternalerkle.speedrun.command;

import java.util.List;

/**
 * Documentation for one command, shown by /help. Every command registers one of these next to its Brigadier node,
 * so the help text lives beside the code it describes.
 *
 * @param name        the command as typed without the slash, e.g. "speedrun tickrate"
 * @param category    grouping shown in /help, e.g. "Run", "Settings", "Stats"
 * @param usage       argument syntax, e.g. "&lt;rate&gt; [now]"; empty when the command takes no arguments
 * @param summary     one line shown in the /help list
 * @param details     longer explanation lines shown by /help &lt;command&gt;
 * @param examples    full example invocations without the slash
 * @param permission  permission node, or null when every player can use it
 */
public record CommandDoc(String name, String category, String usage, String summary, List<String> details, List<String> examples, String permission) {
	public boolean isAdmin() {
		return permission != null;
	}
}
