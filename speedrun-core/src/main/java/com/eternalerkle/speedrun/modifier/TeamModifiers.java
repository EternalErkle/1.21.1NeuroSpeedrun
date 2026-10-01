package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/** tethered, buddy_system, hot_potato, juggernaut, inventory_rotation, no_nametags and radio_silence. */
final class TeamModifiers implements RunFeature {
	private static final double TETHER_DISTANCE = 40.0;
	/** Beyond this, such as after a respawn, a player is teleported to the nearest teammate instead of pulled. */
	private static final double SNAP_DISTANCE = 100.0;
	private static final double BUDDY_RANGE = 16.0;
	private static final long SECOND_NANOS = 1_000_000_000L;
	private static final long POTATO_BURN_NANOS = 5 * SECOND_NANOS;
	private static final float POTATO_DAMAGE = 1.0F;
	private static final String NAMETAG_TEAM = "speedrun_nonametags";
	private static final ResourceLocation JUGGERNAUT_HEALTH = ResourceLocation.fromNamespaceAndPath("speedrun", "modifier/juggernaut_health");
	private static final ResourceLocation JUGGERNAUT_DAMAGE = ResourceLocation.fromNamespaceAndPath("speedrun", "modifier/juggernaut_damage");

	@Nullable
	private static TeamModifiers instance;

	private final RunManager runs;
	private final Random random = new Random();
	@Nullable
	private ActiveRun run;
	@Nullable
	private UUID potato;
	private long nextBurnNanos;
	/** The potato cannot be passed again before this, so two players trading hits do not bounce it every tick. */
	private long passableAfterNanos;
	@Nullable
	private UUID juggernaut;
	private long nextRotationNanos;
	private int ticks;

	private TeamModifiers(RunManager runs) {
		this.runs = runs;
	}

	static void register(RunManager runs) {
		instance = new TeamModifiers(runs);
		runs.addFeature(instance);
		// A crash or a run that was not kept can leave the team behind in the saved scoreboard.
		removeNametagTeam(runs.server());
	}

	static void registerEvents() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			TeamModifiers team = instance;
			if (team != null && entity instanceof ServerPlayer target && player instanceof ServerPlayer attacker) {
				team.tryPassPotato(attacker, target);
			}
			return InteractionResult.PASS;
		});
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (Modifiers.isActive(ModifierCatalog.RADIO_SILENCE, sender.serverLevel())) {
				sender.displayClientMessage(Component.literal("Radio silence: chat is disabled this run.").withStyle(ChatFormatting.RED), true);
				return false;
			}
			return true;
		});
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		this.run = run;
		potato = null;
		juggernaut = null;
		nextBurnNanos = run.clockNanos() + POTATO_BURN_NANOS;
		passableAfterNanos = 0;
		nextRotationNanos = Modifiers.nextTimedNanos(run);
		removeNametagTeam(runs.server());
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		if (potato != null) {
			out.addProperty("hotPotato", potato.toString());
		}
		if (juggernaut != null) {
			out.addProperty("juggernaut", juggernaut.toString());
		}
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (in.has("hotPotato")) {
			potato = UUID.fromString(in.get("hotPotato").getAsString());
		}
		if (in.has("juggernaut")) {
			juggernaut = UUID.fromString(in.get("juggernaut").getAsString());
		}
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		for (ServerPlayer player : runs.server().getPlayerList().getPlayers()) {
			undo(run, player);
		}
		removeNametagTeam(runs.server());
		this.run = null;
		potato = null;
		juggernaut = null;
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.NO_NAMETAGS)) {
			ServerScoreboard scoreboard = runs.server().getScoreboard();
			PlayerTeam team = scoreboard.getPlayerTeam(NAMETAG_TEAM);
			if (team == null) {
				team = scoreboard.addPlayerTeam(NAMETAG_TEAM);
				team.setNameTagVisibility(Team.Visibility.NEVER);
			}
			scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
		}
		if (run.modifiers.contains(ModifierCatalog.JUGGERNAUT) && player.getUUID().equals(juggernaut)) {
			applyJuggernaut(player);
		}
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		undo(run, player);
		ServerScoreboard scoreboard = runs.server().getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam(NAMETAG_TEAM);
		if (team != null && scoreboard.getPlayersTeam(player.getScoreboardName()) == team) {
			scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
		}
	}

	/** Removes every effect and attribute modifier this class may have put on the player for the run's modifiers. */
	private static void undo(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.BUDDY_SYSTEM)) {
			clearInfinite(player, MobEffects.REGENERATION);
			clearInfinite(player, MobEffects.WEAKNESS);
		}
		if (run.modifiers.contains(ModifierCatalog.HOT_POTATO)) {
			clearInfinite(player, MobEffects.GLOWING);
		}
		removeJuggernaut(player);
	}

	@Override
	public void tick(ActiveRun run) {
		List<String> active = run.modifiers;
		boolean tether = active.contains(ModifierCatalog.TETHERED);
		boolean buddy = active.contains(ModifierCatalog.BUDDY_SYSTEM);
		boolean hotPotato = active.contains(ModifierCatalog.HOT_POTATO);
		boolean jugg = active.contains(ModifierCatalog.JUGGERNAUT);
		boolean rotation = active.contains(ModifierCatalog.INVENTORY_ROTATION);
		if (!tether && !buddy && !hotPotato && !jugg && !rotation) {
			return;
		}
		ticks++;
		boolean second = ticks % 20 == 0;
		List<ServerPlayer> players = Modifiers.playersIn(run);
		if (tether && ticks % 5 == 0) {
			tether(players);
		}
		if (buddy && second) {
			buddyUp(players);
		}
		if (hotPotato) {
			tickPotato(run, players, second);
		}
		if (jugg && second) {
			tickJuggernaut(players);
		}
		if (rotation && run.clockNanos() >= nextRotationNanos) {
			nextRotationNanos += Modifiers.TIMED_INTERVAL_NANOS;
			rotateInventories(players);
		}
	}

	// ---- tethered ----

	/** Pulls each player toward their farthest teammate in the same dimension once that teammate is over 40 blocks away. */
	private static void tether(List<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (!player.isAlive()) {
				continue;
			}
			ServerPlayer nearest = null;
			ServerPlayer farthest = null;
			double nearestSqr = Double.MAX_VALUE;
			double farthestSqr = -1;
			for (ServerPlayer other : players) {
				if (other == player || !other.isAlive() || other.level() != player.level()) {
					continue;
				}
				double distanceSqr = other.distanceToSqr(player);
				if (distanceSqr < nearestSqr) {
					nearestSqr = distanceSqr;
					nearest = other;
				}
				if (distanceSqr > farthestSqr) {
					farthestSqr = distanceSqr;
					farthest = other;
				}
			}
			if (nearest == null) {
				continue;
			}
			if (nearestSqr > SNAP_DISTANCE * SNAP_DISTANCE) {
				player.teleportTo(nearest.serverLevel(), nearest.getX(), nearest.getY(), nearest.getZ(), player.getYRot(), player.getXRot());
				player.resetFallDistance();
				player.displayClientMessage(Component.literal("Tethered: pulled back to " + nearest.getGameProfile().getName() + ".").withStyle(ChatFormatting.LIGHT_PURPLE), true);
			} else if (farthestSqr > TETHER_DISTANCE * TETHER_DISTANCE) {
				double excess = Math.sqrt(farthestSqr) - TETHER_DISTANCE;
				double strength = Math.min(0.4 + excess * 0.05, 1.5);
				Vec3 direction = farthest.position().subtract(player.position()).normalize();
				player.setDeltaMovement(direction.x * strength, direction.y * strength + 0.15, direction.z * strength);
				// Makes the server send the new motion to the client, the same way knockback does.
				player.hurtMarked = true;
				player.resetFallDistance();
			}
		}
	}

	// ---- buddy_system ----

	private static void buddyUp(List<ServerPlayer> players) {
		double rangeSqr = BUDDY_RANGE * BUDDY_RANGE;
		for (ServerPlayer player : players) {
			if (!player.isAlive()) {
				continue;
			}
			boolean near = false;
			for (ServerPlayer other : players) {
				if (other != player && other.isAlive() && other.level() == player.level() && other.distanceToSqr(player) <= rangeSqr) {
					near = true;
					break;
				}
			}
			ensureInfinite(player, near ? MobEffects.REGENERATION : MobEffects.WEAKNESS);
			clearInfinite(player, near ? MobEffects.WEAKNESS : MobEffects.REGENERATION);
		}
	}

	private static void ensureInfinite(ServerPlayer player, Holder<MobEffect> effect) {
		MobEffectInstance current = player.getEffect(effect);
		if (current == null || !current.isInfiniteDuration()) {
			player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, true, false));
		}
	}

	/** Removes the effect only when it is an infinite one from this class, leaving potions alone. */
	private static void clearInfinite(ServerPlayer player, Holder<MobEffect> effect) {
		MobEffectInstance current = player.getEffect(effect);
		if (current != null && current.isInfiniteDuration()) {
			player.removeEffect(effect);
		}
	}

	// ---- hot_potato ----

	/** Needs two players. The holder glows and takes half a heart every 5 seconds of run time. */
	private void tickPotato(ActiveRun run, List<ServerPlayer> players, boolean second) {
		if (players.size() < 2) {
			return;
		}
		ServerPlayer holder = find(players, potato);
		if (holder == null) {
			holder = players.get(random.nextInt(players.size()));
			givePotato(run, null, holder);
		}
		long now = run.clockNanos();
		if (now >= nextBurnNanos) {
			nextBurnNanos = now + POTATO_BURN_NANOS;
			if (holder.isAlive()) {
				holder.hurt(holder.damageSources().magic(), POTATO_DAMAGE);
			}
		}
		if (second && holder.isAlive()) {
			ensureInfinite(holder, MobEffects.GLOWING);
			holder.displayClientMessage(Component.literal("You have the Hot Potato! Hit a teammate to pass it.").withStyle(ChatFormatting.GOLD), true);
		}
	}

	private void tryPassPotato(ServerPlayer attacker, ServerPlayer target) {
		ActiveRun live = run;
		if (live == null || !attacker.getUUID().equals(potato) || !Modifiers.isActive(ModifierCatalog.HOT_POTATO, attacker.level())
			|| !live.worlds.contains(target.serverLevel()) || target.isSpectator() || live.clockNanos() < passableAfterNanos) {
			return;
		}
		givePotato(live, attacker, target);
	}

	private void givePotato(ActiveRun run, @Nullable ServerPlayer from, ServerPlayer to) {
		if (from != null) {
			clearInfinite(from, MobEffects.GLOWING);
		}
		potato = to.getUUID();
		long now = run.clockNanos();
		nextBurnNanos = now + POTATO_BURN_NANOS;
		passableAfterNanos = now + SECOND_NANOS;
		ensureInfinite(to, MobEffects.GLOWING);
		String name = to.getGameProfile().getName();
		runs.broadcast(Component.literal(from == null ? name + " has the Hot Potato!" : from.getGameProfile().getName() + " passed the Hot Potato to " + name + "!")
			.withStyle(ChatFormatting.GOLD));
	}

	// ---- juggernaut ----

	private void tickJuggernaut(List<ServerPlayer> players) {
		if (players.isEmpty()) {
			return;
		}
		ServerPlayer chosen = find(players, juggernaut);
		if (chosen == null && juggernaut == null) {
			chosen = players.get(random.nextInt(players.size()));
			juggernaut = chosen.getUUID();
			applyJuggernaut(chosen);
			chosen.setHealth(chosen.getMaxHealth());
			runs.broadcast(Component.literal(chosen.getGameProfile().getName() + " is the Juggernaut: double health and damage.").withStyle(ChatFormatting.DARK_RED));
		} else if (chosen != null) {
			// Reapplied every second because a respawned player starts without transient modifiers.
			applyJuggernaut(chosen);
		}
	}

	private static void applyJuggernaut(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (health != null) {
			health.addOrUpdateTransientModifier(new AttributeModifier(JUGGERNAUT_HEALTH, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		if (damage != null) {
			damage.addOrUpdateTransientModifier(new AttributeModifier(JUGGERNAUT_DAMAGE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
	}

	private static void removeJuggernaut(ServerPlayer player) {
		AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
		if (health != null) {
			health.removeModifier(JUGGERNAUT_HEALTH);
		}
		if (damage != null) {
			damage.removeModifier(JUGGERNAUT_DAMAGE);
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	// ---- inventory_rotation ----

	/** Each player gets the whole inventory, armor and offhand included, of the player before them. Needs two players. */
	private void rotateInventories(List<ServerPlayer> players) {
		if (players.size() < 2) {
			return;
		}
		List<ServerPlayer> order = new ArrayList<>(players);
		order.sort(Comparator.comparing(ServerPlayer::getUUID));
		List<List<ItemStack>> snapshots = new ArrayList<>();
		for (ServerPlayer player : order) {
			Inventory inventory = player.getInventory();
			List<ItemStack> copy = new ArrayList<>();
			for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
				copy.add(inventory.getItem(slot).copy());
			}
			snapshots.add(copy);
		}
		for (int i = 0; i < order.size(); i++) {
			Inventory inventory = order.get(i).getInventory();
			List<ItemStack> incoming = snapshots.get((i + order.size() - 1) % order.size());
			for (int slot = 0; slot < incoming.size(); slot++) {
				inventory.setItem(slot, incoming.get(slot));
			}
			inventory.setChanged();
		}
		runs.broadcast(Component.literal("Inventory Rotation! Everyone got the next player's inventory.").withStyle(ChatFormatting.LIGHT_PURPLE));
	}

	// ---- no_nametags ----

	private static void removeNametagTeam(MinecraftServer server) {
		ServerScoreboard scoreboard = server.getScoreboard();
		PlayerTeam team = scoreboard.getPlayerTeam(NAMETAG_TEAM);
		if (team != null) {
			scoreboard.removePlayerTeam(team);
		}
	}

	@Nullable
	private static ServerPlayer find(List<ServerPlayer> players, @Nullable UUID id) {
		if (id == null) {
			return null;
		}
		for (ServerPlayer player : players) {
			if (player.getUUID().equals(id)) {
				return player;
			}
		}
		return null;
	}
}
