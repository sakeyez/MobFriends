package com.sake.mobfriends.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class EndermanNpcEntity extends EnderMan {

    public EndermanNpcEntity(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return EnderMan.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    // 移除AI
    @Override
    protected void registerGoals() {
        // AI 逻辑将在第三阶段添加
    }
}