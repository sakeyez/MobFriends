package com.sake.mobfriends.client.gui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class CombatCoreTooltip implements TooltipComponent {
    private final CompoundTag entityTag;
    private final int combatLevel;
    private final int combatHappiness;
    private final int combatLevelCap; // 【新增】
    private final int diningFoodsCount; // 【新增】
    private final int ritualFoodsCount; // 【新增】

    public CombatCoreTooltip(CompoundTag entityTag, int combatLevel, int combatHappiness, int combatLevelCap, int diningFoodsCount, int ritualFoodsCount) {
        this.entityTag = entityTag;
        this.combatLevel = combatLevel;
        this.combatHappiness = combatHappiness;
        this.combatLevelCap = combatLevelCap;
        this.diningFoodsCount = diningFoodsCount;
        this.ritualFoodsCount = ritualFoodsCount;
    }

    public CompoundTag getEntityTag() {
        return entityTag;
    }

    public int getCombatLevel() {
        return combatLevel;
    }

    public int getCombatHappiness() {
        return combatHappiness;
    }

    // 【新增】
    public int getCombatLevelCap() {
        return combatLevelCap;
    }

    // 【新增】
    public int getDiningFoodsCount() {
        return diningFoodsCount;
    }

    // 【新增】
    public int getRitualFoodsCount() {
        return ritualFoodsCount;
    }
}