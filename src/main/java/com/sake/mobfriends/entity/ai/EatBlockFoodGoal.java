package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.event.MobFinishedEatingEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;

import java.util.EnumSet;
import java.util.function.Predicate;

// NeoForge 1.21.1 移植要点:
// 1. 构造函数: MoveToBlockGoal 的构造函数签名没有太大变化，但内部逻辑需要适配。
// 2.isValidTarget(): 现在需要两个参数 (LevelReader level, BlockPos pos)。
// 3. tick(): 计时器和逻辑保持类似，但与世界的交互方法需要确认。
// 4. 事件触发: 使用 NeoForge.EVENT_BUS.post() 来发布我们的自定义事件。
// 5. 方块判断: 优先使用方块标签 (Tag) 或 BlockState.is(Block) 进行判断，更具兼容性。
public class EatBlockFoodGoal extends MoveToBlockGoal {

    private final PathfinderMob mob;
    private final Predicate<Block> foodBlockPredicate;
    private int eatingTicks = 0;

    public EatBlockFoodGoal(PathfinderMob mob, double speedModifier, int searchRange, Predicate<Block> foodBlockPredicate) {
        // 调用父类构造函数
        // 参数: 实体, 速度, 搜索范围, y轴搜索范围
        super(mob, speedModifier, searchRange, 6);
        this.mob = mob;
        this.foodBlockPredicate = foodBlockPredicate;
        // 设置 Goal 的类型，确保它不会与其他移动 Goal 冲突
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /**
     * 检查此 Goal 是否可以开始执行。
     */
    @Override
    public boolean canUse() {
        // 只有当生物没有被骑乘时才能使用此 Goal
        return !mob.isVehicle() && super.canUse();
    }

    /**
     * 检查目标方块是否有效。
     */
    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state != null && this.foodBlockPredicate.test(state.getBlock());
    }

    /**
     * 在 Goal 的每个 tick 执行。
     */
    @Override
    public void tick() {
        super.tick();
        BlockPos targetPos = this.getMoveToTarget();

        // 检查生物是否足够接近目标方块以开始“吃”
        if (this.isReachedTarget()) {
            this.eatingTicks++;
            // 让生物看向目标方块
            mob.getLookControl().setLookAt(Vec3.atCenterOf(targetPos));

            // 播放吃东西的动画或声音 (如果需要)
            // mob.getNavigation().stop(); // 可以让它在吃的时候停下来

            // 假设需要 40 ticks (2秒) 来吃完
            if (this.eatingTicks >= 40) {
                // 吃完后，执行操作
                onFoodEaten(mob.level(), targetPos);
                // 重置 Goal，让它可以寻找下一个目标
                this.stop();
            }
        } else {
            // 如果还没到达，重置计时器
            this.eatingTicks = 0;
        }
    }

    /**
     * 当食物被吃掉时调用。
     */
    protected void onFoodEaten(Level level, BlockPos pos) {
        if (!level.isClientSide) { // 只在服务端执行逻辑
            BlockState blockState = level.getBlockState(pos);

            // 发布一个自定义事件，通知其他系统这个生物吃完了东西
            // 这样，我们的事件监听器就可以处理掉落“谢意”等逻辑
            MobFinishedEatingEvent event = new MobFinishedEatingEvent(this.mob, blockState);
            NeoForge.EVENT_BUS.post(event);

            // 默认行为：移除方块
            level.removeBlock(pos, false);
        }
    }

    @Override
    public void start() {
        super.start();
        this.eatingTicks = 0;
    }

    /**
     * 防止生物在寻找方块时上下抖动。
     */
    @Override
    protected BlockPos getMoveToTarget() {
        return this.blockPos;
    }
}