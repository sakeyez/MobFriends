package com.sake.mobfriends.item;

import com.sake.mobfriends.entity.CombatBlaze;
import com.sake.mobfriends.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.UUID;

public class ActiveBlazeCore extends Item {
    public ActiveBlazeCore(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide() || context.getClickedFace() != Direction.UP) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        CompoundTag storedNBT = stack.get(ModDataComponents.STORED_BLAZE_NBT.get());
        UUID storedUUID = stack.get(ModDataComponents.BLAZE_UUID.get());

        if (storedNBT != null && storedUUID != null && level instanceof ServerLevel serverLevel) {
            Entity entity = EntityType.loadEntityRecursive(storedNBT, level, e -> {
                e.setUUID(storedUUID);
                e.setPos(Vec3.atCenterOf(context.getClickedPos().above()));
                return e;
            });
            if (entity != null) {
                serverLevel.addFreshEntity(entity);
                stack.remove(ModDataComponents.STORED_BLAZE_NBT.get());
                level.playSound(null, context.getClickedPos(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
        }
        return super.useOn(context);
    }

    public static void storeBlaze(ItemStack stack, CombatBlaze blaze) {
        if (blaze.isAlive()) {
            CompoundTag nbt = blaze.saveWithoutId(new CompoundTag());
            nbt.putString("id", EntityType.getKey(blaze.getType()).toString());
            stack.set(ModDataComponents.STORED_BLAZE_NBT.get(), nbt);
        }
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return pStack.has(ModDataComponents.STORED_BLAZE_NBT.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(ModDataComponents.STORED_BLAZE_NBT.get())) {
            tooltip.add(Component.translatable("tooltip.mob_friends.active_zombie_core.stored").withStyle(ChatFormatting.BLUE));
        } else {
            tooltip.add(Component.translatable("tooltip.mob_friends.active_zombie_core.linked").withStyle(ChatFormatting.GREEN));
        }
    }
}