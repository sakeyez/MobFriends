package com.sake.mobfriends.client.gui;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class ZombieCoreTooltip implements TooltipComponent {
    private final CompoundTag entityTag;

    public ZombieCoreTooltip(CompoundTag entityTag) {
        this.entityTag = entityTag;
    }

    public CompoundTag getEntityTag() {
        return entityTag;
    }
}