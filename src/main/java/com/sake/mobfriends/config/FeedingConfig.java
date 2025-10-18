package com.sake.mobfriends.config;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FeedingConfig {

    public static Set<Block> getFoodBlocks(EntityType<?> entityType) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (entityId == null) {
            return Collections.emptySet();
        }

        TagKey<Block> foodTag = TagKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(entityId.getNamespace(), "feeds/" + entityId.getPath()));

        Iterable<Holder<Block>> holders = BuiltInRegistries.BLOCK.getTagOrEmpty(foodTag);

        return StreamSupport.stream(holders.spliterator(), false)
                .map(Holder::value)
                .collect(Collectors.toSet());
    }
}