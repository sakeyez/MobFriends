package com.sake.mobfriends.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CombatWither extends WitherSkeleton {

    public CombatWither(EntityType<? extends WitherSkeleton> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return WitherSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
    }

    // --- 【最终修正】装备交互逻辑 ---
    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        if (pPlayer.isShiftKeyDown()) {
            ItemStack playerStack = pPlayer.getItemInHand(pHand);

            // 使用 LivingEntity 的实例方法来获取槽位
            EquipmentSlot slot = this.getEquipmentSlotForItem(playerStack);

            if (playerStack.is(Items.TOTEM_OF_UNDYING)) {
                slot = EquipmentSlot.OFFHAND;
            }

            if (!this.level().isClientSide) {
                ItemStack currentItem = this.getItemBySlot(slot);
                this.setItemSlot(slot, playerStack.copy());
                if (!pPlayer.getAbilities().instabuild) {
                    pPlayer.setItemInHand(pHand, currentItem);
                }

                SoundEvent sound = playerStack.getItem() instanceof ArmorItem armorItem ?
                        armorItem.getEquipSound().value() : SoundEvents.ITEM_PICKUP;
                this.playSound(sound, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(pPlayer, pHand);
    }
}