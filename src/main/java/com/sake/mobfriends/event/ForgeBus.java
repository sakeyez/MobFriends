package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.config.FeedingConfig;
import com.sake.mobfriends.entity.*;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.item.ActiveBlazeCore;
import com.sake.mobfriends.item.ActiveCreeperCore;
import com.sake.mobfriends.trading.TradeManager;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber(modid = com.sake.mobfriends.MobFriends.MOD_ID)
public class ForgeBus {



    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide()) return;

        // --- 【核心修复】 ---
        // 1. 检查吃东西的实体是否是我们的“战士”实体
        if (entity instanceof AbstractWarriorEntity warrior) {
            // 2. 如果是，就调用战士自己的仪式处理方法
            warrior.handleRitualBlockEaten(event.getEatenBlockState());
            // 3. 处理完毕后，直接返回，不再执行后面的掉落谢意逻辑
            return;
        }
        // --- 修复结束 ---

        // 对于其他非战士的生物，保留原有的掉落谢意逻辑
        Supplier<Item> tokenSupplier = null;
        EntityType<?> type = entity.getType();

        if (type.is(ModTags.Entities.ZOMBIES)) {
            tokenSupplier = ModItems.ZOMBIE_TOKEN;
        } else if (type.is(ModTags.Entities.SKELETONS)) {
            tokenSupplier = ModItems.SKELETON_TOKEN;
        }

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

        Set<Block> foodBlocks = FeedingConfig.getFoodBlocks(type);

        if (!foodBlocks.isEmpty()) {
            mob.goalSelector.addGoal(1, new EatBlockFoodGoal(
                    mob,
                    1.0D,
                    6,
                    foodBlocks::contains
            ));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity deadEntity = event.getEntity();
        // --- 僵尸战士死亡逻辑 ---
        if (deadEntity instanceof CombatZombie deadZombie) {
            UUID deadUUID = deadZombie.getUUID();
            for (Player player : deadZombie.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_ZOMBIE_CORE.get())) {
                        UUID coreUUID = stack.get(ModDataComponents.ZOMBIE_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_ZOMBIE_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadZombie.save(data);
                            brokenCore.set(ModDataComponents.STORED_ZOMBIE_NBT.get(), data);
                            brokenCore.set(ModDataComponents.ZOMBIE_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return; // 处理完成，退出
                        }
                    }
                }
            }
        }
        // --- 凋零战士死亡逻辑 ---
        else if (deadEntity instanceof CombatWither deadWither) {
            UUID deadUUID = deadWither.getUUID();
            for (Player player : deadWither.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_WITHER_CORE.get())) {
                        // 【修复】使用正确的 WITHER_UUID
                        UUID coreUUID = stack.get(ModDataComponents.WITHER_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_WITHER_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadWither.save(data);
                            brokenCore.set(ModDataComponents.STORED_WITHER_NBT.get(), data);
                            brokenCore.set(ModDataComponents.WITHER_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
        // --- 【新增】苦力怕战士死亡逻辑 ---
        else if (deadEntity instanceof CombatCreeper deadCreeper) {
            UUID deadUUID = deadCreeper.getUUID();
            for (Player player : deadCreeper.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_CREEPER_CORE.get())) {
                        // 【修复】使用正确的 CREEPER_UUID
                        UUID coreUUID = stack.get(ModDataComponents.CREEPER_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_CREEPER_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadCreeper.save(data);
                            brokenCore.set(ModDataComponents.STORED_CREEPER_NBT.get(), data);
                            brokenCore.set(ModDataComponents.CREEPER_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
        // --- 【新增】烈焰人战士死亡逻辑 ---
        else if (deadEntity instanceof CombatBlaze deadBlaze) {
            UUID deadUUID = deadBlaze.getUUID();
            for (Player player : deadBlaze.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_BLAZE_CORE.get())) {
                        // 【修复】使用正确的 BLAZE_UUID
                        UUID coreUUID = stack.get(ModDataComponents.BLAZE_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_BLAZE_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadBlaze.save(data);
                            brokenCore.set(ModDataComponents.STORED_BLAZE_NBT.get(), data);
                            brokenCore.set(ModDataComponents.BLAZE_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
    }
}