package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.CreeperNpcEntity;
import com.sake.mobfriends.util.CreateCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer; // 【【【新增导入】】】
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CreeperEngineerGoal extends Goal {

    private final CreeperNpcEntity creeper;
    private final double speedModifier;
    private final PathNavigation navigation;

    private int workTimer = 0;
    private Task currentTask = Task.NONE;
    private BlockPos targetPos = null;

    private int cooldown = 0;
    private int pathfindingCooldown = 0;

    private ItemStack woodToPlaceCache = ItemStack.EMPTY;

    private enum Task {
        NONE, STRIP_LOG, CRAFT_CASING,PLACE_LOG
    }

    public CreeperEngineerGoal(CreeperNpcEntity creeper, double speedModifier) {
        this.creeper = creeper;
        this.speedModifier = speedModifier;
        this.navigation = creeper.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ... canUse(), canContinueToUse(), start(), moveToTarget(), stop() ...
    // ... (这些方法保持不变) ...

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }

        this.woodToPlaceCache = getWoodToPlaceFromInventory();

        if (!findAndSetClosestTask()) {
            this.cooldown = 20;
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.currentTask != Task.NONE && this.pathfindingCooldown > 0 && this.targetPos != null;
    }

    @Override
    public void start() {
        this.pathfindingCooldown = 300;
        this.workTimer = 0;
        moveToTarget();
    }

    private void moveToTarget() {
        if (this.targetPos == null) return;

        double targetY;

        if (this.currentTask == Task.PLACE_LOG) {
            // 任务是[放置]
            targetY = this.targetPos.getY();
        } else {
            //
            targetY = this.targetPos.getY() - 1.0D;
        }

        this.navigation.moveTo(
                this.targetPos.getX() + 0.5D,
                targetY,
                this.targetPos.getZ() + 0.5D,
                this.speedModifier
        );
    }

    @Override
    public void stop() {
        this.navigation.stop();
        this.targetPos = null;
        this.currentTask = Task.NONE;
        this.workTimer = 0;
        this.cooldown = 10;
        this.woodToPlaceCache = ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        if (this.targetPos == null) {
            stop();
            return;
        }
        this.pathfindingCooldown--;

        this.creeper.getLookControl().setLookAt(
                (double)this.targetPos.getX() + 0.5D,
                (double)this.targetPos.getY() + 1.0D,
                (double)this.targetPos.getZ() + 0.5D
        );

        Vec3 targetCenter = Vec3.atCenterOf(this.targetPos);
        Vec3 mobPos = this.creeper.position();

        double dx = mobPos.x() - targetCenter.x();
        double dz = mobPos.z() - targetCenter.z();
        double horizontalDistSqr = (dx * dx) + (dz * dz); // 水平距离

        double dy = mobPos.y() - this.targetPos.getY();
        boolean isVerticallyAligned = (dy >= -1 && dy < 3.0D); // Y轴对齐

        // (这一段工作检查逻辑是正确的，不需要修改)
        if (horizontalDistSqr < 4.0D && isVerticallyAligned) {
            this.navigation.stop();

            if (this.workTimer == 0) {
                if (this.currentTask == Task.PLACE_LOG) {
                    this.creeper.playSound(SoundEvents.WOOD_PLACE, 1.0F, 1.0F);
                    this.workTimer = 5;
                } else {
                    this.creeper.playSound(SoundEvents.CREEPER_PRIMED, 1.0F, 0.8F);
                    this.workTimer = 15;
                }
            }

            if (this.currentTask != Task.PLACE_LOG && this.creeper.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        (double)this.targetPos.getX() + 0.5D,
                        (double)this.targetPos.getY() + 1.2D,
                        (double)this.targetPos.getZ() + 0.5D,
                        3, 0.2, 0.1, 0.2, 0.0);
            }

            this.workTimer--;

            if (this.workTimer <= 0) {
                performAndFindNext();
            }

        } else if (this.navigation.isDone() || this.navigation.isStuck()) {
            stop();
        }
    }


    private void performAndFindNext() {
        performWork();

        // 【【【修改：在小范围搜索前，也要更新背包缓存】】】
        this.woodToPlaceCache = getWoodToPlaceFromInventory();

        if (findNextNearbyTask()) {
            this.pathfindingCooldown = 200;
            this.workTimer = 0;
            moveToTarget();
        } else {
            stop();
        }
    }

    /**
     * 【【【新增：背包检查辅助方法】】】
     */
    private ItemStack getWoodToPlaceFromInventory() {
        SimpleContainer inventory = this.creeper.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (CreateCompat.PLACEABLE_LOGS.contains(stack.getItem())) {
                return stack; // 返回找到的第一个木头物品
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 【【【修改：使用缓存的 woodToPlaceCache】】】
     */
    private boolean findAndSetClosestTask() {
        int searchRadius = 16;
        int verticalRadius = 4;

        BlockPos mobPos = this.creeper.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        Task bestTask = Task.NONE;

        // 检查背包状态 (已在 canUse() 中缓存)
        boolean hasWood = !this.woodToPlaceCache.isEmpty();

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);

                    // 传入背包状态
                    Task candidateTask = getTaskAt(this.creeper.level(), mutablePos, hasWood);

                    if (candidateTask != Task.NONE) {
                        double distSq = this.creeper.distanceToSqr(Vec3.atCenterOf(mutablePos));
                        if (distSq < bestDistSq) {
                            if (this.navigation.createPath(mutablePos, 1) != null) {
                                bestDistSq = distSq;
                                bestPos = mutablePos.immutable();
                                bestTask = candidateTask;
                            }
                        }
                    }
                }
            }
        }

        if (bestPos != null) {
            this.targetPos = bestPos;
            this.currentTask = bestTask;
            return true;
        }

        return false;
    }

    /**
     * 【【【修改：使用缓存的 woodToPlaceCache】】】
     */
    private boolean findNextNearbyTask() {
        int searchRadius = 2;
        BlockPos searchCenter = (this.targetPos != null) ? this.targetPos : this.creeper.blockPosition();

        // 检查背包状态 (已在 performAndFindNext() 中缓存)
        boolean hasWood = !this.woodToPlaceCache.isEmpty();

        for (BlockPos pos : BlockPos.betweenClosed(searchCenter.offset(-searchRadius, -1, -searchRadius), searchCenter.offset(searchRadius, 1, searchRadius))) {
            // 传入背包状态
            Task candidateTask = getTaskAt(this.creeper.level(), pos, hasWood);
            if (candidateTask != Task.NONE) {
                this.targetPos = pos.immutable();
                this.currentTask = candidateTask;
                return true;
            }
        }
        return false;
    }


    /**
     * 【【【核心修改：getTaskAt 现在需要背包状态】】】
     */
    private Task getTaskAt(LevelReader pLevel, BlockPos pPos, boolean hasWoodToPlace) {
        BlockState baseState = pLevel.getBlockState(pPos);
        BlockState aboveState = pLevel.getBlockState(pPos.above());
        Block baseBlock = baseState.getBlock();
        Block aboveBlock = aboveState.getBlock();

        // 任务1：造机壳 (去皮木头 在 对应机壳上) - 有成本
        if (CreateCompat.CASING_CRAFTING_MAP.containsKey(baseBlock) && CreateCompat.STRIPPED_LOGS.contains(aboveBlock)) {
            Item requiredItem = CreateCompat.CASING_COST_MAP.get(baseBlock);
            if (requiredItem == null || requiredItem == Items.AIR) {
                return Task.CRAFT_CASING;
            }
            // 检查背包！
            boolean hasItem = false;
            SimpleContainer inventory = this.creeper.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                if (inventory.getItem(i).is(requiredItem)) {
                    hasItem = true;
                    break;
                }
            }
            if (hasItem) {
                return Task.CRAFT_CASING;
            }
        }

        // 任务2：去皮 (木头 在 机壳上) - 无成本
        if (CreateCompat.ALL_CASINGS.contains(baseBlock) && CreateCompat.STRIPPING_MAP.containsKey(aboveBlock)) {
            return Task.STRIP_LOG;
        }

        // 【【【新增任务 3】】】
        // 任务3: 放置木头 (如果背包有木头，且机壳上方是空的)
        if (hasWoodToPlace && CreateCompat.ALL_CASINGS.contains(baseBlock) && aboveState.isAir()) {
            return Task.PLACE_LOG;
        }

        return Task.NONE;
    }
    /**
     * 【【【核心修改：添加 PLACE_LOG 的工作逻辑】】】
     */
    private void performWork() {
        if (this.creeper.level().isClientSide()) return;

        BlockPos basePos = this.targetPos;
        BlockPos targetPos = this.targetPos.above(); // 要操作的方块

        BlockState baseState = this.creeper.level().getBlockState(basePos);
        BlockState aboveState = this.creeper.level().getBlockState(targetPos);

        boolean success = false;

        if (this.currentTask == Task.PLACE_LOG) {
            // 再次从缓存中获取要放置的木头 (canUse/findNext 已经设置了它)
            ItemStack woodToPlace = this.woodToPlaceCache;

            // 再次检查，确保背包里还有，并且目标地点是空的
            if (!woodToPlace.isEmpty() && aboveState.isAir()) {
                Item woodItem = woodToPlace.getItem();
                Block woodBlock = Block.byItem(woodItem);

                if (woodBlock != Blocks.AIR) {
                    // 消耗背包里的物品
                    this.creeper.getInventory().removeItemType(woodItem, 1);

                    // 准备放置的方块状态
                    BlockState woodState = woodBlock.defaultBlockState();
                    // 尝试设置为垂直放置 (Y轴)
                    if (woodState.hasProperty(RotatedPillarBlock.AXIS)) {
                        woodState = woodState.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                    }

                    // 放置方块
                    this.creeper.level().setBlock(targetPos, woodState, 3);
                    success = true;
                    // (音效已经在 tick() 里播放了)
                }
            }
        }
        else if (this.currentTask == Task.STRIP_LOG) {
            Block strippedBlock = CreateCompat.STRIPPING_MAP.get(aboveState.getBlock());
            if (strippedBlock != null) {
                BlockState newState = strippedBlock.defaultBlockState();
                if (aboveState.hasProperty(BlockStateProperties.AXIS) && newState.hasProperty(BlockStateProperties.AXIS)) {
                    newState = newState.setValue(BlockStateProperties.AXIS, aboveState.getValue(BlockStateProperties.AXIS));
                }
                this.creeper.level().setBlock(targetPos, newState, 3);
                success = true;
            }
        } else if (this.currentTask == Task.CRAFT_CASING) {
            Block casingBlock = CreateCompat.CASING_CRAFTING_MAP.get(baseState.getBlock());
            Item costItem = CreateCompat.CASING_COST_MAP.get(baseState.getBlock());

            boolean hasItem = false;
            if (costItem == null || costItem == Items.AIR) {
                hasItem = true;
            } else {
                SimpleContainer inventory = this.creeper.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (inventory.getItem(i).is(costItem)) {
                        hasItem = true;
                        break;
                    }
                }
            }

            if (casingBlock != null && baseState.getBlock() == casingBlock && hasItem) {

                this.creeper.level().setBlock(targetPos, casingBlock.defaultBlockState(), 3);
                success = true;

                if (costItem != null && costItem != Items.AIR) {
                    if (this.creeper.level().random.nextFloat() < 0.20F) {
                        this.creeper.getInventory().removeItemType(costItem, 1);
                        this.creeper.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.5F);
                    }
                }
            }
        }

        // 制造的音效和粒子
        if (success && this.currentTask != Task.PLACE_LOG) {
            this.creeper.playSound(SoundEvents.ANVIL_USE, 0.8F, 1.2F);
            if (this.creeper.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        (double)targetPos.getX() + 0.5D,
                        (double)targetPos.getY() + 0.5D,
                        (double)targetPos.getZ() + 0.5D,
                        10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }
}