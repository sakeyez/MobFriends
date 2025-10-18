package com.sake.mobfriends.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

// NeoForge 1.21.1 移植要点:
// 1. 继承 LivingEvent: 自定义实体事件通常建议继承自一个相关的现有事件。
//    LivingEvent 是一个很好的基类，因为它已经包含了获取实体的方法。
// 2. 提供 Getter: 为事件携带的数据（这里是吃掉的方块状态）提供公开的 getter 方法，
//    这样事件的监听者就能方便地获取这些信息。
// 3. 构造函数: 构造函数接收所有必要的数据，并将其存储在私有字段中。
public class MobFinishedEatingEvent extends LivingEvent {

    private final BlockState eatenBlockState;

    public MobFinishedEatingEvent(LivingEntity entity, BlockState eatenBlockState) {
        super(entity);
        this.eatenBlockState = eatenBlockState;
    }

    /**
     * 获取被吃掉的方块的 BlockState。
     * @return The BlockState of the block that was eaten.
     */
    public BlockState getEatenBlockState() {
        return eatenBlockState;
    }
}