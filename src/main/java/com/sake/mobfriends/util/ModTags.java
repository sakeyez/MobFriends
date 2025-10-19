package com.sake.mobfriends.util;

import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class ModTags {
    public static class Entities {
        public static final TagKey<EntityType<?>> ZOMBIES = tag("zombies");
        public static final TagKey<EntityType<?>> SKELETONS = tag("skeletons");
        public static final TagKey<EntityType<?>> SLIMES = tag("slimes");
        public static final TagKey<EntityType<?>> BLAZES = tag("blazes");
        public static final TagKey<EntityType<?>> CREEPERS = tag("creepers");
        public static final TagKey<EntityType<?>> ENDERMEN = tag("endermen");

        private static TagKey<EntityType<?>> tag(String name) {
            // 核心修复: 使用 ResourceLocation.fromNamespaceAndPath
            return TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, name));
        }
    }
}