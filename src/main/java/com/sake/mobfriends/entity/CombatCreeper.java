package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.CombatCreeperSwellGoal;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
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
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 【修改】继承 AbstractWarriorEntity 并实现 PowerableMob
public class CombatCreeper extends AbstractWarriorEntity implements PowerableMob {

    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.BOOLEAN);
    // --- 【核心修改】为技能添加数据追踪器 ---
    private static final EntityDataAccessor<Boolean> DATA_HAS_FAST_EXPLOSION_SKILL = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.BOOLEAN);

    private int swell;
    // 移除了 final，使其可以被修改
    private int maxSwell = 30;

    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "slime_ball_meal")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    public CombatCreeper(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }


    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                // --- 【核心修改】设置初始属性和索敌范围 ---
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // 【新增】吃东西的AI
        Set<Block> ritualFoodBlocks = new HashSet<>(getTier1RitualBlocks());
        if (!ritualFoodBlocks.isEmpty()) {
            this.eatBlockFoodGoal = new EatBlockFoodGoal(this, 1.2D, 16, ritualFoodBlocks::contains);
            this.goalSelector.addGoal(1, this.eatBlockFoodGoal);
        }

        this.goalSelector.addGoal(2, new CombatCreeperSwellGoal(this));
        this.goalSelector.addGoal(3, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_SWELL_DIR, -1);
        pBuilder.define(DATA_POWERED, false);
        pBuilder.define(DATA_HAS_FAST_EXPLOSION_SKILL, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putShort("Fuse", (short)this.getMaxSwell());
        if (this.isPowered()) {
            pCompound.putBoolean("powered", true);
        }
        pCompound.putBoolean("HasFastExplosionSkill", this.hasFastExplosionSkill());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.entityData.set(DATA_POWERED, pCompound.getBoolean("powered"));
        this.entityData.set(DATA_HAS_FAST_EXPLOSION_SKILL, pCompound.getBoolean("HasFastExplosionSkill"));
        // 读取后立即更新爆炸时间
        updateFuseTime();
    }

    @Override
    public void tick() {
        if (this.isAlive()) {
            int i = this.getSwellDir();
            if (i > 0 && this.swell == 0) {
                this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
            }
            this.swell += i;
            if (this.swell < 0) {
                this.swell = 0;
            }
            // 使用 getMaxSwell() 获取动态的爆炸时间
            if (this.swell >= this.getMaxSwell()) {
                this.swell = 0;
                this.explode();
            }
        }
        super.tick();
    }

    // --- 【核心修改】实现技能解锁和查询 ---
    @Override
    protected void onRitualFoodEaten(ResourceLocation foodId) {
        if (foodId.getPath().equals("slime_ball_meal")) {
            this.entityData.set(DATA_HAS_FAST_EXPLOSION_SKILL, true);
            updateFuseTime(); // 解锁技能后立即更新爆炸时间
        }
    }

    public boolean hasFastExplosionSkill() {
        return this.entityData.get(DATA_HAS_FAST_EXPLOSION_SKILL);
    }

    // --- 【核心修改】获取动态的爆炸时间 ---
    public int getMaxSwell() {
        return this.maxSwell;
    }

    // 更新爆炸时间的辅助方法
    private void updateFuseTime() {
        if (this.hasFastExplosionSkill()) {
            this.maxSwell = 10; // 30 / 3 = 10
        } else {
            this.maxSwell = 30;
        }
    }

    public float getSwelling(float pPartialTicks) {
        // 使用 getMaxSwell() 来正确计算膨胀百分比
        return (this.swell + pPartialTicks) / (float)this.getMaxSwell();
    }
    // --- 【新增】成长属性实现 ---
    @Override protected int getInitialLevelCap() { return 10; }
    @Override protected int getLevelCapIncrease() { return 10; }

    @Override protected double getInitialHealth() { return 20.0; }
    @Override protected double getHealthPerLevel() { return 4.0; }

    @Override protected double getInitialAttack() { return 1.0; }
    @Override protected double getAttackPerLevel() { return 0.0; }

    @Override protected double getInitialAttackMultiplier() { return 1.0; }
    @Override protected double getAttackMultiplierPerLevel() { return 0.0; }

    @Override protected double getInitialSpeed() { return 0.23; }
    @Override protected double getSpeedPerLevel() { return 0.005; }

    @Override protected float getDamageReductionPerLevel() { return 0.04f; }
    @Override
    protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override
    protected Set<Block> getTier2RitualBlocks() { return Collections.emptySet(); } // 苦力怕暂时没有第二阶段



    @Override
    public boolean isPowered() {
        return this.entityData.get(DATA_POWERED);
    }







    private void explode() {
        if (!this.level().isClientSide) {
            float explosionRadius = this.isPowered() ? 6.0F : 3.0F;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius, Level.ExplosionInteraction.NONE);
            this.breakCore();
            this.discard();
        }
    }

    private void breakCore() {
        if (!(this.getOwner() instanceof Player owner)) {
            return;
        }
        UUID deadUUID = this.getUUID();
        Inventory inventory = owner.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(ModItems.ACTIVE_CREEPER_CORE.get())) {
                UUID coreUUID = stack.get(ModDataComponents.CREEPER_UUID.get());
                if (deadUUID.equals(coreUUID)) {
                    ItemStack brokenCore = new ItemStack(ModItems.BROKEN_CREEPER_CORE.get());
                    CompoundTag data = new CompoundTag();
                    this.save(data);
                    brokenCore.set(ModDataComponents.STORED_CREEPER_NBT.get(), data);
                    brokenCore.set(ModDataComponents.CREEPER_UUID.get(), deadUUID);
                    inventory.setItem(i, brokenCore);
                    owner.playSound(SoundEvents.GLASS_BREAK, 1.0F, 1.0F);
                    return;
                }
            }
        }
    }

    public int getSwellDir() { return this.entityData.get(DATA_SWELL_DIR); }
    public void setSwellDir(int pState) { this.entityData.set(DATA_SWELL_DIR, pState); }


    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        // 【核心修改】
        // 我们移除了之前关于“潜行+右键使其坐下”的逻辑。
        // 现在，这个方法只会调用父类（AbstractWarriorEntity）的交互方法。
        // 父类的方法包含了喂食和升级的逻辑，所以这些功能会被完整保留。
        return super.mobInteract(player, hand);
    }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.CREEPER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CREEPER_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.GRASS_STEP, 0.15F, 1.0F); }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
}