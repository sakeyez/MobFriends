package com.sake.mobfriends.item;

import com.sake.mobfriends.entity.CombatCreeper;
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

public class CreeperCore extends Item {
    public CreeperCore(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.sidedSuccess(true);
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        CombatCreeper creeper = ModEntities.COMBAT_CREEPER.get().create(level);
        if (creeper == null) return InteractionResult.FAIL;

        creeper.tame(player);
        creeper.moveTo(context.getClickedPos().above(), context.getRotation(), 0);
        if (level instanceof ServerLevel serverLevel) {
            creeper.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(context.getClickedPos()), MobSpawnType.SPAWN_EGG, null);
            level.addFreshEntity(creeper);

            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_CREEPER_CORE.get());
            activeCore.set(ModDataComponents.CREEPER_UUID.get(), creeper.getUUID());
            player.setItemInHand(context.getHand(), activeCore);

            level.playSound(null, context.getClickedPos(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}