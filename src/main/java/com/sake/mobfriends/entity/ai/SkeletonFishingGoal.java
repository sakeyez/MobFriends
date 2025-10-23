package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.entity.SkeletonNpcEntity;
import com.sake.mobfriends.util.FishConversionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class SkeletonFishingGoal extends Goal {
    private final SkeletonNpcEntity skeleton;
    private final ServerLevel level;
    private BlockPos fishingPos = null;
    private BlockPos waterPos = null;
    private boolean isFishing = false;
    private int fishingTimer = 0;
    private int cooldown = 0;

    public SkeletonFishingGoal(SkeletonNpcEntity skeleton) {
        this.skeleton = skeleton;
        this.level = (ServerLevel) skeleton.level();
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!(this.skeleton.getMainHandItem().getItem() instanceof FishingRodItem)) {
            return false;
        }

        Optional<BlockPos> optionalPos = findFishingSpot();
        if (optionalPos.isPresent()) {
            this.fishingPos = optionalPos.get();
            // waterPos is set inside findFishingSpot
            return true;
        } else {
            this.cooldown = 100; // Rest if no spot is found
            return false;
        }
    }

    @Override
    public void start() {
        if (this.fishingPos != null) {
            this.skeleton.getNavigation().moveTo(this.fishingPos.getX() + 0.5, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5, 1.0D);
        }
    }

    @Override
    public void tick() {
        if (this.fishingPos == null || this.waterPos == null) {
            stop();
            return;
        }

        this.skeleton.getLookControl().setLookAt(Vec3.atCenterOf(this.waterPos));

        // If skeleton is too far away, keep moving and do nothing else.
        if (this.skeleton.distanceToSqr(Vec3.atCenterOf(this.fishingPos)) > 4.0D) {
            if (this.skeleton.getNavigation().isDone()) {
                this.skeleton.getNavigation().moveTo(this.fishingPos.getX() + 0.5, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5, 1.0D);
            }
            return; // Don't execute fishing logic while moving
        }

        // Stop moving once close enough and start fishing
        this.skeleton.getNavigation().stop();

        if (!isFishing) {
            this.skeleton.swing(InteractionHand.MAIN_HAND);
            this.level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.FISHING_BOBBER_THROW, skeleton.getSoundSource(), 1.0F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F));
            isFishing = true;
            this.fishingTimer = 100 + this.level.getRandom().nextInt(200);
        } else {
            fishingTimer--;
            if (this.level.getRandom().nextInt(10) == 0) {
                this.level.sendParticles(ParticleTypes.SPLASH, this.waterPos.getX() + 0.5 + (this.level.getRandom().nextDouble() - 0.5), this.waterPos.getY() + 1.0, this.waterPos.getZ() + 0.5 + (this.level.getRandom().nextDouble() - 0.5), 1, 0, 0, 0, 0);
            }

            if (fishingTimer <= 0) {
                this.skeleton.swing(InteractionHand.MAIN_HAND);
                this.level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, skeleton.getSoundSource(), 1.0F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F));
                generateLoot();
                stop();
            }
        }
    }

    @Override
    public void stop() {
        this.skeleton.getNavigation().stop();
        this.fishingPos = null;
        this.waterPos = null;
        this.isFishing = false;
        this.fishingTimer = 0;
        this.cooldown = 200 + this.level.getRandom().nextInt(400);
    }

    @Override
    public boolean canContinueToUse() {
        return this.fishingPos != null && this.waterPos != null && this.skeleton.getMainHandItem().getItem() instanceof FishingRodItem;
    }

    private void generateLoot() {
        LootParams lootparams = new LootParams.Builder(this.level)
                .withParameter(LootContextParams.ORIGIN, this.skeleton.position())
                .withParameter(LootContextParams.TOOL, this.skeleton.getMainHandItem())
                .withLuck(0)
                .create(LootContextParamSets.FISHING);

        MinecraftServer server = this.level.getServer();
        if (server == null) return;

        // Correct 1.21.1 way to get loot table
        LootTable loottable = server.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> items = loottable.getRandomItems(lootparams);
        for (ItemStack itemstack : items) {
            FishConversionHelper.processLoot(this.level, itemstack, this.waterPos, this.skeleton);
        }
    }

    /**
     * Ported directly from the 1.20.1 version.
     * Finds a standable position next to water.
     */
    private Optional<BlockPos> findFishingSpot() {
        BlockPos center = this.skeleton.blockPosition();
        // Search in a 17x5x17 area around the skeleton
        for (BlockPos posToStand : BlockPos.betweenClosed(center.offset(-8, -2, -8), center.offset(8, 2, 8))) {
            if (isStandable(posToStand)) {
                // Check adjacent blocks for water
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos adjacentPos = posToStand.relative(direction);
                    BlockPos belowAdjacentPos = adjacentPos.below();

                    // Check for water at the same level or one block below
                    if (level.getFluidState(adjacentPos).is(FluidTags.WATER) || level.getFluidState(belowAdjacentPos).is(FluidTags.WATER)) {
                        this.waterPos = level.getFluidState(belowAdjacentPos).is(FluidTags.WATER) ? belowAdjacentPos.immutable() : adjacentPos.immutable();
                        return Optional.of(posToStand.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isStandable(BlockPos pos) {
        return level.getBlockState(pos.below()).entityCanStandOn(level, pos.below(), skeleton) &&
                level.isEmptyBlock(pos) &&
                level.isEmptyBlock(pos.above());
    }
}