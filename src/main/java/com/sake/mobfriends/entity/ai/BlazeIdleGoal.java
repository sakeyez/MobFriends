package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.CombatBlaze;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathComputationType;

import java.util.EnumSet;

/**
 * 烈焰人空闲状态的AI - 【V4 修复版】
 * 修复了导致其无法移动的 isPathfindable 检查
 */
public class BlazeIdleGoal extends Goal {
    private final CombatBlaze blaze;
    private int wanderCooldown = 0;

    public BlazeIdleGoal(CombatBlaze blaze) {
        this.blaze = blaze;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.blaze.getTarget() != null) {
            return false;
        }
        // 【【【修改】】】
        // 确保 EatBlockFoodGoal 存在且正在运行时，此AI才让步
        return this.blaze.eatBlockFoodGoal == null || !this.blaze.eatBlockFoodGoal.isRunning();
    }

    @Override
    public void start() {
        this.wanderCooldown = 20 + this.blaze.getRandom().nextInt(20);
    }

    @Override
    public void tick() {
        if (this.wanderCooldown-- <= 0) {
            findAndMoveToHoverPos();
        }
    }

    private void findAndMoveToHoverPos() {
        this.wanderCooldown = 120 + this.blaze.getRandom().nextInt(120);

        for (int i = 0; i < 10; i++) {
            BlockPos currentPos = this.blaze.blockPosition();
            int x = this.blaze.getRandom().nextInt(21) - 10;
            int z = this.blaze.getRandom().nextInt(21) - 10;
            BlockPos groundPos = findGround(currentPos.offset(x, 0, z));

            // --- 【【【核心修复】】】 ---
            // 移除了错误的 if (isPathfindable) 检查。
            // 只要找到了地面，就直接飞过去。

            double targetX = groundPos.getX() + 0.5;
            double targetY = groundPos.getY() + 0.1; // 悬浮
            double targetZ = groundPos.getZ() + 0.5;

            this.blaze.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 0.5D); // 0.5D 悠闲速度
            return;
        }
    }

    private BlockPos findGround(BlockPos startPos) {
        BlockPos.MutableBlockPos mutablePos = startPos.mutable();
        // 向下找到第一个非空气方块
        while (mutablePos.getY() > blaze.level().getMinBuildHeight() && blaze.level().isEmptyBlock(mutablePos.below())) {
            mutablePos.move(0, -1, 0);
        }
        // 从那里向上找到第一个空气方块（即地面）
        while (mutablePos.getY() < blaze.level().getMaxBuildHeight() && !blaze.level().isEmptyBlock(mutablePos)) {
            mutablePos.move(0, 1, 0);
        }
        return mutablePos.immutable();
    }
}