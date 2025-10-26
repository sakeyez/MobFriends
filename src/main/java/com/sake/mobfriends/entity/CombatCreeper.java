package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.CombatCreeperSwellGoal;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

// 【修复】实现 PowerableMob 接口
public class CombatCreeper extends TamableAnimal implements PowerableMob {

    private static final EntityDataAccessor<Integer> DATA_SWELL_DIR = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.INT);
    // 【修复】添加一个新的数据同步器，用于标记是否为闪电苦力怕
    private static final EntityDataAccessor<Boolean> DATA_POWERED = SynchedEntityData.defineId(CombatCreeper.class, EntityDataSerializers.BOOLEAN);

    private int swell;
    private final int maxSwell = 30;

    public CombatCreeper(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
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
        pBuilder.define(DATA_POWERED, false); // 注册新的数据
    }

    // --- 【核心修复】实现 isPowered 方法 ---
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

            // 【核心修复】在消失前，手动触发核心破碎逻辑
            this.breakCore();

            this.discard();
        }
    }

    /**
     * 【新增】一个专门用于在自爆时处理核心破碎的方法
     */
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
                    this.save(data); // 保存当前所有数据
                    brokenCore.set(ModDataComponents.STORED_CREEPER_NBT.get(), data);
                    brokenCore.set(ModDataComponents.CREEPER_UUID.get(), deadUUID);
                    inventory.setItem(i, brokenCore);
                    // 播放一个音效给主人，提示核心破碎
                    owner.playSound(SoundEvents.GLASS_BREAK, 1.0F, 1.0F);
                    return; // 找到并处理完成，退出
                }
            }
        }
    }

    public int getSwellDir() { return this.entityData.get(DATA_SWELL_DIR); }
    public void setSwellDir(int pState) { this.entityData.set(DATA_SWELL_DIR, pState); }
    public float getSwelling(float pPartialTicks) { return (this.swell + pPartialTicks) / (float)this.maxSwell; }
    @Override public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) { if (this.isOwnedBy(player) && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) { if (!level().isClientSide) { this.setOrderedToSit(!this.isOrderedToSit()); } return InteractionResult.sidedSuccess(this.level().isClientSide()); } return super.mobInteract(player, hand); }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.CREEPER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CREEPER_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.GRASS_STEP, 0.15F, 1.0F); }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
}