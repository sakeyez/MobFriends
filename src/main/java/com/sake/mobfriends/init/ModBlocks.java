package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.block.WorkstationBlock; // <-- 【新增导入】
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, MobFriends.MOD_ID);

    private static final Supplier<BlockBehaviour.Properties> WORKSTATION_PROPERTIES =
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops();

    // --- 【核心修改】 ---
    // 全部替换为 new WorkstationBlock(...)，并传入正确的参数

    public static final Supplier<Block> ZOMBIE_BLOCK = BLOCKS.register("zombie_block",
            () -> new WorkstationBlock(WORKSTATION_PROPERTIES.get(),
                    ModEntities.ZOMBIE_NPC, // 召唤的NPC
                    ModItems.ZOMBIE_TOKEN   // 升级用的谢意
            ));

    public static final Supplier<Block> SKELETON_BLOCK = BLOCKS.register("skeleton_block",
            () -> new WorkstationBlock(WORKSTATION_PROPERTIES.get(),
                    ModEntities.SKELETON_NPC,
                    ModItems.SKELETON_TOKEN
            ));

    public static final Supplier<Block> CREEPER_BLOCK = BLOCKS.register("creeper_block",
            () -> new WorkstationBlock(WORKSTATION_PROPERTIES.get(),
                    ModEntities.CREEPER_NPC,
                    ModItems.CREEPER_TOKEN
            ));

    public static final Supplier<Block> ENDERMAN_BLOCK = BLOCKS.register("enderman_block",
            () -> new WorkstationBlock(WORKSTATION_PROPERTIES.get(),
                    ModEntities.ENDERMAN_NPC,
                    ModItems.ENDERMAN_TOKEN
            ));

    public static final Supplier<Block> SLIME_BLOCK = BLOCKS.register("slime_block",
            () -> new WorkstationBlock(WORKSTATION_PROPERTIES.get().sound(SoundType.SLIME_BLOCK),
                    ModEntities.SLIME_NPC,
                    ModItems.SLIME_TOKEN
            ));

    public static final Supplier<Block> BLAZE_BLOCK = BLOCKS.register("blaze_block",
            () -> new WorkstationBlock(WORKSTATION_PROPERTIES.get().lightLevel(s -> 8),
                    ModEntities.BLAZE_NPC,
                    ModItems.BLAZE_TOKEN
            ));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}