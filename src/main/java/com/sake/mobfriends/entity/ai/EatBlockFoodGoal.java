package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.event.MobFinishedEatingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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

    // ... (变量和构造函数保持不变) ...
    protected final PathfinderMob mob;
    private final double speedModifier;
    private final int searchRange;
    private final Predicate<Block> foodBlockPredicate;

    protected BlockPos targetPos;
    private int eatingCooldown;
    private int findNewTargetCooldown;

    public EatBlockFoodGoal(PathfinderMob mob, double speedModifier, int searchRange, Predicate<Block> foodBlockPredicate) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.searchRange = searchRange;
        this.foodBlockPredicate = foodBlockPredicate;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    // ... (canUse, canContinueToUse, start, stop 保持不变) ...
    @Override
    public boolean canUse() {
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
    }

    @Override
    public void stop() {
        this.mob.removeEffect(MobEffects.GLOWING);
        this.targetPos = null;
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
            this.mob.getNavigation().moveTo(this.targetPos.getX() + 0.5, this.targetPos.getY(), this.targetPos.getZ() + 0.5, this.speedModifier);
            if (!this.mob.hasEffect(MobEffects.GLOWING)) {
                this.mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        } else {
            this.mob.getNavigation().stop();
            this.mob.removeEffect(MobEffects.GLOWING);

            // 如果还在冷却中，则不做任何事
            if (this.eatingCooldown > 0) {
                this.eatingCooldown--;
                return;
            }

            // --- 冷却结束，执行“咬一口”的动作 ---
            Level level = this.mob.level();
            BlockState state = level.getBlockState(this.targetPos);

            // --- 核心修正点：将Glow粒子生成移到这里 ---
            // 只有在“咬”的那一刻才触发
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.GLOW,
                        this.mob.getRandomX(0.5D),
                        this.mob.getRandomY() + 0.5D,
                        this.mob.getRandomZ(0.5D),
                        5, 0, 0, 0, 0.02D);
            }
            // --- 修正点结束 ---

            // 为方块生成碎屑粒子
            spawnEatingParticles(level, this.targetPos, state);

            // 处理方块状态变化
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

            // 重置冷却计时器
            this.eatingCooldown = 20;
        }
    }

    // ... (onEatFood 和其他辅助方法保持不变) ...
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