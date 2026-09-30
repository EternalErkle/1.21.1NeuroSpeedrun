package com.eternalerkle.speedrun.modifier;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Map;

/**
 * Modifiers built from player attribute modifiers. Each uses a fixed {@code speedrun:modifier/...} id and is added as a
 * transient modifier, so it is never written to player data and removal by id is always complete.
 */
final class PlayerModifiers {
	private record Entry(Holder<Attribute> attribute, ResourceLocation id, double amount, Operation operation) {
		AttributeModifier modifier() {
			return new AttributeModifier(id, amount, operation);
		}
	}

	private static final Map<String, List<Entry>> BY_MODIFIER = Map.of(
		// Base max health is 20: subtracting leaves 2 (one heart) or 10 (five hearts).
		ModifierCatalog.ONE_HEART, List.of(entry(Attributes.MAX_HEALTH, "one_heart", -18.0, Operation.ADD_VALUE)),
		ModifierCatalog.HALF_HEALTH, List.of(entry(Attributes.MAX_HEALTH, "half_health", -10.0, Operation.ADD_VALUE)),
		ModifierCatalog.HEAVY_LANDING, List.of(entry(Attributes.FALL_DAMAGE_MULTIPLIER, "heavy_landing", 2.0, Operation.ADD_MULTIPLIED_BASE)),
		ModifierCatalog.TINY, List.of(entry(Attributes.SCALE, "tiny", -0.5, Operation.ADD_MULTIPLIED_BASE)),
		ModifierCatalog.GIANT, List.of(
			entry(Attributes.SCALE, "giant", 1.0, Operation.ADD_MULTIPLIED_BASE),
			entry(Attributes.BLOCK_INTERACTION_RANGE, "giant_block_reach", 1.0, Operation.ADD_MULTIPLIED_BASE),
			entry(Attributes.ENTITY_INTERACTION_RANGE, "giant_entity_reach", 1.0, Operation.ADD_MULTIPLIED_BASE)),
		ModifierCatalog.MOON_GRAVITY, List.of(
			entry(Attributes.GRAVITY, "moon_gravity", -0.7, Operation.ADD_MULTIPLIED_BASE),
			// Low gravity makes plain jumps fall about 4 blocks, which would hurt on every landing. Raising the safe fall
			// distance from 3 to 10 roughly matches the slower landing speed.
			entry(Attributes.SAFE_FALL_DISTANCE, "moon_gravity_safe_fall", 7.0, Operation.ADD_VALUE)),
		ModifierCatalog.HEAVY_GRAVITY, List.of(
			entry(Attributes.GRAVITY, "heavy_gravity", 1.0, Operation.ADD_MULTIPLIED_BASE),
			// Doubled gravity alone caps jumps near 0.6 blocks, so players could not climb a single block.
			// 30% more jump strength keeps jumps lower than normal but still clears one block.
			entry(Attributes.JUMP_STRENGTH, "heavy_gravity_jump", 0.3, Operation.ADD_MULTIPLIED_BASE))
	);

	private PlayerModifiers() {
	}

	private static Entry entry(Holder<Attribute> attribute, String path, double amount, Operation operation) {
		return new Entry(attribute, ResourceLocation.fromNamespaceAndPath("speedrun", "modifier/" + path), amount, operation);
	}

	static void apply(List<String> modifiers, ServerPlayer player) {
		for (String id : modifiers) {
			for (Entry entry : BY_MODIFIER.getOrDefault(id, List.of())) {
				AttributeInstance instance = player.getAttribute(entry.attribute());
				if (instance != null) {
					instance.addOrUpdateTransientModifier(entry.modifier());
				}
			}
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	/** Removes every attribute modifier this class can add, whether or not it is currently applied. */
	static void remove(ServerPlayer player) {
		for (List<Entry> entries : BY_MODIFIER.values()) {
			for (Entry entry : entries) {
				AttributeInstance instance = player.getAttribute(entry.attribute());
				if (instance != null) {
					instance.removeModifier(entry.id());
				}
			}
		}
	}
}
