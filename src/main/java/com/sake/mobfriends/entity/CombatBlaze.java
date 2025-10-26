package com.sake.mobfriends.entity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CombatBlaze extends TamableAnimal implements RangedAttackMob {

    public CombatBlaze(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);
        this.moveControl = new MoveControl(this);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));

        // 【核心修改】将战斗AI和闲置飞行AI分开
        this.goalSelector.addGoal(2, new BlazeAttackGoal(this)); // 战斗时使用的AI
        this.goalSelector.addGoal(3, new BlazeFloatGoal(this));  // 非战斗时使用的AI

        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity pTarget, float pVelocity) {
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
        // --- 火球准度控制区 ---
        double d0 = pTarget.getX() - this.getX();
        double d1 = pTarget.getEyeY() - this.getEyeY();
        double d2 = pTarget.getZ() - this.getZ();
        // --- 结束 ---
        Vec3 movement = new Vec3(d0, d1, d2);
        SmallFireball smallfireball = new SmallFireball(this.level(), this, movement);
        smallfireball.setPos(this.getX(), this.getEyeY(), this.getZ());
        this.level().addFreshEntity(smallfireball);
    }

    // 战斗AI
    static class BlazeAttackGoal extends Goal {
        private final CombatBlaze blaze;
        private int attackCooldown = 0;

        public BlazeAttackGoal(CombatBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 只有在有目标时才启动
            return this.blaze.getTarget() != null;
        }

        @Override
        public void stop() {
            // 【核心修改】当AI结束时（敌人死亡或消失），重置导航
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
                return; // 目标消失，等待AI被停止
            }

            this.blaze.getLookControl().setLookAt(target, 10.0F, 10.0F);

            // 高度控制逻辑保持不变
            double desiredY = target.getY() + 3.0D;
            double yDiff = desiredY - this.blaze.getY();
            if (Math.abs(yDiff) > 0.5D) {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(0, (yDiff > 0 ? 0.05 : -0.05), 0));
            } else {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().multiply(1, 0, 1));
            }

            // 距离和攻击逻辑保持不变
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

    // 【新增】闲置时的飞行/降落AI
    static class BlazeFloatGoal extends Goal {
        private final CombatBlaze blaze;

        public BlazeFloatGoal(CombatBlaze blaze) {
            this.blaze = blaze;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // 只有在没有目标且不在坐下时才启动
            return this.blaze.getTarget() == null && !this.blaze.isOrderedToSit();
        }

        @Override
        public void tick() {
            // 如果不在地面上，就缓慢下降
            if (!this.blaze.onGround()) {
                this.blaze.setDeltaMovement(this.blaze.getDeltaMovement().add(0, -0.04, 0));
            }
        }
    }


    @Override public boolean isNoGravity() { return true; }
    @Override public boolean causeFallDamage(float pFallDistance, float pMultiplier, @NotNull DamageSource pSource) { return false; }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel pLevel, @NotNull AgeableMob pOtherParent) { return null; }
    @Override public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) { if (this.isOwnedBy(player) && player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) { if (!level().isClientSide) { this.setOrderedToSit(!this.isOrderedToSit()); } return InteractionResult.sidedSuccess(this.level().isClientSide()); } return super.mobInteract(player, hand); }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { }
}