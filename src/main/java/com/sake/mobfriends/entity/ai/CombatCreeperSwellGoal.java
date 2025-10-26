package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.CombatCreeper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

public class CombatCreeperSwellGoal extends Goal {
    private final CombatCreeper creeper;
    private LivingEntity target;

    public CombatCreeperSwellGoal(CombatCreeper combatCreeper) {
        this.creeper = combatCreeper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.creeper.getTarget();
        // 【核心修改】移除了对爆炸冷却的检查
        return livingentity != null && this.creeper.distanceToSqr(livingentity) < 9.0D && !this.creeper.isOrderedToSit();
    }

    @Override
    public void start() {
        this.creeper.getNavigation().stop();
        this.target = this.creeper.getTarget();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null || this.creeper.distanceToSqr(this.target) > 49.0D || !this.creeper.getSensing().hasLineOfSight(this.target)) {
            this.creeper.setSwellDir(-1); // 如果目标丢失或太远，停止膨胀
        } else {
            this.creeper.setSwellDir(1); // 否则，开始膨胀
        }
    }
}