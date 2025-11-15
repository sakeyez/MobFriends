package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.IWorkstationNPC;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob; // 【【【修改：从 PathfinderMob 改为 Mob】】】
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * 方案 4a: 限制NPC活动范围的AI Goal (V2 - 修复史莱姆兼容性)
 * 如果NPC超出了绑定的工作站范围 (16格半径)，此AI将激活并使其导航回家。
 */
public class ReturnToWorkstationGoal extends Goal {

    private final Mob mob; // 【【【修改：从 PathfinderMob 改为 Mob】】】
    private final IWorkstationNPC npc;
    private final double speedModifier;
    private final double range;

    private BlockPos homePos = null;
    private AABB boundingBox = null;

    // 【【【修改：从 PathfinderMob 改为 Mob】】】
    public ReturnToWorkstationGoal(Mob mob, double range, double speedModifier) {
        this.mob = mob;
        // 确保我们的 Mob 实现了 IWorkstationNPC 接口
        if (!(mob instanceof IWorkstationNPC)) {
            throw new IllegalArgumentException("ReturnToWorkstationGoal 只能用于实现了 IWorkstationNPC 的实体！");
        }
        this.npc = (IWorkstationNPC) mob;
        this.range = range;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    /**
     * 检查是否满足执行条件
     */
    @Override
    public boolean canUse() {
        this.homePos = npc.getWorkstationPos();

        if (this.homePos == null || this.mob.level().isRaining()) {
            return false;
        }

        if (this.boundingBox == null) {
            this.boundingBox = new AABB(this.homePos).inflate(this.range);
        }

        return !this.boundingBox.contains(this.mob.position());
    }

    /**
     * 决定是否应该继续执行
     */
    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone() &&
                this.homePos != null &&
                this.boundingBox != null &&
                !this.boundingBox.contains(this.mob.position());
    }

    /**
     * AI 开始执行
     */
    @Override
    public void start() {
        if (this.homePos != null) {
            this.mob.getNavigation().moveTo(
                    this.homePos.getX() + 0.5,
                    this.homePos.getY() + 1.0,
                    this.homePos.getZ() + 0.5,
                    this.speedModifier
            );
        }
    }

    /**
     * AI 停止执行
     */
    @Override
    public void stop() {
        this.homePos = null;
        this.boundingBox = null;
    }
}