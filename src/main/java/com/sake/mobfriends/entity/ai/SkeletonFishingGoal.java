package com.sake.mobfriends.entity.ai;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.SkeletonNpcEntity;
import com.sake.mobfriends.util.FishConversionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
    private BlockPos fishingPos = null; // 站立钓鱼的目标点
    private BlockPos waterPos = null;   // 目标水面
    private boolean isFishing = false;
    private int fishingTimer = 0;
    private int cooldown = 0;

    // 【新增】标记AI是否处于“坐姿钓鱼”模式
    private boolean isPassengerFishing = false;

    // (鱼竿ID定义保持不变)
    private static final ResourceLocation IRON_ROD_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "iron_fishing_rod");
    private static final ResourceLocation GOLD_ROD_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "gold_fishing_rod");
    private static final ResourceLocation DIAMOND_ROD_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "diamond_fishing_rod");
    private static final ResourceLocation NEPTUNIUM_ROD_ID = ResourceLocation.fromNamespaceAndPath("aquaculture", "neptunium_fishing_rod");

    // (RodStats 内部类保持不变)
    private static class RodStats {
        final int globalCooldown;
        final int minTimer;
        final int maxTimer;
        final float doubleCatchChance;

        RodStats(int cd, int min, int max, float chance) {
            this.globalCooldown = cd;
            this.minTimer = min;
            this.maxTimer = max;
            this.doubleCatchChance = chance;
        }
    }
    private static final RodStats DEFAULT_STATS = new RodStats(20, 10, 15, 0);

    public SkeletonFishingGoal(SkeletonNpcEntity skeleton) {
        this.skeleton = skeleton;
        this.level = (ServerLevel) skeleton.level();
        // 【修改】旗标，允许在坐着时运行
        this.setFlags(EnumSet.of(Flag.LOOK)); // 移除 MOVE 旗标，因为坐着时不能移动
    }

    /**
     * 【【【核心修改：重写 canUse】】】
     */
    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        ItemStack rodStack = this.skeleton.getMainHandItem();
        if (!(rodStack.getItem() instanceof FishingRodItem)) {
            return false;
        }

        // --- 逻辑 1：检查坐姿钓鱼 ---
        if (this.skeleton.isPassenger()) {
            Optional<BlockPos> waterNearby = findWaterNearby(3); // 在3格内找水
            if (waterNearby.isPresent()) {
                this.waterPos = waterNearby.get();
                this.fishingPos = null; // 坐着时不需要站立点
                this.isPassengerFishing = true; // 标记为坐姿模式
                return true;
            }
            // 坐着但没水，不钓鱼
            return false;
        }

        // --- 逻辑 2：检查站立钓鱼 (旧逻辑) ---
        this.isPassengerFishing = false; // 标记为站立模式
        Optional<BlockPos> optionalPos = findFishingSpot();
        if (optionalPos.isPresent()) {
            this.fishingPos = optionalPos.get(); // waterPos 已经在 findFishingSpot 中被设置
            return true;
        }

        // 站着但找不到钓点
        this.cooldown = 100;
        return false;
    }

    /**
     * 【【【核心修改：重写 start】】】
     */
    @Override
    public void start() {
        // 重置计时器
        this.fishingTimer = 0;
        this.isFishing = false;

        // 如果是站立钓鱼，才需要移动
        if (!this.isPassengerFishing && this.fishingPos != null) {
            this.skeleton.getNavigation().moveTo(this.fishingPos.getX() + 0.5, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5, 1.0D);
            // 【新增】重新设置 MOVE 旗标，因为站立时需要移动
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        } else {
            // 如果是坐姿钓鱼，确保没有 MOVE 旗标
            this.setFlags(EnumSet.of(Flag.LOOK));
        }
    }

    /**
     * 【【【核心修改：重写 tick】】】
     */
    @Override
    public void tick() {
        ItemStack currentRod = this.skeleton.getMainHandItem();
        if (this.waterPos == null || !(currentRod.getItem() instanceof FishingRodItem)) {
            stop();
            return;
        }

        // 无论如何都看向水面
        this.skeleton.getLookControl().setLookAt(Vec3.atCenterOf(this.waterPos));

        // --- 逻辑 1：如果是站立钓鱼，处理移动 ---
        if (!this.isPassengerFishing) {
            if (this.fishingPos == null) { // 安全检查
                stop();
                return;
            }
            // 如果还没到站立点，就继续移动
            if (this.skeleton.distanceToSqr(Vec3.atCenterOf(this.fishingPos)) > 4.0D) {
                if (this.skeleton.getNavigation().isDone()) {
                    this.skeleton.getNavigation().moveTo(this.fishingPos.getX() + 0.5, this.fishingPos.getY(), this.fishingPos.getZ() + 0.5, 1.0D);
                }
                return; // 还在移动中，不执行后续钓鱼逻辑
            } else {
                // 到达站立点，停止移动
                this.skeleton.getNavigation().stop();
            }
        }
        // --- 逻辑 2：如果是坐姿钓鱼，直接跳过移动 ---
        // (到达这里，意味着要么在坐着，要么已到达站立点)

        // --- 通用钓鱼逻辑 (和以前一样) ---
        if (!isFishing) {
            this.skeleton.swing(InteractionHand.MAIN_HAND);
            this.level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.FISHING_BOBBER_THROW, skeleton.getSoundSource(), 1.0F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F));

            RodStats stats = this.getStatsForRod(currentRod);

            // (日志代码保持不变)
            MobFriends.LOGGER.info(
                    "[SkeletonFishingGoal] Skeleton at {} starting to fish with: {} (Passenger: {}). Stats: [Cooldown: {}s]",
                    this.skeleton.blockPosition().toString(),
                    currentRod.getHoverName().getString(),
                    this.isPassengerFishing, // 【新增】在日志中显示是否在坐着
                    stats.globalCooldown
            );

            var enchantmentRegistry = this.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            int lureLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    enchantmentRegistry.getHolderOrThrow(Enchantments.LURE),
                    currentRod
            );
            int lureReduction = lureLevel * 100;
            int minTicks = Math.max(20, stats.minTimer * 20 - lureReduction);
            int maxTicks = Math.max(minTicks + 40, stats.maxTimer * 20 - lureReduction);

            this.fishingTimer = this.level.getRandom().nextInt(minTicks, maxTicks);
            isFishing = true;

        } else {
            // (等待逻辑保持不变)
            fishingTimer--;
            if (this.level.getRandom().nextInt(10) == 0) {
                this.level.sendParticles(ParticleTypes.SPLASH, this.waterPos.getX() + 0.5 + (this.level.getRandom().nextDouble() - 0.5), this.waterPos.getY() + 1.0, this.waterPos.getZ() + 0.5 + (this.level.getRandom().nextDouble() - 0.5), 1, 0, 0, 0, 0);
            }

            if (fishingTimer <= 0) {
                this.skeleton.swing(InteractionHand.MAIN_HAND);
                this.level.playSound(null, skeleton.getX(), skeleton.getY(), skeleton.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, skeleton.getSoundSource(), 1.0F, 0.4F / (this.level.getRandom().nextFloat() * 0.4F + 0.8F));
                generateLoot(currentRod);
                stop();
            }
        }
    }

    /**
     * 【【【核心修改：重写 canContinueToUse】】】
     */
    @Override
    public boolean canContinueToUse() {
        // 必须有水
        if (this.waterPos == null || !(this.skeleton.getMainHandItem().getItem() instanceof FishingRodItem)) {
            return false;
        }

        // 如果是坐姿模式，必须保持坐姿
        if (this.isPassengerFishing) {
            return this.skeleton.isPassenger();
        }
        // 如果是站立模式，必须还有站立点
        else {
            return this.fishingPos != null;
        }
    }

    /**
     * 【【【核心修改：重写 stop】】】
     */
    @Override
    public void stop() {
        // 站立模式才需要停止寻路
        if (!this.isPassengerFishing) {
            this.skeleton.getNavigation().stop();
        }

        ItemStack currentRod = this.skeleton.getMainHandItem();
        RodStats stats = this.getStatsForRod(currentRod);
        this.cooldown = stats.globalCooldown * 20 + this.level.getRandom().nextInt(100);

        // 重置所有状态
        this.fishingPos = null;
        this.waterPos = null;
        this.isFishing = false;
        this.fishingTimer = 0;
        this.isPassengerFishing = false; // 重置模式
    }

    // (generateLoot, getStatsForRod, isStandable 保持不变)
    private void generateLoot(ItemStack rodStack) {
        LootParams lootparams = new LootParams.Builder(this.level)
                .withParameter(LootContextParams.ORIGIN, this.skeleton.position())
                .withParameter(LootContextParams.TOOL, rodStack)
                .withLuck(0)
                .create(LootContextParamSets.FISHING);
        MinecraftServer server = this.level.getServer();
        if (server == null) return;
        LootTable loottable = server.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);

        List<ItemStack> items = loottable.getRandomItems(lootparams);
        boolean caughtSomething = false;
        for (ItemStack itemstack : items) {
            FishConversionHelper.processLoot(this.level, itemstack, this.waterPos, this.skeleton);
            caughtSomething = true;
        }

        if (caughtSomething) {
            rodStack.hurtAndBreak(1, this.skeleton, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
        }

        RodStats stats = this.getStatsForRod(rodStack);
        if (stats.doubleCatchChance > 0 && this.level.getRandom().nextFloat() * 100.0f < stats.doubleCatchChance) {
            List<ItemStack> extraItems = loottable.getRandomItems(lootparams);
            boolean caughtExtra = false;
            for (ItemStack extraStack : extraItems) {
                FishConversionHelper.processLoot(this.level, extraStack, this.waterPos, this.skeleton);
                caughtExtra = true;
            }
            if (caughtExtra) {
                rodStack.hurtAndBreak(1, this.skeleton, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
            }
        }
    }

    private RodStats getStatsForRod(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FishingRodItem)) {
            return DEFAULT_STATS;
        }
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);

        if (id.equals(IRON_ROD_ID)) {
            return new RodStats(10, 8, 12, 3);
        }
        if (id.equals(GOLD_ROD_ID)) {
            return new RodStats(8, 6, 10, 10);
        }
        if (id.equals(DIAMOND_ROD_ID)) {
            return new RodStats(5, 5, 8, 10);
        }
        if (id.equals(NEPTUNIUM_ROD_ID)) {
            return new RodStats(2, 2, 8, 20);
        }
        if (id.equals(BuiltInRegistries.ITEM.getKey(Items.FISHING_ROD))) {
            return DEFAULT_STATS;
        }
        return DEFAULT_STATS;
    }

    private boolean isStandable(BlockPos pos) {
        return level.getBlockState(pos.below()).entityCanStandOn(level, pos.below(), skeleton) &&
                level.isEmptyBlock(pos) &&
                level.isEmptyBlock(pos.above());
    }

    /**
     * 【【【新增方法】】】
     * 检查骷髅附近指定半径内是否有水
     */
    private Optional<BlockPos> findWaterNearby(int radius) {
        BlockPos center = this.skeleton.blockPosition();

        // 搜索骷髅周围 (radius)x(radius) 的区域
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                // 找到了水，返回这个水方块的位置
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }

    // (旧的 findFishingSpot 保持不变)
    private Optional<BlockPos> findFishingSpot() {
        BlockPos center = this.skeleton.blockPosition();
        for (BlockPos posToStand : BlockPos.betweenClosed(center.offset(-8, -2, -8), center.offset(8, 2, 8))) {
            if (isStandable(posToStand)) {
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos adjacentPos = posToStand.relative(direction);
                    BlockPos belowAdjacentPos = adjacentPos.below();
                    if (level.getFluidState(adjacentPos).is(FluidTags.WATER) || level.getFluidState(belowAdjacentPos).is(FluidTags.WATER)) {
                        this.waterPos = level.getFluidState(belowAdjacentPos).is(FluidTags.WATER) ? belowAdjacentPos.immutable() : adjacentPos.immutable();
                        return Optional.of(posToStand.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }
}