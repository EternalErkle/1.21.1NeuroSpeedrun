package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.mixin.ModifierBodyLivingEntityAccessor;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The body group: health, damage, senses and death rules. Event handlers and mixins call the static methods here;
 * per-run state lives on the instance and is saved with the run.
 */
public final class BodyModifiers implements RunFeature {
	private static final long SECOND_NANOS = 1_000_000_000L;
	private static final long SUDDEN_DEATH_START_NANOS = 30L * 60L * SECOND_NANOS;
	private static final long SUDDEN_DEATH_STEP_NANOS = 5L * 60L * SECOND_NANOS;
	/** Never takes the last heart. */
	private static final int SUDDEN_DEATH_MAX_LOST = 9;
	private static final long DARKNESS_INTERVAL_NANOS = 30L * SECOND_NANOS;
	private static final long SHELLSHOCK_NANOS = 5L * SECOND_NANOS;
	/** Sprinting multiplies speed by 1.3; this cancels it exactly. */
	private static final double SPRINT_CANCEL = 1.0 / 1.3 - 1.0;
	private static final String PUMPKIN_TAG = "speedrun_pumpkin_head";
	/** Damage at or above this is a kill (/kill, the void) and is never scaled. */
	private static final float LETHAL_LIMIT = 3.4028235E37F;

	private record Perm(Holder<MobEffect> effect, int amplifier) {
	}

	private static final Map<String, Perm> PERMANENT_EFFECTS = Map.of(
		ModifierCatalog.FIREPROOF, new Perm(MobEffects.FIRE_RESISTANCE, 0),
		ModifierCatalog.NIGHT_VISION, new Perm(MobEffects.NIGHT_VISION, 0),
		ModifierCatalog.HASTE, new Perm(MobEffects.DIG_SPEED, 1),
		ModifierCatalog.WEAK_HANDS, new Perm(MobEffects.DIG_SLOWDOWN, 0),
		ModifierCatalog.GILLS, new Perm(MobEffects.WATER_BREATHING, 0)
	);

	private record Attr(Holder<Attribute> attribute, ResourceLocation id, double amount, Operation operation) {
	}

	private static final Map<String, List<Attr>> ATTRIBUTES = Map.of(
		ModifierCatalog.EXTRA_HEARTS, List.of(attr(Attributes.MAX_HEALTH, "extra_hearts", 10.0, Operation.ADD_VALUE)),
		ModifierCatalog.LONG_ARMS, List.of(
			attr(Attributes.BLOCK_INTERACTION_RANGE, "long_arms_block", 3.0, Operation.ADD_VALUE),
			attr(Attributes.ENTITY_INTERACTION_RANGE, "long_arms_entity", 3.0, Operation.ADD_VALUE)),
		// Base reach is 4.5 for blocks and 3 for entities.
		ModifierCatalog.SHORT_ARMS, List.of(
			attr(Attributes.BLOCK_INTERACTION_RANGE, "short_arms_block", -2.5, Operation.ADD_VALUE),
			attr(Attributes.ENTITY_INTERACTION_RANGE, "short_arms_entity", -1.0, Operation.ADD_VALUE)),
		ModifierCatalog.NO_JUMPING, List.of(attr(Attributes.JUMP_STRENGTH, "no_jumping", -1.0, Operation.ADD_MULTIPLIED_TOTAL))
	);
	private static final ResourceLocation SUDDEN_DEATH_ID = id("sudden_death");
	private static final ResourceLocation SHELLSHOCK_ID = id("shellshock");

	@Nullable
	private static BodyModifiers instance;
	private static boolean eventsRegistered;
	/** The player inside Player.attack and whether that swing is a critical hit. Set and cleared by a mixin. */
	@Nullable
	private static Player attacking;
	private static boolean attackIsCrit;

	private final RunManager runs;
	@Nullable
	private ActiveRun current;
	/** Players who already got their first-entry setup (totem, full extra hearts) this run. */
	private final Set<UUID> entered = new HashSet<>();
	/** last_one_standing: players who died and now spectate. */
	private final Set<UUID> dead = new HashSet<>();
	private boolean secondWindUsed;
	private long nextDarknessNanos;
	/** Hearts sudden_death has taken so far, or -1 before the first check so a restore does not announce anything. */
	private int heartsLost = -1;
	private final Map<UUID, Long> shellshockUntil = new HashMap<>();

	private BodyModifiers(RunManager runs) {
		this.runs = runs;
	}

	static void register(RunManager runs) {
		instance = new BodyModifiers(runs);
		runs.addFeature(instance);
		if (!eventsRegistered) {
			eventsRegistered = true;
			registerEvents();
		}
	}

	private static Attr attr(Holder<Attribute> attribute, String path, double amount, Operation operation) {
		return new Attr(attribute, id(path), amount, operation);
	}

	private static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath("speedrun", "modifier/" + path);
	}

	private static boolean active(String id, Entity entity) {
		return Modifiers.isActive(id, entity.level());
	}

	// ---- events ----

	private static void registerEvents() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(BodyModifiers::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> afterDamage(entity, source, taken, blocked));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			// vampire: every kill heals the killer 2 hearts.
			if (source.getEntity() instanceof ServerPlayer killer && killer != entity && active(ModifierCatalog.VAMPIRE, killer)) {
				killer.heal(4.0F);
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && mob instanceof Enemy && Modifiers.isActive(ModifierCatalog.HELLFIRE_MOBS, level)) {
				ignite(mob);
			}
		});
	}

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (entity instanceof ServerPlayer player) {
			if (source.is(DamageTypeTags.IS_FALL)) {
				if (active(ModifierCatalog.FEATHERWEIGHT, player)) {
					return false;
				}
				if (active(ModifierCatalog.BOUNCY, player)) {
					bounce(player);
					return false;
				}
			}
			if (source.is(DamageTypeTags.IS_EXPLOSION) && active(ModifierCatalog.BLAST_PROOF, player)) {
				return false;
			}
		}
		if (active(ModifierCatalog.CRITICAL_ONLY, entity)) {
			if (source.is(DamageTypes.PLAYER_ATTACK) && attacking != null && source.getEntity() == attacking && !attackIsCrit) {
				return false;
			}
			if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof Player && !arrow.isCritArrow()) {
				return false;
			}
		}
		return true;
	}

	private static void afterDamage(LivingEntity entity, DamageSource source, float taken, boolean blocked) {
		if (entity instanceof ServerPlayer player) {
			if (blocked || taken <= 0.0F) {
				return;
			}
			if (taken > 6.0F && active(ModifierCatalog.BLEEDING, player)) {
				player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
			}
			if (source.getEntity() instanceof Mob) {
				if (active(ModifierCatalog.VENOMOUS, player)) {
					player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
				}
				if (active(ModifierCatalog.HELLFIRE_MOBS, player)) {
					player.igniteForSeconds(4.0F);
				}
			}
			if (active(ModifierCatalog.SHELLSHOCK, player) && instance != null) {
				instance.shellshockUntil.put(player.getUUID(), System.nanoTime() + SHELLSHOCK_NANOS);
			}
		} else if (taken > 0.0F && source.getEntity() instanceof ServerPlayer attacker && active(ModifierCatalog.THORNED_MOBS, entity)) {
			attacker.hurt(attacker.damageSources().thorns(entity), DamageScaling.scale(taken, 0.25F, LETHAL_LIMIT));
		}
	}

	/** bouncy: a fall that would hurt launches the player back up to about 80% of the height fallen. */
	private static void bounce(ServerPlayer player) {
		double height = player.fallDistance;
		// Ender pearls deal fall damage too; a pearl landing just cancels the damage.
		if (height <= player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)) {
			return;
		}
		double speed = Math.min(3.0, Math.sqrt(2.0 * player.getGravity() * 0.8 * height));
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(motion.x, speed, motion.z);
		player.hurtMarked = true;
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	// ---- mixin entry points ----

	/** Called at the head of LivingEntity.hurt. */
	public static float scaleIncomingDamage(LivingEntity entity, DamageSource source, float amount) {
		if (entity instanceof ServerPlayer && source.is(DamageTypeTags.IS_FIRE) && active(ModifierCatalog.FIRE_WEAKNESS, entity)) {
			return DamageScaling.scale(amount, 2.0F, LETHAL_LIMIT);
		}
		return amount;
	}

	public static double scaleKnockback(LivingEntity entity, double strength) {
		return entity instanceof ServerPlayer && active(ModifierCatalog.KNOCKBACK_CHAOS, entity) ? strength * 3.0 : strength;
	}

	/** short_breath: air drops by 3 instead of 1, stopping exactly at -20 so vanilla's drowning check still fires. */
	public static int scaleAirLoss(LivingEntity entity, int before, int after) {
		if (after < before && entity instanceof ServerPlayer && active(ModifierCatalog.SHORT_BREATH, entity)) {
			return Math.max(before - 3, -20);
		}
		return after;
	}

	public static boolean invertsHealing(LivingEntity entity) {
		return entity instanceof ServerPlayer && active(ModifierCatalog.UNDEAD, entity);
	}

	/** undead: undead mobs ignore players unless that player hit them recently. The Wither still fights. */
	public static boolean undeadIgnores(Mob mob, @Nullable LivingEntity target) {
		return target instanceof ServerPlayer player
			&& mob.getType().is(EntityTypeTags.UNDEAD)
			&& !(mob instanceof WitherBoss)
			&& mob.getLastHurtByMob() != player
			&& active(ModifierCatalog.UNDEAD, player);
	}

	public static boolean blocksNaturalRegen(Player player) {
		return active(ModifierCatalog.VAMPIRE, player);
	}

	/** explosive_arrows: true when the projectile exploded and its normal hit must be skipped. */
	public static boolean explodeArrow(Entity projectile) {
		if (!(projectile instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof AbstractSkeleton)
			|| !active(ModifierCatalog.EXPLOSIVE_ARROWS, arrow)) {
			return false;
		}
		arrow.level().explode(arrow, arrow.getX(), arrow.getY(), arrow.getZ(), 2.0F, Level.ExplosionInteraction.MOB);
		arrow.discard();
		return true;
	}

	public static float scaleExplosion(Level level, float radius) {
		return Modifiers.isActive(ModifierCatalog.BIG_BOOMS, level) ? radius * 2.0F : radius;
	}

	public static void startAttack(Player player, Entity target) {
		attacking = player;
		attackIsCrit = player.getAttackStrengthScale(0.5F) > 0.9F
			&& player.fallDistance > 0.0F
			&& !player.onGround()
			&& !player.onClimbable()
			&& !player.isInWater()
			&& !player.hasEffect(MobEffects.BLINDNESS)
			&& !player.isPassenger()
			&& target instanceof LivingEntity
			&& !player.isSprinting();
	}

	public static void endAttack() {
		attacking = null;
		attackIsCrit = false;
	}

	/**
	 * Called by {@link RunManager#allowDeath} for a fatal hit to a player in the live run. Returns true when the player
	 * was saved and the run must go on. Totems work in every run; vanilla checks them after Fabric's death event, which
	 * the run manager always cancels, so without this they would never trigger.
	 */
	public static boolean preventDeath(ServerPlayer player, DamageSource source) {
		if (((ModifierBodyLivingEntityAccessor) player).speedrun$checkTotemDeathProtection(source)) {
			return true;
		}
		BodyModifiers body = instance;
		return body != null && body.current != null && body.forgive(body.current, player, source);
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		current = run;
		entered.clear();
		dead.clear();
		secondWindUsed = false;
		heartsLost = -1;
		shellshockUntil.clear();
		long elapsed = Math.max(0, run.clockNanos() - run.startNanos);
		nextDarknessNanos = run.startNanos + (elapsed / DARKNESS_INTERVAL_NANOS + 1) * DARKNESS_INTERVAL_NANOS;
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		JsonObject body = new JsonObject();
		body.add("entered", uuids(entered));
		body.add("dead", uuids(dead));
		body.addProperty("secondWindUsed", secondWindUsed);
		out.add("body", body);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (!in.has("body")) {
			return;
		}
		JsonObject body = in.getAsJsonObject("body");
		readUuids(body, "entered", entered);
		readUuids(body, "dead", dead);
		secondWindUsed = body.has("secondWindUsed") && body.get("secondWindUsed").getAsBoolean();
	}

	private static JsonArray uuids(Set<UUID> set) {
		JsonArray array = new JsonArray();
		set.forEach(id -> array.add(id.toString()));
		return array;
	}

	private static void readUuids(JsonObject body, String key, Set<UUID> into) {
		if (body.has(key)) {
			for (JsonElement id : body.getAsJsonArray(key)) {
				into.add(UUID.fromString(id.getAsString()));
			}
		}
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		for (ServerPlayer player : runs.server().getPlayerList().getPlayers()) {
			undo(run, player);
		}
		current = null;
		entered.clear();
		dead.clear();
		shellshockUntil.clear();
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		if (dead.contains(player.getUUID())) {
			// Reconnecting puts players back in survival; the dead stay out.
			player.setGameMode(GameType.SPECTATOR);
			return;
		}
		for (String id : run.modifiers) {
			for (Attr entry : ATTRIBUTES.getOrDefault(id, List.of())) {
				AttributeInstance attribute = player.getAttribute(entry.attribute());
				if (attribute != null) {
					attribute.addOrUpdateTransientModifier(new AttributeModifier(entry.id(), entry.amount(), entry.operation()));
				}
			}
		}
		if (entered.add(player.getUUID())) {
			if (run.modifiers.contains(ModifierCatalog.EXTRA_HEARTS)) {
				player.setHealth(player.getMaxHealth());
			}
			if (run.modifiers.contains(ModifierCatalog.TOTEM_START)) {
				ItemStack totem = new ItemStack(Items.TOTEM_OF_UNDYING);
				if (player.getOffhandItem().isEmpty()) {
					player.setItemSlot(EquipmentSlot.OFFHAND, totem);
				} else {
					player.getInventory().placeItemBackInInventory(totem);
				}
			}
		}
	}

	@Override
	public void onPlayerLeaveRun(ActiveRun run, ServerPlayer player) {
		undo(run, player);
	}

	/** Removes everything this class put on a player. Safe to call on players who never had any of it. */
	private static void undo(ActiveRun run, ServerPlayer player) {
		for (List<Attr> entries : ATTRIBUTES.values()) {
			for (Attr entry : entries) {
				removeModifier(player, entry.attribute(), entry.id());
			}
		}
		removeModifier(player, Attributes.MAX_HEALTH, SUDDEN_DEATH_ID);
		removeModifier(player, Attributes.MOVEMENT_SPEED, SHELLSHOCK_ID);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		for (String id : run.modifiers) {
			Perm perm = PERMANENT_EFFECTS.get(id);
			MobEffectInstance effect = perm == null ? null : player.getEffect(perm.effect());
			if (effect != null && effect.isInfiniteDuration()) {
				player.removeEffect(perm.effect());
			}
		}
		removePumpkins(player);
	}

	private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}

	@Override
	public void tick(ActiveRun run) {
		if (run.modifiers.isEmpty()) {
			return;
		}
		List<ServerPlayer> players = Modifiers.playersIn(run);
		List<String> mods = run.modifiers;
		long now = run.clockNanos();
		boolean darkness = mods.contains(ModifierCatalog.DARKNESS_PULSE) && now >= nextDarknessNanos;
		if (darkness) {
			nextDarknessNanos += DARKNESS_INTERVAL_NANOS;
		}
		int lost = suddenDeathHearts(run);
		for (ServerPlayer player : players) {
			for (String id : mods) {
				Perm perm = PERMANENT_EFFECTS.get(id);
				if (perm != null) {
					keepEffect(player, perm);
				}
			}
			if (mods.contains(ModifierCatalog.SUDDEN_DEATH)) {
				applySuddenDeath(player, lost);
			}
			if (darkness) {
				player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false, true));
			}
			if (mods.contains(ModifierCatalog.VERTIGO) && player.getY() > 100.0) {
				MobEffectInstance nausea = player.getEffect(MobEffects.CONFUSION);
				if (nausea == null || nausea.getDuration() < 60) {
					player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, false, false, true));
				}
			}
			if (mods.contains(ModifierCatalog.HYDROPHOBIC) && player.isInWater() && run.gameTicks % 10 == 0) {
				player.hurt(player.damageSources().drown(), 1.0F);
			}
			if (mods.contains(ModifierCatalog.FROST_WALKER) && player.onGround()) {
				freezeWater(player);
			}
			if (mods.contains(ModifierCatalog.PUMPKIN_HEAD)) {
				wearPumpkin(player);
			}
			if (mods.contains(ModifierCatalog.UNDEAD)) {
				burnInSun(player);
			}
			if (mods.contains(ModifierCatalog.SHELLSHOCK)) {
				shellshock(player);
			}
		}
		if (mods.contains(ModifierCatalog.SUDDEN_DEATH) && lost != heartsLost) {
			if (heartsLost >= 0 && lost > heartsLost) {
				runs.broadcast(Component.literal("Sudden Death! Max health is down " + lost + (lost == 1 ? " heart." : " hearts.")).withStyle(ChatFormatting.DARK_RED));
			}
			heartsLost = lost;
		}
		if (mods.contains(ModifierCatalog.HELLFIRE_MOBS) && run.gameTicks % 20 == 0) {
			for (ServerLevel level : List.of(run.worlds.overworld(), run.worlds.nether(), run.worlds.end())) {
				for (Entity entity : level.getAllEntities()) {
					if (entity instanceof Mob mob && mob instanceof Enemy && mob.isAlive()) {
						ignite(mob);
					}
				}
			}
		}
	}

	private static void keepEffect(ServerPlayer player, Perm perm) {
		MobEffectInstance effect = player.getEffect(perm.effect());
		if (effect == null || (!effect.isInfiniteDuration() && effect.getAmplifier() <= perm.amplifier())) {
			player.addEffect(new MobEffectInstance(perm.effect(), MobEffectInstance.INFINITE_DURATION, perm.amplifier(), false, false, true));
		}
	}

	/** Hearts lost so far: one at 30 minutes of run time, then one more every 5 minutes, never the last heart. */
	static int suddenDeathHearts(long elapsedNanos) {
		if (elapsedNanos < SUDDEN_DEATH_START_NANOS) {
			return 0;
		}
		return (int) Math.min(SUDDEN_DEATH_MAX_LOST, 1 + (elapsedNanos - SUDDEN_DEATH_START_NANOS) / SUDDEN_DEATH_STEP_NANOS);
	}

	private static int suddenDeathHearts(ActiveRun run) {
		return suddenDeathHearts(Math.max(0, run.clockNanos() - run.startNanos));
	}

	private static void applySuddenDeath(ServerPlayer player, int lost) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		AttributeModifier existing = maxHealth.getModifier(SUDDEN_DEATH_ID);
		double amount = -2.0 * lost;
		if (lost == 0 ? existing == null : existing != null && existing.amount() == amount) {
			return;
		}
		if (lost == 0) {
			maxHealth.removeModifier(SUDDEN_DEATH_ID);
		} else {
			maxHealth.addOrUpdateTransientModifier(new AttributeModifier(SUDDEN_DEATH_ID, amount, Operation.ADD_VALUE));
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	/** Same rule as the Frost Walker I enchantment: still water sources within 3 blocks below the player turn to frosted ice. */
	private static void freezeWater(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos center = player.blockPosition().below();
		int radius = 3;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
			if (pos.distToCenterSqr(player.getX(), pos.getY() + 0.5, player.getZ()) < radius * radius
				&& level.getBlockState(pos.above()).isAir()
				&& level.getBlockState(pos).is(Blocks.WATER)
				&& level.getFluidState(pos).is(Fluids.WATER)
				&& level.getFluidState(pos).isSource()
				&& level.isUnobstructed(null, Shapes.block().move(pos.getX(), pos.getY(), pos.getZ()))) {
				level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
				level.scheduleTick(pos, Blocks.FROSTED_ICE, Mth.nextInt(player.getRandom(), 60, 120));
			}
		}
	}

	private static boolean isPumpkin(ItemStack stack) {
		return stack.is(Items.CARVED_PUMPKIN) && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(PUMPKIN_TAG);
	}

	/** Puts the cursed pumpkin back on if anything replaced it. A helmet that was worn moves to the inventory. */
	private static void wearPumpkin(ServerPlayer player) {
		ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
		if (isPumpkin(head)) {
			return;
		}
		ItemStack pumpkin = new ItemStack(Items.CARVED_PUMPKIN);
		CustomData.update(DataComponents.CUSTOM_DATA, pumpkin, tag -> tag.putBoolean(PUMPKIN_TAG, true));
		pumpkin.enchant(player.serverLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BINDING_CURSE), 1);
		player.setItemSlot(EquipmentSlot.HEAD, pumpkin);
		if (!head.isEmpty()) {
			player.getInventory().placeItemBackInInventory(head);
		}
	}

	private static void removePumpkins(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			if (isPumpkin(inventory.getItem(slot))) {
				inventory.setItem(slot, ItemStack.EMPTY);
			}
		}
		if (isPumpkin(player.containerMenu.getCarried())) {
			player.containerMenu.setCarried(ItemStack.EMPTY);
		}
	}

	/** undead: the zombie sunlight rule. A helmet takes the damage instead, like it does for zombies. */
	private static void burnInSun(ServerPlayer player) {
		Level level = player.level();
		if (!level.isDay() || player.isInWaterRainOrBubble() || player.isInPowderSnow || player.wasInPowderSnow) {
			return;
		}
		float light = player.getLightLevelDependentMagicValue();
		if (light <= 0.5F || player.getRandom().nextFloat() * 30.0F >= (light - 0.4F) * 2.0F
			|| !level.canSeeSky(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()))) {
			return;
		}
		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		if (!helmet.isEmpty()) {
			if (helmet.isDamageableItem()) {
				helmet.hurtAndBreak(player.getRandom().nextInt(2), player, EquipmentSlot.HEAD);
			}
			return;
		}
		player.igniteForSeconds(8.0F);
	}

	/** shellshock: while shocked, a speed penalty cancels the sprint bonus whenever the player sprints. */
	private void shellshock(ServerPlayer player) {
		Long until = shellshockUntil.get(player.getUUID());
		boolean shocked = until != null && System.nanoTime() < until;
		if (until != null && !shocked) {
			shellshockUntil.remove(player.getUUID());
		}
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		if (shocked && player.isSprinting()) {
			speed.addOrUpdateTransientModifier(new AttributeModifier(SHELLSHOCK_ID, SPRINT_CANCEL, Operation.ADD_MULTIPLIED_TOTAL));
		} else {
			speed.removeModifier(SHELLSHOCK_ID);
		}
	}

	/** hellfire_mobs: hostile mobs stay on fire, with Fire Resistance so the fire never kills them. */
	private static void ignite(Mob mob) {
		if (!mob.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		}
		if (mob.getRemainingFireTicks() < 40) {
			mob.setRemainingFireTicks(100);
		}
	}

	// ---- deaths ----

	private boolean forgive(ActiveRun run, ServerPlayer player, DamageSource source) {
		if (run.modifiers.contains(ModifierCatalog.SECOND_WIND) && !secondWindUsed) {
			secondWindUsed = true;
			runs.broadcast(source.getLocalizedDeathMessage(player).copy().withStyle(ChatFormatting.RED));
			runs.broadcast(Component.literal("Second Wind! " + player.getGameProfile().getName() + " gets another chance. The next death counts.")
				.withStyle(ChatFormatting.GOLD));
			dropLikeDeath(player);
			revive(player);
			BlockPos spawn = run.worlds.spawn();
			player.stopRiding();
			player.teleportTo(run.worlds.overworld(), spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
			player.setDeltaMovement(Vec3.ZERO);
			return true;
		}
		if (run.modifiers.contains(ModifierCatalog.LAST_ONE_STANDING)) {
			int alive = 0;
			for (ServerPlayer other : Modifiers.playersIn(run)) {
				if (other != player && !dead.contains(other.getUUID())) {
					alive++;
				}
			}
			if (alive == 0) {
				return false;
			}
			dead.add(player.getUUID());
			runs.broadcast(source.getLocalizedDeathMessage(player).copy().withStyle(ChatFormatting.RED));
			runs.broadcast(Component.literal(player.getGameProfile().getName() + " is out. " + alive + (alive == 1 ? " player is" : " players are") + " still standing.")
				.withStyle(ChatFormatting.GOLD));
			dropLikeDeath(player);
			revive(player);
			player.setGameMode(GameType.SPECTATOR);
			return true;
		}
		return false;
	}

	/** Drops the inventory and experience where the player fell, as a vanilla death would. */
	private static void dropLikeDeath(ServerPlayer player) {
		removePumpkins(player);
		ServerLevel level = player.serverLevel();
		if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
			return;
		}
		player.getInventory().dropAll();
		int xp = Math.min(player.experienceLevel * 7, 100);
		if (xp > 0) {
			ExperienceOrb.award(level, player.position(), xp);
		}
		player.setExperienceLevels(0);
		player.setExperiencePoints(0);
		player.totalExperience = 0;
	}

	private static void revive(ServerPlayer player) {
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);
		player.getFoodData().setExhaustion(0);
		player.clearFire();
		player.resetFallDistance();
		player.setAirSupply(player.getMaxAirSupply());
	}
}
