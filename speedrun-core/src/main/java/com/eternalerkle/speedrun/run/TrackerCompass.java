package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.modifier.Modifiers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Every player gets a compass at the start of each run. Right-click it to pick the next player to track; it then
 * points at that player. The needle comes from a lodestone target that is moved to the tracked player twice a second.
 */
public final class TrackerCompass implements RunFeature {
	private static final String TAG = "speedrun_tracker";
	private static final String TARGET = "target";
	private static final int UPDATE_TICKS = 10;
	private static final int HOTBAR_SLOT = 8;

	private final RunManager runs;
	/** Players who already got this run's compass, so reconnecting does not hand out a second one. */
	private final Set<UUID> given = new HashSet<>();
	private int ticks;

	@Nullable
	private static TrackerCompass instance;
	private static boolean eventsRegistered;

	private TrackerCompass(RunManager runs) {
		this.runs = runs;
	}

	public static void register(RunManager runs) {
		instance = new TrackerCompass(runs);
		runs.addFeature(instance);
		if (!eventsRegistered) {
			eventsRegistered = true;
			UseItemCallback.EVENT.register((player, world, hand) -> {
				ItemStack stack = player.getItemInHand(hand);
				TrackerCompass compass = instance;
				if (compass == null || !isTracker(stack) || !(player instanceof ServerPlayer serverPlayer)) {
					return InteractionResultHolder.pass(ItemStack.EMPTY);
				}
				compass.selectNext(serverPlayer, stack);
				return InteractionResultHolder.success(stack);
			});
		}
	}

	@Override
	public void onRunStart(ActiveRun run) {
		given.clear();
		ticks = 0;
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		given.clear();
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		if (!runs.settings().trackerCompass || !given.add(player.getUUID())) {
			return;
		}
		ItemStack stack = create();
		Inventory inventory = player.getInventory();
		if (inventory.getItem(HOTBAR_SLOT).isEmpty()) {
			inventory.setItem(HOTBAR_SLOT, stack);
		} else {
			player.getInventory().placeItemBackInInventory(stack);
		}
	}

	@Override
	public void tick(ActiveRun run) {
		if (++ticks % UPDATE_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : Modifiers.playersIn(run)) {
			Inventory inventory = player.getInventory();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				ItemStack stack = inventory.getItem(slot);
				if (isTracker(stack)) {
					point(stack, target(stack, run));
				}
			}
		}
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		JsonArray ids = new JsonArray();
		given.forEach(id -> ids.add(id.toString()));
		out.add("trackerCompassGiven", ids);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (in.has("trackerCompassGiven")) {
			for (JsonElement id : in.getAsJsonArray("trackerCompassGiven")) {
				given.add(UUID.fromString(id.getAsString()));
			}
		}
	}

	private static ItemStack create() {
		ItemStack stack = new ItemStack(Items.COMPASS);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean(TAG, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.ITEM_NAME, Component.literal("Player Tracker").withStyle(ChatFormatting.AQUA));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			Component.literal("Right-click to pick a player to track").withStyle(ChatFormatting.GRAY))));
		return stack;
	}

	private static boolean isTracker(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return stack.is(Items.COMPASS) && data != null && data.contains(TAG);
	}

	/** Moves this compass to the next player in the run, by name, wrapping around. */
	private void selectNext(ServerPlayer holder, ItemStack stack) {
		ActiveRun run = runs.run();
		List<ServerPlayer> others = run == null ? List.of() : Modifiers.playersIn(run).stream()
			.filter(player -> player != holder)
			.sorted(Comparator.comparing(player -> player.getGameProfile().getName().toLowerCase(Locale.ROOT)))
			.toList();
		if (others.isEmpty()) {
			holder.displayClientMessage(Component.literal("No other players to track.").withStyle(ChatFormatting.RED), true);
			return;
		}
		ServerPlayer current = target(stack, run);
		int index = current == null ? -1 : others.indexOf(current);
		ServerPlayer next = others.get((index + 1) % others.size());
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putUUID(TARGET, next.getUUID()));
		point(stack, next);
		holder.displayClientMessage(Component.literal("Tracking ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(next.getGameProfile().getName()).withStyle(ChatFormatting.AQUA)), true);
		holder.serverLevel().playSound(null, holder.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.5F);
	}

	/** The tracked player, if online and in the run. */
	@Nullable
	private static ServerPlayer target(ItemStack stack, @Nullable ActiveRun run) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (run == null || data == null || !data.contains(TARGET)) {
			return null;
		}
		ServerPlayer player = run.worlds.overworld().getServer().getPlayerList().getPlayer(data.copyTag().getUUID(TARGET));
		return player != null && run.worlds.contains(player.serverLevel()) ? player : null;
	}

	/** Points the needle at {@code target}, or makes it spin when there is none. In another dimension it spins too. */
	private static void point(ItemStack stack, @Nullable ServerPlayer target) {
		LodestoneTracker tracker = new LodestoneTracker(
			target == null ? Optional.empty() : Optional.of(GlobalPos.of(target.level().dimension(), target.blockPosition())), false);
		if (!tracker.equals(stack.get(DataComponents.LODESTONE_TRACKER))) {
			stack.set(DataComponents.LODESTONE_TRACKER, tracker);
		}
	}
}
