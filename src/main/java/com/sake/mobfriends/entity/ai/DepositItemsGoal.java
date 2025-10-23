package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.ZombieNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class DepositItemsGoal extends MoveToBlockGoal {

    private final ZombieNpcEntity mob;
    private int depositCooldown = 0;
    private boolean hasDeposited = false;

    public DepositItemsGoal(ZombieNpcEntity mob, double speedModifier) {
        super(mob, speedModifier, 16, 8);
        this.mob = mob;
        this.verticalSearchStart = -1;
    }

    /**
     * 决定AI是否应该启动。
     */
    @Override
    public boolean canUse() {
        // 如果正在冷却中，则不启动
        if (depositCooldown > 0) {
            depositCooldown--;
            return false;
        }
        // 【核心修改】如果背包是空的，就不需要存东西
        if (this.mob.getInventory().isEmpty()) {
            return false;
        }
        // 调用父类的方法来寻找附近是否有箱子
        return super.canUse();
    }

    /**
     * 决定AI是否应该继续执行。
     */
    @Override
    public boolean canContinueToUse() {
        // 只要还没存完东西，并且目标箱子仍然有效，就继续
        return !hasDeposited && super.canContinueToUse();
    }

    @Override
    public void start() {
        this.hasDeposited = false;
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        // 任务结束后，设置一个冷却时间，避免过于频繁地寻找箱子
        depositCooldown = 200; // 10秒
    }

    @Override
    public void tick() {
        // 确保目标有效
        if (!this.isValidTarget(this.mob.level(), this.blockPos)) {
            stop();
            return;
        }
        super.tick();

        // 当到达箱子旁边时
        if (this.isReachedTarget()) {
            BlockEntity blockEntity = this.mob.level().getBlockEntity(this.blockPos);
            if (blockEntity instanceof ChestBlockEntity chest) {
                playChestAnimation(chest, true); // 打开箱子动画
                depositAllItems(chest);          // 执行存款逻辑
                playChestAnimation(chest, false);// 关闭箱子动画
                this.hasDeposited = true;        // 标记为已存款，以便 stop() 被调用
            }
            stop(); // 无论成功与否，到达后就结束本次任务
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        // 目标必须是一个箱子实体
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof ChestBlockEntity;
    }

    /**
     * 【核心修改】简化的存款逻辑。
     * 遍历僵尸背包，将所有物品尝试移入箱子。
     */
    private void depositAllItems(Container chest) {
        SimpleContainer mobInventory = this.mob.getInventory();
        for (int i = 0; i < mobInventory.getContainerSize(); i++) {
            ItemStack stackInMob = mobInventory.getItem(i);
            if (!stackInMob.isEmpty()) {
                // 将物品转移到箱子，并获取无法存入的剩余部分
                ItemStack remainingStack = transferToContainer(chest, stackInMob);
                // 用剩余部分更新僵尸的背包
                mobInventory.setItem(i, remainingStack);
            }
        }
    }

    /**
     * 辅助方法：将一个物品堆转移到目标容器中。
     * @param destination 目标容器（箱子）
     * @param stackToTransfer 要转移的物品堆
     * @return 无法转移的剩余物品堆
     */
    private ItemStack transferToContainer(Container destination, ItemStack stackToTransfer) {
        ItemStack remaining = stackToTransfer.copy();

        // 阶段1: 尝试与现有物品堆叠
        for (int i = 0; i < destination.getContainerSize(); ++i) {
            if (remaining.isEmpty()) break;
            ItemStack stackInSlot = destination.getItem(i);
            if (ItemStack.isSameItemSameComponents(stackInSlot, remaining) && stackInSlot.isStackable() && stackInSlot.getCount() < stackInSlot.getMaxStackSize()) {
                int transferable = Math.min(remaining.getCount(), stackInSlot.getMaxStackSize() - stackInSlot.getCount());
                if (transferable > 0) {
                    remaining.shrink(transferable);
                    stackInSlot.grow(transferable);
                }
            }
        }

        // 阶段2: 尝试放入空格子
        for (int i = 0; i < destination.getContainerSize(); ++i) {
            if (remaining.isEmpty()) break;
            if (destination.getItem(i).isEmpty()) {
                destination.setItem(i, remaining.copy());
                remaining.setCount(0);
            }
        }

        return remaining;
    }

    private void playChestAnimation(ChestBlockEntity chest, boolean open) {
        Level level = this.mob.level();
        level.blockEvent(this.blockPos, chest.getBlockState().getBlock(), 1, open ? 1 : 0);
    }
}