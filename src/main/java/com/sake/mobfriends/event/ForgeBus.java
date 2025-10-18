package com.sake.mobfriends.event;

import com.sake.mobfriends.config.FeedingConfig;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Set;
import java.util.function.Supplier;

@EventBusSubscriber(modid = com.sake.mobfriends.MobFriends.MOD_ID)
public class ForgeBus {

    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide) return;

        Supplier<Item> tokenSupplier = null;
        EntityType<?> type = entity.getType();

        if (type.is(ModTags.Entities.ZOMBIES) || type == ModEntities.ZOMBIE_NPC.get()) {
            tokenSupplier = ModItems.ZOMBIE_TOKEN;
        } else if (type.is(ModTags.Entities.SKELETONS)) {
            tokenSupplier = ModItems.SKELETON_TOKEN;
        } // ... 其他家族判断

        if (tokenSupplier != null && FeedingConfig.getFoodBlocks(type).contains(event.getEatenBlockState().getBlock())) {
            ItemEntity itemEntity = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(tokenSupplier.get()));
            level.addFreshEntity(itemEntity);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }
        EntityType<?> type = mob.getType();

        // 检查这个生物是否有对应的食谱
        Set<Block> foodBlocks = FeedingConfig.getFoodBlocks(type);
        if (!foodBlocks.isEmpty()) {
            mob.goalSelector.addGoal(1, new EatBlockFoodGoal(
                    mob,
                    1.0D,
                    16,
                    foodBlocks::contains
            ));
        }
    }
}