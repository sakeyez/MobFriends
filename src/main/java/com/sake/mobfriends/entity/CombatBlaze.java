package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.BlazeIdleGoal;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 【【【V3 最终AI修复版】】】
 * 1. 修复：交换 FollowOwnerGoal 和 BlazeIdleGoal 的优先级 (解决问题1)
 * 2. 修复：扩大 FOLLOW_RANGE 索敌范围 (解决问题3)
 */
public class CombatBlaze extends AbstractWarriorEntity implements RangedAttackMob {

    // (所有 TIER_1_BLOCKS 和 数据追踪器 保持不变)
    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "chorus_fried_egg"),
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "blaze_lamb_chop")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());
    private static final EntityDataAccessor<Float> DATA_FIREBALL_DAMAGE = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_INTERVAL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_ATTACK_SPEED_SKILL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_DRAGON_BREATH_SKILL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_MAGIC_IMMUNITY_SKILL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.BOOLEAN);

    public CombatBlaze(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothBlazeMoveControl(this); // (使用 V2 的平滑控制器)
    }

    // (defineSynchedData, addAdditionalSaveData, readAdditionalSaveData, tick, ... 保持不变 ...)
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FIREBALL_DAMAGE, 6.0f);
        builder.define(DATA_ATTACK_INTERVAL, 40);
        builder.define(DATA_HAS_ATTACK_SPEED_SKILL, false);
        builder.define(DATA_HAS_DRAGON_BREATH_SKILL, false);
        builder.define(DATA_HAS_MAGIC_IMMUNITY_SKILL, false);
    }
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("FireballDamage", this.getFireballDamage());
        compound.putInt("AttackInterval", this.getAttackInterval());
        compound.putBoolean("HasAttackSpeedSkill", this.hasAttackSpeedSkill());
        compound.putBoolean("HasDragonBreathSkill", this.hasDragonBreathSkill());
        compound.putBoolean("HasMagicImmunitySkill", this.hasMagicImmunitySkill());
    }
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("FireballDamage")) {
            this.setFireballDamage(compound.getFloat("FireballDamage"));
        }
        if (compound.contains("AttackInterval")) {
            this.setAttackInterval(compound.getInt("AttackInterval"));
        }
        this.entityData.set(DATA_HAS_ATTACK_SPEED_SKILL, compound.getBoolean("HasAttackSpeedSkill"));
        this.entityData.set(DATA_HAS_DRAGON_BREATH_SKILL, compound.getBoolean("HasDragonBreathSkill"));
        this.entityData.set(DATA_HAS_MAGIC_IMMUNITY_SKILL, compound.getBoolean("HasMagicImmunitySkill"));
    }
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 1; ++i) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                        this.getRandomX(0.5D),
                        this.getRandomY(),
                        this.getRandomZ(0.5D),
                        0.0D, 0.0D, 0.0D);
            }
            if (this.hasDragonBreathSkill()) {
                if (this.random.nextFloat() < 0.5F) {
                    this.level().addParticle(ParticleTypes.PORTAL,
                            this.getRandomX(0.5D),
                            this.getRandomY(),
                            this.getRandomZ(0.5D),
                            0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
    @Override
    public void applyLevelBasedStats() {
        super.applyLevelBasedStats();
        if (this.level().isClientSide()) return;
        int level = this.getWarriorLevel();
        this.setFireballDamage(6.0f + 0.3f * level);
        int baseInterval = 40 - level;
        int finalInterval = this.hasAttackSpeedSkill() ? baseInterval - 20 : baseInterval;
        this.setAttackInterval(Math.max(1, finalInterval));
    }
    @Override
    protected void onRitualFoodEaten(ResourceLocation foodId) {
        String path = foodId.getPath();
        if (path.equals("blaze_lamb_chop")) {
            this.entityData.set(DATA_HAS_ATTACK_SPEED_SKILL, true);
        } else if (path.equals("chorus_fried_egg")) {
            this.entityData.set(DATA_HAS_DRAGON_BREATH_SKILL, true);
            this.entityData.set(DATA_HAS_MAGIC_IMMUNITY_SKILL, true);
        }
        this.setWarriorLevel(this.getWarriorLevel());
    }
    public boolean hasAttackSpeedSkill() {
        return this.entityData.get(DATA_HAS_ATTACK_SPEED_SKILL);
    }
    public boolean hasDragonBreathSkill() {
        return this.entityData.get(DATA_HAS_DRAGON_BREATH_SKILL);
    }
    public boolean hasMagicImmunitySkill() {
        return this.entityData.get(DATA_HAS_MAGIC_IMMUNITY_SKILL);
    }
    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (pSource.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (this.hasMagicImmunitySkill()) {
            if (pSource.is(DamageTypes.MAGIC) || pSource.is(DamageTypes.INDIRECT_MAGIC)) {
                return false;
            }
        }
        return super.hurt(pSource, pAmount);
    }
    @Override
    public void performRangedAttack(@NotNull LivingEntity pTarget, float pVelocity) {
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getEyeY() - this.getEyeY();
        double d2 = pTarget.getZ() - this.getZ();
        Vec3 movement = new Vec3(d0, d1, d2);
        if (this.hasDragonBreathSkill() && this.random.nextFloat() < 0.10F) {
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
            DragonFireball dragonFireball = new DragonFireball(this.level(), this, movement);
            dragonFireball.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(dragonFireball);
        } else {
            this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
            SmallFireball smallfireball = new SmallFireball(this.level(), this, movement);
            smallfireball.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(smallfireball);
        }
    }

    /**
     * 【【【修复点 2：修改 createAttributes】】】
     */
    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.FLYING_SPEED, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /**
     * 【【【修复点 1：修改 registerGoals】】】
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 吃东西的AI
        Set<Block> ritualFoodBlocks = new HashSet<>(getTier1RitualBlocks());
        if (!ritualFoodBlocks.isEmpty()) {
            this.eatBlockFoodGoal = new EatBlockFoodGoal(this, 1.0D, 16, ritualFoodBlocks::contains);
            this.goalSelector.addGoal(1, this.eatBlockFoodGoal);
        }

        // 战斗AI
        this.goalSelector.addGoal(2, new BlazeAttackGoal(this));

        // 【修改】交换 Prio 3 和 5
        // Prio 3: 跟随主人 (更高优先级)
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        // Prio 5: 空闲悬浮 (更低优先级)
        this.goalSelector.addGoal(5, new BlazeIdleGoal(this));

        // 其他通用AI
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // 目标选择AI (保持不变)
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    // (setWarriorLevel 和 getBonusDamageReduction 保持不变)
    @Override
    public void setWarriorLevel(int level) {
        super.setWarriorLevel(level);
        if (this.level().isClientSide()) return;
        int x = this.getEatenRitualFoodsCount();
        AttributeInstance flySpeedAttr = this.getAttribute(Attributes.FLYING_SPEED);
        if (flySpeedAttr != null) {
            flySpeedAttr.setBaseValue(0.5D + (x * 0.01D));
        }
        AttributeInstance armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.setBaseValue(level * 1.0D);
        }
        this.setFireballDamage(6.0f + 0.3f * level);
        int finalInterval = this.hasAttackSpeedSkill() ? 20 : 40;
        this.setAttackInterval(finalInterval);
    }
    @Override
    protected float getBonusDamageReduction() {
        int x = this.getEatenRitualFoodsCount();
        float reduction = x * 0.02f;
        return Math.min(1.0f, reduction);
    }

    // (其他杂项方法保持不变)
    @Override public boolean isNoGravity() { return true; }
    @Override protected int getFinalLevelCap() { return 20; }
    @Override protected int getInitialLevelCap() { return 10; }
    @Override protected int getLevelCapIncrease() { return 10; }
    @Override protected double getInitialHealth() { return 20.0; }
    @Override protected double getHealthPerLevel() { return 2.0; }
    @Override protected double getInitialAttack() { return 0; }
    @Override protected double getAttackPerLevel() { return 0; }
    @Override protected double getInitialAttackMultiplier() { return 1.0; }
    @Override protected double getAttackMultiplierPerLevel() { return 0; }
    @Override protected double getInitialSpeed() { return 0.3; }
    @Override protected double getSpeedPerLevel() { return 0; }
    @Override protected float getDamageReductionPerLevel() { return 0.02f; }
    @Override protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override protected Set<Block> getTier2RitualBlocks() { return Collections.emptySet(); }
    public float getFireballDamage() { return this.entityData.get(DATA_FIREBALL_DAMAGE); }
    public void setFireballDamage(float damage) { this.entityData.set(DATA_FIREBALL_DAMAGE, damage); }
    public int getAttackInterval() { return this.entityData.get(DATA_ATTACK_INTERVAL); }
    public void setAttackInterval(int interval) { this.entityData.set(DATA_ATTACK_INTERVAL, interval); }
    @Override public boolean causeFallDamage(float pFallDistance, float pMultiplier, @NotNull DamageSource pSource) { return false; }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
    @Override public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) { return super.mobInteract(player, hand); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { }

    // (BlazeAttackGoal 和 SmoothBlazeMoveControl 内部类保持 V2 不变)
    static class BlazeAttackGoal extends Goal {
        private final CombatBlaze blaze;
        private int attackCooldown = 0;
        public BlazeAttackGoal(CombatBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }
        @Override
        public boolean canUse() {
            return this.blaze.getTarget() != null;
        }
        @Override
        public void stop() {
            this.attackCooldown = 0;
            this.blaze.getMoveControl().setWantedPosition(this.blaze.getX(), this.blaze.getY(), this.blaze.getZ(), 0);
        }
        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
        @Override
        public void tick() {
            LivingEntity target = this.blaze.getTarget();
            if (target == null || !target.isAlive()) {
                return;
            }
            this.blaze.getLookControl().setLookAt(target, 10.0F, 10.0F);
            double idealX = target.getX();
            double idealY = target.getY() + 3.0D;
            double idealZ = target.getZ();
            double distSqr = this.blaze.distanceToSqr(target.getX(), this.blaze.getY(), target.getZ());
            if (distSqr < 25.0D) {
                Vec3 awayVec = this.blaze.position().subtract(target.position()).normalize();
                idealX = this.blaze.getX() + awayVec.x * 5.0;
                idealZ = this.blaze.getZ() + awayVec.z * 5.0;
            }
            this.blaze.getMoveControl().setWantedPosition(idealX, idealY, idealZ, 1.0D);
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (this.attackCooldown == 0 && this.blaze.hasLineOfSight(target)) {
                this.blaze.performRangedAttack(target, 1.0f);
                this.attackCooldown = this.blaze.getAttackInterval();
            }
        }
    }
    static class SmoothBlazeMoveControl extends MoveControl {
        private final CombatBlaze blaze;
        public SmoothBlazeMoveControl(CombatBlaze pBlaze) {
            super(pBlaze);
            this.blaze = pBlaze;
        }
        @Override
        public void tick() {
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(0.0, -0.02, 0.0));
            } else {
                Vec3 targetPos = new Vec3(this.wantedX, this.wantedY, this.wantedZ);
                Vec3 blazePos = this.blaze.position();
                Vec3 moveVec = targetPos.subtract(blazePos);
                double distance = moveVec.length();
                if (distance < 0.5) {
                    this.operation = MoveControl.Operation.WAIT;
                    this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().scale(0.5));
                    return;
                }
                moveVec = moveVec.normalize();
                double dynamicSpeed = this.speedModifier * (0.1 + 0.9 * Math.min(distance / 5.0, 1.0));
                Vec3 desiredVelocity = moveVec.scale(dynamicSpeed);
                Vec3 newVelocity = this.blaze.getDeltaMovement().lerp(desiredVelocity, 0.1);
                this.blaze.setDeltaMovement(newVelocity);
            }
        }
    }
}