package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.mixin.ModifierMobsACreeperAccessor;
import com.eternalerkle.speedrun.mixin.ModifierMobsAMobAccessor;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * sharpshooters, short_fuse, enderman_rage, invisible_mobs, glowing_mobs, silent_mobs, baby_zombies, spider_jockeys,
 * kamikaze, spawn_randomizer, nether_invasion, phantom_menace, patrol_season, revenge, ambush, pet_wolves,
 * piglin_friends, pearl_bonanza, blaze_swarm, armored_dragon and arrow_dragon. The ModifierMobsA* mixins call into here.
 */
public final class MobsAModifiers implements RunFeature {
	/** Marks a spider that already had its spider_jockeys rider, so a reload or a dead rider never brings a new one. */
	private static final String JOCKEY_TAG = "speedrun_jockey";
	private static final ResourceLocation ARMORED_DRAGON_ID = ResourceLocation.fromNamespaceAndPath("speedrun", "armored_dragon");
	private static final float KAMIKAZE_POWER = 2.0F;
	/** patrol_season waits this long into the run before its first patrol. */
	private static final long PATROL_GRACE_TICKS = 2400L;
	/** Mobs that can never come out of spawn_randomizer: bosses and unused or overpowered types. */
	private static final Set<EntityType<?>> RANDOMIZER_EXCLUDED = Set.of(EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.WARDEN,
		EntityType.ELDER_GUARDIAN, EntityType.GIANT, EntityType.ILLUSIONER);
	private static final List<EntityType<? extends Mob>> AMBUSHERS = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER,
		EntityType.CREEPER, EntityType.WITCH, EntityType.HUSK, EntityType.STRAY, EntityType.CAVE_SPIDER, EntityType.PILLAGER,
		EntityType.VINDICATOR, EntityType.BLAZE);

	/** Extra spawn entries. Reused instances, because the spawner matches entries by identity. */
	private static final List<MobSpawnSettings.SpawnerData> INVASION = List.of(new MobSpawnSettings.SpawnerData(EntityType.BLAZE, 40, 1, 2),
		new MobSpawnSettings.SpawnerData(EntityType.PIGLIN, 40, 2, 4), new MobSpawnSettings.SpawnerData(EntityType.GHAST, 15, 1, 1));
	private static final List<MobSpawnSettings.SpawnerData> SWARM = List.of(new MobSpawnSettings.SpawnerData(EntityType.BLAZE, 50, 1, 3));

	@Nullable
	private static MobsAModifiers instance;
	private static boolean eventsRegistered;
	private static final Map<MobCategory, List<EntityType<?>>> randomizerCandidates = new EnumMap<>(MobCategory.class);
	/** Pearls the current enderman's loot table dropped. Server thread only, reset before each loot roll. */
	private static int pearlsDropped;

	private final Random random = new Random();
	/** Spawns and explosions requested from inside entity events, run on the next tick instead. */
	private final List<Runnable> pending = new ArrayList<>();
	/** Players who already got this run's pet_wolves wolf. */
	private final Set<UUID> wolfGiven = new HashSet<>();
	private PhantomSpawner phantoms = new PhantomSpawner();
	private PatrolSpawner patrols = new PatrolSpawner();
	private long nextAmbushNanos;

	MobsAModifiers() {
		instance = this;
		if (!eventsRegistered) {
			eventsRegistered = true;
			ServerEntityEvents.ENTITY_LOAD.register(MobsAModifiers::onLoad);
			ServerLivingEntityEvents.AFTER_DEATH.register(MobsAModifiers::onDeath);
		}
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		pending.clear();
		wolfGiven.clear();
		phantoms = new PhantomSpawner();
		patrols = new PatrolSpawner();
		nextAmbushNanos = Modifiers.nextTimedNanos(run);
		// A dragon that existed before the run started never fired a load event during the run.
		for (EnderDragon dragon : run.worlds.end().getDragons()) {
			armorDragon(dragon);
		}
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		JsonArray given = new JsonArray();
		wolfGiven.forEach(id -> given.add(id.toString()));
		out.add("petWolfGiven", given);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (in.has("petWolfGiven")) {
			for (JsonElement id : in.getAsJsonArray("petWolfGiven")) {
				wolfGiven.add(UUID.fromString(id.getAsString()));
			}
		}
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		pending.clear();
		wolfGiven.clear();
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.PET_WOLVES) && run.worlds.contains(player.serverLevel()) && wolfGiven.add(player.getUUID())) {
			Wolf wolf = EntityType.WOLF.spawn(player.serverLevel(), player.blockPosition(), MobSpawnType.EVENT);
			if (wolf != null) {
				wolf.tame(player);
			}
		}
	}

	@Override
	public void tick(ActiveRun run) {
		if (!pending.isEmpty()) {
			List<Runnable> now = new ArrayList<>(pending);
			pending.clear();
			now.forEach(Runnable::run);
		}
		ServerLevel overworld = run.worlds.overworld();
		boolean spawning = overworld.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && overworld.tickRateManager().runsNormally();
		boolean enemies = overworld.getDifficulty() != Difficulty.PEACEFUL;
		// Run worlds have no custom spawners of their own, so these two are ticked here.
		if (spawning && run.modifiers.contains(ModifierCatalog.PHANTOM_MENACE)) {
			phantoms.tick(overworld, enemies, true);
		}
		if (spawning && run.modifiers.contains(ModifierCatalog.PATROL_SEASON) && run.gameTicks > PATROL_GRACE_TICKS) {
			patrols.tick(overworld, enemies, true);
		}
		if (run.modifiers.contains(ModifierCatalog.AMBUSH) && run.clockNanos() >= nextAmbushNanos) {
			nextAmbushNanos += Modifiers.TIMED_INTERVAL_NANOS;
			List<ServerPlayer> players = Modifiers.playersIn(run);
			for (ServerPlayer player : players) {
				ambush(player);
			}
			if (!players.isEmpty()) {
				overworld.getServer().getPlayerList().broadcastSystemMessage(Component.literal("Ambush!").withStyle(ChatFormatting.RED), false);
			}
		}
	}

	// ---- events ----

	private static void onLoad(Entity entity, ServerLevel level) {
		if (entity instanceof EnderDragon dragon) {
			armorDragon(dragon);
		}
		if (!(entity instanceof Mob mob)) {
			return;
		}
		if (mob instanceof Creeper creeper && Modifiers.isActive(ModifierCatalog.SHORT_FUSE, level)) {
			((ModifierMobsACreeperAccessor) creeper).speedrun$setMaxSwell(15);
		}
		if (mob instanceof EnderMan && Modifiers.isActive(ModifierCatalog.ENDERMAN_RAGE, level)) {
			((ModifierMobsAMobAccessor) mob).speedrun$targetSelector().addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, true));
		}
		if (mob instanceof Enemy) {
			if (Modifiers.isActive(ModifierCatalog.INVISIBLE_MOBS, level)) {
				addInfinite(mob, MobEffects.INVISIBILITY);
			}
			if (Modifiers.isActive(ModifierCatalog.GLOWING_MOBS, level)) {
				addInfinite(mob, MobEffects.GLOWING);
			}
			if (Modifiers.isActive(ModifierCatalog.SILENT_MOBS, level)) {
				mob.setSilent(true);
			}
		}
		if (mob instanceof Zombie zombie && !zombie.isBaby() && Modifiers.isActive(ModifierCatalog.BABY_ZOMBIES, level)) {
			zombie.setBaby(true);
		}
		// nether_invasion piglins would turn into zombified piglins outside the Nether after 15 seconds.
		if (mob instanceof Piglin piglin && !level.dimensionType().piglinSafe() && Modifiers.isActive(ModifierCatalog.NETHER_INVASION, level)) {
			piglin.setImmuneToZombification(true);
		}
		if (mob instanceof Spider spider && spider.getType() == EntityType.SPIDER && Modifiers.isActive(ModifierCatalog.SPIDER_JOCKEYS, level) && spider.addTag(JOCKEY_TAG)
			&& !spider.isVehicle() && instance != null) {
			instance.pending.add(() -> addRider(spider, level));
		}
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (instance == null || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		if (entity instanceof Enemy && !(entity instanceof EnderDragon) && Modifiers.isActive(ModifierCatalog.KAMIKAZE, level)) {
			double x = entity.getX();
			double y = entity.getY();
			double z = entity.getZ();
			instance.pending.add(() -> level.explode(null, x, y, z, KAMIKAZE_POWER, Level.ExplosionInteraction.MOB));
		}
		if ((entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AbstractVillager)
			&& source.getEntity() instanceof ServerPlayer killer && Modifiers.isActive(ModifierCatalog.REVENGE, level)) {
			BlockPos pos = entity.blockPosition();
			instance.pending.add(() -> {
				Zombie zombie = EntityType.ZOMBIE.spawn(level, pos, MobSpawnType.EVENT);
				if (zombie != null && killer.level() == level) {
					zombie.setTarget(killer);
				}
			});
		}
	}

	private static void addInfinite(Mob mob, Holder<MobEffect> effect) {
		if (!mob.hasEffect(effect)) {
			mob.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
	}

	private static void addRider(Spider spider, ServerLevel level) {
		if (spider.isRemoved() || spider.isVehicle()) {
			return;
		}
		Skeleton skeleton = EntityType.SKELETON.create(level);
		if (skeleton == null) {
			return;
		}
		skeleton.moveTo(spider.getX(), spider.getY(), spider.getZ(), spider.getYRot(), 0.0F);
		skeleton.finalizeSpawn(level, level.getCurrentDifficultyAt(spider.blockPosition()), MobSpawnType.JOCKEY, null);
		skeleton.startRiding(spider);
		level.addFreshEntity(skeleton);
	}

	/** armored_dragon: doubles max health once. The modifier is saved with the dragon, so a reload does not stack it. */
	private static void armorDragon(EnderDragon dragon) {
		if (!Modifiers.isActive(ModifierCatalog.ARMORED_DRAGON, dragon.level())) {
			return;
		}
		AttributeInstance maxHealth = dragon.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null || maxHealth.hasModifier(ARMORED_DRAGON_ID)) {
			return;
		}
		boolean full = dragon.getHealth() >= dragon.getMaxHealth();
		maxHealth.addPermanentModifier(new AttributeModifier(ARMORED_DRAGON_ID, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		if (full) {
			dragon.setHealth(dragon.getMaxHealth());
		}
	}

	/** Spawns one random hostile mob on open ground next to the player, aimed at them. */
	private void ambush(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		EntityType<? extends Mob> type = AMBUSHERS.get(random.nextInt(AMBUSHERS.size()));
		BlockPos pos = player.blockPosition();
		search:
		for (int attempt = 0; attempt < 20; attempt++) {
			int dx = random.nextInt(7) - 3;
			int dz = random.nextInt(7) - 3;
			if (Math.abs(dx) < 2 && Math.abs(dz) < 2) {
				continue;
			}
			for (int dy = 2; dy >= -2; dy--) {
				BlockPos candidate = player.blockPosition().offset(dx, dy, dz);
				if (!level.getBlockState(candidate.below()).getCollisionShape(level, candidate.below()).isEmpty()
					&& level.noCollision(type.getSpawnAABB(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5))) {
					pos = candidate;
					break search;
				}
			}
		}
		Mob mob = type.spawn(level, pos, MobSpawnType.EVENT);
		if (mob != null) {
			mob.setTarget(player);
		}
	}

	// ---- called from mixins ----

	/** RangedBowAttackGoal always draws the bow this many ticks before its cooldown starts. */
	private static final int BOW_DRAW_TICKS = 20;

	/** sharpshooters halves the whole shot cycle, draw plus cooldown, so skeletons fire twice as often. */
	public static int bowInterval(Mob skeleton, int interval) {
		return Modifiers.isActive(ModifierCatalog.SHARPSHOOTERS, skeleton.level()) ? Math.max(0, (interval - BOW_DRAW_TICKS) / 2) : interval;
	}

	/** spawn_randomizer swaps a natural spawn for a random mob of the same category, so spawn caps still hold. */
	public static EntityType<?> randomizeSpawn(ServerLevel level, EntityType<?> type) {
		if (type.getCategory() == MobCategory.MISC || !Modifiers.isActive(ModifierCatalog.SPAWN_RANDOMIZER, level)) {
			return type;
		}
		List<EntityType<?>> candidates = randomizerCandidates.computeIfAbsent(type.getCategory(), category -> {
			List<EntityType<?>> list = new ArrayList<>();
			for (EntityType<?> candidate : BuiltInRegistries.ENTITY_TYPE) {
				if (candidate.getCategory() == category && candidate.canSummon() && candidate.isEnabled(level.enabledFeatures())
					&& !RANDOMIZER_EXCLUDED.contains(candidate)) {
					list.add(candidate);
				}
			}
			return list;
		});
		return candidates.isEmpty() ? type : candidates.get(level.random.nextInt(candidates.size()));
	}

	/** nether_invasion adds nether mobs to overworld night spawns; blaze_swarm adds blazes everywhere in the Nether. */
	public static WeightedRandomList<MobSpawnSettings.SpawnerData> extraSpawns(ServerLevel level, MobCategory category,
		WeightedRandomList<MobSpawnSettings.SpawnerData> spawns) {
		if (category != MobCategory.MONSTER) {
			return spawns;
		}
		List<MobSpawnSettings.SpawnerData> extra = null;
		// Run worlds use their own dimension keys, so the dimension type tells the overworld and the Nether apart.
		if (level.dimensionType().natural() && level.dimensionType().hasSkyLight()) {
			if (!level.isDay() && Modifiers.isActive(ModifierCatalog.NETHER_INVASION, level)) {
				extra = INVASION;
			}
		} else if (level.dimensionType().ultraWarm() && Modifiers.isActive(ModifierCatalog.BLAZE_SWARM, level)) {
			extra = SWARM;
		}
		if (extra == null) {
			return spawns;
		}
		List<MobSpawnSettings.SpawnerData> all = new ArrayList<>(spawns.unwrap());
		all.addAll(extra);
		return WeightedRandomList.create(all);
	}

	/** piglin_friends: piglins and brutes never pick a player as their attack target. */
	public static boolean blocksPiglinTarget(Mob piglin, @Nullable LivingEntity target) {
		return target instanceof Player && Modifiers.isActive(ModifierCatalog.PIGLIN_FRIENDS, piglin.level());
	}

	/** arrow_dragon: everything but arrows and tridents is ignored. Commands like /kill still work. */
	public static boolean dragonIgnores(EnderDragon dragon, DamageSource source) {
		return Modifiers.isActive(ModifierCatalog.ARROW_DRAGON, dragon.level())
			&& !(source.getDirectEntity() instanceof AbstractArrow) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	/** pearl_bonanza: drops the loot table's pearls, then tops them up so an enderman always drops at least 2. */
	public static Consumer<ItemStack> pearlLoot(LivingEntity entity, Consumer<ItemStack> drop) {
		if (!(entity instanceof EnderMan) || !Modifiers.isActive(ModifierCatalog.PEARL_BONANZA, entity.level())) {
			return drop;
		}
		return stack -> {
			if (stack.is(Items.ENDER_PEARL)) {
				pearlsDropped += stack.getCount();
			}
			drop.accept(stack);
		};
	}

	public static void startLoot() {
		pearlsDropped = 0;
	}

	public static void finishLoot(LivingEntity entity) {
		if (entity instanceof EnderMan && Modifiers.isActive(ModifierCatalog.PEARL_BONANZA, entity.level()) && pearlsDropped < 2) {
			entity.spawnAtLocation(new ItemStack(Items.ENDER_PEARL, 2 - pearlsDropped));
		}
		pearlsDropped = 0;
	}
}
