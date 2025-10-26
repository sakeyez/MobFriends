package com.sake.mobfriends.item;

import com.sake.mobfriends.entity.CombatBlaze;
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class BlazeCore extends Item {
    public BlazeCore(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.sidedSuccess(true);
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        CombatBlaze blaze = ModEntities.COMBAT_BLAZE.get().create(level);
        if (blaze == null) return InteractionResult.FAIL;

        blaze.tame(player);
        blaze.moveTo(context.getClickedPos().above(), context.getRotation(), 0);
        if (level instanceof ServerLevel serverLevel) {
            blaze.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(context.getClickedPos()), MobSpawnType.SPAWN_EGG, null);
            level.addFreshEntity(blaze);

            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_BLAZE_CORE.get());
            activeCore.set(ModDataComponents.BLAZE_UUID.get(), blaze.getUUID());
            player.setItemInHand(context.getHand(), activeCore);

            level.playSound(null, context.getClickedPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}