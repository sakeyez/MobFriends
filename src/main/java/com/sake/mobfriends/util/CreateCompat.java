package com.sake.mobfriends.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item; // 【新增】
import net.minecraft.world.item.Items; // 【新增】
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateCompat {

    // --- 方块 ---
    public static final Block ANDESITE_CASING = getBlock("create:andesite_casing");
    public static final Block BRASS_CASING = getBlock("create:brass_casing");
    public static final Block COPPER_CASING = getBlock("create:copper_casing");
    public static final Block RAILWAY_CASING = getBlock("create:railway_casing");

    // 【【【新增：物品 (材料)】】】
    public static final Item ANDESITE_ALLOY = getItem("create:andesite_alloy");
    public static final Item BRASS_INGOT = getItem("create:brass_ingot");
    public static final Item COPPER_INGOT = getItem("minecraft:copper_ingot"); // 机械动力使用原版铜锭
    // public static final Block GOLD_BLOCK = getBlock("minecraft:gold_block"); // 放弃精密构件，不再需要

    // 我们需要的所有机壳 (用于“去皮”任务)
    public static final Set<Block> ALL_CASINGS = ImmutableSet.of(
            ANDESITE_CASING,
            BRASS_CASING,
            COPPER_CASING,
            RAILWAY_CASING
    ).stream().filter(b -> b != Blocks.AIR).collect(Collectors.toSet());

    // --- 转换表 ---

    // 1. 木头 -> 去皮木头
    public static final Map<Block, Block> STRIPPING_MAP = new ImmutableMap.Builder<Block, Block>()
            .put(Blocks.OAK_LOG, Blocks.STRIPPED_OAK_LOG)
            .put(Blocks.SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG)
            .put(Blocks.BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG)
            .put(Blocks.JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG)
            .put(Blocks.ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG)
            .put(Blocks.DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG)
            .put(Blocks.MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG)
            .put(Blocks.CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG)
            .put(Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_WOOD)
            .put(Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD)
            .put(Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD)
            .put(Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD)
            .put(Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD)
            .put(Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD)
            .put(Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD)
            .put(Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD)
            .build();


    public static final Set<Item> PLACEABLE_LOGS = STRIPPING_MAP.keySet().stream()
            .map(Block::asItem)
            .filter(item -> item != Items.AIR)
            .collect(Collectors.toSet());

    public static final Set<Block> STRIPPED_LOGS = STRIPPING_MAP.values().stream().collect(Collectors.toSet());

    // 2. 去皮木头 + 机壳 -> 新机壳
    public static final Map<Block, Block> CASING_CRAFTING_MAP = new ImmutableMap.Builder<Block, Block>()
            .put(ANDESITE_CASING, ANDESITE_CASING)
            .put(BRASS_CASING, BRASS_CASING)
            .put(COPPER_CASING, COPPER_CASING)
            .put(RAILWAY_CASING, RAILWAY_CASING)
            .build();

    // 【【【新增：机壳 -> 成本 映射】】】
    public static final Map<Block, Item> CASING_COST_MAP = new ImmutableMap.Builder<Block, Item>()
            .put(ANDESITE_CASING, ANDESITE_ALLOY)
            .put(BRASS_CASING, BRASS_INGOT)
            .put(COPPER_CASING, COPPER_INGOT)
            .put(RAILWAY_CASING, ANDESITE_ALLOY) // 假设列车机壳也用安山合金
            .build();


    // 辅助方法，安全地获取方块
    private static Block getBlock(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return (block == null || block == Blocks.AIR) ? Blocks.AIR : block;
    }

    // 【【【新增：辅助方法】】】
    private static Item getItem(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return (item == null || item == Items.AIR) ? Items.AIR : item;
    }
}