package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.*;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.item.*;
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

@EventBusSubscriber(modid = MobFriends.MOD_ID)
public class MobInteractionEvents {

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