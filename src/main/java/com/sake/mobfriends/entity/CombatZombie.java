package com.sake.mobfriends.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob; // <-- 添加 Mob 导入
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CombatZombie extends TamableAnimal {

    public CombatZombie(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        // --- 【核心修正 #1】 ---
        // 使用 Mob.createMobAttributes() 作为基础，而不是 TamableAnimal
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, false));
        // --- 【核心修正 #2】 ---
        // 移除最后一个布尔值参数
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public boolean canAttack(@NotNull LivingEntity target) {
        if (target instanceof Player) {
            return false;
        }
        if (this.isOwnedBy(target)) {
            return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    public boolean isFood(@NotNull ItemStack pStack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) {
        return null;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        if (pPlayer.isShiftKeyDown()) {
            ItemStack playerStack = pPlayer.getItemInHand(pHand);
            if (!playerStack.isEmpty()) {
                EquipmentSlot slot = this.getEquipmentSlotForItem(playerStack);
                if (playerStack.is(Items.TOTEM_OF_UNDYING)) {
                    slot = EquipmentSlot.OFFHAND;
                }
                if (slot.getType() != EquipmentSlot.Type.HAND || playerStack.getItem() instanceof ArmorItem) {
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
            }
        }

        if (this.isOwnedBy(pPlayer)) {
            if (!this.level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(pPlayer, pHand);
    }
}