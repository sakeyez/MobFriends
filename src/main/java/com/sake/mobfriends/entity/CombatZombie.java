package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class CombatZombie extends AbstractWarriorEntity {

    private int healSkillCooldown = 0;
    private int teleportSkillCooldown = 0;
    private int thornsCooldown = 0;
    private float damageTakenSinceLastThorns = 0;


    // --- 仪式食物定义 (不变) ---
    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "crystal_lamb_chop")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    private static final Set<Block> TIER_2_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "sweet_and_sour_ender_pearls"),
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "golden_salad")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    // --- 技能数据追踪器 (不变) ---
    private static final EntityDataAccessor<Boolean> DATA_HAS_TELEPORT_SKILL = SynchedEntityData.defineId(CombatZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_HEAL_SKILL = SynchedEntityData.defineId(CombatZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_THORNS_SKILL = SynchedEntityData.defineId(CombatZombie.class, EntityDataSerializers.BOOLEAN);

    public CombatZombie(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /**
     * 【逻辑修正】
     * 1. 治疗计时器：只在受伤时运行。
     * 2. 战斗计时器：只在有目标时运行。
     * 3. 传送技能：改为8秒冷却，先嘲讽，再检查5格距离。
     */
    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        // --- 技能 1：黄金沙拉 (只在受伤时计时) ---
        if (this.hasHealSkill()) {
            if (this.getHealth() < this.getMaxHealth()) {
                if (this.healSkillCooldown > 0) {
                    this.healSkillCooldown--;
                } else {
                    triggerHealSkill();
                    this.healSkillCooldown = 160; // 8秒
                }
            }
        }

        // --- 获取战斗目标 ---
        LivingEntity target = this.getTarget();

        // --- 技能 2：珍珠咕咾肉 (只在战斗中计时) ---
        if (this.hasTeleportSkill()) {
            if (target != null && target.isAlive()) {
                // 在战斗中，开始计时
                if (this.teleportSkillCooldown > 0) {
                    this.teleportSkillCooldown--;
                } else {
                    // 时间到，执行效果
                    // 1. 先嘲讽
                    tauntTarget(target);

                    // 2. 再检查距离 (> 5格)
                    double distanceSq = this.distanceToSqr(target);
                    if (distanceSq > 25.0D) { // 5*5=25
                        teleportNearTarget(target);
                    }

                    // 3. 重置冷却 (8秒)
                    this.teleportSkillCooldown = 160;
                }
            } else {
                // 脱战，重置计时器
                this.teleportSkillCooldown = 0;
            }
        }

        // --- 技能 3：水晶羊排 (只在战斗中计时) ---
        if (this.hasThornsSkill()) {
            if (target != null && target.isAlive()) {
                // 在战斗中，开始计时
                if (this.thornsCooldown > 0) {
                    this.thornsCooldown--;
                } else {
                    triggerThornsSkill();
                    this.thornsCooldown = 160; // 8秒
                    this.damageTakenSinceLastThorns = 0;
                }
            } else {
                // 脱战，重置计时器和伤害
                this.thornsCooldown = 0;
                this.damageTakenSinceLastThorns = 0;
            }
        }
    }

    /**
     * 记录伤害并触发“充能”粒子
     */
    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (!this.level().isClientSide && this.hasThornsSkill()) {
            if (pSource.getEntity() != null && pSource.getEntity() != this && !pSource.is(DamageTypes.THORNS)) {
                this.damageTakenSinceLastThorns += pAmount;

                // 充能粒子
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            this.getRandomX(0.8D),
                            this.getRandomY(),
                            this.getRandomZ(0.8D),
                            5, 0, 0, 0, 0.05);
                }
            }
        }

        return super.hurt(pSource, pAmount);
    }

    // --- 治疗技能 (不变) ---
    private void triggerHealSkill() {
        new ArrayList<>(this.getActiveEffects()).stream()
                .filter(effect -> !effect.getEffect().value().isBeneficial())
                .forEach(effect -> this.removeEffect(effect.getEffect()));
        this.heal(8.0F);
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 160, 1));
    }

    // --- 传送技能 (不变) ---
    private void teleportNearTarget(LivingEntity target) {
        Vec3 targetPos = target.position();
        for (int i = 0; i < 10; ++i) {
            double x = targetPos.x + (this.getRandom().nextDouble() - 0.5D) * 8.0D;
            double y = targetPos.y + (this.getRandom().nextInt(5) - 2);
            double z = targetPos.z + (this.getRandom().nextDouble() - 0.5D) * 8.0D;
            if (this.randomTeleport(x, y, z, true)) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
                return;
            }
        }
    }

    // --- 嘲讽技能 (不变) ---
    private void tauntTarget(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setTarget(this);
        }
    }

    /**
     * 触发反伤技能 (圆环 + 圆面)
     */
    private void triggerThornsSkill() {
        if (this.damageTakenSinceLastThorns <= 0) {
            return;
        }

        float reflectDamage = this.damageTakenSinceLastThorns * 0.4f;

        this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 3.0F, 0.8F);

        if (this.level() instanceof ServerLevel serverLevel) {

            // --- 粒子效果 1：水晶圆环 (100个) ---
            final int RING_PARTICLE_COUNT = 100;
            final double RADIUS = 5.0D;
            final BlockParticleOption CRYSTAL_PARTICLE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState());
            double y = this.getY() + 0.3; // 粒子生成在脚底附近

            for (int i = 0; i < RING_PARTICLE_COUNT; ++i) {
                double angle = this.random.nextDouble() * 2.0D * Math.PI;
                double x = this.getX() + RADIUS * Mth.cos((float)angle);
                double z = this.getZ() + RADIUS * Mth.sin((float)angle);
                double vx = Mth.cos((float)angle) * 0.2D;
                double vz = Mth.sin((float)angle) * 0.2D;
                serverLevel.sendParticles(CRYSTAL_PARTICLE, x, y, z, 1, vx, 0.1D, vz, 0.0D);
            }

            // --- 粒子效果 2：灰尘圆面 (200个) ---
            final int DISC_PARTICLE_COUNT = 200;
            final double RADIUS_SQUARED = RADIUS * RADIUS;
            final DustParticleOptions DUST_PARTICLE = new DustParticleOptions(new Vector3f(200.0f / 255.0f, 160.0f / 255.0f, 255.0f / 255.0f), 1.0f);

            for (int i = 0; i < DISC_PARTICLE_COUNT; ++i) {
                double rX = (this.random.nextDouble() * 2.0D - 1.0D) * RADIUS;
                double rZ = (this.random.nextDouble() * 2.0D - 1.0D) * RADIUS;
                if (rX * rX + rZ * rZ <= RADIUS_SQUARED) {
                    double spawnX = this.getX() + rX;
                    double spawnZ = this.getZ() + rZ;
                    serverLevel.sendParticles(DUST_PARTICLE, spawnX, y, spawnZ, 1, 0.0D, 0.1D, 0.0D, 0.0D);
                }
            }
        }

        // --- 索敌和伤害 (不变) ---
        AABB searchBox = this.getBoundingBox().inflate(5.0D);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> {
            if (e instanceof Mob mob) {
                return mob.isAlive() && mob.getTarget() == this;
            }
            return false;
        });

        for (LivingEntity enemy : targets) {
            enemy.hurt(this.damageSources().mobAttack(this), reflectDamage);
        }
    }


    // --- (以下所有代码保持不变，无需修改) ---

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_TELEPORT_SKILL, false);
        builder.define(DATA_HAS_HEAL_SKILL, false);
        builder.define(DATA_HAS_THORNS_SKILL, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasTeleportSkill", this.hasTeleportSkill());
        compound.putBoolean("HasHealSkill", this.hasHealSkill());
        compound.putBoolean("HasThornsSkill", this.hasThornsSkill());
        compound.putInt("HealCooldown", this.healSkillCooldown);
        compound.putInt("TeleportCooldown", this.teleportSkillCooldown);
        compound.putInt("ThornsCooldown", this.thornsCooldown);
        compound.putFloat("DamageTaken", this.damageTakenSinceLastThorns);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_HAS_TELEPORT_SKILL, compound.getBoolean("HasTeleportSkill"));
        this.entityData.set(DATA_HAS_HEAL_SKILL, compound.getBoolean("HasHealSkill"));
        this.entityData.set(DATA_HAS_THORNS_SKILL, compound.getBoolean("HasThornsSkill"));
        this.healSkillCooldown = compound.getInt("HealCooldown");
        this.teleportSkillCooldown = compound.getInt("TeleportCooldown");
        this.thornsCooldown = compound.getInt("ThornsCooldown");
        this.damageTakenSinceLastThorns = compound.getFloat("DamageTaken");

        if (!this.level().isClientSide) {
            applyRitualBasedStats();
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));

        Set<Block> ritualFoodBlocks = new HashSet<>();
        ritualFoodBlocks.addAll(getTier1RitualBlocks());
        ritualFoodBlocks.addAll(getTier2RitualBlocks());
        if (!ritualFoodBlocks.isEmpty()) {
            this.eatBlockFoodGoal = new EatBlockFoodGoal(this, 1.2D, 16, ritualFoodBlocks::contains);
            this.goalSelector.addGoal(3, this.eatBlockFoodGoal);
        }

        this.goalSelector.addGoal(4, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D);
    }

    @Override
    public void setWarriorLevel(int level) {
        super.setWarriorLevel(level);

        if (!this.level().isClientSide) {
            applyRitualBasedStats();
        }
    }

    private void applyRitualBasedStats() {
        if (this.level().isClientSide()) return;

        int ritualFoodCount = this.getEatenRitualFoodsCount();

        double newSpeed = getInitialSpeed() + (ritualFoodCount * 0.008d);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
    }

    @Override
    protected void onRitualFoodEaten(ResourceLocation foodId) {
        String path = foodId.getPath();
        if (path.equals("crystal_lamb_chop")) {
            this.entityData.set(DATA_HAS_THORNS_SKILL, true);
        } else if (path.equals("sweet_and_sour_ender_pearls")) {
            this.entityData.set(DATA_HAS_TELEPORT_SKILL, true);
        } else if (path.equals("golden_salad")) {
            this.entityData.set(DATA_HAS_HEAL_SKILL, true);
        }

        this.setWarriorLevel(this.getWarriorLevel());
    }

    @Override
    protected float getBonusDamageReduction() {
        float reduction = this.getEatenRitualFoodsCount() * 0.01f;
        return Math.min(1.0f, reduction);
    }

    public boolean hasTeleportSkill() {
        return this.entityData.get(DATA_HAS_TELEPORT_SKILL);
    }
    public boolean hasHealSkill() {
        return this.entityData.get(DATA_HAS_HEAL_SKILL);
    }
    public boolean hasThornsSkill() {
        return this.entityData.get(DATA_HAS_THORNS_SKILL);
    }

    @Override protected int getInitialLevelCap() { return 15; }
    @Override protected int getLevelCapIncrease() { return 5; }

    @Override protected double getInitialHealth() { return 30.0; }
    @Override protected double getHealthPerLevel() { return 3.0; }

    @Override protected double getInitialAttack() { return 1.0; }
    @Override protected double getAttackPerLevel() { return 0.1; }

    @Override protected double getInitialAttackMultiplier() { return 0.5; }
    @Override protected double getAttackMultiplierPerLevel() { return 0.0; }

    @Override protected double getInitialSpeed() { return 0.23; }
    @Override protected double getSpeedPerLevel() { return 0.0; }

    @Override protected float getDamageReductionPerLevel() { return 0.0f; }

    @Override protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override protected Set<Block> getTier2RitualBlocks() { return TIER_2_BLOCKS; }

    @Override protected int getFinalLevelCap() { return 30; }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) { return null; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pSource) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F); }
}