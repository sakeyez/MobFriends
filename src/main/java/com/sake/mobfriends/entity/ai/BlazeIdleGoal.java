package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.CombatBlaze;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.PathComputationType;

import java.util.EnumSet;

/**
 * 烈焰人空闲状态的AI - 【V3 悬浮版】
 * 实现了平滑的离地0.1格悬浮行走。
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
        return this.blaze.eatBlockFoodGoal == null || !this.blaze.eatBlockFoodGoal.isRunning();
    }

    /**
     * 【核心修复】AI启动时给一个短暂的延迟，防止立即移动。
     */
    @Override
    public void start() {
        this.wanderCooldown = 20 + this.blaze.getRandom().nextInt(20);
    }

    @Override
    public void tick() {
        // 只有当冷却结束后才寻找新目标
        if (this.wanderCooldown-- <= 0) {
            findAndMoveToHoverPos();
        }
    }

    /**
     * 【核心修复】寻找并移动到目标悬浮点的逻辑。
     * 这次它会计算一个地面以上0.1格的精确目标点。
     */
    private void findAndMoveToHoverPos() {
        // 重置冷却时间，让下一次行走有间隔
        this.wanderCooldown = 120 + this.blaze.getRandom().nextInt(120);

        for (int i = 0; i < 10; i++) {
            BlockPos currentPos = this.blaze.blockPosition();
            int x = this.blaze.getRandom().nextInt(21) - 10;
            int z = this.blaze.getRandom().nextInt(21) - 10;
            BlockPos groundPos = findGround(currentPos.offset(x, 0, z));

            // 检查目标点是否可达
            if (this.blaze.level().getBlockState(groundPos.below()).isPathfindable(PathComputationType.LAND)) {
                // --- 【你的绝妙想法】 ---
                // 目标Y坐标 = 地面高度 + 0.1，实现悬浮效果！
                double targetX = groundPos.getX() + 0.5;
                double targetY = groundPos.getY() + 0.1;
                double targetZ = groundPos.getZ() + 0.5;

                // 命令移动控制器以悠闲的速度飞向这个悬浮点
                this.blaze.getMoveControl().setWantedPosition(targetX, targetY, targetZ, 0.5D);
                return; // 找到目标就结束
            }
        }
    }

    private BlockPos findGround(BlockPos startPos) {
        BlockPos.MutableBlockPos mutablePos = startPos.mutable();
        while (mutablePos.getY() > blaze.level().getMinBuildHeight() && blaze.level().isEmptyBlock(mutablePos.below())) {
            mutablePos.move(0, -1, 0);
        }
        while (mutablePos.getY() < blaze.level().getMaxBuildHeight() && !blaze.level().isEmptyBlock(mutablePos)) {
            mutablePos.move(0, 1, 0);
        }
        return mutablePos.immutable();
    }
}