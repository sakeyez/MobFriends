package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CombatWither extends AbstractWarriorEntity {

    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "fondant_spider_eye"),
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "pan_seared_knight_steak")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    private static final Set<Block> TIER_2_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "frost_lamb_chop")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());
    private static final EntityDataAccessor<Boolean> DATA_HAS_SLOW_SKILL = SynchedEntityData.defineId(CombatWither.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_POISON_SKILL = SynchedEntityData.defineId(CombatWither.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_LIFESTEAL_SKILL = SynchedEntityData.defineId(CombatWither.class, EntityDataSerializers.BOOLEAN);

    public CombatWither(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_SLOW_SKILL, false);
        builder.define(DATA_HAS_POISON_SKILL, false);
        builder.define(DATA_HAS_LIFESTEAL_SKILL, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasSlowSkill", this.hasSlowSkill());
        compound.putBoolean("HasPoisonSkill", this.hasPoisonSkill());
        compound.putBoolean("HasLifestealSkill", this.hasLifestealSkill());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_HAS_SLOW_SKILL, compound.getBoolean("HasSlowSkill"));
        this.entityData.set(DATA_HAS_POISON_SKILL, compound.getBoolean("HasPoisonSkill"));
        this.entityData.set(DATA_HAS_LIFESTEAL_SKILL, compound.getBoolean("HasLifestealSkill"));
    }




    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (!super.doHurtTarget(pEntity)) {
            return false;
        }

        if (pEntity instanceof LivingEntity target) {
            // 【修改】定义持续时间为 8 秒 (8 * 20 = 160 ticks)
            final int DURATION_IN_TICKS = 160;

            // 基础技能：凋零
            MobEffectInstance witherEffect = target.getEffect(MobEffects.WITHER);
            int witherAmplifier = (witherEffect == null) ? 0 : Math.min(2, witherEffect.getAmplifier() + 1);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, DURATION_IN_TICKS, witherAmplifier));

            // 技能：减速
            if (this.hasSlowSkill()) {
                MobEffectInstance slowEffect = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                int slowAmplifier = (slowEffect == null) ? 0 : Math.min(1, slowEffect.getAmplifier() + 1);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_IN_TICKS, slowAmplifier));
            }

            // 技能：中毒
            if (this.hasPoisonSkill()) {
                MobEffectInstance poisonEffect = target.getEffect(MobEffects.POISON);
                int poisonAmplifier = (poisonEffect == null) ? 0 : Math.min(1, poisonEffect.getAmplifier() + 1);
                target.addEffect(new MobEffectInstance(MobEffects.POISON, DURATION_IN_TICKS, poisonAmplifier));
            }
        }
        return true;
    }
    @Override
    protected void onRitualFoodEaten(ResourceLocation foodId) {
        String path = foodId.getPath();
        if (path.equals("frost_lamb_chop")) {
            this.entityData.set(DATA_HAS_SLOW_SKILL, true);
        } else if (path.equals("fondant_spider_eye")) {
            this.entityData.set(DATA_HAS_POISON_SKILL, true);
        } else if (path.equals("pan_seared_knight_steak")) {
            this.entityData.set(DATA_HAS_LIFESTEAL_SKILL, true);
        }
    }

    public boolean hasSlowSkill() {
        return this.entityData.get(DATA_HAS_SLOW_SKILL);
    }
    public boolean hasPoisonSkill() {
        return this.entityData.get(DATA_HAS_POISON_SKILL);
    }
    public boolean hasLifestealSkill() {
        return this.entityData.get(DATA_HAS_LIFESTEAL_SKILL);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        Set<Block> ritualFoodBlocks = new HashSet<>();
        ritualFoodBlocks.addAll(getTier1RitualBlocks());
        ritualFoodBlocks.addAll(getTier2RitualBlocks());
        if (!ritualFoodBlocks.isEmpty()) {
            this.eatBlockFoodGoal = new EatBlockFoodGoal(this, 1.2D, 16, ritualFoodBlocks::contains);
            this.goalSelector.addGoal(1, this.eatBlockFoodGoal);
        }

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

    @Override protected int getInitialLevelCap() { return 15; }
    @Override protected int getLevelCapIncrease() { return 5; }

    @Override protected double getInitialHealth() { return 20.0; }
    @Override protected double getHealthPerLevel() { return 2.0; }

    @Override protected double getInitialAttack() { return 1.0; }
    @Override protected double getAttackPerLevel() { return 0.0; }

    @Override protected double getInitialAttackMultiplier() { return 1.0; }
    @Override protected double getAttackMultiplierPerLevel() { return 0.0; }

    @Override protected double getInitialSpeed() { return 0.23; }
    @Override protected double getSpeedPerLevel() { return 0.005; }

    @Override protected float getDamageReductionPerLevel() { return 0.0066f; }
    @Override
    protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override
    protected Set<Block> getTier2RitualBlocks() { return TIER_2_BLOCKS; }


    @Override
    public boolean isFood(@NotNull ItemStack pStack) {
        return false;
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (this.isOwnedBy(player) && player.isShiftKeyDown()) {
            ItemAttributeModifiers modifiers = heldItem.get(DataComponents.ATTRIBUTE_MODIFIERS);
            boolean hasAttackDamage = false;
            if (modifiers != null) {
                for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                    if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                        hasAttackDamage = true;
                        break;
                    }
                }
            }

            boolean isEquippable = heldItem.getItem() instanceof ArmorItem ||
                    heldItem.getItem() instanceof ShieldItem ||
                    heldItem.is(Items.TOTEM_OF_UNDYING) ||
                    hasAttackDamage;

            if (!heldItem.isEmpty() && isEquippable) {
                // 【核心修复】在这里添加对不死图腾的特殊判断
                EquipmentSlot slot = getEquipmentSlotForItem(heldItem);
                if (heldItem.is(Items.TOTEM_OF_UNDYING)) {
                    slot = EquipmentSlot.OFFHAND; // 强制指定为副手
                }

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
            }

        }

        return super.mobInteract(player, hand);
    }

    @Override public float getScale() { return 1.1f; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) { return null; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F); }
}