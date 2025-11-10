package com.sake.mobfriends.item;

import com.sake.mobfriends.entity.CombatZombie;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ZombieCore extends AbstractCoreItem {

    public ZombieCore(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return super.getName(pStack).plainCopy().withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        CombatZombie zombie = ModEntities.COMBAT_ZOMBIE.get().create(level);
        if (zombie == null) {
            return InteractionResult.FAIL;
        }

        zombie.tame(player);
        zombie.moveTo(context.getClickedPos().above(), context.getRotation(), 0);

        if (level instanceof ServerLevel serverLevel) {
            zombie.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(context.getClickedPos()), MobSpawnType.SPAWN_EGG, null);
            level.addFreshEntity(zombie);

            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_ZOMBIE_CORE.get());
            setZombieUUID(activeCore, zombie.getUUID());
            player.setItemInHand(context.getHand(), activeCore);

            // 【修改】统一的召唤音效和粒子
            // 音效 (传送门)
            level.playSound(null, context.getClickedPos(), SoundEvents. ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
            // 粒子 (附魔)
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    zombie.getX(), zombie.getY() + zombie.getBbHeight() / 2.0, zombie.getZ(),
                    100, // 大量
                    zombie.getBbWidth() / 2.0, zombie.getBbHeight() / 2.0, zombie.getBbWidth() / 2.0,
                    0.1); // 粒子速度

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}