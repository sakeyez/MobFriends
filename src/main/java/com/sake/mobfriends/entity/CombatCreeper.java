// 文件路径: main/java/com/sake/mobfriends/entity/CombatCreeper.java
package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.CombatCreeperSwellGoal;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance; // 【新增】导入
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

// 继承 PowerableMob 保持不变
public class CombatCreeper extends AbstractWarriorEntity implements PowerableMob {

    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_FAST_EXPLOSION_SKILL = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.BOOLEAN);

    private int swell;
    private int maxSwell = 30; // 默认引信时间

    // 【新增】连环爆炸技能的追踪器
    private int chainExplosionsRemaining = 0;
    private int chainExplosionTicker = 0; // 爆炸间隔计时器

    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "slime_ball_meal")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    public CombatCreeper(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }


    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                // --- 【数值修改】 ---
                .add(Attributes.MAX_HEALTH, 20.0D) // 基础生命: 20 (不变)
                .add(Attributes.MOVEMENT_SPEED, 0.25D) // 速度: 0.25 (原为 0.23)
                .add(Attributes.ATTACK_DAMAGE, 1.0D) // 攻击力: 1.0 (不变)
                .add(Attributes.ARMOR, 0.0D) // 盔甲值: 0 (原为 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0D); // 索敌范围: 40 (不变)
    }

    // registerGoals() 保持不变
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

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

    // defineSynchedData() 保持不变
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
        // 【新增】保存连环爆炸的状态
        pCompound.putInt("ChainExplosionsRemaining", this.chainExplosionsRemaining);
        pCompound.putInt("ChainExplosionTicker", this.chainExplosionTicker);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.entityData.set(DATA_POWERED, pCompound.getBoolean("powered"));
        this.entityData.set(DATA_HAS_FAST_EXPLOSION_SKILL, pCompound.getBoolean("HasFastExplosionSkill"));
        updateFuseTime(); // 读取后立即更新爆炸时间
        // 【新增】读取连环爆炸的状态
        this.chainExplosionsRemaining = pCompound.getInt("ChainExplosionsRemaining");
        this.chainExplosionTicker = pCompound.getInt("ChainExplosionTicker");
    }

    /**
     * 【核心修改】重写 Tick 逻辑以支持连环爆炸
     */
    @Override
    public void tick() {
        if (this.isAlive()) {
            // --- 连环爆炸逻辑 ---
            if (this.chainExplosionsRemaining > 0) {
                // 如果正在连环爆，则膨胀逻辑暂停
                this.swell = 0;
                this.setSwellDir(-1);

                this.chainExplosionTicker--;
                if (this.chainExplosionTicker <= 0) {
                    this.explodeChain(); // 间隔时间到，执行下一次爆炸
                }
            } else {
                // --- 正常膨胀逻辑 ---
                int i = this.getSwellDir();
                if (i > 0 && this.swell == 0) {
                    this.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.5F);
                }
                this.swell += i;
                if (this.swell < 0) {
                    this.swell = 0;
                }

                // 膨胀到达最大值
                if (this.swell >= this.getMaxSwell()) {
                    this.swell = 0;

                    if (this.hasFastExplosionSkill()) {
                        // 技能：启动连环爆炸
                        this.chainExplosionsRemaining = 3; // 总共炸3次
                        this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
                        this.explodeChain(); // 立即执行第一次
                    } else {
                        // 普通：执行单次爆炸
                        this.explode();
                    }
                }
            }
        }
        super.tick();
    }

    // --- 技能解锁回调 (保持不变) ---
    @Override
    protected void onRitualFoodEaten(ResourceLocation foodId) {
        if (foodId.getPath().equals("slime_ball_meal")) {
            this.entityData.set(DATA_HAS_FAST_EXPLOSION_SKILL, true);
            updateFuseTime(); // 解锁技能后立即更新爆炸时间
        }
    }

    // --- 技能查询 (保持不变) ---
    public boolean hasFastExplosionSkill() {
        return this.entityData.get(DATA_HAS_FAST_EXPLOSION_SKILL);
    }

    // --- 引信时间 (保持不变) ---
    public int getMaxSwell() {
        return this.maxSwell;
    }
    private void updateFuseTime() {
        // 【技能效果】引信时间缩短为 10 ticks (0.5秒)
        if (this.hasFastExplosionSkill()) {
            this.maxSwell = 10;
        } else {
            this.maxSwell = 30;
        }
    }
    public float getSwelling(float pPartialTicks) {
        if (this.chainExplosionsRemaining > 0) {
            // 连环爆期间，让模型闪烁
            return (float)this.chainExplosionTicker / 20.0f;
        }
        return (this.swell + pPartialTicks) / (float)this.getMaxSwell();
    }

    // --- 【新增】属性成长：基于 "Dining Food" ---

    /**
     * 覆盖 setWarriorLevel 来应用动态属性
     * 1. 速度 (基于 dining food 数量 x)
     * 2. 盔甲 (基于等级)
     */
    @Override
    public void setWarriorLevel(int level) {
        super.setWarriorLevel(level); // 这会处理生命值、攻击力，并调用 getBonusDamageReduction

        // 仅在服务器端应用属性修改
        if (!this.level().isClientSide) {
            // x = 吃过的不同种 dining_food 数量
            int x = this.getEatenRitualFoodsCount();
            AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                // 速度 = 基础速度 + 0.01 * x
                double newSpeed = getInitialSpeed() + (x * 0.01d);
                speedAttr.setBaseValue(newSpeed);
            }

            // 盔甲值 = 0 (基础) + 1 * 等级
            AttributeInstance armorAttr = this.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                double newArmor = 0.0 + (level * 1.0d);
                armorAttr.setBaseValue(newArmor);
            }
        }
    }

    /**
     * 覆盖 getBonusDamageReduction 来实现动态免伤
     * 免伤 (基于 dining food 数量 x)
     */
    @Override
    protected float getBonusDamageReduction() {
        // x = 吃过的不同种 dining_food 数量
        int x = this.getEatenRitualFoodsCount();
        // 免伤 = 0.02 * x (即 2% * x)
        float reduction = x * 0.02f;
        return Math.min(1.0f, reduction); // 封顶 100%
    }


    // --- 【修改】成长属性定义 ---
    @Override protected int getInitialLevelCap() { return 10; }
    @Override protected int getLevelCapIncrease() { return 10; } // 仪式后上限到20

    @Override protected double getInitialHealth() { return 20.0; }
    @Override protected double getHealthPerLevel() { return 4.0; } // 生命: +4.0 每级

    @Override protected double getInitialAttack() { return 1.0; }
    @Override protected double getAttackPerLevel() { return 0.0; } // 攻击力: +0 每级

    @Override protected double getInitialAttackMultiplier() { return 1.0; }
    @Override protected double getAttackMultiplierPerLevel() { return 0.0; }

    @Override protected double getInitialSpeed() { return 0.25; } // 基础速度: 0.25
    @Override protected double getSpeedPerLevel() { return 0.0; } // 速度成长(每级): 0 (改为 dining food 动态计算)

    @Override protected float getDamageReductionPerLevel() { return 0.0f; } // 免伤(每级): 0 (改为 dining food 动态计算)

    @Override protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override protected Set<Block> getTier2RitualBlocks() { return Collections.emptySet(); }
    @Override protected int getFinalLevelCap() { return 20; }

    // --- 爆炸逻辑 ---

    /**
     * 【修改】普通爆炸 (技能未激活时)
     */
    private void explode() {
        if (!this.level().isClientSide) {
            // 【修改】爆炸威力: 基础 4.0F, 闪电 6.0F
            float explosionRadius = this.isPowered() ? 6.0F : 4.0F;
            // 粒子效果会自动匹配这个威力
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius, Level.ExplosionInteraction.NONE);

            if (this.level() instanceof ServerLevel serverLevel) {
                // 1. 第一个参数是粒子类型 (EXPLOSION_EMITTER 就是那个大云团)
                // 2. 坐标
                // 3. 粒子数量 (1个就够了)
                // 4. xyz 偏移量 (0,0,0)
                // 5. 粒子速度 (0.0)
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            }

            this.breakCore(); // 破坏核心
            this.discard();   // 实体消失
        }
    }

    /**
     * 【新增】连环爆炸 (技能激活时)
     */
    private void explodeChain() {
        if (!this.level().isClientSide) {
            // 【技能效果】爆炸威力 3, 爆炸范围 6
            // (原版方法中威力和范围是同一个值，我们优先满足 "范围 6")
            float explosionRadius = 6.0F;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), explosionRadius, Level.ExplosionInteraction.NONE);

            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            }

            this.chainExplosionsRemaining--; // 剩余次数 -1

            if (this.chainExplosionsRemaining <= 0) {
                // 3次已炸完
                this.breakCore(); // 破坏核心
                this.discard();   // 实体消失
            } else {
                // 准备下一次
                this.chainExplosionTicker = 20; // 1秒 (20 ticks) 后再炸
            }
        }
    }


    // --- (以下为保持不变的方法) ---

    // 破坏核心的逻辑 (保持不变)
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

    // PowerableMob 接口实现 (保持不变)
    @Override
    public boolean isPowered() {
        return this.entityData.get(DATA_POWERED);
    }

    // Swell 状态同步 (保持不变)
    public int getSwellDir() { return this.entityData.get(DATA_SWELL_DIR); }
    public void setSwellDir(int pState) { this.entityData.set(DATA_SWELL_DIR, pState); }

    // 交互逻辑 (保持不变, 继承父类)
    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    // 声音和杂项 (保持不变)
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.CREEPER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CREEPER_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.GRASS_STEP, 0.15F, 1.0F); }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
}