package com.sake.mobfriends.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

// NeoForge 1.21.1 移植要点:
// 1. 继承: 实体类通常继承自一个原版实体，这里我们继承 Zombie。
// 2. 构造函数: 构造函数签名变为 (EntityType<? extends YourEntity> type, Level level)。
//    必须调用 super(type, level)。
// 3. 属性创建: 创建一个静态的 createAttributes() 方法，返回 AttributeSupplier.Builder。
//    在这里定义实体的基础属性，这个方法会在 ModEventBusEvents 中被调用。
public class CombatZombie extends Zombie {

    public CombatZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    // 定义实体属性
    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    // 我们暂时将 AI 逻辑 (registerGoals) 留空，在第三阶段再进行移植。
    // NeoForge 会自动继承父类 (Zombie) 的 AI，所以它现在会像一个普通的僵尸一样行动。
    @Override
    protected void registerGoals() {
        super.registerGoals();
        // 清除默认AI，如果需要的话
        // this.goalSelector.clear();
        // this.targetSelector.clear();

        // 在下一阶段添加自定义 AI
    }
}