package com.eternalerkle.speedrun.gametest;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.config.Goal;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.run.RunState;
import com.eternalerkle.speedrun.stats.RunRecord;
import com.eternalerkle.speedrun.stats.Stats;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.DimensionTransition;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Drives the real run lifecycle on the GameTest server with mock players joined through the vanilla player list.
 * Every test uses its own batch, so tests run one after another against the single shared {@link RunManager}.
 * Each test first empties the server and waits for the lobby, then starts a fresh run.
 * The GameTest run sets {@code speedrun.emptyGraceSeconds} to {@link #GRACE_SECONDS} in build.gradle.
 */
public class RunLifecycleTests implements FabricGameTest {
	/**
	 * The GameTest server ticks as fast as it can, so tick timeouts mean nothing for the real-time run lifecycle.
	 * Tests never time out by ticks; every wait has a wall-clock deadline instead.
	 */
	private static final int TIMEOUT = Integer.MAX_VALUE;
	/** World generation, the countdown, the death room and the grace period all run in real time. */
	private static final long DEADLINE_SECONDS = 180;
	private static final double GRACE_SECONDS = 10.0;

	/** State shared between the steps of one test. */
	private static final class Ctx {
		final List<ServerPlayer> players = new ArrayList<>();
		ActiveRun run;
		int history;
		int deaths;
		long nanos;
		Goal goal;
		final long deadline = System.nanoTime() + DEADLINE_SECONDS * 1_000_000_000L;
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "reset_once", timeoutTicks = TIMEOUT)
	public void deathDuringRunTriggersOneReset(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			ServerPlayer player = ctx.players.get(0);
			ctx.history = stats().history.size();
			ctx.deaths = stats().player(player.getUUID(), null).lifetimeDeaths;
			kill(player);
			assertState(helper, RunState.RESETTING);
			helper.assertTrue(runs().run() == null, "the run was not cleared");
			helper.assertTrue(stats().history.size() == ctx.history + 1, "expected exactly one run record, got " + (stats().history.size() - ctx.history));
			helper.assertTrue(last().result == RunRecord.Result.DIED, "the run was recorded as " + last().result);
			helper.assertTrue(last().attempt == ctx.run.attempt, "the record is for the wrong attempt");
			helper.assertTrue(stats().player(player.getUUID(), null).lifetimeDeaths == ctx.deaths + 1, "the death was not counted once");
			helper.assertTrue(player.isAlive() && player.getHealth() == player.getMaxHealth(), "the player was not kept alive");
		});
		waitFor(helper, seq, ctx, () -> runs().state() == RunState.RUNNING, "the next run has not started")
			.thenExecute(() -> {
				helper.assertTrue(runs().run().attempt == ctx.run.attempt + 1, "the next run skipped an attempt");
				helper.assertTrue(stats().history.size() == ctx.history + 1, "the reset recorded extra runs");
			});
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "second_death", timeoutTicks = TIMEOUT)
	public void secondDeathWhileResettingIsIgnored(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 2);
		seq.thenExecute(() -> {
			ServerPlayer first = ctx.players.get(0);
			ServerPlayer second = ctx.players.get(1);
			ctx.history = stats().history.size();
			ctx.deaths = stats().player(second.getUUID(), null).lifetimeDeaths;
			kill(first);
			assertState(helper, RunState.RESETTING);
			int afterFirst = stats().history.size();
			kill(second);
			boolean allowed = runs().allowDeath(second, second.damageSources().fellOutOfWorld());
			helper.assertFalse(allowed, "a death during RESETTING was allowed");
			assertState(helper, RunState.RESETTING);
			helper.assertTrue(afterFirst == ctx.history + 1, "the first death did not record the run");
			helper.assertTrue(stats().history.size() == afterFirst, "the second death recorded another run");
			helper.assertTrue(stats().player(second.getUUID(), null).lifetimeDeaths == ctx.deaths, "the second death was counted");
			helper.assertTrue(second.isAlive() && second.getHealth() == second.getMaxHealth(), "the second player was not kept alive");
		});
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "inventories", timeoutTicks = TIMEOUT)
	public void newRunClearsInventoriesAndEnderChests(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			ServerPlayer player = ctx.players.get(0);
			player.getInventory().add(new ItemStack(Items.DIAMOND_PICKAXE));
			player.getInventory().add(new ItemStack(Items.COBBLESTONE, 32));
			player.getEnderChestInventory().setItem(0, new ItemStack(Items.ENDER_PEARL, 8));
			kill(player);
			assertState(helper, RunState.RESETTING);
			// Clearing belongs to the run start, so the items survive the death itself.
			helper.assertFalse(player.getInventory().isEmpty(), "the inventory was cleared before the next run");
			helper.assertFalse(player.getEnderChestInventory().isEmpty(), "the ender chest was cleared before the next run");
		});
		waitFor(helper, seq, ctx, () -> runs().state() == RunState.RUNNING, "the next run has not started")
			.thenExecute(() -> {
				ServerPlayer player = ctx.players.get(0);
				helper.assertTrue(player.getInventory().isEmpty(), "the inventory was not cleared");
				helper.assertTrue(player.getEnderChestInventory().isEmpty(), "the ender chest was not cleared");
			});
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "new_seed", timeoutTicks = TIMEOUT)
	public void nextRunUsesNewSeedAndWorlds(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> kill(ctx.players.get(0)));
		waitFor(helper, seq, ctx, () -> runs().state() == RunState.RUNNING, "the next run has not started")
			.thenExecute(() -> {
				ActiveRun next = runs().run();
				ServerPlayer player = ctx.players.get(0);
				helper.assertTrue(next != ctx.run, "the run did not change");
				helper.assertTrue(next.worlds != ctx.run.worlds, "the run reused its worlds");
				helper.assertTrue(next.worlds.seed != ctx.run.worlds.seed, "the run reused seed " + next.worlds.seed);
				helper.assertTrue(ctx.run.worlds.isDeleted(), "the old worlds were not deleted");
				helper.assertTrue(player.serverLevel() == next.worlds.overworld(), "the player is not in the new overworld");
			});
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "empty_grace", timeoutTicks = TIMEOUT)
	public void emptyServerLosesRunAfterGrace(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			ctx.history = stats().history.size();
			ctx.nanos = System.nanoTime();
			leaveAll(helper);
			assertState(helper, RunState.RUNNING);
		});
		waitFor(helper, seq, ctx, () -> elapsedSeconds(ctx) >= 2, "two seconds have not passed").thenExecute(() -> {
			helper.assertTrue(elapsedSeconds(ctx) < GRACE_SECONDS, "the server was too slow to check the grace period");
			assertState(helper, RunState.RUNNING);
			helper.assertTrue(runs().run() == ctx.run, "the run changed during the grace period");
		});
		waitFor(helper, seq, ctx, () -> runs().state() == RunState.LOBBY, "the run was not lost")
			.thenExecute(() -> {
				helper.assertTrue(elapsedSeconds(ctx) >= GRACE_SECONDS, "the run was lost after " + elapsedSeconds(ctx) + "s");
				helper.assertTrue(runs().run() == null, "the run was not cleared");
				helper.assertTrue(stats().history.size() == ctx.history + 1, "expected one run record");
				helper.assertTrue(last().result == RunRecord.Result.ABANDONED, "the run was recorded as " + last().result);
				helper.assertTrue(ctx.run.worlds.isDeleted(), "the abandoned worlds were not deleted");
			})
			.thenSucceed();
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "grace_rejoin", timeoutTicks = TIMEOUT)
	public void rejoinDuringGraceKeepsRun(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			ctx.history = stats().history.size();
			ctx.nanos = System.nanoTime();
			leaveAll(helper);
		});
		waitFor(helper, seq, ctx, () -> elapsedSeconds(ctx) >= 2, "two seconds have not passed").thenExecute(() -> {
			assertState(helper, RunState.RUNNING);
			ctx.players.add(join(helper));
			ctx.nanos = System.nanoTime();
		});
		waitFor(helper, seq, ctx, () -> elapsedSeconds(ctx) > GRACE_SECONDS + 1, "waiting past the grace period")
			.thenExecute(() -> {
				assertState(helper, RunState.RUNNING);
				helper.assertTrue(runs().run() == ctx.run, "the run changed");
				helper.assertTrue(stats().history.size() == ctx.history, "a run was recorded");
				ServerPlayer player = ctx.players.get(ctx.players.size() - 1);
				assertOnlyIn(helper, player, ctx.run.worlds.overworld());
			});
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "mid_run_join", timeoutTicks = TIMEOUT)
	public void midRunJoinPlacesPlayerOnlyInRunWorld(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			ServerPlayer late = join(helper);
			ctx.players.add(late);
			assertOnlyIn(helper, late, ctx.run.worlds.overworld());
		});
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "all_bosses", timeoutTicks = TIMEOUT)
	public void allBossesWinsOnlyAfterAllThreeKills(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		ctx.goal = runs().settings().goal;
		runs().settings().goal = Goal.ALLBOSSES;
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			helper.assertTrue(ctx.run.goal == Goal.ALLBOSSES, "the run did not copy the allbosses goal");
			ctx.history = stats().history.size();
			ServerLevel overworld = ctx.run.worlds.overworld();
			BlockPos spawn = ctx.run.worlds.spawn();

			killBoss(helper, EntityType.WARDEN, overworld, spawn);
			assertBosses(helper, ctx, EnumSet.of(ActiveRun.Boss.WARDEN));
			killBoss(helper, EntityType.WARDEN, overworld, spawn);
			assertBosses(helper, ctx, EnumSet.of(ActiveRun.Boss.WARDEN));
			killBoss(helper, EntityType.WITHER, overworld, spawn);
			assertBosses(helper, ctx, EnumSet.of(ActiveRun.Boss.WARDEN, ActiveRun.Boss.WITHER));

			// EnderDragon.kill() skips LivingEntity.die, and a real dragon only dies after its death animation,
			// so the dragon's AFTER_DEATH is delivered directly.
			EnderDragon dragon = EntityType.ENDER_DRAGON.create(ctx.run.worlds.end());
			runs().afterDeath(dragon, dragon.damageSources().genericKill());
			assertState(helper, RunState.VICTORY);
			helper.assertTrue(ctx.run.bossesKilled.equals(EnumSet.allOf(ActiveRun.Boss.class)), "not every boss was recorded");
			helper.assertTrue(stats().history.size() == ctx.history + 1, "expected one run record");
			helper.assertTrue(last().result == RunRecord.Result.WON, "the run was recorded as " + last().result);
		});
		seq.thenExecute(() -> runs().settings().goal = ctx.goal);
		finish(helper, seq);
	}

	@GameTest(template = EMPTY_STRUCTURE, batch = "portals", timeoutTicks = TIMEOUT)
	public void netherPortalsRouteBetweenRunDimensions(GameTestHelper helper) {
		Ctx ctx = new Ctx();
		GameTestSequence seq = helper.startSequence();
		startFreshRun(helper, seq, ctx, 1);
		seq.thenExecute(() -> {
			ServerLevel overworld = ctx.run.worlds.overworld();
			ServerLevel nether = ctx.run.worlds.nether();
			ServerLevel end = ctx.run.worlds.end();
			helper.assertTrue(runs().worlds().netherPortalTarget(overworld) == nether, "the run overworld does not lead to the run nether");
			helper.assertTrue(runs().worlds().netherPortalTarget(nether) == overworld, "the run nether does not lead to the run overworld");
			helper.assertTrue(runs().worlds().netherPortalTarget(end) == null, "the run end has a nether portal target");
			helper.assertTrue(runs().worlds().netherPortalTarget(helper.getLevel().getServer().overworld()) == null, "the hub has a nether portal target");

			// Exercises the NetherPortalBlock mixin end to end, including exit portal placement.
			ServerPlayer player = ctx.players.get(0);
			NetherPortalBlock portal = (NetherPortalBlock) Blocks.NETHER_PORTAL;
			DimensionTransition toNether = portal.getPortalDestination(overworld, player, player.blockPosition());
			helper.assertTrue(toNether != null && toNether.newLevel() == nether, "an overworld portal did not lead to the run nether");
			DimensionTransition toOverworld = portal.getPortalDestination(nether, player, player.blockPosition());
			helper.assertTrue(toOverworld != null && toOverworld.newLevel() == overworld, "a nether portal did not lead to the run overworld");
		});
		finish(helper, seq);
	}

	// ---- helpers ----

	private static RunManager runs() {
		return SpeedrunCore.runs();
	}

	private static Stats stats() {
		return runs().stats();
	}

	private static RunRecord last() {
		List<RunRecord> history = stats().history;
		return history.get(history.size() - 1);
	}

	private static double elapsedSeconds(Ctx ctx) {
		return (System.nanoTime() - ctx.nanos) / 1e9;
	}

	private static void assertState(GameTestHelper helper, RunState expected) {
		helper.assertTrue(runs().state() == expected, "expected state " + expected + " but was " + runs().state());
	}

	/** Empties the server, waits for the lobby, joins {@code count} mock players and waits for their run to start. */
	private static void startFreshRun(GameTestHelper helper, GameTestSequence seq, Ctx ctx, int count) {
		seq.thenExecute(() -> leaveAll(helper));
		waitFor(helper, seq, ctx, () -> runs().state() == RunState.LOBBY, "the server has not returned to the lobby")
			.thenExecute(() -> {
				for (int i = 0; i < count; i++) {
					ctx.players.add(join(helper));
				}
				runs().requestStart();
			});
		waitFor(helper, seq, ctx, () -> runs().state() == RunState.RUNNING && runs().run() != null, "the run has not started")
			.thenExecute(() -> {
				ctx.run = runs().run();
				for (ServerPlayer player : ctx.players) {
					helper.assertTrue(ctx.run.worlds.contains(player.serverLevel()), "a player was not moved into the run");
				}
			});
	}

	/** Waits until {@code condition} holds, failing the test once the wall-clock deadline passes. */
	private static GameTestSequence waitFor(GameTestHelper helper, GameTestSequence seq, Ctx ctx, BooleanSupplier condition, String message) {
		return seq
			.thenWaitUntil(() -> {
				if (!condition.getAsBoolean() && System.nanoTime() < ctx.deadline) {
					throw new GameTestAssertException(message);
				}
			})
			// A failed wait assertion only retries, so the deadline failure is raised from a step that fails the test.
			.thenExecute(() -> helper.assertTrue(condition.getAsBoolean(), message + " after " + DEADLINE_SECONDS + "s"));
	}

	private static void finish(GameTestHelper helper, GameTestSequence seq) {
		seq.thenExecute(() -> leaveAll(helper)).thenSucceed();
	}

	/** Joins a mock player through {@code PlayerList.placeNewPlayer}, which fires Fabric's JOIN event. */
	@SuppressWarnings("removal")
	private static ServerPlayer join(GameTestHelper helper) {
		return helper.makeMockServerPlayerInLevel();
	}

	/** Disconnects every player through the normal network path, which fires Fabric's DISCONNECT event. */
	private static void leaveAll(GameTestHelper helper) {
		for (ServerPlayer player : List.copyOf(helper.getLevel().getServer().getPlayerList().getPlayers())) {
			player.connection.disconnect(Component.literal("GameTest finished"));
		}
		helper.assertTrue(helper.getLevel().getServer().getPlayerList().getPlayers().isEmpty(), "players are still connected");
	}

	/** A fatal hit through vanilla damage handling, so Fabric's ALLOW_DEATH reaches the run manager. */
	private static void kill(ServerPlayer player) {
		// A mock player has no client to acknowledge the last dimension change, which leaves it invulnerable.
		player.hasChangedDimension();
		player.hurt(player.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
	}

	private static void killBoss(GameTestHelper helper, EntityType<? extends LivingEntity> type, ServerLevel level, BlockPos pos) {
		LivingEntity boss = type.create(level);
		boss.moveTo(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0, 0);
		helper.assertTrue(level.addFreshEntity(boss), "could not spawn " + type.getDescriptionId());
		boss.kill();
		helper.assertTrue(boss.isDeadOrDying(), type.getDescriptionId() + " did not die");
	}

	/** The player is in {@code expected}, and no other level tracks them. */
	private static void assertOnlyIn(GameTestHelper helper, ServerPlayer player, ServerLevel expected) {
		helper.assertTrue(player.serverLevel() == expected, "the player is in " + player.serverLevel().dimension().location());
		helper.assertTrue(expected.players().contains(player), "the target level does not track the player");
		for (ServerLevel level : helper.getLevel().getServer().getAllLevels()) {
			helper.assertFalse(level != expected && level.players().contains(player), "the player is also tracked by " + level.dimension().location());
		}
	}

	private static void assertBosses(GameTestHelper helper, Ctx ctx, EnumSet<ActiveRun.Boss> expected) {
		assertState(helper, RunState.RUNNING);
		helper.assertTrue(ctx.run.bossesKilled.equals(expected), "expected bosses " + expected + " but got " + ctx.run.bossesKilled);
	}
}
