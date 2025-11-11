package com.sake.mobfriends.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
// 【【【导入：机械动力的类】】】
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 【【【修复版 V2】】】
 * 这个类现在 *必须* 依赖 Create 的 API。
 * 我们的 AI (BlazeRefuelGoal) 会在调用它之前检查 Create 是否已加载。
 */
public class CreateCompat {

    // --- 方块 ---
    public static final Block ANDESITE_CASING = getBlock("create:andesite_casing");
    public static final Block BRASS_CASING = getBlock("create:brass_casing");
    public static final Block COPPER_CASING = getBlock("create:copper_casing");
    public static final Block RAILWAY_CASING = getBlock("create:railway_casing");
    public static final Block BLAZE_BURNER = getBlock("create:blaze_burner");


    // --- 物品 (材料) ---
    public static final Item ANDESITE_ALLOY = getItem("create:andesite_alloy");
    public static final Item BRASS_INGOT = getItem("create:brass_ingot");
    public static final Item COPPER_INGOT = getItem("minecraft:copper_ingot");

    // (苦力怕工程师的相关映射表)
    public static final Set<Block> ALL_CASINGS = ImmutableSet.of(
            ANDESITE_CASING,
            BRASS_CASING,
            COPPER_CASING,
            RAILWAY_CASING
    ).stream().filter(b -> b != Blocks.AIR).collect(Collectors.toSet());

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

    public static final Map<Block, Block> CASING_CRAFTING_MAP = new ImmutableMap.Builder<Block, Block>()
            .put(ANDESITE_CASING, ANDESITE_CASING)
            .put(BRASS_CASING, BRASS_CASING)
            .put(COPPER_CASING, COPPER_CASING)
            .put(RAILWAY_CASING, RAILWAY_CASING)
            .build();

    public static final Map<Block, Item> CASING_COST_MAP = new ImmutableMap.Builder<Block, Item>()
            .put(ANDESITE_CASING, ANDESITE_ALLOY)
            .put(BRASS_CASING, BRASS_INGOT)
            .put(COPPER_CASING, COPPER_INGOT)
            .put(RAILWAY_CASING, ANDESITE_ALLOY)
            .build();

    // --- 辅助方法 (安全获取) ---
    private static Block getBlock(String id) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return (block == null || block == Blocks.AIR) ? Blocks.AIR : block;
    }
    private static Item getItem(String id) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        return (item == null || item == Items.AIR) ? Items.AIR : item;
    }

    // --- 【【【唯一需要的方法】】】 ---

    /**
     * 检查一个燃烧室是否需要燃料。
     * 我们只为"熄灭"(NONE)或"点燃"(KINDLED)状态的燃烧室添加燃料。
     * * 【重要】调用此方法前，必须先检查 ModList.get().isLoaded("create")
     */
    public static boolean isBurnerInactive(BlockState state) {
        if (!state.is(BLAZE_BURNER)) {
            return false;
        }
        try {
            BlazeBurnerBlock.HeatLevel heat = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);

            // 【【【修改：添加 FADING】】】
            return heat == BlazeBurnerBlock.HeatLevel.SMOULDERING ||
                    heat == BlazeBurnerBlock.HeatLevel.KINDLED ||
                    heat == BlazeBurnerBlock.HeatLevel.FADING;

        } catch (Exception e) {
            return false;
        }
    }

    // (旧的、错误的 refuelBlazeBurner 方法已被完全删除)
}