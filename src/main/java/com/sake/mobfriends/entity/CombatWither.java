package com.sake.mobfriends.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CombatWither extends WitherSkeleton {

    public CombatWither(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return WitherSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // AI 逻辑将在第三阶段添加
    }
}