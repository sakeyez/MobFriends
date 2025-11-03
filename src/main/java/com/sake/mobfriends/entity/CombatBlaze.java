package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.BlazeIdleGoal;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
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

public class CombatBlaze extends AbstractWarriorEntity implements RangedAttackMob {

    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "chorus_fried_egg"),
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "blaze_lamb_chop")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    private static final EntityDataAccessor<Float> DATA_FIREBALL_DAMAGE = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_ATTACK_INTERVAL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_ATTACK_SPEED_SKILL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_DRAGON_BREATH_SKILL = SynchedEntityData.defineId(CombatBlaze.class, EntityDataSerializers.BOOLEAN);


    public CombatBlaze(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        // 【恢复】飞行生物需要一个特殊的移动控制器
        this.moveControl = new BlazeMoveControl(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FIREBALL_DAMAGE, 6.0f);
        builder.define(DATA_ATTACK_INTERVAL, 40);
        builder.define(DATA_HAS_ATTACK_SPEED_SKILL, false);
        builder.define(DATA_HAS_DRAGON_BREATH_SKILL, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("FireballDamage", this.getFireballDamage());
        compound.putInt("AttackInterval", this.getAttackInterval());
        compound.putBoolean("HasAttackSpeedSkill", this.hasAttackSpeedSkill());
        compound.putBoolean("HasDragonBreathSkill", this.hasDragonBreathSkill());
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
            applyLevelBasedStats();
        } else if (path.equals("chorus_fried_egg")) {
            this.entityData.set(DATA_HAS_DRAGON_BREATH_SKILL, true);
        }
    }

    public boolean hasAttackSpeedSkill() {
        return this.entityData.get(DATA_HAS_ATTACK_SPEED_SKILL);
    }

    public boolean hasDragonBreathSkill() {
        return this.entityData.get(DATA_HAS_DRAGON_BREATH_SKILL);
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity pTarget, float pVelocity) {
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getEyeY() - this.getEyeY();
        double d2 = pTarget.getZ() - this.getZ();

        if (this.hasDragonBreathSkill()) {
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
            Vec3 movement = new Vec3(d0, d1, d2);
            DragonFireball dragonFireball = new DragonFireball(this.level(), this, movement);
            dragonFireball.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(dragonFireball);
        } else {
            this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
            Vec3 movement = new Vec3(d0, d1, d2);
            SmallFireball smallfireball = new SmallFireball(this.level(), this, movement);
            smallfireball.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(smallfireball);
        }
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                // 【恢复】飞行生物需要飞行速度
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.ARMOR, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 吃东西的AI
        Set<Block> ritualFoodBlocks = new HashSet<>(getTier1RitualBlocks());
        if (!ritualFoodBlocks.isEmpty()) {
            this.eatBlockFoodGoal = new EatBlockFoodGoal(this, 1.0D, 16, ritualFoodBlocks::contains);
            this.goalSelector.addGoal(1, this.eatBlockFoodGoal);
        }

        // 【核心修改】战斗AI: 飞起来攻击
        this.goalSelector.addGoal(2, new BlazeAttackGoal(this));

        // 【核心修改】非战斗AI: 降落并在地面行走
        this.goalSelector.addGoal(3, new BlazeIdleGoal(this));

        // 其他通用AI
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // 目标选择AI (保持不变)
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    // 【恢复】让实体不受重力影响
    @Override public boolean isNoGravity() { return true; }

    // 其他属性和方法的定义...
    @Override protected int getInitialLevelCap() { return 10; }
    @Override protected int getLevelCapIncrease() { return 10; }
    @Override protected double getInitialHealth() { return 10.0; }
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

    // --- 【恢复】战斗AI内部类 ---
    static class BlazeAttackGoal extends Goal {
        private final CombatBlaze blaze;
        private int attackCooldown = 0;

        public BlazeAttackGoal(CombatBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 只在有目标时激活 (战斗状态)
            return this.blaze.getTarget() != null;
        }

        @Override
        public void stop() {
            this.attackCooldown = 0;
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

            // 核心逻辑: 飞到比目标高3格的位置
            double desiredY = target.getY() + 3.0D;
            double yDiff = desiredY - this.blaze.getY();
            if (Math.abs(yDiff) > 1.0D) { // 如果高度差大于1，调整高度
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(0, (yDiff > 0 ? 0.05 : -0.05), 0));
            } else { // 高度合适，悬停
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().multiply(1, 0.5, 1));
            }

            // 与目标保持距离
            double distSqr = this.blaze.distanceToSqr(target);
            if (distSqr < 9.0D) { // 离得太近就后退
                Vec3 awayVec = this.blaze.position().subtract(target.position()).normalize();
                this.blaze.getMoveControl().setWantedPosition(this.blaze.getX() + awayVec.x, this.blaze.getY(), this.blaze.getZ() + awayVec.z, 1.0D);
            } else if (distSqr > 64.0D) { // 离得太远就靠近
                this.blaze.getMoveControl().setWantedPosition(target.getX(), this.blaze.getY(), target.getZ(), 1.0D);
            }

            // 攻击逻辑
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (this.attackCooldown == 0 && this.blaze.hasLineOfSight(target)) {
                this.blaze.performRangedAttack(target, 1.0f);
                this.attackCooldown = this.blaze.getAttackInterval();
            }
        }
    }

    // --- 【恢复】飞行移动控制器内部类 ---
    static class BlazeMoveControl extends MoveControl {
        private final CombatBlaze blaze;
        private int floatDuration;

        public BlazeMoveControl(CombatBlaze pBlaze) {
            super(pBlaze);
            this.blaze = pBlaze;
        }

        @Override
        public void tick() {
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                return;
            }
            if (this.floatDuration-- <= 0) {
                this.floatDuration += this.blaze.getRandom().nextInt(5) + 2;
                Vec3 vec3 = new Vec3(this.wantedX - this.blaze.getX(), this.wantedY - this.blaze.getY(), this.wantedZ - this.blaze.getZ());
                double d0 = vec3.length();
                vec3 = vec3.normalize();
                if (this.canReach(vec3, (int)Math.ceil(d0))) {
                    this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(vec3.scale(0.1)));
                } else {
                    this.operation = MoveControl.Operation.WAIT;
                }
            }
        }

        private boolean canReach(Vec3 pDirection, int pSteps) {
            AABB aabb = this.blaze.getBoundingBox();
            for (int i = 1; i < pSteps; ++i) {
                aabb = aabb.move(pDirection);
                if (!this.blaze.level().noCollision(this.blaze, aabb)) {
                    return false;
                }
            }
            return true;
        }
    }
}