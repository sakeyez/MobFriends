package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.*;
import com.sake.mobfriends.item.AbstractCoreItem;
import com.sake.mobfriends.item.ActiveZombieCore;
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

        // --- 回收僵尸战士的逻辑 ---
        if (event.getTarget() instanceof CombatZombie zombie && playerStack.getItem() instanceof ActiveZombieCore) {
            UUID coreUUID = AbstractCoreItem.getZombieUUID(playerStack);
            if (zombie.getUUID().equals(coreUUID)) {
                // 只有当核心是空的（未储存实体）时，才允许回收
                if (!playerStack.has(com.sake.mobfriends.init.ModDataComponents.STORED_ZOMBIE_NBT.get())) {
                    if (!event.getLevel().isClientSide()) {
                        ActiveZombieCore.storeZombie(playerStack, zombie);
                        zombie.discard(); // 从世界中安全地移除僵尸
                        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.5f, 1.2f);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return; // 逻辑结束，避免执行后续的原版交互
                }
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
}