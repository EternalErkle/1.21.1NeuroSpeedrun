package com.eternalerkle.speedrun.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** relentless and stalker change target goals; splitters copies get an empty loot table. */
@Mixin(Mob.class)
public interface ModifierMobsBMobAccessor {
	@Accessor("targetSelector")
	GoalSelector speedrun$targetSelector();

	@Accessor("lootTable")
	void speedrun$setLootTable(ResourceKey<LootTable> lootTable);
}
