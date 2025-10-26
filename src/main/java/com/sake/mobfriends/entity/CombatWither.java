package com.sake.mobfriends.entity;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class CombatWither extends TamableAnimal {

    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(CombatWither.class, EntityDataSerializers.INT);
    private Set<ResourceLocation> eatenFoods = Sets.newHashSet();
    private boolean needsStatUpdate = true;

    private static final Set<ResourceLocation> UNFEEDABLE_FOODS = Sets.newHashSet();

    public CombatWither(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    // --- 【最终正确修复】添加缺失的 isFood 方法 ---
    @Override
    public boolean isFood(@NotNull ItemStack pStack) {
        // 返回 false，因为凋零战士不通过这种方式进食
        return false;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (!this.isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }
        if (!player.isShiftKeyDown()) {
            if (heldItem.has(DataComponents.FOOD)) {
                player.sendSystemMessage(Component.literal("凋零战士暂时不想吃东西..."));
                return InteractionResult.FAIL;
            }
        } else {
            EquipmentSlot slot = this.getEquipmentSlotForItem(heldItem);
            if (heldItem.isEmpty()) {
                slot = null;
            }
            if (slot != null) {
                if (!this.level().isClientSide) {
                    ItemStack currentItem = this.getItemBySlot(slot);
                    this.setItemSlot(slot, heldItem.copy());
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, currentItem);
                    }
                    SoundEvent sound = heldItem.getItem() instanceof ArmorItem armorItem ?
                            armorItem.getEquipSound().value() : SoundEvents.ITEM_PICKUP;
                    this.playSound(sound, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            } else {
                if (!this.level().isClientSide) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }
        return super.mobInteract(player, hand);
    }

    public void applyStats(boolean isLevelReset) {
        if (this.level().isClientSide()) return;
        int level = this.getLevel();
        Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(60.0 + (level - 1) * 20.0);
        Objects.requireNonNull(this.getAttribute(Attributes.ATTACK_DAMAGE)).setBaseValue(8.0 + (level - 1) * 2.0);
        if (isLevelReset) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LEVEL, 1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CombatWitherLevel", this.getLevel());
        ListTag listtag = new ListTag();
        for (ResourceLocation resourcelocation : this.eatenFoods) {
            listtag.add(StringTag.valueOf(resourcelocation.toString()));
        }
        compound.put("EatenFoodsWither", listtag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CombatWitherLevel")) {
            this.setLevel(compound.getInt("CombatWitherLevel"));
        } else {
            this.setLevel(1);
        }
        this.eatenFoods.clear();
        if (compound.contains("EatenFoodsWither", 9)) {
            ListTag listtag = compound.getList("EatenFoodsWither", 8);
            for (int i = 0; i < listtag.size(); ++i) {
                ResourceLocation resourcelocation = ResourceLocation.tryParse(listtag.getString(i));
                if (resourcelocation != null) {
                    this.eatenFoods.add(resourcelocation);
                }
            }
        }
        this.needsStatUpdate = true;
    }

    public int getLevel() { return this.entityData.get(DATA_LEVEL); }
    public void setLevel(int level) { this.entityData.set(DATA_LEVEL, level); applyStats(true); this.refreshDimensions(); }
    public boolean isRitualPending() { return false; }
    @Override public float getScale() { return 1.2f; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) { return null; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F); }
}