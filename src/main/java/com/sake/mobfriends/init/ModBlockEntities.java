package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.block.WorkstationBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MobFriends.MOD_ID);

    /**
     * 注册我们的通用工作站方块实体。
     * 关键点：BlockEntityType.Builder.of() 的第二个参数是它适用的 *Block*。
     * 我们必须在这里列出所有 6 个方块。
     */
    public static final Supplier<BlockEntityType<WorkstationBlockEntity>> WORKSTATION_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("workstation_block_entity", () ->
                    BlockEntityType.Builder.of(
                            WorkstationBlockEntity::new,
                            // 将所有6个工作方块添加进来
                            ModBlocks.ZOMBIE_BLOCK.get(),
                            ModBlocks.SKELETON_BLOCK.get(),
                            ModBlocks.CREEPER_BLOCK.get(),
                            ModBlocks.ENDERMAN_BLOCK.get(),
                            ModBlocks.SLIME_BLOCK.get(),
                            ModBlocks.BLAZE_BLOCK.get()
                    ).build(null) // build(null) 对于非数据修复的BE是标准做法
            );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}