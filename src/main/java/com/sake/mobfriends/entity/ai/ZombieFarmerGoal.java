package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.ZombieNpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.EnumSet;
import java.util.List;

public class ZombieFarmerGoal extends Goal {

    private final ZombieNpcEntity mob;
    private final double speedModifier;
    private final Level level;
    private final PathNavigation navigation;

    private BlockPos targetPos = null;
    private Task currentTask = Task.NONE;
    private int cooldown = 0;
    private int pathfindingCooldown = 0;

    private enum Task {
        NONE, HARVEST, PLANT, TILL
    }

    public ZombieFarmerGoal(ZombieNpcEntity mob, double speedModifier) {
        this.mob = mob;
        this.level = mob.level();
        this.speedModifier = speedModifier;
        this.navigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            --this.cooldown;
            return false;
        }
        return findAndSetClosestTask();
    }

    @Override
    public boolean canContinueToUse() {
        return this.currentTask != Task.NONE && this.pathfindingCooldown > 0 && this.targetPos != null;
    }

    @Override
    public void start() {
        this.pathfindingCooldown = 300;
        moveToTarget();
    }

    private void moveToTarget() {
        if (this.targetPos != null) {
            this.navigation.moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, this.speedModifier);
        }
    }

    @Override
    public void stop() {
        this.navigation.stop();
        this.targetPos = null;
        this.currentTask = Task.NONE;
        this.cooldown = 10;
    }

    @Override
    public void tick() {
        if (this.targetPos == null) {
            stop();
            return;
        }
        this.pathfindingCooldown--;

        if (this.mob.distanceToSqr(Vec3.atCenterOf(this.targetPos)) < 4.0D) {
            performAndFindNext();
        } else if (this.navigation.isDone() || this.navigation.isStuck()) {
            stop();
        }
    }

    private void performAndFindNext() {
        performTask();
        if (findNextNearbyTask()) {
            this.pathfindingCooldown = 200;
            moveToTarget();
        } else {
            stop();
        }
    }

    private boolean findAndSetClosestTask() {
        int searchRadius = 16;
        int verticalRadius = 2;

        BlockPos mobPos = this.mob.blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        Task bestTask = Task.NONE;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int y = -verticalRadius; y <= verticalRadius; y++) {
            for (int x = -searchRadius; x <= searchRadius; x++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    mutablePos.set(mobPos.getX() + x, mobPos.getY() + y, mobPos.getZ() + z);
                    BlockState state = level.getBlockState(mutablePos);

                    Task candidateTask = Task.NONE;
                    if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
                        candidateTask = Task.HARVEST;
                    } else if (hasSeedInInventory() && state.is(Blocks.FARMLAND) && level.isEmptyBlock(mutablePos.above())) {
                        candidateTask = Task.PLANT;
                    } else if (hasHoeInHand() && (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)) && level.isEmptyBlock(mutablePos.above())) {
                        if (isNearWater(mutablePos, 4)) {
                            candidateTask = Task.TILL;
                        }
                    }

                    if (candidateTask != Task.NONE) {
                        double distSq = this.mob.distanceToSqr(Vec3.atCenterOf(mutablePos));

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

    private boolean findNextNearbyTask() {
        int searchRadius = 2;
        BlockPos mobPos = this.mob.blockPosition();

        if (this.currentTask == Task.HARVEST && hasSeedInInventory()) {
            BlockPos lastTarget = this.targetPos != null ? this.targetPos : mobPos;
            if (level.getBlockState(lastTarget).is(Blocks.FARMLAND) && level.isEmptyBlock(lastTarget.above())) {
                this.targetPos = lastTarget.immutable();
                this.currentTask = Task.PLANT;
                return true;
            }
        }

        for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-searchRadius, -1, -searchRadius), mobPos.offset(searchRadius, 1, searchRadius))) {
            BlockState cropState = level.getBlockState(pos);
            if (cropState.getBlock() instanceof CropBlock crop && crop.isMaxAge(cropState)) {
                this.targetPos = pos.immutable();
                this.currentTask = Task.HARVEST;
                return true;
            }
        }

        if (hasSeedInInventory()) {
            for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-searchRadius, -1, -searchRadius), mobPos.offset(searchRadius, 1, searchRadius))) {
                if (level.getBlockState(pos).is(Blocks.FARMLAND) && level.isEmptyBlock(pos.above())) {
                    this.targetPos = pos.immutable();
                    this.currentTask = Task.PLANT;
                    return true;
                }
            }
        }

        if (hasHoeInHand()) {
            for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-searchRadius, -1, -searchRadius), mobPos.offset(searchRadius, 1, searchRadius))) {
                BlockState groundState = level.getBlockState(pos);
                if ((groundState.is(Blocks.DIRT) || groundState.is(Blocks.GRASS_BLOCK)) && level.isEmptyBlock(pos.above())) {
                    if (isNearWater(pos, 4)) {
                        this.targetPos = pos.immutable();
                        this.currentTask = Task.TILL;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void performTask() {
        if (this.targetPos == null) return;
        this.mob.getLookControl().setLookAt(Vec3.atCenterOf(this.targetPos));
        this.mob.swing(InteractionHand.MAIN_HAND);

        switch (this.currentTask) {
            case HARVEST: {
                BlockState cropState = level.getBlockState(this.targetPos);
                Block block = cropState.getBlock();

                // 确保这确实是一个作物方块
                if (block instanceof CropBlock cropBlock) {
                    if (this.level instanceof ServerLevel serverLevel) {
                        // --- 获取掉落物的逻辑保持不变 ---
                        LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.targetPos))
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .withOptionalParameter(LootContextParams.THIS_ENTITY, this.mob);
                        List<ItemStack> drops = cropState.getDrops(lootBuilder);
                        for (ItemStack drop : drops) {
                            this.mob.getInventory().addItem(drop);
                        }
                    }

                    // --- 【核心修改】 ---
                    // 不再是替换为耕地，而是将作物重置为年龄0的初始状态
                    this.level.setBlock(this.targetPos, cropBlock.getStateForAge(0), 3);

                } else {
                    // 如果因为某些奇怪的原因，目标不是作物了，就直接破坏掉，避免AI卡住
                    this.level.setBlock(this.targetPos, Blocks.AIR.defaultBlockState(), 3);
                }
                break;
            }
            case PLANT: {
                plantSeed(this.targetPos);
                break;
            }
            case TILL: {
                this.level.playSound(null, this.targetPos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.level.setBlock(this.targetPos, Blocks.FARMLAND.defaultBlockState(), 11);
                this.mob.getMainHandItem().hurtAndBreak(1, this.mob, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
                break;
            }
        }
    }

    private boolean isNearWater(BlockPos pos, int radius) {
        for (BlockPos checkPos : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius), pos.offset(radius, 1, radius))) {
            if (level.getFluidState(checkPos).is(Fluids.WATER)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasHoeInHand() {
        return this.mob.getMainHandItem().getItem() instanceof HoeItem;
    }

    private boolean hasSeedInInventory() {
        for(int i = 0; i < this.mob.getInventory().getContainerSize(); ++i) {
            Item item = this.mob.getInventory().getItem(i).getItem();
            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock) {
                return true;
            }
            if (item == Items.POTATO || item == Items.CARROT || item == Items.BEETROOT_SEEDS || item == Items.WHEAT_SEEDS) {
                return true;
            }
        }
        return false;
    }

    private void plantSeed(BlockPos pos) {
        BlockPos plantPos = pos.above();
        if (!level.isEmptyBlock(plantPos)) return;

        for(int i = 0; i < this.mob.getInventory().getContainerSize(); ++i) {
            ItemStack stack = this.mob.getInventory().getItem(i);
            Item item = stack.getItem();
            BlockState seedState = null;

            if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof CropBlock cropBlock) {
                seedState = cropBlock.defaultBlockState();
            } else if (item == Items.WHEAT_SEEDS) {
                seedState = Blocks.WHEAT.defaultBlockState();
            } else if (item == Items.POTATO) {
                seedState = Blocks.POTATOES.defaultBlockState();
            } else if (item == Items.CARROT) {
                seedState = Blocks.CARROTS.defaultBlockState();
            } else if (item == Items.BEETROOT_SEEDS) {
                seedState = Blocks.BEETROOTS.defaultBlockState();
            }

            if (seedState != null) {
                this.level.setBlock(plantPos, seedState, 3);
                stack.shrink(1);
                return;
            }
        }
    }
}