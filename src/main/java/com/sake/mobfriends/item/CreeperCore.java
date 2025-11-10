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
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes; // 【新增】导入

public class CreeperCore extends Item {
    public CreeperCore(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return super.getName(pStack).plainCopy().withStyle(ChatFormatting.LIGHT_PURPLE);
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

            // 【修改】统一的召唤音效和粒子
            // 音效 (传送门)
            level.playSound(null, context.getClickedPos(), SoundEvents. ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
            // 粒子 (附魔)
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    creeper.getX(), creeper.getY() + creeper.getBbHeight() / 2.0, creeper.getZ(),
                    100, // 大量
                    creeper.getBbWidth() / 2.0, creeper.getBbHeight() / 2.0, creeper.getBbWidth() / 2.0,
                    0.1); // 粒子速度

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}