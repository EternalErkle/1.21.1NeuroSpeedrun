package com.eternalerkle.speedrun.modifier;

/** One entry of the modifier catalog, as listed in docs/modifiers.md. */
public record ModifierInfo(String id, String name, String effect, Tag tag) {
	public enum Tag {
		HARDER("Harder"),
		CHAOS("Chaos"),
		BRUTAL("Brutal"),
		HELPFUL("Helpful");

		public final String label;

		Tag(String label) {
			this.label = label;
		}
	}
}
