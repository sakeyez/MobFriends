package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.BlazeNpcEntity;
import com.sake.mobfriends.util.CreateCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumSet;

/**
 * 【【【V6版：实现全部新功能】】】
 * 1. 修复：只检查 SMOULDERING 和 KINDLED
 * 2. 新功能：10秒冷却，1 tick 连锁搜寻
 * 3. 新功能：搜寻范围改为 5x6x5
 * 4. 新功能：燃烧时间改为 7200 ticks (6分钟)
 */
public class BlazeRefuelGoal extends Goal {

    private final BlazeNpcEntity mob;
    private final double speedModifier;
    private final Level level;
    private final PathNavigation navigation;

    private BlockPos targetPos = null;
    private int cooldown = 0; // 【修改】现在由 canUse 和 stop 控制
    private int pathfindingCooldown = 0;
    private int workTimer = 0;

    private final boolean isCreateLoaded;

    public BlazeRefuelGoal(BlazeNpcEntity mob, double speedModifier) {
        this.mob = mob;
        this.level = mob.level();
        this.speedModifier = speedModifier;
        this.navigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));

        this.isCreateLoaded = net.neoforged.fml.ModList.get().isLoaded("create");
    }

    /**
     * 【【【修改：实现功能 1 (搜寻逻辑)】】】
     */
    @Override
    public boolean canUse() {
        if (!this.isCreateLoaded) {
            return false;
        }

        // 1. 如果在冷却中，则不搜寻
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }

        // 2. 尝试寻找目标
        if (!findAndSetClosestTask()) {
            // 3. 【如果没找到】，设置 10 秒长冷却
            this.cooldown = 200; // 10 seconds
            return false;
        }

        // 4. 【如果找到了】，立即执行 (cooldown 保持 0)
        return true;
    }

    /**
     * 【【【修复：canContinueToUse】】】
     * 使用 CreateCompat.isBurnerInactive()，它现在只检查 SMOULDERING 和 KINDLED
     */
    @Override
    public boolean canContinueToUse() {
        if (this.targetPos == null || this.pathfindingCooldown <= 0 || !this.isCreateLoaded) {
            return false;
        }
        BlockState state = this.level.getBlockState(this.targetPos);
        // 调用我们修复后的辅助方法 (SMOULDERING 或 KINDLED)
        return CreateCompat.isBurnerInactive(state);
    }

    @Override
    public void start() {
        this.pathfindingCooldown = 300;
        this.workTimer = 0;
        moveToTarget();
    }

    private void moveToTarget() {
        if (this.targetPos != null) {
            this.navigation.moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 0.5D, this.targetPos.getZ() + 0.5D, this.speedModifier);
        }
    }

    /**
     * 【【【修改：实现功能 1 (连锁搜寻)】】】
     */
    @Override
    public void stop() {
        this.navigation.stop();
        this.targetPos = null;
        this.workTimer = 0;
        // 【修改】任务结束后，设置 1 tick 冷却，强制 canUse() 立即重新搜寻
        this.cooldown = 1;
    }

    @Override
    public void tick() {
        if (this.targetPos == null) {
            stop();
            return;
        }
        this.pathfindingCooldown--;

        this.mob.getLookControl().setLookAt(Vec3.atCenterOf(this.targetPos));

        double dX = this.mob.getX() - (this.targetPos.getX() + 0.5D);
        double dZ = this.mob.getZ() - (this.targetPos.getZ() + 0.5D);
        double dY = this.mob.getY() - (this.targetPos.getY() + 0.5D);

        double horizontalDistSqr = dX * dX + dZ * dZ;
        boolean isVerticallyAligned = dY > -6.0D && dY < 1.0D; // (Y轴交互修复，保持不变)

        if (horizontalDistSqr < 4.0D && isVerticallyAligned) {
            performTask();
        } else if (this.navigation.isDone() || this.navigation.isStuck()) {
            // 寻路失败，不要连锁搜寻，进入10秒冷却
            this.cooldown = 200;
            stop(); // 调用 stop()，但 cooldown 会被上面的 200 覆盖
        }
    }

    /**
     * 【【【修改：实现功能 3 (燃烧时间)】】】
     */
    private void performTask() {
        this.workTimer++;
        if (this.workTimer < 10) {
            if (this.level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FLAME, this.mob.getRandomX(0.5D), this.mob.getY() + 0.5D, this.mob.getRandomZ(0.5D), 1, 0, 0, 0, 0);
            }
            return;
        }

        if (this.level instanceof ServerLevel serverLevel) {
            BlockState state = level.getBlockState(this.targetPos);

            // 1. 确认方块是“可点燃”状态 (SMOULDERING 或 KINDLED)
            if (CreateCompat.isBurnerInactive(state)) {

                // 2. 强制将状态设置为 SEETHING
                level.setBlock(this.targetPos, state.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SEETHING), 3);

                // 3. 获取方块实体
                BlockEntity be = level.getBlockEntity(this.targetPos);

                // 4. 修改NBT
                if (be instanceof BlazeBurnerBlockEntity) {
                    CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());

                    nbt.putInt("fuelLevel", 2); // 2 = FuelType.SPECIAL

                    nbt.putInt("burnTimeRemaining", 2400); //
                    nbt.putBoolean("isCreative", false);

                    be.loadWithComponents(nbt, level.registryAccess());
                    be.setChanged();
                    serverLevel.getChunkSource().blockChanged(this.targetPos);
                }

                // 5. 播放效果
                serverLevel.playSound(null, this.targetPos, SoundEvents.BLAZE_SHOOT, this.mob.getSoundSource(), 1.0F, 1.2F);
                serverLevel.sendParticles(ParticleTypes.FLAME, this.targetPos.getX() + 0.5, this.targetPos.getY() + 1.0, this.targetPos.getZ() + 0.5, 20, 0.2, 0.1, 0.2, 0.1);
                this.mob.swing(InteractionHand.MAIN_HAND);
            }
        }

        stop(); // 触发 stop()，cooldown 设为 1，立即开始下一次搜寻
    }

    /**
     * 【【【修改：实现功能 2 (搜寻范围)】】】
     */
    private boolean findAndSetClosestTask() {
        // 【修改】X/Z 半径为 5
        int searchRadius = 5;
        // 【修改】Y 范围 -2 到 +3 (总共 6 格高)
        int verticalSearchDown = 2;
        int verticalSearchUp = 3;

        BlockPos mobPos = this.mob.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = -verticalSearchDown; y <= verticalSearchUp; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);
                    BlockState state = level.getBlockState(mutablePos);

                    // 调用我们修复后的辅助方法 (SMOULDERING 或 KINDLED)
                    if (this.isCreateLoaded && CreateCompat.isBurnerInactive(state)) {

                        double distSq = this.mob.distanceToSqr(Vec3.atCenterOf(mutablePos));
                        if (distSq < bestDistSq) {
                            if (this.navigation.createPath(mutablePos, 1) != null) {
                                bestDistSq = distSq;
                                bestPos = mutablePos.immutable();
                            }
                        }
                    }
                }
            }
        }

        if (bestPos != null) {
            this.targetPos = bestPos;
            return true;
        }

        return false;
    }
}