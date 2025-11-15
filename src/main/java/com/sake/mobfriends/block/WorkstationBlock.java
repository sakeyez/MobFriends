package com.sake.mobfriends.block;

// 【【【新增导入】】】
import com.sake.mobfriends.init.ModBlockEntities;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
// (其他导入保持不变)
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class WorkstationBlock extends Block implements EntityBlock {

    // (构造函数 保持不变)
    private final Supplier<? extends EntityType<?>> npcTypeSupplier;
    private final Supplier<Item> tokenItemSupplier;

    public WorkstationBlock(Properties pProperties, Supplier<? extends EntityType<?>> npcType, Supplier<Item> tokenItem) {
        super(pProperties);
        this.npcTypeSupplier = npcType;
        this.tokenItemSupplier = tokenItem;
    }

    // (newBlockEntity 保持不变)
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new WorkstationBlockEntity(pPos, pState);
    }

    // --- 【【【新增 Ticker 注册】】】 ---
    /**
     * 告诉游戏我们的方块实体需要 Ticking
     */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // 我们只在服务器端执行 tick 逻辑
        if (level.isClientSide()) {
            return null;
        }

        // 检查传入的类型是否是我们的工作站方块实体
        if (blockEntityType == ModBlockEntities.WORKSTATION_BLOCK_ENTITY.get()) {
            // 如果是，就返回一个 Ticker，它会调用我们的静态 serverTick 方法
            return (lvl, pos, st, be) -> {
                if (be instanceof WorkstationBlockEntity wbe) {
                    WorkstationBlockEntity.serverTick(lvl, pos, st, wbe);
                }
            };
        }
        return null; // 其他类型的方块实体我们不关心
    }

    // (useItemOn 保持不变)
    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
        if (pLevel.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof WorkstationBlockEntity workstation)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (pPlayer.isShiftKeyDown()) {
            // 【【【修改】】】
            workstation.startShowingRange();
            return ItemInteractionResult.SUCCESS;
        }
        if (pStack.is(this.tokenItemSupplier.get())) {
            workstation.upgradeLevel(pPlayer, pStack);
            return ItemInteractionResult.CONSUME;
        }
        if (pStack.is(Items.TOTEM_OF_UNDYING)) {
            workstation.reviveNpc(pPlayer, this.npcTypeSupplier.get());
            if (!pPlayer.getAbilities().instabuild) {
                pStack.shrink(1);
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 玩家 空手 右键方块 (处理 召唤 和 显示范围)
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, BlockHitResult pHitResult) {
        if (pLevel.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = pLevel.getBlockEntity(pPos);
        if (!(be instanceof WorkstationBlockEntity workstation)) {
            return InteractionResult.PASS;
        }

        // 方案 4 (Shift): 玩家 Shift + 空手右键 -> 显示范围
        if (pPlayer.isShiftKeyDown()) {
            // 【【【修改】】】
            workstation.startShowingRange(); // <--- 从 showRange 改为 startShowingRange
        }
        // 方案 2 (空手): 玩家 空手 + 右键 -> 召唤NPC
        else {
            workstation.spawnNpc(pLevel, pPos, this.npcTypeSupplier.get());
        }
        return InteractionResult.SUCCESS;
    }
}