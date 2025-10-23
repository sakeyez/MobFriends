package com.sake.mobfriends.util;

import com.sake.mobfriends.entity.SkeletonNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FishConversionHelper {

    private static final Map<Item, EntityType<?>> FISH_MAP = new HashMap<>();

    /**
     * 初始化鱼物品到实体的映射。
     * 这里包含了原版鱼和一些 Aquaculture 模组的鱼作为示例。
     */
    public static void initialize() {
        // 原版鱼
        FISH_MAP.put(Items.COD, EntityType.COD);
        FISH_MAP.put(Items.SALMON, EntityType.SALMON);
        FISH_MAP.put(Items.TROPICAL_FISH, EntityType.TROPICAL_FISH);
        FISH_MAP.put(Items.PUFFERFISH, EntityType.PUFFERFISH);

        // 添加 Aquaculture 模组的鱼（如果该模组存在）
        findAndAddFish("aquaculture:arapaima");
        findAndAddFish("aquaculture:arrau_turtle");
        findAndAddFish("aquaculture:atlantic_cod");
        findAndAddFish("aquaculture:atlantic_halibut");
        findAndAddFish("aquaculture:atlantic_herring");
        findAndAddFish("aquaculture:bayad");
        findAndAddFish("aquaculture:blackfish");
        findAndAddFish("aquaculture:bluegill");
        findAndAddFish("aquaculture:boulti");
        findAndAddFish("aquaculture:box_turtle");
        findAndAddFish("aquaculture:brown_shrooma");
        findAndAddFish("aquaculture:brown_trout");
        findAndAddFish("aquaculture:capitaine");
        findAndAddFish("aquaculture:carp");
        findAndAddFish("aquaculture:catfish");
        findAndAddFish("aquaculture:gar");
        findAndAddFish("aquaculture:jellyfish");
        findAndAddFish("aquaculture:minnow");
        findAndAddFish("aquaculture:muskellunge");
        findAndAddFish("aquaculture:pacific_halibut");
        findAndAddFish("aquaculture:perch");
        findAndAddFish("aquaculture:pink_salmon");
        findAndAddFish("aquaculture:piranha");
        findAndAddFish("aquaculture:pollock");
        findAndAddFish("aquaculture:rainbow_trout");
        findAndAddFish("aquaculture:red_grouper");
        findAndAddFish("aquaculture:red_shrooma");
        findAndAddFish("aquaculture:smallmouth_bass");
        findAndAddFish("aquaculture:starshell_turtle");
        findAndAddFish("aquaculture:synodontis");
        findAndAddFish("aquaculture:tambaqui");
        findAndAddFish("aquaculture:tuna");
        // 你可以按照这个格式添加更多来自其他模组的鱼
    }

    /**
     * 一个辅助方法，通过ID安全地查找并添加鱼的映射。
     * @param id 物品和实体的ID (例如 "minecraft:cod")
     */
    private static void findAndAddFish(String id) {
        ResourceLocation location = ResourceLocation.parse(id);
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(location);
        // 假设物品ID和实体ID是相同的
        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(location);
        if (item.isPresent() && entityType.isPresent()) {
            FISH_MAP.put(item.get(), entityType.get());
        }
    }

    /**
     * 处理战利品。尝试将其转换为鱼实体，如果失败则作为物品实体生成。
     * @param level 当前世界
     * @param stack 要处理的物品堆
     * @param waterPos 鱼被钓起的位置
     * @param skeleton 钓鱼的骷髅
     */
    public static void processLoot(ServerLevel level, ItemStack stack, BlockPos waterPos, SkeletonNpcEntity skeleton) {
        EntityType<?> fishType = FISH_MAP.get(stack.getItem());

        if (fishType != null) {
            Entity fishEntity = fishType.create(level);
            if (fishEntity != null) {
                // 对于热带鱼等需要随机NBT的生物，需要调用此方法来生成随机样式
                if (fishEntity instanceof Mob fishMob) {
                    fishMob.finalizeSpawn(level, level.getCurrentDifficultyAt(waterPos), MobSpawnType.NATURAL, null);
                }

                // 使用参考模组的逻辑，让鱼飞起来
                fishUpEntity(fishEntity, waterPos, skeleton);
                return; // 成功生成为实体，任务结束
            }
        }

        // 如果不是可转换的鱼，或者实体创建失败，则作为普通物品生成
        spawnItemEntity(level, stack, waterPos, skeleton);
    }

    /**
     * 生成被钓起的实体，并赋予其飞向骷髅的动力。
     * 核心逻辑参考自 FishingReal 模组。
     */
    private static void fishUpEntity(Entity entity, BlockPos waterPos, SkeletonNpcEntity skeleton) {
        Vec3 waterVec = Vec3.atCenterOf(waterPos);
        double dX = skeleton.getX() - waterVec.x();
        double dY = skeleton.getY() - waterVec.y();
        double dZ = skeleton.getZ() - waterVec.z();
        double strength = 0.12;
        double verticalStrength = 0.18; // 调整这个值可以让鱼飞得更高或更低

        entity.setPos(waterVec.x(), waterVec.y(), waterVec.z());
        entity.setDeltaMovement(dX * strength, dY * strength + Math.sqrt(Math.sqrt(dX * dX + dY * dY + dZ * dZ)) * verticalStrength, dZ * strength);
        skeleton.level().addFreshEntity(entity);
    }

    /**
     * 将战利品作为普通的物品实体生成。
     */
    private static void spawnItemEntity(ServerLevel level, ItemStack stack, BlockPos waterPos, SkeletonNpcEntity skeleton) {
        ItemEntity itemEntity = new ItemEntity(level,
                waterPos.getX() + 0.5, waterPos.getY() + 0.5, waterPos.getZ() + 0.5,
                stack);

        Vec3 toSkeleton = skeleton.position().subtract(itemEntity.position());
        itemEntity.setDeltaMovement(toSkeleton.normalize().scale(0.3).add(0, 0.3, 0));

        level.addFreshEntity(itemEntity);
    }
}