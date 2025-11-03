package com.sake.mobfriends.util;

import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {

    /**
     * 包含所有与实体类型相关的标签。
     * 用于定义哪些生物属于哪个派系。
     */
    public static class Entities {
        public static final TagKey<EntityType<?>> ZOMBIES = tag("zombies");
        public static final TagKey<EntityType<?>> SKELETONS = tag("skeletons");
        public static final TagKey<EntityType<?>> SLIMES = tag("slimes");
        public static final TagKey<EntityType<?>> BLAZES = tag("blazes");
        public static final TagKey<EntityType<?>> CREEPERS = tag("creepers");
        public static final TagKey<EntityType<?>> ENDERMEN = tag("endermen");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, name));
        }
    }

    /**
     * 包含所有与物品相关的标签。
     * 主要用于战士的食物系统。
     */
    public static class Items {
        // 定义哪些食物是“大餐”，首次食用有额外幸福感加成
        public static final TagKey<Item> DINING_FOODS = tag("dining_food");

        // --- 僵尸战士突破食物 ---
        public static final TagKey<Item> ZOMBIE_TIER_1_RITUAL_FOOD = tag("zombie_tier_1_ritual_food");
        public static final TagKey<Item> ZOMBIE_TIER_2_RITUAL_FOOD = tag("zombie_tier_2_ritual_food");

        // --- 凋零战士突破食物 ---
        public static final TagKey<Item> WITHER_TIER_1_RITUAL_FOOD = tag("wither_tier_1_ritual_food");
        public static final TagKey<Item> WITHER_TIER_2_RITUAL_FOOD = tag("wither_tier_2_ritual_food");

        // --- 苦力怕战士突破食物 ---
        public static final TagKey<Item> CREEPER_TIER_1_RITUAL_FOOD = tag("creeper_tier_1_ritual_food");
        public static final TagKey<Item> CREEPER_TIER_2_RITUAL_FOOD = tag("creeper_tier_2_ritual_food");

        // --- 烈焰人战士突破食物 ---
        public static final TagKey<Item> BLAZE_TIER_1_RITUAL_FOOD = tag("blaze_tier_1_ritual_food");
        public static final TagKey<Item> BLAZE_TIER_2_RITUAL_FOOD = tag("blaze_tier_2_ritual_food");

        public static final TagKey<Item> THROWABLE_BAOZI = tag("throwable_baozi");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, name));
        }
    }
}