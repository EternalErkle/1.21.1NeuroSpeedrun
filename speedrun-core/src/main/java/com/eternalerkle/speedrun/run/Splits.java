package com.eternalerkle.speedrun.run;

import com.eternalerkle.speedrun.stats.RunRecord;
import com.eternalerkle.speedrun.util.Time;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.LinkedHashMap;
import java.util.Map;

/** Records milestone times and compares them against the category record. */
public final class Splits implements RunFeature {
	/** Split ids in display order, with their names. */
	public static final Map<String, String> NAMES = new LinkedHashMap<>();

	static {
		NAMES.put("nether", "Enter Nether");
		NAMES.put("fortress", "Fortress");
		NAMES.put("blaze_rod", "Blaze Rod");
		NAMES.put("bastion", "Bastion");
		NAMES.put("stronghold", "Stronghold");
		NAMES.put("end", "Enter End");
		NAMES.put("dragon", "Dragon");
		NAMES.put("warden", "Warden");
		NAMES.put("wither", "Wither");
	}

	private static final int CHECK_INTERVAL_TICKS = 10;

	private final RunManager manager;
	private int counter;

	Splits(RunManager manager) {
		this.manager = manager;
	}

	@Override
	public void tick(ActiveRun run) {
		if (++counter % CHECK_INTERVAL_TICKS != 0) {
			return;
		}
		for (ServerPlayer player : manager.server().getPlayerList().getPlayers()) {
			ServerLevel level = player.serverLevel();
			if (level == run.worlds.nether()) {
				reach(run, "nether");
				if (!run.splits.containsKey("fortress") && inStructure(level, player, BuiltinStructures.FORTRESS)) {
					reach(run, "fortress");
				}
				if (!run.splits.containsKey("bastion") && inStructure(level, player, BuiltinStructures.BASTION_REMNANT)) {
					reach(run, "bastion");
				}
			} else if (level == run.worlds.overworld()) {
				if (!run.splits.containsKey("stronghold") && inStructure(level, player, BuiltinStructures.STRONGHOLD)) {
					reach(run, "stronghold");
				}
			} else if (level == run.worlds.end()) {
				reach(run, "end");
			}
			if (!run.splits.containsKey("blaze_rod") && player.getInventory().contains(Items.BLAZE_ROD.getDefaultInstance())) {
				reach(run, "blaze_rod");
			}
		}
	}

	void onBossKilled(ActiveRun run, ActiveRun.Boss boss) {
		reach(run, switch (boss) {
			case DRAGON -> "dragon";
			case WARDEN -> "warden";
			case WITHER -> "wither";
		});
	}

	private static boolean inStructure(ServerLevel level, ServerPlayer player, ResourceKey<Structure> key) {
		return level.structureManager().getStructureWithPieceAt(player.blockPosition(), (Holder<Structure> holder) -> holder.is(key)).isValid();
	}

	private void reach(ActiveRun run, String id) {
		if (!run.split(id)) {
			return;
		}
		long time = run.splits.get(id);
		MutableComponent message = Component.literal("⏱ " + NAMES.getOrDefault(id, id) + " ").withStyle(ChatFormatting.AQUA)
			.append(Component.literal(Time.format(time)).withStyle(ChatFormatting.WHITE));
		Long best = bestSplit(run, id);
		if (best != null) {
			long delta = time - best;
			message.append(Component.literal(" (" + Time.formatDelta(delta) + ")").withStyle(delta <= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
		manager.broadcast(message);
	}

	@org.jetbrains.annotations.Nullable
	private Long bestSplit(ActiveRun run, String id) {
		RunRecord record = manager.stats().records.get(run.category);
		return record == null ? null : record.splits.get(id);
	}

	/** Green when ahead of the record at the latest shared split, red when behind or past the record time. */
	ChatFormatting paceColor(ActiveRun run) {
		RunRecord record = manager.stats().records.get(run.category);
		if (record == null) {
			return ChatFormatting.WHITE;
		}
		if (run.realMillis() > record.realMillis) {
			return ChatFormatting.RED;
		}
		String latest = null;
		for (String id : run.splits.keySet()) {
			if (record.splits.containsKey(id)) {
				latest = id;
			}
		}
		if (latest == null) {
			return ChatFormatting.WHITE;
		}
		return run.splits.get(latest) <= record.splits.get(latest) ? ChatFormatting.GREEN : ChatFormatting.RED;
	}
}
