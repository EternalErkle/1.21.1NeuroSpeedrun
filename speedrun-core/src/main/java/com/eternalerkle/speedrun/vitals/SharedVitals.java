package com.eternalerkle.speedrun.vitals;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.run.RunState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.food.FoodData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Shared health and shared hunger, each toggled independently by settings and fixed for a run at its start.
 * <p>
 * Design: the feature keeps one canonical value per vital (health, food level, saturation, exhaustion). Every tick it
 * compares each player's value with what it wrote to that player last tick, adds the changes of all players to the
 * canonical value, clamps it, and writes it back to everyone. Vanilla keeps running damage, healing, eating and
 * exhaustion per player, so no damage or food path needs a hook. Two players each taking 3 damage in one tick cost
 * the shared bar 6. Each player's natural regeneration and starvation count separately, so both scale with the
 * number of players.
 * <p>
 * Health is clamped to the highest max health in the run and written to each player capped at their own max health,
 * so max-health modifiers and absorption keep working. A drop in a player's max health is not counted as damage.
 * <p>
 * Health that reaches 0 only through the sum of several hits never passes vanilla's death check, and writing 0 would
 * leave players in a broken dying state. Instead one player, the one who lost the most this tick, is hurt with their
 * last damage source, falling back to a generic kill. That routes through {@link RunManager#allowDeath}, which resets
 * the run once. Only players inside the run's worlds during RUNNING are ever touched.
 * <p>
 * Every hit a player takes is announced in chat with the amount and cause, so the team can see who is draining the
 * shared bar. Lethal hits are left to the death summary.
 */
public final class SharedVitals implements RunFeature {
	private static final float MAX_FOOD = 20.0F;
	private static final float MAX_EXHAUSTION = 40.0F;

	private final RunManager runs;
	private final SharedValue health = new SharedValue();
	private final SharedValue food = new SharedValue();
	private final SharedValue saturation = new SharedValue();
	private final SharedValue exhaustion = new SharedValue();
	private final Set<UUID> members = new HashSet<>();

	/** The instance for the running server, read by the damage event, which is registered once per JVM. */
	@Nullable
	private static SharedVitals instance;
	private static boolean eventsRegistered;

	private SharedVitals(RunManager runs) {
		this.runs = runs;
	}

	public static void register(RunManager runs) {
		instance = new SharedVitals(runs);
		runs.addFeature(instance);
		if (!eventsRegistered) {
			eventsRegistered = true;
			ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
				SharedVitals vitals = instance;
				if (vitals != null && entity instanceof ServerPlayer player && damageTaken > 0) {
					vitals.onDamage(player, source, damageTaken);
				}
			});
		}
	}

	/** Announces a hit while shared health is on. Uses the damage after armor, before it is pooled. */
	private void onDamage(ServerPlayer player, DamageSource source, float amount) {
		ActiveRun run = runs.run();
		if (run != null && run.sharedHealth && isLive(run) && takesPart(run, player)) {
			announceDamage(player, source, amount);
		}
	}

	@Override
	public void onRunStart(ActiveRun run) {
		clear();
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		clear();
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		if (isLive(run) && takesPart(run, player)) {
			adopt(run, player);
		}
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		forget(player.getUUID());
	}

	@Override
	public void tick(ActiveRun run) {
		if ((!run.sharedHealth && !run.sharedHunger) || !isLive(run)) {
			return;
		}
		List<ServerPlayer> players = new ArrayList<>();
		Set<UUID> present = new HashSet<>();
		for (ServerPlayer player : runs.server().getPlayerList().getPlayers()) {
			if (takesPart(run, player)) {
				players.add(player);
				present.add(player.getUUID());
			}
		}
		for (UUID id : List.copyOf(members)) {
			if (!present.contains(id)) {
				forget(id);
			}
		}
		// Newcomers adopt the shared values this tick and contribute changes from the next one.
		List<ServerPlayer> tracked = new ArrayList<>();
		for (ServerPlayer player : players) {
			if (members.contains(player.getUUID())) {
				tracked.add(player);
			} else {
				adopt(run, player);
			}
		}
		if (run.sharedHunger) {
			tickHunger(tracked);
		}
		if (run.sharedHealth) {
			tickHealth(tracked);
		}
	}

	private void tickHunger(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			UUID id = player.getUUID();
			FoodData data = player.getFoodData();
			food.observe(id, data.getFoodLevel(), MAX_FOOD);
			saturation.observe(id, data.getSaturationLevel(), Float.MAX_VALUE);
			exhaustion.observe(id, data.getExhaustionLevel(), MAX_EXHAUSTION);
		}
		float foodLevel = food.apply(0, MAX_FOOD);
		// Vanilla never lets saturation exceed the food level.
		saturation.apply(0, foodLevel);
		exhaustion.apply(0, MAX_EXHAUSTION);
		for (ServerPlayer player : players) {
			writeHunger(player);
		}
	}

	private void tickHealth(List<ServerPlayer> players) {
		if (players.isEmpty()) {
			return;
		}
		float highestMax = 0;
		ServerPlayer hardestHit = players.get(0);
		float biggestLoss = 0;
		for (ServerPlayer player : players) {
			float max = player.getMaxHealth();
			highestMax = Math.max(highestMax, max);
			float delta = health.observe(player.getUUID(), player.getHealth(), max);
			if (delta < biggestLoss) {
				biggestLoss = delta;
				hardestHit = player;
			}
		}
		if (health.apply(0, highestMax) <= 0) {
			killOne(players, hardestHit);
			return;
		}
		for (ServerPlayer player : players) {
			writeHealth(player);
		}
	}

	/** Broadcasts "❤ Alpha -1.5 (Zombie)": who took damage, how many hearts, and what caused it. */
	private void announceDamage(ServerPlayer player, DamageSource source, float amount) {
		MutableComponent message = Component.literal("❤ ").withStyle(ChatFormatting.RED)
			.append(player.getDisplayName().copy().withStyle(ChatFormatting.WHITE))
			.append(Component.literal(" -" + formatHearts(amount)).withStyle(ChatFormatting.RED));
		message.append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
			.append(causeName(source))
			.append(Component.literal(")").withStyle(ChatFormatting.GRAY));
		runs.broadcast(message);
	}

	/** Health points as hearts, with at most one decimal: 3 health is "1.5", 4 is "2". */
	static String formatHearts(float health) {
		float hearts = Math.round(health * 5.0F) / 10.0F;
		return hearts == (int) hearts ? Integer.toString((int) hearts) : Float.toString(hearts);
	}

	/** The attacker's name when there is one, otherwise the damage type spelled out ("inFire" becomes "in fire"). */
	private static Component causeName(DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker != null) {
			return attacker.getDisplayName().copy().withStyle(ChatFormatting.GRAY);
		}
		return Component.literal(spellOut(source.getMsgId())).withStyle(ChatFormatting.GRAY);
	}

	static String spellOut(String msgId) {
		return msgId.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ').replace('.', ' ').toLowerCase(Locale.ROOT);
	}

	/** Kills one player through normal damage so the run manager handles the death. Stops as soon as it resets. */
	private void killOne(List<ServerPlayer> players, ServerPlayer first) {
		List<ServerPlayer> order = new ArrayList<>(players.size());
		order.add(first);
		for (ServerPlayer player : players) {
			if (player != first) {
				order.add(player);
			}
		}
		for (ServerPlayer player : order) {
			DamageSource last = player.getLastDamageSource();
			if (last != null) {
				player.hurt(last, Float.MAX_VALUE);
				if (runs.state() != RunState.RUNNING) {
					return;
				}
			}
			player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
			if (runs.state() != RunState.RUNNING) {
				return;
			}
		}
		SpeedrunCore.LOGGER.warn("Shared health reached 0 but no player could be killed; retrying next tick");
	}

	private void adopt(ActiveRun run, ServerPlayer player) {
		members.add(player.getUUID());
		if (run.sharedHealth) {
			if (!health.isInitialized()) {
				health.set(player.getHealth());
			}
			writeHealth(player);
		}
		if (run.sharedHunger) {
			if (!food.isInitialized()) {
				FoodData data = player.getFoodData();
				food.set(data.getFoodLevel());
				saturation.set(data.getSaturationLevel());
				exhaustion.set(data.getExhaustionLevel());
			}
			writeHunger(player);
		}
	}

	private void writeHealth(ServerPlayer player) {
		float value = health.writeFor(player.getUUID(), player.getMaxHealth());
		if (player.getHealth() != value) {
			player.setHealth(value);
		}
	}

	private void writeHunger(ServerPlayer player) {
		UUID id = player.getUUID();
		FoodData data = player.getFoodData();
		int foodLevel = Math.round(food.writeFor(id, MAX_FOOD));
		data.setFoodLevel(foodLevel);
		data.setSaturation(saturation.writeFor(id, foodLevel));
		data.setExhaustion(exhaustion.writeFor(id, MAX_EXHAUSTION));
	}

	private boolean isLive(ActiveRun run) {
		return runs.state() == RunState.RUNNING && runs.run() == run;
	}

	private static boolean takesPart(ActiveRun run, ServerPlayer player) {
		return run.worlds.contains(player.serverLevel()) && !player.isSpectator() && !player.isCreative();
	}

	private void forget(UUID id) {
		members.remove(id);
		health.forget(id);
		food.forget(id);
		saturation.forget(id);
		exhaustion.forget(id);
	}

	private void clear() {
		members.clear();
		health.clear();
		food.clear();
		saturation.clear();
		exhaustion.clear();
	}
}
