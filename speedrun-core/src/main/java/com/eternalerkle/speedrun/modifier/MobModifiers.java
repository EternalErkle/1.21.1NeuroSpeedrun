package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.mixin.ModifierCreeperMixin;
import com.eternalerkle.speedrun.run.ActiveRun;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/** charged_up, hasty_mobs, horde, mob_randomizer and blaze_boost. Mixins call into here. */
public final class MobModifiers {
	/** Creative-only or technical items never handed out by mob_randomizer, on top of spawn eggs and operator blocks. */
	private static final Set<Item> EXCLUDED = Set.of(Items.AIR, Items.BARRIER, Items.LIGHT, Items.STRUCTURE_VOID, Items.DEBUG_STICK,
		Items.KNOWLEDGE_BOOK, Items.COMMAND_BLOCK_MINECART, Items.BEDROCK, Items.END_PORTAL_FRAME, Items.REINFORCED_DEEPSLATE,
		Items.BUDDING_AMETHYST, Items.PETRIFIED_OAK_SLAB, Items.SPAWNER, Items.TRIAL_SPAWNER, Items.VAULT, Items.FROGSPAWN);

	/**
	 * True only while the natural spawner runs for a run level with horde active. Set and cleared around
	 * NaturalSpawner.spawnForChunk by a mixin, and read by the MobCategory cap mixin. Server thread only.
	 */
	public static boolean doublingMonsterCap;

	private static final Map<EntityType<?>, Item> randomDrops = new HashMap<>();
	private static Random dropRandom = new Random();
	private static List<Item> dropCandidates;

	private MobModifiers() {
	}

	static void registerEvents() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Creeper creeper && Modifiers.isActive(ModifierCatalog.CHARGED_UP, level) && !creeper.isPowered()) {
				creeper.getEntityData().set(ModifierCreeperMixin.speedrun$poweredData(), true);
			}
			if (entity instanceof Mob mob && mob instanceof Enemy && Modifiers.isActive(ModifierCatalog.HASTY_MOBS, level)
				&& !mob.hasEffect(MobEffects.MOVEMENT_SPEED)) {
				mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));
			}
		});
	}

	static void onRunStart(ActiveRun run, long seed) {
		randomDrops.clear();
		dropRandom = new Random(seed);
		dropCandidates = null;
	}

	static void onRunEnd() {
		randomDrops.clear();
		dropCandidates = null;
		doublingMonsterCap = false;
	}

	/** Called at the start of dropFromLootTable. Returns true when the loot table must be skipped. */
	public static boolean replaceLoot(LivingEntity entity) {
		if (!(entity instanceof Mob) || !Modifiers.isActive(ModifierCatalog.MOB_RANDOMIZER, entity.level())) {
			return false;
		}
		Item item = randomDrops.computeIfAbsent(entity.getType(), type -> pickItem(((ServerLevel) entity.level()).enabledFeatures()));
		entity.spawnAtLocation(new ItemStack(item));
		return true;
	}

	/** Wraps the loot consumer so blazes drop twice as many rods while blaze_boost is active. */
	public static Consumer<ItemStack> boostLoot(LivingEntity entity, Consumer<ItemStack> drop) {
		if (!(entity instanceof Blaze) || !Modifiers.isActive(ModifierCatalog.BLAZE_BOOST, entity.level())) {
			return drop;
		}
		return stack -> {
			if (stack.is(Items.BLAZE_ROD)) {
				stack.setCount(stack.getCount() * 2);
			}
			drop.accept(stack);
		};
	}

	private static Item pickItem(FeatureFlagSet features) {
		if (dropCandidates == null) {
			dropCandidates = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (!EXCLUDED.contains(item) && !(item instanceof SpawnEggItem) && !(item instanceof GameMasterBlockItem) && item.isEnabled(features)) {
					dropCandidates.add(item);
				}
			}
		}
		return dropCandidates.get(dropRandom.nextInt(dropCandidates.size()));
	}
}
