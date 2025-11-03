package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.event.MobFinishedEatingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Predicate;

public class EatBlockFoodGoal extends Goal {


    protected final PathfinderMob mob;
    private final double speedModifier;
    private final int searchRange;
    private final Predicate<Block> foodBlockPredicate;

    protected BlockPos targetPos;
    private int eatingCooldown;
    private int findNewTargetCooldown;
    private int searchCooldown = 0;
    private boolean isRunning = false; // 【新增】用于从外部判断此AI是否在运行
    public EatBlockFoodGoal(PathfinderMob mob, double speedModifier, int searchRange, Predicate<Block> foodBlockPredicate) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRange = searchRange;
        this.foodBlockPredicate = foodBlockPredicate;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }


    @Override
    public boolean canUse() {
        if (this.searchCooldown > 0) {
            --this.searchCooldown;
            return false;
        }

        this.searchCooldown = 20 + this.mob.getRandom().nextInt(20); // 每1-2秒搜索一次
        Optional<BlockPos> nearestFood = findNearestFoodBlock();
        if (nearestFood.isPresent()) {
            this.targetPos = nearestFood.get();
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetPos != null && foodBlockPredicate.test(this.mob.level().getBlockState(this.targetPos).getBlock());
    }

    @Override
    public void start() {
        this.eatingCooldown = 0;
        this.findNewTargetCooldown = 0;
        this.isRunning = true; // 【新增】标记为开始运行
    }

    @Override
    public void stop() {
        this.mob.removeEffect(MobEffects.GLOWING);
        this.targetPos = null;
        this.isRunning = false; // 【新增】标记为停止运行
    }

    // 【新增】公开的getter方法
    public boolean isRunning() {
        return this.isRunning;
    }


    @Override
    public void tick() {
        if (this.targetPos == null) {
            return;
        }

        if (this.findNewTargetCooldown-- <= 0) {
            this.findNewTargetCooldown = 40;
            findNearestFoodBlock().ifPresent(pos -> this.targetPos = pos);
        }

        mob.getLookControl().setLookAt(Vec3.atCenterOf(this.targetPos));

        Vec3 targetCenter = Vec3.atCenterOf(this.targetPos);

        double dx = this.mob.getX() - targetCenter.x();
        double dz = this.mob.getZ() - targetCenter.z();
        double horizontalDistSq = dx * dx + dz * dz;

        double verticalDist = Math.abs(this.mob.getY() - this.targetPos.getY());

        if (horizontalDistSq > 2.25 || verticalDist > 1) {
            // --- 【核心修改】 ---
            // 不再使用地面寻路，而是使用通用的移动控制器。
            // 这使得飞行生物也能正确移动到目标点。
            this.mob.getMoveControl().setWantedPosition(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, this.speedModifier);

            if (!this.mob.hasEffect(MobEffects.GLOWING)) {
                this.mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        } else {
            // 到达目标后，停止移动
            this.mob.getNavigation().stop();
            this.mob.setDeltaMovement(Vec3.ZERO); // 对飞行生物尤其重要，清除动量
            this.mob.removeEffect(MobEffects.GLOWING);

            if (this.eatingCooldown > 0) {
                this.eatingCooldown--;
                return;
            }

            Level level = this.mob.level();
            BlockState state = level.getBlockState(this.targetPos);
            this.mob.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.GLOW,
                        this.mob.getRandomX(0.5D),
                        this.mob.getRandomY() + 0.5D,
                        this.mob.getRandomZ(0.5D),
                        5, 0, 0, 0, 0.02D);
            }

            spawnEatingParticles(level, this.targetPos, state);

            Optional<IntegerProperty> bitesProperty = getBitesProperty(state);
            if (bitesProperty.isPresent()) {
                IntegerProperty prop = bitesProperty.get();
                int currentBites = state.getValue(prop);
                int maxBites = prop.getPossibleValues().stream().max(Integer::compareTo).orElse(0);

                if (currentBites < maxBites) {
                    level.setBlock(this.targetPos, state.setValue(prop, currentBites + 1), 3);
                    onEatFood(level, state, false);
                } else {
                    level.removeBlock(this.targetPos, false);
                    onEatFood(level, state, true);
                    this.targetPos = null;
                }
            } else {
                level.removeBlock(this.targetPos, false);
                onEatFood(level, state, true);
                this.targetPos = null;
            }

            this.eatingCooldown = 7;
        }
    }

    private void onEatFood(Level level, BlockState eatenState, boolean finished) {
        if (!level.isClientSide) {
            this.mob.heal(2.0F);
            if (finished) {
                NeoForge.EVENT_BUS.post(new MobFinishedEatingEvent(this.mob, eatenState));
            }
        }
    }

    private void spawnEatingParticles(Level level, BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    10, 0.3, 0.2, 0.3, 0.1D);
        }
    }

    private Optional<IntegerProperty> getBitesProperty(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("bites") && prop instanceof IntegerProperty) {
                return Optional.of((IntegerProperty) prop);
            }
        }
        return Optional.empty();
    }

    private Optional<BlockPos> findNearestFoodBlock() {
        return BlockPos.findClosestMatch(
                this.mob.blockPosition(),
                this.searchRange,
                4,
                pos -> this.foodBlockPredicate.test(this.mob.level().getBlockState(pos).getBlock())
        );
    }
}