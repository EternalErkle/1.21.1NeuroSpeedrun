package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.run.ActiveRun;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** starter_kit, hotbar_only, shared_inventory and shuffle. */
final class InventoryModifiers {
	private static final int HOTBAR_SIZE = Inventory.getSelectionSize();

	private final Random random;
	/** Players who already got this run's starter kit, so reconnecting does not hand out a second one. */
	private final Set<UUID> kitted = new HashSet<>();
	/** shared_inventory's canonical copy of every inventory slot, or null before the first sync. */
	@Nullable
	private List<ItemStack> shared;
	private long nextShuffleNanos;

	InventoryModifiers(Random random) {
		this.random = random;
	}

	void onRunStart(ActiveRun run) {
		kitted.clear();
		shared = null;
		nextShuffleNanos = run.startNanos + Modifiers.TIMED_INTERVAL_NANOS;
	}

	void onRunEnd() {
		kitted.clear();
		shared = null;
	}

	void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.STARTER_KIT) && kitted.add(player.getUUID())) {
			giveKit(player);
		}
		if (run.modifiers.contains(ModifierCatalog.SHARED_INVENTORY) && shared != null) {
			// A joining player takes the shared inventory; otherwise their own (empty or stale) one would win the next sync.
			write(player, shared);
		}
	}

	void tick(ActiveRun run, List<ServerPlayer> players) {
		if (run.modifiers.contains(ModifierCatalog.SHUFFLE) && System.nanoTime() >= nextShuffleNanos) {
			nextShuffleNanos += Modifiers.TIMED_INTERVAL_NANOS;
			for (ServerPlayer player : players) {
				shuffle(player);
			}
			if (!players.isEmpty()) {
				players.get(0).getServer().getPlayerList().broadcastSystemMessage(
					Component.literal("Shuffle! Inventories have been scrambled.").withStyle(ChatFormatting.LIGHT_PURPLE), false);
			}
		}
		if (run.modifiers.contains(ModifierCatalog.HOTBAR_ONLY)) {
			for (ServerPlayer player : players) {
				enforceHotbar(player);
			}
		}
		if (run.modifiers.contains(ModifierCatalog.SHARED_INVENTORY)) {
			syncShared(players);
		}
	}

	private static void giveKit(ServerPlayer player) {
		for (ItemStack stack : List.of(new ItemStack(Items.STONE_PICKAXE), new ItemStack(Items.STONE_AXE), new ItemStack(Items.STONE_SHOVEL),
			new ItemStack(Items.STONE_SWORD), new ItemStack(Items.BUCKET), new ItemStack(Items.BREAD, 16))) {
			player.getInventory().placeItemBackInInventory(stack);
		}
	}

	/**
	 * hotbar_only moves anything in main slots 9-35 back into the hotbar, and drops what does not fit.
	 * A per-tick sweep is used instead of filling the slots with placeholder items, because placeholders would have to be
	 * guarded against moving, dropping, crafting and every container screen. The cost: with a full hotbar, picked up items
	 * land in a main slot and are dropped again, re-collectable after the usual 2 second pickup delay.
	 */
	private static void enforceHotbar(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = HOTBAR_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
			ItemStack stack = inventory.items.get(slot);
			if (stack.isEmpty()) {
				continue;
			}
			inventory.items.set(slot, ItemStack.EMPTY);
			for (int hotbar = 0; hotbar < HOTBAR_SIZE && !stack.isEmpty(); hotbar++) {
				ItemStack target = inventory.items.get(hotbar);
				if (target.isEmpty()) {
					inventory.items.set(hotbar, stack);
					stack = ItemStack.EMPTY;
				} else if (ItemStack.isSameItemSameComponents(target, stack) && target.getCount() < target.getMaxStackSize()) {
					int moved = Math.min(stack.getCount(), target.getMaxStackSize() - target.getCount());
					target.grow(moved);
					stack.shrink(moved);
				}
			}
			if (!stack.isEmpty()) {
				player.drop(stack, false, false);
			}
			inventory.setChanged();
		}
	}

	/** Shuffles the 36 main and hotbar slots. Armor and offhand stay put. */
	private void shuffle(ServerPlayer player) {
		List<ItemStack> items = new ArrayList<>(player.getInventory().items);
		Collections.shuffle(items, random);
		for (int i = 0; i < items.size(); i++) {
			player.getInventory().items.set(i, items.get(i));
		}
		player.getInventory().setChanged();
	}

	/**
	 * shared_inventory keeps one canonical copy. Each tick, the first player whose inventory differs from it becomes the
	 * new canonical copy, which is then written to everyone else. Limitations: if two players change their inventories in
	 * the same tick, only the first player's change survives; items held on the cursor or in a crafting grid are not
	 * shared; with one player the copy just follows that player.
	 */
	private void syncShared(List<ServerPlayer> players) {
		if (players.isEmpty()) {
			return;
		}
		if (shared == null) {
			shared = snapshot(players.get(0));
		}
		ServerPlayer changed = null;
		for (ServerPlayer player : players) {
			if (!matches(player, shared)) {
				changed = player;
				break;
			}
		}
		if (changed == null) {
			return;
		}
		shared = snapshot(changed);
		for (ServerPlayer player : players) {
			if (player != changed) {
				write(player, shared);
			}
		}
	}

	private static List<ItemStack> snapshot(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		List<ItemStack> copy = new ArrayList<>(inventory.getContainerSize());
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			copy.add(inventory.getItem(i).copy());
		}
		return copy;
	}

	private static boolean matches(ServerPlayer player, List<ItemStack> copy) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < copy.size(); i++) {
			if (!ItemStack.matches(inventory.getItem(i), copy.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static void write(ServerPlayer player, List<ItemStack> copy) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < copy.size(); i++) {
			inventory.setItem(i, copy.get(i).copy());
		}
		inventory.setChanged();
	}
}
