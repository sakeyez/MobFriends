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

    private int swell;
    private final int maxSwell = 30;

    // 【新增】定义仪式食物
    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "slime_ball_meal")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    public CombatCreeper(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D) // 添加攻击力
                .add(Attributes.ARMOR, 2.0D)       // 添加护甲
                .add(Attributes.FOLLOW_RANGE, 32.0D);
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

    // --- 【新增】成长属性实现 ---
    @Override
    protected double getHealthForLevel(int level) { return 40.0 + (level - 1) * 4.0; }
    @Override
    protected double getDamageForLevel(int level) { return 5.0 + (level - 1) * 0.75; }
    @Override
    protected double getSpeedForLevel(int level) { return 0.25 + (level - 1) * 0.002; }
    @Override
    protected double getArmorForLevel(int level) { return 2.0 + (level - 1) * 0.3; }
    @Override
    protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override
    protected Set<Block> getTier2RitualBlocks() { return Collections.emptySet(); } // 苦力怕暂时没有第二阶段

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(DATA_SWELL_DIR, -1);
        pBuilder.define(DATA_POWERED, false);
    }

    @Override
    public boolean isPowered() {
        return this.entityData.get(DATA_POWERED);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putShort("Fuse", (short)this.maxSwell);
        if (this.isPowered()) {
            pCompound.putBoolean("powered", true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.entityData.set(DATA_POWERED, pCompound.getBoolean("powered"));
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
            if (this.swell >= this.maxSwell) {
                this.swell = 0;
                this.explode();
            }
        }
        super.tick();
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
    public float getSwelling(float pPartialTicks) { return (this.swell + pPartialTicks) / (float)this.maxSwell; }
    @Override public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        // 让父类的交互逻辑优先处理
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) {
            return result;
        }
        // 如果父类没处理，再执行坐下逻辑
        if (this.isOwnedBy(player) && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) {
                this.setOrderedToSit(!this.isOrderedToSit());
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
    }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.CREEPER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CREEPER_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.GRASS_STEP, 0.15F, 1.0F); }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
}