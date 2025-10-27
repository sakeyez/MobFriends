package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 【修改】继承 AbstractWarriorEntity
public class CombatBlaze extends AbstractWarriorEntity implements RangedAttackMob {

    // 【新增】定义仪式食物
    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "chorus_fried_egg"),
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "blaze_lamb_chop")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    public CombatBlaze(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new MoveControl(this);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 3.0D) // 添加护甲
                .add(Attributes.FOLLOW_RANGE, 48.0D);
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

        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new BlazeAttackGoal(this));
        this.goalSelector.addGoal(4, new BlazeFloatGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    // --- 【新增】成长属性实现 ---
    @Override
    protected double getHealthForLevel(int level) { return 50.0 + (level - 1) * 5.0; }
    @Override
    protected double getDamageForLevel(int level) { return 6.0 + (level - 1) * 0.8; }
    @Override
    protected double getSpeedForLevel(int level) { return 0.3 + (level - 1) * 0.003; }
    @Override
    protected double getArmorForLevel(int level) { return 3.0 + (level - 1) * 0.35; }
    @Override
    protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override
    protected Set<Block> getTier2RitualBlocks() { return Collections.emptySet(); }


    @Override
    public void performRangedAttack(@NotNull LivingEntity pTarget, float pVelocity) {
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getEyeY() - this.getEyeY();
        double d2 = pTarget.getZ() - this.getZ();
        Vec3 movement = new Vec3(d0, d1, d2);
        SmallFireball smallfireball = new SmallFireball(this.level(), this, movement);
        smallfireball.setPos(this.getX(), this.getEyeY(), this.getZ());
        this.level().addFreshEntity(smallfireball);
    }

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
            this.blaze.getNavigation().stop();
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

            double desiredY = target.getY() + 3.0D;
            double yDiff = desiredY - this.blaze.getY();
            if (Math.abs(yDiff) > 0.5D) {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(0, (yDiff > 0 ? 0.05 : -0.05), 0));
            } else {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().multiply(1, 0, 1));
            }

            double distSqr = this.blaze.distanceToSqr(target);
            if (distSqr < 9.0D) {
                Vec3 awayVec = this.blaze.position().subtract(target.position()).normalize();
                this.blaze.getMoveControl().setWantedPosition(this.blaze.getX() + awayVec.x, this.blaze.getY(), this.blaze.getZ() + awayVec.z, 1.0D);
            } else if (distSqr > 64.0D) {
                this.blaze.getMoveControl().setWantedPosition(target.getX(), this.blaze.getY(), target.getZ(), 1.0D);
            } else {
                this.attackCooldown = Math.max(0, this.attackCooldown - 1);
                if (this.attackCooldown == 0) {
                    this.blaze.performRangedAttack(target, 1.0f);
                    this.attackCooldown = 10;
                }
            }
        }
    }

    static class BlazeFloatGoal extends Goal {
        private final CombatBlaze blaze;

        public BlazeFloatGoal(CombatBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return this.blaze.getTarget() == null && !this.blaze.isOrderedToSit();
        }

        @Override
        public void tick() {
            if (!this.blaze.onGround()) {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(0, -0.04, 0));
            }
        }
    }


    @Override public boolean isNoGravity() { return true; }
    @Override public boolean causeFallDamage(float pFallDistance, float pMultiplier, @NotNull DamageSource pSource) { return false; }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
    @Override public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        InteractionResult result = super.mobInteract(player, hand);
        if (result.consumesAction()) {
            return result;
        }
        if (this.isOwnedBy(player) && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) { this.setOrderedToSit(!this.isOrderedToSit()); }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return InteractionResult.PASS;
    }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { }
}