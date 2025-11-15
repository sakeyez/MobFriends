package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.ItemStack;
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
                .add(Attributes.ARMOR, 0.0D) // 【修改】0.0D
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

        // 【新增】读取NBT后，应用仪式属性
        if (!this.level().isClientSide) {
            applyRitualBasedStats();
        }
    }


    /**
     * 【核心修改】重写整个 doHurtTarget 方法以实现动态伤害倍率
     */
    @Override
    public boolean doHurtTarget(Entity pEntity) {
        if (!(pEntity instanceof LivingEntity target)) {
            return false;
        }

        // --- 1. 伤害计算 ---
        float baseDamage = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float multiplier = 0.9f; // 基础倍率 (0.9)

        // 如果解锁了技能，计算额外倍率
        if (this.hasLifestealSkill()) {
            int debuffLevelSum = 0;
            // 遍历目标所有 *负面* 效果
            for (MobEffectInstance effect : target.getActiveEffects()) {
                if (!effect.getEffect().value().isBeneficial()) {
                    // 累加 (效果等级 + 1)
                    debuffLevelSum += (effect.getAmplifier() + 1);
                }
            }
            // 加上额外倍率 (0.2 * 等级之和)
            multiplier += (0.2f * debuffLevelSum);
        }

        float finalDamage = baseDamage * multiplier;

        // --- 2. 造成伤害 ---
        boolean success = target.hurt(this.damageSources().mobAttack(this), finalDamage);

        // --- 3. 施加 Debuff ---
        if (success) {

            this.triggerAttackAnimation();
            final int DURATION_IN_TICKS = 200; // 【修改】10 秒

            // --- 1. 凋零 Wither (上限 III) ---
            MobEffectInstance currentWither = target.getEffect(MobEffects.WITHER);
            int witherAmp;
            if (currentWither == null) {
                witherAmp = 0; // 第一次
            } else if (currentWither.getAmplifier() < 2) {
                witherAmp = currentWither.getAmplifier() + 1; // 升级
            } else {
                witherAmp = 2; // 保持满级 (用于刷新)
            }
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, DURATION_IN_TICKS, witherAmp));

            // --- 2. 中毒 Poison (上限 II) ---
            if (this.hasPoisonSkill()) {
                MobEffectInstance currentPoison = target.getEffect(MobEffects.POISON);
                int poisonAmp;
                if (currentPoison == null) {
                    poisonAmp = 0; // 第一次
                } else if (currentPoison.getAmplifier() < 1) {
                    poisonAmp = currentPoison.getAmplifier() + 1; // 升级
                } else {
                    poisonAmp = 1; // 保持满级 (用于刷新)
                }
                target.addEffect(new MobEffectInstance(MobEffects.POISON, DURATION_IN_TICKS, poisonAmp));
            }

            // --- 3. 减速 Slow (上限 II) + 虚弱 Weakness (上限 I) ---
            if (this.hasSlowSkill()) {
                MobEffectInstance currentSlow = target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
                int slowAmp;
                if (currentSlow == null) {
                    slowAmp = 0; // 第一次
                } else if (currentSlow.getAmplifier() < 1) {
                    slowAmp = currentSlow.getAmplifier() + 1; // 升级
                } else {
                    slowAmp = 1; // 保持满级 (用于刷新)
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_IN_TICKS, slowAmp));

                // 【新增】虚弱效果
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURATION_IN_TICKS, 0));
            }
        }
        return success;
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

        // 【新增】吃下仪式食物后，刷新属性
        this.setWarriorLevel(this.getWarriorLevel());
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

    // --- 【新增】继承自僵尸的成长机制 ---

    @Override
    public void setWarriorLevel(int level) {
        super.setWarriorLevel(level);
        if (!this.level().isClientSide) {
            applyRitualBasedStats();
        }
    }

    private void applyRitualBasedStats() {
        if (this.level().isClientSide()) return;

        int ritualFoodCount = this.getEatenRitualFoodsCount(); // 'x'

        // 应用新速度： 基础速度 + x * 0.008
        double newSpeed = getInitialSpeed() + (ritualFoodCount * 0.008d);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
    }

    @Override
    protected float getBonusDamageReduction() {
        // 免伤： x * 0.01 (即 x%)
        float reduction = this.getEatenRitualFoodsCount() * 0.01f;
        return Math.min(1.0f, reduction);
    }

    // --- 【修改】成长属性 ---

    @Override protected int getInitialLevelCap() { return 15; }
    @Override protected int getLevelCapIncrease() { return 5; }

    @Override protected double getInitialHealth() { return 20.0; }
    @Override protected double getHealthPerLevel() { return 2.0; }
    @Override protected int getFinalLevelCap() { return 30; }
    @Override protected double getInitialAttack() { return 1.0; }
    @Override protected double getAttackPerLevel() { return 0.2; } // 【修改】0.0 -> 0.2

    @Override protected double getInitialAttackMultiplier() { return 0.9; } // 【修改】1.0 -> 0.9 (新公式基础值)
    @Override protected double getAttackMultiplierPerLevel() { return 0.0; } // 【修改】0.0 (由技能动态计算)

    @Override protected double getInitialSpeed() { return 0.23; }
    @Override protected double getSpeedPerLevel() { return 0.0; } // 【修改】0.005 -> 0.0 (由仪式食物决定)

    @Override protected float getDamageReductionPerLevel() { return 0.0f; } // 【修改】0.0066f -> 0.0f (由仪式食物决定)

    @Override protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override protected Set<Block> getTier2RitualBlocks() { return TIER_2_BLOCKS; }


    @Override
    public boolean isFood(@NotNull ItemStack pStack) {
        return false;
    }


    @Override public float getScale() { return 1.1f; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) { return null; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F); }
}