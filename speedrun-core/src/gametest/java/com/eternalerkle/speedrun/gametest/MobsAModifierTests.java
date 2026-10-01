package com.eternalerkle.speedrun.gametest;

import com.eternalerkle.speedrun.SpeedrunCore;
import com.eternalerkle.speedrun.modifier.ModifierCatalog;
import com.eternalerkle.speedrun.modifier.Modifiers;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunManager;
import com.eternalerkle.speedrun.run.RunState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;

import java.util.ArrayList;
import java.util.List;

/** Starts a run with several mobs-a modifiers forced and checks their effect on freshly spawned mobs. */
public class MobsAModifierTests implements FabricGameTest {
	private static final long DEADLINE_NANOS = 180L * 1_000_000_000L;

	@SuppressWarnings("removal")
	@GameTest(template = EMPTY_STRUCTURE, batch = "mobs_a", timeoutTicks = Integer.MAX_VALUE)
	public void mobsAModifiersApply(GameTestHelper helper) {
		long deadline = System.nanoTime() + DEADLINE_NANOS;
		List<Entity> spawned = new ArrayList<>();
		ServerPlayer[] player = new ServerPlayer[1];
		Spider[] spider = new Spider[1];
		GameTestSequence seq = helper.startSequence();
		seq.thenExecute(() -> leaveAll(helper));
		waitFor(helper, seq, deadline, () -> runs().state() == RunState.LOBBY, "the server has not returned to the lobby")
			.thenExecute(() -> {
				Modifiers.instance().force(List.of(ModifierCatalog.BABY_ZOMBIES, ModifierCatalog.SHORT_FUSE, ModifierCatalog.SPIDER_JOCKEYS,
					ModifierCatalog.ARMORED_DRAGON, ModifierCatalog.ARROW_DRAGON, ModifierCatalog.PET_WOLVES));
				player[0] = helper.makeMockServerPlayerInLevel();
				runs().requestStart();
			});
		waitFor(helper, seq, deadline, () -> runs().state() == RunState.RUNNING && runs().run() != null, "the run has not started")
			.thenExecute(() -> {
				ActiveRun run = runs().run();
				helper.assertTrue(run.modifiers.contains(ModifierCatalog.BABY_ZOMBIES), "the forced modifiers were not used: " + run.modifiers);
				ServerLevel level = player[0].serverLevel();
				helper.assertTrue(run.worlds.contains(level), "the player is not in the run");
				BlockPos pos = player[0].blockPosition();

				Zombie zombie = spawn(spawned, EntityType.ZOMBIE, level, pos);
				helper.assertTrue(zombie.isBaby(), "baby_zombies: the zombie is not a baby");

				Creeper creeper = spawn(spawned, EntityType.CREEPER, level, pos);
				CompoundTag tag = new CompoundTag();
				creeper.addAdditionalSaveData(tag);
				helper.assertTrue(tag.getShort("Fuse") == 15, "short_fuse: fuse is " + tag.getShort("Fuse"));

				EnderDragon dragon = spawn(spawned, EntityType.ENDER_DRAGON, level, pos.above(10));
				helper.assertTrue(dragon.getMaxHealth() == 400.0F && dragon.getHealth() == 400.0F,
					"armored_dragon: health " + dragon.getHealth() + "/" + dragon.getMaxHealth());
				helper.assertFalse(dragon.hurt(level.damageSources().playerAttack(player[0]), 10.0F), "arrow_dragon: a melee hit was accepted");
				helper.assertTrue(dragon.getHealth() == 400.0F, "arrow_dragon: a melee hit did damage");

				spider[0] = spawn(spawned, EntityType.SPIDER, level, pos);

				List<Wolf> wolves = level.getEntitiesOfClass(Wolf.class, player[0].getBoundingBox().inflate(4.0), wolf -> wolf.isOwnedBy(player[0]));
				helper.assertTrue(wolves.size() == 1, "pet_wolves: expected one tamed wolf, found " + wolves.size());
				spawned.addAll(wolves);
			})
			.thenIdle(3)
			.thenExecute(() -> {
				helper.assertTrue(spider[0].getFirstPassenger() instanceof Skeleton, "spider_jockeys: the spider has no skeleton rider");
				spawned.add(spider[0].getFirstPassenger());
				spawned.forEach(Entity::discard);
				leaveAll(helper);
			})
			.thenSucceed();
	}

	private static <T extends Entity> T spawn(List<Entity> spawned, EntityType<T> type, ServerLevel level, BlockPos pos) {
		T entity = type.spawn(level, pos, MobSpawnType.COMMAND);
		if (entity == null) {
			throw new GameTestAssertException("could not spawn " + type.getDescriptionId());
		}
		spawned.add(entity);
		return entity;
	}

	private static RunManager runs() {
		return SpeedrunCore.runs();
	}

	private static GameTestSequence waitFor(GameTestHelper helper, GameTestSequence seq, long deadline, java.util.function.BooleanSupplier condition, String message) {
		return seq
			.thenWaitUntil(() -> {
				if (!condition.getAsBoolean() && System.nanoTime() < deadline) {
					throw new GameTestAssertException(message);
				}
			})
			.thenExecute(() -> helper.assertTrue(condition.getAsBoolean(), message));
	}

	private static void leaveAll(GameTestHelper helper) {
		for (ServerPlayer player : List.copyOf(helper.getLevel().getServer().getPlayerList().getPlayers())) {
			player.connection.disconnect(Component.literal("GameTest finished"));
		}
	}
}
