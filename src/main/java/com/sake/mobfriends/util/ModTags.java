package com.sake.mobfriends.util;

import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModTags {
    public static class Entities {
        public static final TagKey<EntityType<?>> ZOMBIES =
                createTag("zombies");

        // --- 新增代码 ---
        public static final TagKey<EntityType<?>> SKELETONS =
                createTag("skeletons");
        public static final TagKey<EntityType<?>> SLIMES =
                createTag("slimes");
        public static final TagKey<EntityType<?>> BLAZES =
                createTag("blazes");
        // --- 新增代码结束 ---

        // 辅助方法，让代码更简洁
        private static TagKey<EntityType<?>> createTag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, name));
        }
    }
}