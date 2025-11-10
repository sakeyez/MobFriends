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

public class CombatBlaze extends AbstractWarriorEntity implements RangedAttackMob {

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
        // 1. 运行父类的 tick (例如 AbstractWarriorEntity的食物治疗)
        super.tick();

        // 2. 仅在客户端执行粒子生成
        if (this.level().isClientSide()) {

            // 3. 生成 2 个烟雾粒子 (原版烈焰人效果)
            for (int i = 0; i < 1; ++i) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE,
                        this.getRandomX(0.5D), // 在实体宽度 + 0.5 范围内随机X
                        this.getRandomY(),        // 在实体高度范围内随机Y
                        this.getRandomZ(0.5D), // 在实体深度 + 0.5 范围内随机Z
                        0.0D, 0.0D, 0.0D);      // 粒子无初始速度
            }

            // 4. 如果有龙息技能，额外生成粒子
            if (this.hasDragonBreathSkill()) {
                // 烟雾粒子是2个/tick, 1/4的比例就是 0.5个/tick
                // 我们通过 50% 的几率每 tick 生成 1 个粒子来实现
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
        // 【修改】吃完食物后，立即调用 setWarriorLevel 来刷新所有属性
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
        // 1. 基础：免疫火焰伤害 (烈焰人天生特性)
        // 【【【修复点 1：使用 DamageTypeTags.IS_FIRE 标签】】】
        if (pSource.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }

        // 2. 技能：如果解锁了技能
        if (this.hasMagicImmunitySkill()) {
            // 【【【修复点 2：确认 MAGIC 和 INDIRECT_MAGIC 正确】】】
            // (这些在 DamageTypes.java 中存在，是正确的)
            if (pSource.is(DamageTypes.MAGIC) || pSource.is(DamageTypes.INDIRECT_MAGIC)) {
                return false; // 直接返回 false，取消伤害
            }
        }

        // 3. 如果不是免疫的伤害，则调用父类的 hurt() 方法
        return super.hurt(pSource, pAmount);
    }


    /**
     * 【核心修改】龙息技能改为10%概率触发
     */
    @Override
    public void performRangedAttack(@NotNull LivingEntity pTarget, float pVelocity) {
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getEyeY() - this.getEyeY();
        double d2 = pTarget.getZ() - this.getZ();
        Vec3 movement = new Vec3(d0, d1, d2); // 移动向量只需计算一次

        // 【修改】有10%概率发射龙火球
        if (this.hasDragonBreathSkill() && this.random.nextFloat() < 0.10F) { // 10% 概率
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 1.0F, 1.0F);
            DragonFireball dragonFireball = new DragonFireball(this.level(), this, movement);
            dragonFireball.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(dragonFireball);
        } else {
            // 90%概率或未解锁技能时，发射小火球
            this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
            SmallFireball smallfireball = new SmallFireball(this.level(), this, movement);
            smallfireball.setPos(this.getX(), this.getEyeY(), this.getZ());
            this.level().addFreshEntity(smallfireball);
        }
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D) // 基础生命: 20
                .add(Attributes.FLYING_SPEED, 0.5D) // 飞行速度: 0.5
                .add(Attributes.MOVEMENT_SPEED, 0.3D) // 移动速度: 0.3
                .add(Attributes.ATTACK_DAMAGE, 0.0D) // 攻击力: 0.0
                .add(Attributes.ARMOR, 0.0D) // 盔甲值: 0
                .add(Attributes.FOLLOW_RANGE, 16.0D); // 索敌范围: 16
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

    @Override
    public void setWarriorLevel(int level) {
        super.setWarriorLevel(level); // 负责应用基础生命值和调用 getBonusDamageReduction
        if (this.level().isClientSide()) return;

        // x = 吃过的不同种 RITUAL_FOOD 数量
        // (我们使用仪式食物数量来保持与 Zobmie/Wither 一致)
        int x = this.getEatenRitualFoodsCount();

        // 1. 更新飞行速度 (基于 x)
        AttributeInstance flySpeedAttr = this.getAttribute(Attributes.FLYING_SPEED);
        if (flySpeedAttr != null) {
            // 基础 0.5 + 0.01 * x
            flySpeedAttr.setBaseValue(0.5D + (x * 0.01D));
        }

        // 2. 更新盔甲 (基于 level)
        AttributeInstance armorAttr = this.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) {
            // 基础 0 + 1 * 等级
            armorAttr.setBaseValue(level * 1.0D);
        }

        // 3. 更新火球伤害 (基于 level)
        // 公式: 6.0 + (0.3 * 等级)
        this.setFireballDamage(6.0f + 0.3f * level);

        // 4. 更新攻击间隔 (基于技能)
        // 公式: 40 (基础), 20 (有技能)
        int finalInterval = this.hasAttackSpeedSkill() ? 20 : 40;
        this.setAttackInterval(finalInterval);
    }

    @Override
    protected float getBonusDamageReduction() {
        // (我们使用仪式食物数量来保持与 Zobmie/Wither 一致)
        int x = this.getEatenRitualFoodsCount();
        // 免伤 = 0.02 * x
        float reduction = x * 0.02f;
        return Math.min(1.0f, reduction); // 封顶 100%
    }

    // 【恢复】让实体不受重力影响
    @Override public boolean isNoGravity() { return true; }

    @Override protected int getFinalLevelCap() { return 20; }
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

            double distSqr = this.blaze.distanceToSqr(target);
            if (distSqr < 25.0D) { // 1. 离得太近 (小于5格) -> 后退
                Vec3 awayVec = this.blaze.position().subtract(target.position()).normalize();
                this.blaze.getMoveControl().setWantedPosition(this.blaze.getX() + awayVec.x, this.blaze.getY(), this.blaze.getZ() + awayVec.z, 1.0D);

            } else if (distSqr > 64.0D) { // 2. 离得太远 (大于8格) -> 靠近
                this.blaze.getMoveControl().setWantedPosition(target.getX(), this.blaze.getY(), target.getZ(), 1.0D);

            } else { // 3. 【【【新增逻辑】】】 距离合适 (5-8格) -> 停止水平移动
                // 告诉移动控制器 "飞向你现在的位置"，这会有效地停止水平飞行
                // 允许 "任务2" 的悬停逻辑完全接管
                this.blaze.getMoveControl().setWantedPosition(this.blaze.getX(), this.blaze.getY(), this.blaze.getZ(), 1.0D);
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