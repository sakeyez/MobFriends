package com.sake.mobfriends.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SkeletonNpcEntity extends Skeleton {

    public SkeletonNpcEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    // 我们将移除AI，让它暂时不动，等待第三阶段的移植
    @Override
    protected void registerGoals() {
        // AI 逻辑将在第三阶段添加
    }
}