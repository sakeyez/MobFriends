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

public class ZombieCore extends AbstractCoreItem {

    public ZombieCore(Properties properties) {
        super(properties);
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

        // --- 【核心修正：驯服僵尸】 ---
        zombie.tame(player); // 将召唤者设置为主人

        zombie.moveTo(context.getClickedPos().above(), context.getRotation(), 0);
        if (level instanceof ServerLevel serverLevel) {
            zombie.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(context.getClickedPos()), MobSpawnType.SPAWN_EGG, null);
            level.addFreshEntity(zombie);

            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_ZOMBIE_CORE.get());
            setZombieUUID(activeCore, zombie.getUUID());
            player.setItemInHand(context.getHand(), activeCore);

            // 音效
            level.playSound(null, context.getClickedPos(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            // 粒子
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, zombie.getX(), zombie.getY() + 1.5, zombie.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}