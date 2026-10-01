package com.eternalerkle.speedrun.modifier;

import com.eternalerkle.speedrun.mixin.ModifierItemsEyeOfEnderAccessor;
import com.eternalerkle.speedrun.run.ActiveRun;
import com.eternalerkle.speedrun.run.RunFeature;
import com.eternalerkle.speedrun.run.RunManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The items group: item bans, durability, drops, chest loot, starting items and combat restrictions, plus loot_chaos
 * and jackpot, which share the block drop hook so they stack. Mixins call the public static methods.
 */
public final class ItemModifiers implements RunFeature {
	private static final double MAGNET_RANGE = 8.0;
	private static final int VEIN_LIMIT = 64;
	private static final int TREE_LIMIT = 256;
	private static final float APPLE_CHANCE = 0.1F;
	private static final int JACKPOT_MAX = 640;
	private static final int MAX_STACKS_PER_DROP = 64;
	/** Chosen so that 40% of rolls go above x1 and 1 in 64 of those reach x100: overall about 1 in 160. */
	private static final double JACKPOT_TAIL = Math.log(64) / Math.log(99);
	private static final Random RANDOM = new Random();

	private static boolean eventsRegistered;
	/** The live run, for its seed. Null outside a run. */
	@Nullable
	private static ActiveRun current;
	@Nullable
	private static List<Item> chaosCandidates;
	/** Set while vein_miner or timber breaks extra blocks, so those breaks do not start another sweep. */
	private static boolean chainBreaking;

	/** Players who already got this run's pearl_start or wings items. */
	private final Set<UUID> kitted = new HashSet<>();
	private int ticks;

	private ItemModifiers() {
	}

	static void register(RunManager runs) {
		runs.addFeature(new ItemModifiers());
		TeamModifiers.register(runs);
		if (!eventsRegistered) {
			eventsRegistered = true;
			// Hot potato passes on a hit, so its attack listener must run before bow_only cancels the hit.
			TeamModifiers.registerEvents();
			registerEvents();
		}
	}

	// ---- RunFeature ----

	@Override
	public void onRunStart(ActiveRun run) {
		current = run;
		kitted.clear();
	}

	@Override
	public void onRunEnd(ActiveRun run) {
		current = null;
		kitted.clear();
		chainBreaking = false;
	}

	@Override
	public void save(ActiveRun run, JsonObject out) {
		JsonArray given = new JsonArray();
		kitted.forEach(id -> given.add(id.toString()));
		out.add("itemKitsGiven", given);
	}

	@Override
	public void restore(ActiveRun run, JsonObject in) {
		if (in.has("itemKitsGiven")) {
			for (JsonElement id : in.getAsJsonArray("itemKitsGiven")) {
				kitted.add(UUID.fromString(id.getAsString()));
			}
		}
	}

	@Override
	public void onPlayerEnterRun(ActiveRun run, ServerPlayer player) {
		boolean pearls = run.modifiers.contains(ModifierCatalog.PEARL_START);
		boolean wings = run.modifiers.contains(ModifierCatalog.WINGS);
		if ((pearls || wings) && kitted.add(player.getUUID())) {
			if (pearls) {
				player.getInventory().placeItemBackInInventory(new ItemStack(Items.ENDER_PEARL, 4));
			}
			if (wings) {
				player.getInventory().placeItemBackInInventory(new ItemStack(Items.ELYTRA));
				player.getInventory().placeItemBackInInventory(new ItemStack(Items.FIREWORK_ROCKET, 16));
			}
		}
	}

	@Override
	public void tick(ActiveRun run) {
		ticks++;
		List<String> active = run.modifiers;
		boolean bans = active.contains(ModifierCatalog.NO_SHIELDS) || active.contains(ModifierCatalog.NO_DIAMOND_GEAR)
			|| active.contains(ModifierCatalog.STONE_AGE) || active.contains(ModifierCatalog.NO_BOATS);
		boolean noArmor = active.contains(ModifierCatalog.NO_ARMOR);
		boolean cursed = active.contains(ModifierCatalog.CURSED_ARMOR);
		boolean magnet = active.contains(ModifierCatalog.ITEM_MAGNET) && ticks % 2 == 0;
		if (!bans && !noArmor && !cursed && !magnet) {
			return;
		}
		for (ServerPlayer player : Modifiers.playersIn(run)) {
			if (bans) {
				purgeBanned(player);
			}
			if (noArmor) {
				stripArmor(player);
			}
			if (cursed) {
				curseArmor(player);
			}
			if (magnet && player.isAlive()) {
				pullItems(player);
			}
		}
	}

	// ---- events ----

	private static void registerEvents() {
		// melee_only: bows and crossbows refuse to draw or fire.
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if ((stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) && Modifiers.isActive(ModifierCatalog.MELEE_ONLY, world)) {
				deny(player, "Bows and crossbows do not work this run.");
				return InteractionResultHolder.fail(stack);
			}
			return InteractionResultHolder.pass(ItemStack.EMPTY);
		});
		// bow_only: melee hits on living things (and the dragon's parts) are cancelled.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if ((entity instanceof LivingEntity || entity instanceof EnderDragonPart) && Modifiers.isActive(ModifierCatalog.BOW_ONLY, world)) {
				deny(player, "Melee does no damage this run.");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		// villager_strike: no trade screen for villagers or wandering traders.
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (entity instanceof AbstractVillager && Modifiers.isActive(ModifierCatalog.VILLAGER_STRIKE, world)) {
				if (hand == InteractionHand.MAIN_HAND) {
					deny(player, "The villagers are on strike.");
					world.playSound(null, entity.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
				}
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (Modifiers.isActive(ModifierCatalog.INSOMNIA, player.level())) {
				deny(player, "You can't sleep this run.");
				return Player.BedSleepingProblem.OTHER_PROBLEM;
			}
			return null;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseTaken, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && !blocked && taken > 0 && player.isAlive()
				&& Modifiers.isActive(ModifierCatalog.BUTTERFINGERS, player.level()) && !player.getMainHandItem().isEmpty()) {
				ItemStack held = player.getMainHandItem();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.drop(held, true, false);
			}
		});
		// The eye decides whether it survives when thrown, before it is added to the level.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof EyeOfEnder eye) {
				if (Modifiers.isActive(ModifierCatalog.FRAGILE_EYES, level)) {
					((ModifierItemsEyeOfEnderAccessor) eye).speedrun$setSurviveAfterDeath(false);
				} else if (Modifiers.isActive(ModifierCatalog.STURDY_EYES, level)) {
					((ModifierItemsEyeOfEnderAccessor) eye).speedrun$setSurviveAfterDeath(true);
				}
			}
		});
		// vein_miner and timber. Sneaking breaks a single block as usual.
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (chainBreaking || !(player instanceof ServerPlayer serverPlayer) || player.isShiftKeyDown() || serverPlayer.isCreative()) {
				return;
			}
			if (state.is(ConventionalBlockTags.ORES) && Modifiers.isActive(ModifierCatalog.VEIN_MINER, world) && player.hasCorrectToolForDrops(state)) {
				breakConnected(serverPlayer, pos, other -> other.is(state.getBlock()) && serverPlayer.hasCorrectToolForDrops(other), VEIN_LIMIT);
			} else if (state.is(BlockTags.LOGS) && Modifiers.isActive(ModifierCatalog.TIMBER, world)) {
				breakConnected(serverPlayer, pos, other -> other.is(BlockTags.LOGS), TREE_LIMIT);
			}
		});
	}

	private static void deny(Player player, String message) {
		player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), true);
	}

	/** Breaks blocks touching {@code start} (diagonals included) that match, as if the player mined each one. */
	private static void breakConnected(ServerPlayer player, BlockPos start, Predicate<BlockState> matches, int limit) {
		Level level = player.level();
		List<BlockPos> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty() && found.size() < limit) {
			BlockPos pos = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
				if (found.size() >= limit) {
					break;
				}
				BlockPos immutable = next.immutable();
				if (seen.add(immutable) && matches.test(level.getBlockState(immutable))) {
					found.add(immutable);
					queue.add(immutable);
				}
			}
		}
		boolean hadTool = !player.getMainHandItem().isEmpty();
		chainBreaking = true;
		try {
			for (BlockPos pos : found) {
				// Stop when the tool breaks partway, so the rest is not mined by hand.
				if (hadTool && player.getMainHandItem().isEmpty() || !matches.test(level.getBlockState(pos))) {
					break;
				}
				player.gameMode.destroyBlock(pos);
			}
		} finally {
			chainBreaking = false;
		}
	}

	// ---- item bans, armor and magnet ----

	/** Holder so the item sets are only built once Minecraft's registries exist. */
	private static final class Gear {
		static final Set<Item> DIAMOND = Set.of(Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL,
			Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
		static final Set<Item> IRON = Set.of(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE,
			Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.CHAINMAIL_HELMET,
			Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
		/** Creative-only or technical items loot_chaos never hands out, on top of spawn eggs and operator blocks. */
		static final Set<Item> NOT_LOOT = Set.of(Items.AIR, Items.BARRIER, Items.LIGHT, Items.STRUCTURE_VOID, Items.DEBUG_STICK,
			Items.KNOWLEDGE_BOOK, Items.COMMAND_BLOCK_MINECART, Items.BEDROCK, Items.END_PORTAL_FRAME, Items.REINFORCED_DEEPSLATE,
			Items.BUDDING_AMETHYST, Items.PETRIFIED_OAK_SLAB, Items.SPAWNER, Items.TRIAL_SPAWNER, Items.VAULT, Items.FROGSPAWN);
	}

	/** Whether the item is banned in this level. Buckets are only banned from crafting. */
	private static boolean banned(Item item, Level level, boolean crafting) {
		if (item == Items.SHIELD) {
			return Modifiers.isActive(ModifierCatalog.NO_SHIELDS, level);
		}
		if (item instanceof BoatItem) {
			return Modifiers.isActive(ModifierCatalog.NO_BOATS, level);
		}
		if (item == Items.BUCKET) {
			return crafting && Modifiers.isActive(ModifierCatalog.BUCKETLESS, level);
		}
		if (Gear.DIAMOND.contains(item)) {
			return Modifiers.isActive(ModifierCatalog.NO_DIAMOND_GEAR, level);
		}
		if (Gear.IRON.contains(item)) {
			return Modifiers.isActive(ModifierCatalog.STONE_AGE, level);
		}
		return false;
	}

	/** Called by the crafting mixin with the result about to be shown. Banned results come out empty. */
	public static ItemStack filterCraft(ItemStack result, Player player) {
		if (!result.isEmpty() && banned(result.getItem(), player.level(), true)) {
			deny(player, result.getHoverName().getString() + " can't be crafted this run.");
			return ItemStack.EMPTY;
		}
		return result;
	}

	/** Deletes banned items that reached the inventory some other way, such as chest loot or mob drops. */
	private static void purgeBanned(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && banned(stack.getItem(), player.level(), false)) {
				inventory.setItem(slot, ItemStack.EMPTY);
				deny(player, stack.getHoverName().getString() + " is banned this run.");
			}
		}
	}

	/** no_armor moves worn armor back into the inventory. Elytra, pumpkins and heads are not armor and stay. */
	private static void stripArmor(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.armor.size(); slot++) {
			ItemStack stack = inventory.armor.get(slot);
			if (stack.getItem() instanceof ArmorItem) {
				inventory.armor.set(slot, ItemStack.EMPTY);
				inventory.placeItemBackInInventory(stack);
				deny(player, "Armor can't be worn this run.");
			}
		}
	}

	private static void curseArmor(ServerPlayer player) {
		Holder<Enchantment> binding = player.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BINDING_CURSE);
		for (ItemStack stack : player.getInventory().armor) {
			if (stack.getItem() instanceof ArmorItem && EnchantmentHelper.getItemEnchantmentLevel(binding, stack) == 0) {
				stack.enchant(binding, 1);
			}
		}
	}

	/** item_magnet moves loose items within range onto the player, who then picks them up as usual. */
	private static void pullItems(ServerPlayer player) {
		double rangeSqr = MAGNET_RANGE * MAGNET_RANGE;
		for (ItemEntity item : player.serverLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(MAGNET_RANGE),
			item -> !item.hasPickUpDelay() && item.distanceToSqr(player) <= rangeSqr)) {
			item.setPos(player.getX(), player.getY(), player.getZ());
		}
	}

	// ---- drops and loot, called from mixins ----

	/**
	 * Every block drop passes through here: loot_chaos swaps the item, then ore_rush and jackpot multiply the count,
	 * then orchard may add an apple. Jackpot only counts blocks a player broke.
	 */
	public static List<ItemStack> blockDrops(BlockState state, LootParams.Builder params, List<ItemStack> drops) {
		ServerLevel level = params.getLevel();
		boolean chaos = Modifiers.isActive(ModifierCatalog.LOOT_CHAOS, level);
		boolean rush = Modifiers.isActive(ModifierCatalog.ORE_RUSH, level) && state.is(ConventionalBlockTags.ORES);
		Entity breaker = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
		boolean jackpot = breaker instanceof Player && Modifiers.isActive(ModifierCatalog.JACKPOT, level);
		boolean orchard = Modifiers.isActive(ModifierCatalog.ORCHARD, level) && state.is(BlockTags.LEAVES);
		if (!chaos && !rush && !jackpot && !orchard) {
			return drops;
		}
		int multiplier = rush ? 2 : 1;
		if (jackpot && !drops.isEmpty()) {
			int roll = jackpotMultiplier(RANDOM.nextDouble(), RANDOM.nextDouble());
			multiplier *= roll;
			if (roll >= 10) {
				((Player) breaker).displayClientMessage(Component.literal("Jackpot! x" + roll).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
			}
		}
		Item chaosItem = chaos ? chaosItem("block/" + BuiltInRegistries.BLOCK.getKey(state.getBlock()), level) : null;
		List<ItemStack> out = new ArrayList<>();
		for (ItemStack stack : drops) {
			ItemStack base = chaosItem != null ? new ItemStack(chaosItem) : stack.copyWithCount(1);
			split(base, (long) stack.getCount() * multiplier, out::add);
		}
		if (orchard && level.random.nextFloat() < APPLE_CHANCE) {
			out.add(new ItemStack(Items.APPLE));
		}
		return out;
	}

	/** Wraps a mob's loot consumer so loot_chaos turns each drop into the item mapped to that mob type. */
	public static Consumer<ItemStack> mobDrops(LivingEntity entity, Consumer<ItemStack> drop) {
		if (entity instanceof Player || !(entity.level() instanceof ServerLevel level) || !Modifiers.isActive(ModifierCatalog.LOOT_CHAOS, level)) {
			return drop;
		}
		Item item = chaosItem("entity/" + EntityType.getKey(entity.getType()), level);
		return stack -> split(new ItemStack(item), stack.getCount(), drop);
	}

	/** Gold Rush: every barter result is handed out twice. */
	public static List<ItemStack> barter(Piglin piglin, List<ItemStack> items) {
		if (!Modifiers.isActive(ModifierCatalog.GOLD_RUSH, piglin.level())) {
			return items;
		}
		List<ItemStack> doubled = new ArrayList<>(items);
		for (ItemStack stack : items) {
			doubled.add(stack.copy());
		}
		return doubled;
	}

	public static boolean chestLooted(LootParams params) {
		return Modifiers.isActive(ModifierCatalog.LOOTED, params.getLevel());
	}

	public static boolean chestDoubled(LootParams params) {
		return Modifiers.isActive(ModifierCatalog.TREASURE_HUNTER, params.getLevel());
	}

	/** Splits a count into full stacks. Unstackable jackpots are capped so one block cannot flood the server with entities. */
	private static void split(ItemStack base, long count, Consumer<ItemStack> out) {
		int max = base.getMaxStackSize();
		count = Math.min(count, (long) max * MAX_STACKS_PER_DROP);
		while (count > 0) {
			int size = (int) Math.min(count, max);
			out.accept(base.copyWithCount(size));
			count -= size;
		}
	}

	/** The loot_chaos item for a drop source. Derived from the run's seed, so it is the same after a restart and new each seed. */
	private static Item chaosItem(String source, ServerLevel level) {
		List<Item> candidates = chaosCandidates(level.enabledFeatures());
		ActiveRun run = current;
		long seed = run == null ? 0L : run.worlds.seed;
		return candidates.get(chaosIndex(seed, source, candidates.size()));
	}

	private static List<Item> chaosCandidates(FeatureFlagSet features) {
		if (chaosCandidates == null) {
			List<Item> items = new ArrayList<>();
			for (Item item : BuiltInRegistries.ITEM) {
				if (!Gear.NOT_LOOT.contains(item) && !(item instanceof SpawnEggItem) && !(item instanceof GameMasterBlockItem) && item.isEnabled(features)) {
					items.add(item);
				}
			}
			chaosCandidates = items;
		}
		return chaosCandidates;
	}

	/** Index into the candidate list for a source under a seed. Pure, so it can be unit tested. */
	static int chaosIndex(long seed, String source, int size) {
		long z = seed + 0x9E3779B97F4A7C15L * (source.hashCode() + 1L);
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		z ^= z >>> 31;
		return (int) Math.floorMod(z, (long) size);
	}

	/**
	 * Jackpot's multiplier from two uniform numbers in [0, 1). 60% of the time x1; otherwise a power-law tail
	 * starting at x2, where x100 or more comes up 1 in 64 times. Capped at 640.
	 */
	static int jackpotMultiplier(double u1, double u2) {
		if (u1 < 0.6) {
			return 1;
		}
		double tail = Math.pow(1.0 - u2, -1.0 / JACKPOT_TAIL);
		return (int) Math.min(JACKPOT_MAX, 1 + Math.floor(tail));
	}
}
