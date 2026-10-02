package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.mixin.ModifierMobsBMobAccessor;
import com.eternalerkle.speedrun.mixin.ModifierMobsBTargetGoalAccessor;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * creeper_rain, sniper_skeletons, stalker, angry_neutrals, juiced_mobs, armored_horde, splitters, elites, swarm, relentless,
 * vex_curse, warden_alarm, ghast_air_force, endless_raids, marked and thieves.
 * Changes to mobs live on the mobs themselves, which are deleted with the run's worlds. Player effects are removed at run end.
 */
public final class MobsBModifiers implements RunFeature {
	private static final long SECOND = 1_000_000_000L;
	private static final long CREEPER_RAIN_INTERVAL = 60 * SECOND;
	private static final long STALKER_INTERVAL = 120 * SECOND;
	private static final long GHAST_INTERVAL = 30 * SECOND;
	private static final long MARK_INTERVAL = 300 * SECOND;
	private static final long MARK_DURATION = 60 * SECOND;
	private static final long RETRY = 10 * SECOND;
	/** warden_alarm fires once at a random point between these run times. */
	private static final long WARDEN_EARLIEST = 10 * 60 * SECOND;
	private static final long WARDEN_LATEST = 50 * 60 * SECOND;

	/** Marks a mob already handled by the spawn-time modifiers (armored_horde, elites), so reloading a chunk does not roll again. */
	static final String SEEN_TAG = "speedrun.mobs_b";
	/** Marks a splitters copy: it never splits again, drops no loot and never rolls armored_horde or elites. */
	public static final String SPLIT_TAG = "speedrun.split";
	/** A stalker carries this prefix followed by its target's UUID. */
	private static final String STALKER_TAG = "speedrun.stalker.";

	private static final ResourceLocation ELITE_HEALTH = id("elite_health");
	private static final ResourceLocation SPLIT_HEALTH = id("split_health");
	private static final ResourceLocation SPLIT_SCALE = id("split_scale");
	private static final ResourceLocation SNIPER_RANGE = id("sniper_range");
	private static final ResourceLocation RELENTLESS_RANGE = id("relentless_range");
	private static final ResourceLocation STALKER_SPEED = id("stalker_speed");
	private static final ResourceLocation STALKER_RANGE = id("stalker_range");

	private static final Random RANDOM = new Random();
	private static boolean eventsRegistered;

	/** swarm copies queued from Mob.finalizeSpawn and spawned on the next tick, once the original is in the world. */
	private static final List<Pending> pending = new ArrayList<>();

	private record Pending(ServerLevel level, EntityType<?> type, Vec3 pos) {
	}

	private long nextCreeperRain;
	private long nextStalker;
	private long nextGhasts;
	private long nextMark;
	/** Run clock time of the warden, or -1 once it has spawned. */
	private long wardenAt;
	@Nullable
	private UUID marked;
	private long markEnd;

	private MobsBModifiers() {
	}

	public static void register(RunManager runs) {
		runs.addFeature(new MobsBModifiers());
		if (!eventsRegistered) {
			eventsRegistered = true;
			registerEvents();
		}
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("speedrun", "modifier/" + path);
	}

	/** The next multiple of the interval in run time, so a restored run continues the schedule. */
	private static long nextAt(ActiveRun run, long interval) {
		long elapsed = Math.max(0, run.clockNanos() - run.startNanos);
		return run.startNanos + (elapsed / interval + 1) * interval;
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		pending.clear();
		nextCreeperRain = nextAt(run, CREEPER_RAIN_INTERVAL);
		nextStalker = nextAt(run, STALKER_INTERVAL);
		nextGhasts = nextAt(run, GHAST_INTERVAL);
		nextMark = nextAt(run, MARK_INTERVAL);
		wardenAt = run.startNanos + WARDEN_EARLIEST + (long) (RANDOM.nextDouble() * (WARDEN_LATEST - WARDEN_EARLIEST));
		marked = null;
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		JsonObject state = new JsonObject();
		state.addProperty("wardenAtMillis", wardenAt < 0 ? -1 : (wardenAt - run.startNanos) / 1_000_000L);
		if (marked != null) {
			state.addProperty("marked", marked.toString());
			state.addProperty("markEndMillis", (markEnd - run.startNanos) / 1_000_000L);
		}
		out.add("mobsB", state);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (!in.has("mobsB")) {
			return;
		}
		JsonObject state = in.getAsJsonObject("mobsB");
		long warden = state.get("wardenAtMillis").getAsLong();
		wardenAt = warden < 0 ? -1 : run.startNanos + warden * 1_000_000L;
		if (state.has("marked")) {
			marked = UUID.fromString(state.get("marked").getAsString());
			markEnd = run.startNanos + state.get("markEndMillis").getAsLong() * 1_000_000L;
		}
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		pending.clear();
		marked = null;
		for (ServerPlayer player : run.worlds.overworld().getServer().getPlayerList().getPlayers()) {
			clearPlayer(run, player);
		}
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		clearPlayer(run, player);
	}

	private static void clearPlayer(ActiveRun run, ServerPlayer player) {
		if (run.modifiers.contains(ModifierCatalog.ENDLESS_RAIDS)) {
			removeInfinite(player, MobEffects.BAD_OMEN);
			player.removeEffect(MobEffects.RAID_OMEN);
		}
		if (run.modifiers.contains(ModifierCatalog.MARKED)) {
			removeInfinite(player, MobEffects.GLOWING);
		}
	}

	private static void removeInfinite(ServerPlayer player, Holder<MobEffect> effect) {
		MobEffectInstance instance = player.getEffect(effect);
		if (instance != null && instance.isInfiniteDuration()) {
			player.removeEffect(effect);
		}
	}

	@Override
	public void tick(ActiveRun run) {
		if (run.modifiers.isEmpty()) {
			return;
		}
		long now = run.clockNanos();
		List<ServerPlayer> players = Modifiers.playersIn(run);
		boolean slowTick = run.worlds.overworld().getServer().getTickCount() % 10 == 0;
		spawnPending(run);
		if (run.modifiers.contains(ModifierCatalog.CREEPER_RAIN) && now >= nextCreeperRain) {
			nextCreeperRain = nextAt(run, CREEPER_RAIN_INTERVAL);
			players.forEach(MobsBModifiers::creeperRain);
		}
		if (run.modifiers.contains(ModifierCatalog.STALKER) && now >= nextStalker) {
			nextStalker = !players.isEmpty() && spawnStalker(players.get(RANDOM.nextInt(players.size()))) ? nextAt(run, STALKER_INTERVAL) : now + RETRY;
		}
		if (run.modifiers.contains(ModifierCatalog.GHAST_AIR_FORCE) && now >= nextGhasts) {
			nextGhasts = nextAt(run, GHAST_INTERVAL);
			for (ServerPlayer player : players) {
				if (player.serverLevel() == run.worlds.overworld()) {
					ghastPatrol(player);
				}
			}
		}
		if (run.modifiers.contains(ModifierCatalog.WARDEN_ALARM) && wardenAt >= 0 && now >= wardenAt) {
			wardenAt = !players.isEmpty() && spawnWarden(players.get(RANDOM.nextInt(players.size()))) ? -1 : now + RETRY;
		}
		if (run.modifiers.contains(ModifierCatalog.MARKED)) {
			tickMarked(run, now, players, slowTick);
		}
		if (!slowTick) {
			return;
		}
		if (run.modifiers.contains(ModifierCatalog.ANGRY_NEUTRALS)) {
			players.forEach(MobsBModifiers::angerNeutrals);
		}
		if (run.modifiers.contains(ModifierCatalog.ENDLESS_RAIDS)) {
			for (ServerPlayer player : players) {
				if (!player.hasEffect(MobEffects.BAD_OMEN) && !player.hasEffect(MobEffects.RAID_OMEN)) {
					player.addEffect(new MobEffectInstance(MobEffects.BAD_OMEN, MobEffectInstance.INFINITE_DURATION, 0));
				}
			}
		}
	}

	// ---- events ----

	private static void registerEvents() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob) {
				onMobLoad(mob, level);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register(MobsBModifiers::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(MobsBModifiers::afterDeath);
	}

	private static boolean isHostile(Mob mob) {
		return mob instanceof Enemy && !(mob instanceof EnderDragon) && !(mob instanceof WitherBoss) && !(mob instanceof Warden);
	}

	/** Applies the per-mob modifiers. Runs on every load, so everything here is idempotent. */
	private static void onMobLoad(Mob mob, ServerLevel level) {
		boolean hostile = isHostile(mob);
		if (hostile && (Modifiers.isActive(ModifierCatalog.ARMORED_HORDE, level) || Modifiers.isActive(ModifierCatalog.ELITES, level))
			&& mob.addTag(SEEN_TAG)) {
			if (Modifiers.isActive(ModifierCatalog.ARMORED_HORDE, level) && (mob instanceof Zombie || mob instanceof AbstractSkeleton)) {
				armor(mob);
			}
			if (Modifiers.isActive(ModifierCatalog.ELITES, level) && RANDOM.nextInt(10) == 0) {
				makeElite(mob);
			}
		}
		if (hostile && Modifiers.isActive(ModifierCatalog.JUICED_MOBS, level)) {
			addInfinite(mob, MobEffects.DAMAGE_BOOST);
			addInfinite(mob, MobEffects.MOVEMENT_SPEED);
		}
		if (mob instanceof AbstractSkeleton && Modifiers.isActive(ModifierCatalog.SNIPER_SKELETONS, level)) {
			// Base follow range is 16; the doubled bow range is handled in ModifierMobsBBowGoalMixin.
			addModifier(mob, Attributes.FOLLOW_RANGE, SNIPER_RANGE, 16.0, AttributeModifier.Operation.ADD_VALUE);
			refreshTargetRange(mob);
		}
		if (hostile && Modifiers.isActive(ModifierCatalog.RELENTLESS, level)) {
			double base = mob.getAttributeBaseValue(Attributes.FOLLOW_RANGE);
			if (base < 64.0) {
				addModifier(mob, Attributes.FOLLOW_RANGE, RELENTLESS_RANGE, 64.0 - base, AttributeModifier.Operation.ADD_VALUE);
			}
			for (WrappedGoal goal : targetSelector(mob).getAvailableGoals()) {
				if (goal.getGoal() instanceof TargetGoal target) {
					target.setUnseenMemoryTicks(Integer.MAX_VALUE / 4);
				}
			}
			refreshTargetRange(mob);
		}
		UUID stalked = stalkerTarget(mob);
		if (stalked != null) {
			targetSelector(mob).removeAllGoals(goal -> true);
			targetSelector(mob).addGoal(1, new NearestAttackableTargetGoal<>(mob, Player.class, 10, false, false, target -> target.getUUID().equals(stalked)));
		}
	}

	/** Target goals copy the follow range when they are built, so a later follow range bonus never reaches them on its own. */
	private static void refreshTargetRange(Mob mob) {
		double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
		for (WrappedGoal goal : targetSelector(mob).getAvailableGoals()) {
			if (goal.getGoal() instanceof NearestAttackableTargetGoal<?> target) {
				((ModifierMobsBTargetGoalAccessor) target).speedrun$targetConditions().range(range);
			}
		}
	}

	private static net.minecraft.world.entity.ai.goal.GoalSelector targetSelector(Mob mob) {
		return ((ModifierMobsBMobAccessor) mob).speedrun$targetSelector();
	}

	private static void addInfinite(LivingEntity entity, Holder<MobEffect> effect) {
		if (!entity.hasEffect(effect)) {
			entity.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
	}

	private static void addModifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = entity.getAttribute(attribute);
		if (instance != null && !instance.hasModifier(id)) {
			instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
		}
	}

	/** armored_horde: a full set, each piece iron or (one in four) diamond. The armor never drops. */
	private static void armor(Mob mob) {
		boolean[] diamond = new boolean[4];
		for (int i = 0; i < 4; i++) {
			diamond[i] = RANDOM.nextInt(4) == 0;
		}
		equip(mob, EquipmentSlot.HEAD, diamond[0] ? Items.DIAMOND_HELMET.getDefaultInstance() : Items.IRON_HELMET.getDefaultInstance());
		equip(mob, EquipmentSlot.CHEST, diamond[1] ? Items.DIAMOND_CHESTPLATE.getDefaultInstance() : Items.IRON_CHESTPLATE.getDefaultInstance());
		equip(mob, EquipmentSlot.LEGS, diamond[2] ? Items.DIAMOND_LEGGINGS.getDefaultInstance() : Items.IRON_LEGGINGS.getDefaultInstance());
		equip(mob, EquipmentSlot.FEET, diamond[3] ? Items.DIAMOND_BOOTS.getDefaultInstance() : Items.IRON_BOOTS.getDefaultInstance());
	}

	private static void equip(Mob mob, EquipmentSlot slot, ItemStack stack) {
		mob.setItemSlot(slot, stack);
		mob.setDropChance(slot, 0.0F);
	}

	/** elites: triple health and a visible name. */
	private static void makeElite(Mob mob) {
		addModifier(mob, Attributes.MAX_HEALTH, ELITE_HEALTH, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
		mob.setHealth(mob.getMaxHealth());
		mob.setCustomName(Component.literal("Elite ").append(mob.getType().getDescription()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		mob.setCustomNameVisible(true);
	}

	/** Called from the end of Mob.finalizeSpawn. swarm: every natural hostile spawn brings two more of its kind. */
	public static void onFinalizeSpawn(Mob mob, MobSpawnType type) {
		if (type == MobSpawnType.NATURAL && mob instanceof Enemy && mob.level() instanceof ServerLevel level
			&& Modifiers.isActive(ModifierCatalog.SWARM, level)) {
			pending.add(new Pending(level, mob.getType(), mob.position()));
		}
	}

	private static void spawnPending(ActiveRun run) {
		if (pending.isEmpty()) {
			return;
		}
		List<Pending> batch = new ArrayList<>(pending);
		pending.clear();
		for (Pending entry : batch) {
			if (!run.worlds.contains(entry.level())) {
				continue;
			}
			for (int i = 0; i < 2; i++) {
				Entity copy = entry.type().create(entry.level(), null, BlockPos.containing(entry.pos()), MobSpawnType.MOB_SUMMONED, false, false);
				if (copy != null) {
					copy.moveTo(entry.pos().x, entry.pos().y, entry.pos().z, copy.getYRot(), 0.0F);
					entry.level().addFreshEntityWithPassengers(copy);
				}
			}
		}
	}

	private static void afterDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level) || !isHostile(mob)
			|| mob instanceof Slime || mob instanceof Vex || mob instanceof ElderGuardian || mob.getTags().contains(SPLIT_TAG)
			|| !Modifiers.isActive(ModifierCatalog.SPLITTERS, level)) {
			return;
		}
		for (int i = 0; i < 2; i++) {
			Entity created = mob.getType().create(level, copy -> {
				copy.addTag(SPLIT_TAG);
				copy.addTag(SEEN_TAG);
			}, mob.blockPosition(), MobSpawnType.MOB_SUMMONED, false, false);
			if (created instanceof Mob copy) {
				copy.moveTo(mob.getX() + (i == 0 ? -0.3 : 0.3), mob.getY(), mob.getZ(), mob.getYRot(), 0.0F);
				addModifier(copy, Attributes.MAX_HEALTH, SPLIT_HEALTH, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
				addModifier(copy, Attributes.SCALE, SPLIT_SCALE, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
				copy.setHealth(copy.getMaxHealth());
				((ModifierMobsBMobAccessor) copy).speedrun$setLootTable(BuiltInLootTables.EMPTY);
				if (source.getEntity() instanceof LivingEntity killer && copy.canAttack(killer)) {
					copy.setTarget(killer);
				}
				level.addFreshEntityWithPassengers(copy);
			}
		}
	}

	private static void afterDamage(LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) {
		if (!(entity instanceof ServerPlayer player) || blocked || taken <= 0.0F || !player.isAlive()) {
			return;
		}
		ServerLevel level = player.serverLevel();
		Entity attacker = source.getEntity();
		if (attacker instanceof Mob && !(attacker instanceof Vex) && Modifiers.isActive(ModifierCatalog.VEX_CURSE, level) && RANDOM.nextInt(5) == 0) {
			summonVex(level, player);
		}
		if ((attacker instanceof Zombie || attacker instanceof EnderMan) && source.getDirectEntity() == attacker
			&& Modifiers.isActive(ModifierCatalog.THIEVES, level)) {
			steal((Mob) attacker, player);
		}
	}

	/** vex_curse: one vex that lives 30 to 45 seconds. */
	private static void summonVex(ServerLevel level, ServerPlayer player) {
		Vex vex = EntityType.VEX.create(level, null, player.blockPosition().above(2), MobSpawnType.MOB_SUMMONED, false, false);
		if (vex == null) {
			return;
		}
		vex.moveTo(player.getX() + RANDOM.nextDouble() * 2 - 1, player.getY() + 1.5, player.getZ() + RANDOM.nextDouble() * 2 - 1, vex.getYRot(), 0.0F);
		vex.setLimitedLife(20 * (30 + RANDOM.nextInt(16)));
		vex.setTarget(player);
		level.addFreshEntity(vex);
	}

	/**
	 * thieves: the mob takes one random non-empty stack from the main inventory or offhand into its own offhand, and
	 * always drops it on death. A mob that already holds something in its offhand cannot steal.
	 */
	private static void steal(Mob thief, ServerPlayer player) {
		if (!thief.getOffhandItem().isEmpty()) {
			return;
		}
		Inventory inventory = player.getInventory();
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < inventory.items.size(); i++) {
			if (!inventory.items.get(i).isEmpty()) {
				slots.add(i);
			}
		}
		if (!inventory.offhand.get(0).isEmpty()) {
			slots.add(Inventory.SLOT_OFFHAND);
		}
		if (slots.isEmpty()) {
			return;
		}
		int slot = slots.get(RANDOM.nextInt(slots.size()));
		ItemStack stolen = inventory.removeItemNoUpdate(slot);
		inventory.setChanged();
		thief.setItemSlot(EquipmentSlot.OFFHAND, stolen);
		thief.setGuaranteedDrop(EquipmentSlot.OFFHAND);
		thief.setPersistenceRequired();
		player.displayClientMessage(Component.literal("A thief took your ").append(stolen.getHoverName()).append("!").withStyle(ChatFormatting.RED), true);
	}

	// ---- timed ----

	/** creeper_rain: three creepers 15 blocks above the ground near a player under open sky. Charged ones under charged_up. */
	private static void creeperRain(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (!level.canSeeSky(player.blockPosition().above())) {
			return;
		}
		for (int i = 0; i < 3; i++) {
			int x = Mth.floor(player.getX()) + RANDOM.nextInt(17) - 8;
			int z = Mth.floor(player.getZ()) + RANDOM.nextInt(17) - 8;
			int y = Math.min(level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 15, level.getMaxBuildHeight() - 3);
			if (!level.noCollision(EntityType.CREEPER.getSpawnAABB(x + 0.5, y, z + 0.5))) {
				continue;
			}
			Entity creeper = EntityType.CREEPER.create(level, null, new BlockPos(x, y, z), MobSpawnType.MOB_SUMMONED, false, false);
			if (creeper != null) {
				level.addFreshEntity(creeper);
			}
		}
		player.displayClientMessage(Component.literal("Creepers incoming!").withStyle(ChatFormatting.GREEN), true);
	}

	/** stalker: a fast, sun-proof zombie a few blocks behind the player that only ever targets that player. */
	private static boolean spawnStalker(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 look = player.getLookAngle().multiply(1, 0, 1);
		Vec3 back = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, -1) : look.normalize().reverse();
		for (int distance = 8; distance >= 3; distance--) {
			BlockPos ground = findGround(level, EntityType.ZOMBIE, player.position().add(back.scale(distance)), player.getBlockY());
			if (ground == null) {
				continue;
			}
			Zombie zombie = EntityType.ZOMBIE.create(level);
			if (zombie == null) {
				return false;
			}
			zombie.moveTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, player.getYRot(), 0.0F);
			zombie.addTag(STALKER_TAG + player.getUUID());
			zombie.addTag(SEEN_TAG);
			addModifier(zombie, Attributes.MOVEMENT_SPEED, STALKER_SPEED, 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
			addModifier(zombie, Attributes.FOLLOW_RANGE, STALKER_RANGE, 100.0, AttributeModifier.Operation.ADD_VALUE);
			zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
			zombie.setCustomName(Component.literal("Stalker").withStyle(ChatFormatting.DARK_RED));
			zombie.setPersistenceRequired();
			zombie.setTarget(player);
			level.addFreshEntity(zombie);
			player.displayClientMessage(Component.literal("You feel watched...").withStyle(ChatFormatting.DARK_RED), true);
			return true;
		}
		return false;
	}

	@Nullable
	private static UUID stalkerTarget(Mob mob) {
		for (String tag : mob.getTags()) {
			if (tag.startsWith(STALKER_TAG)) {
				try {
					return UUID.fromString(tag.substring(STALKER_TAG.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** The first spot at or below a few blocks above {@code y} where the mob stands on a solid block. */
	@Nullable
	private static BlockPos findGround(ServerLevel level, EntityType<?> type, Vec3 around, int y) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(around.x), y + 4, Mth.floor(around.z));
		for (int i = 0; i < 12; i++, pos.move(0, -1, 0)) {
			BlockPos below = pos.below();
			if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()
				&& level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
				return pos.immutable();
			}
		}
		return null;
	}

	/** ghast_air_force: keeps up to two ghasts in the sky around each overworld player. */
	private static void ghastPatrol(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		if (level.getEntitiesOfClass(Ghast.class, player.getBoundingBox().inflate(80.0)).size() >= 2) {
			return;
		}
		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = RANDOM.nextDouble() * Math.PI * 2;
			double distance = 20 + RANDOM.nextDouble() * 16;
			double x = player.getX() + Math.cos(angle) * distance;
			double z = player.getZ() + Math.sin(angle) * distance;
			int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
			double y = Math.min(Math.max(player.getY() + 12 + RANDOM.nextInt(10), ground + 8), level.getMaxBuildHeight() - 6);
			AABB box = EntityType.GHAST.getSpawnAABB(x, y, z);
			if (!level.hasChunkAt(BlockPos.containing(x, y, z)) || !level.noCollision(box) || level.containsAnyLiquid(box)) {
				continue;
			}
			Ghast ghast = EntityType.GHAST.create(level, null, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED, false, false);
			if (ghast != null) {
				ghast.moveTo(x, y, z, ghast.getYRot(), 0.0F);
				level.addFreshEntity(ghast);
			}
			return;
		}
	}

	/** warden_alarm: an emerging warden near the player, already angry at them. */
	private static boolean spawnWarden(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Warden warden = SpawnUtil.trySpawnMob(EntityType.WARDEN, MobSpawnType.TRIGGERED, level, player.blockPosition(), 20, 6, 6,
			SpawnUtil.Strategy.ON_TOP_OF_COLLIDER).orElse(null);
		if (warden == null) {
			return false;
		}
		warden.increaseAngerAt(player, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
		level.getServer().getPlayerList().broadcastSystemMessage(
			Component.literal("Warden Alarm! A warden is coming for " + player.getGameProfile().getName() + ".").withStyle(ChatFormatting.DARK_AQUA), false);
		return true;
	}

	/** marked: every 5 minutes a random player glows and every hostile mob within 48 blocks targets them for 60 seconds. */
	private void tickMarked(ActiveRun run, long now, List<ServerPlayer> players, boolean slowTick) {
		ServerPlayer target = marked == null ? null : run.worlds.overworld().getServer().getPlayerList().getPlayer(marked);
		if (marked != null && now >= markEnd) {
			if (target != null) {
				removeInfinite(target, MobEffects.GLOWING);
			}
			marked = null;
			target = null;
		}
		if (now >= nextMark) {
			nextMark = nextAt(run, MARK_INTERVAL);
			if (!players.isEmpty()) {
				if (target != null) {
					removeInfinite(target, MobEffects.GLOWING);
				}
				target = players.get(RANDOM.nextInt(players.size()));
				marked = target.getUUID();
				markEnd = now + MARK_DURATION;
				target.getServer().getPlayerList().broadcastSystemMessage(Component.literal(target.getGameProfile().getName()
					+ " is marked! Every mob hunts them for 60 seconds.").withStyle(ChatFormatting.RED), false);
			}
		}
		if (target == null || !slowTick || !players.contains(target)) {
			return;
		}
		if (!target.hasEffect(MobEffects.GLOWING)) {
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
		for (Mob mob : target.serverLevel().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(48.0),
			mob -> isHostile(mob) && stalkerTarget(mob) == null)) {
			if (mob.getTarget() != target && mob.canAttack(target)) {
				mob.setTarget(target);
			}
		}
	}

	/** angry_neutrals: neutral mobs within 20 blocks of a player attack them. Tamed wolves and player-built golems are spared. */
	private static void angerNeutrals(ServerPlayer player) {
		if (!player.canBeSeenAsEnemy()) {
			return;
		}
		for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20.0), MobsBModifiers::isAngryNeutral)) {
			if (mob.getTarget() != null) {
				continue;
			}
			if (mob instanceof Piglin piglin) {
				piglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), 600L);
			} else if (mob instanceof NeutralMob neutral) {
				neutral.setPersistentAngerTarget(player.getUUID());
				neutral.startPersistentAngerTimer();
				mob.setTarget(player);
			}
		}
	}

	private static boolean isAngryNeutral(Mob mob) {
		return (mob instanceof Wolf wolf && !wolf.isTame()) || mob instanceof Bee || mob instanceof EnderMan || mob instanceof Piglin
			|| mob instanceof ZombifiedPiglin || (mob instanceof IronGolem golem && !golem.isPlayerCreated()) || mob instanceof PolarBear;
	}

	// ---- sniper_skeletons aim ----

	/** Arrow physics per tick: move, multiply velocity by DRAG, then subtract GRAVITY from the vertical speed. */
	static final double ARROW_DRAG = 0.99;
	static final double ARROW_GRAVITY = 0.05;

	/**
	 * The upward slope (dy / horizontal) to aim an arrow at so that, at the given speed, it passes through a point
	 * {@code horizontal} blocks away and {@code vertical} blocks higher. Uses the flat arc. Targets out of range get 45 degrees.
	 */
	public static double aimSlope(double horizontal, double vertical, double speed) {
		double low = -Math.PI / 2 + 0.01;
		double high = Math.PI / 4;
		if (heightAt(high, horizontal, speed) < vertical) {
			return Math.tan(high);
		}
		for (int i = 0; i < 40; i++) {
			double mid = (low + high) / 2;
			if (heightAt(mid, horizontal, speed) < vertical) {
				low = mid;
			} else {
				high = mid;
			}
		}
		return Math.tan((low + high) / 2);
	}

	/** The arrow's height when it has covered {@code horizontal} blocks, or -infinity if it never gets there. */
	static double heightAt(double angle, double horizontal, double speed) {
		double vx = Math.cos(angle) * speed;
		double vy = Math.sin(angle) * speed;
		double x = 0;
		double y = 0;
		for (int tick = 0; tick < 400; tick++) {
			double nx = x + vx;
			double ny = y + vy;
			if (nx >= horizontal) {
				return y + (ny - y) * (horizontal - x) / (nx - x);
			}
			x = nx;
			y = ny;
			vx *= ARROW_DRAG;
			vy = vy * ARROW_DRAG - ARROW_GRAVITY;
		}
		return Double.NEGATIVE_INFINITY;
	}
}
