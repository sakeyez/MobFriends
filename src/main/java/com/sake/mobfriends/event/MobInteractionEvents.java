package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.*;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

@EventBusSubscriber(modid = MobFriends.MOD_ID)
public class MobInteractionEvents {
    @SubscribeEvent
    public static void onPlayerRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();

        if (!player.isShiftKeyDown()) {
            return;
        }
        if (!stack.is(ModTags.Items.THROWABLE_BAOZI)) {
            return;
        }

        // 只在服务器端执行实际的投掷逻辑
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

            Snowball projectile = new Snowball(level, player);
            projectile.setItem(stack.copy());

            Vec3 lookVector = player.getLookAngle();
            projectile.shoot(lookVector.x, lookVector.y, lookVector.z, 1.5F, 1.0F);

            level.addFreshEntity(projectile);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        // 返回成功，让游戏知道我们处理了这次点击
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * 事件二：【核心修复】阻止吃的动画
     * 这是一个安全保障，确保"开始使用物品"（即吃）的动作被彻底取消。
     */
    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.isShiftKeyDown()) {
            ItemStack stack = event.getItem();
            // 【修改】检查物品是否在我们的新标签里
            if (stack.is(ModTags.Items.THROWABLE_BAOZI)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickMob(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack playerStack = player.getItemInHand(hand);

        // 1. 回收僵尸战士
        if (event.getTarget() instanceof CombatZombie zombie && playerStack.is(ModItems.ACTIVE_ZOMBIE_CORE.get())) {
            UUID coreUUID = playerStack.get(ModDataComponents.ZOMBIE_UUID.get());
            if (zombie.getUUID().equals(coreUUID) && !playerStack.has(ModDataComponents.STORED_ZOMBIE_NBT.get())) {
                handleRecall(player, playerStack, zombie, () -> ActiveZombieCore.storeZombie(playerStack, zombie), event);
                return;
            }
        }
        // 2. 【新增】回收凋零战士
        else if (event.getTarget() instanceof CombatWither wither && playerStack.is(ModItems.ACTIVE_WITHER_CORE.get())) {
            UUID coreUUID = playerStack.get(ModDataComponents.WITHER_UUID.get());
            if (wither.getUUID().equals(coreUUID) && !playerStack.has(ModDataComponents.STORED_WITHER_NBT.get())) {
                handleRecall(player, playerStack, wither, () -> ActiveWitherCore.storeWither(playerStack, wither), event);
                return;
            }
        }
        // 3. 【新增】回收苦力怕战士
        else if (event.getTarget() instanceof CombatCreeper creeper && playerStack.is(ModItems.ACTIVE_CREEPER_CORE.get())) {
            UUID coreUUID = playerStack.get(ModDataComponents.CREEPER_UUID.get());
            if (creeper.getUUID().equals(coreUUID) && !playerStack.has(ModDataComponents.STORED_CREEPER_NBT.get())) {
                handleRecall(player, playerStack, creeper, () -> ActiveCreeperCore.storeCreeper(playerStack, creeper), event);
                return;
            }
        }
        // 4. 【新增】回收烈焰人战士
        else if (event.getTarget() instanceof CombatBlaze blaze && playerStack.is(ModItems.ACTIVE_BLAZE_CORE.get())) {
            UUID coreUUID = playerStack.get(ModDataComponents.BLAZE_UUID.get());
            if (blaze.getUUID().equals(coreUUID) && !playerStack.has(ModDataComponents.STORED_BLAZE_NBT.get())) {
                handleRecall(player, playerStack, blaze, () -> ActiveBlazeCore.storeBlaze(playerStack, blaze), event);
                return;
            }
        }

        // --- 原有的给原版生物穿装备的逻辑 ---
        if (event.getLevel().isClientSide() || hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) {
            return;
        }

        if (!(event.getTarget() instanceof LivingEntity target)) {
            return;
        }

        // 排除所有自定义NPC，因为它们有自己的交互逻辑
        if (target instanceof ZombieNpcEntity || target instanceof SkeletonNpcEntity ||
                target instanceof CombatZombie || target instanceof CombatWither ||
                target instanceof CreeperNpcEntity || target instanceof EndermanNpcEntity ||
                target instanceof SlimeNpcEntity || target instanceof BlazeNpcEntity) {
            return;
        }

        boolean isVanillaZombie = target instanceof Zombie;
        boolean isVanillaSkeleton = target instanceof AbstractSkeleton;

        if (isVanillaZombie || isVanillaSkeleton) {
            EquipmentSlot slot = target.getEquipmentSlotForItem(playerStack);

            if (playerStack.is(Items.TOTEM_OF_UNDYING)) {
                slot = EquipmentSlot.OFFHAND;
            }

            // 执行交换
            ItemStack currentItem = target.getItemBySlot(slot);
            target.setItemSlot(slot, playerStack.copy());
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, currentItem);
            }

            // 播放声音
            SoundEvent sound = playerStack.getItem() instanceof ArmorItem armorItem ?
                    armorItem.getEquipSound().value() : SoundEvents.ITEM_PICKUP;
            player.playSound(sound, 1.0F, 1.0F);

            event.setCanceled(true);
        }
    }
    private static void handleRecall(Player player, ItemStack stack, LivingEntity warrior, Runnable storeAction, PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()) {
            storeAction.run(); // 执行具体的存储操作
            warrior.discard(); // 从世界中移除战士
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
}
}