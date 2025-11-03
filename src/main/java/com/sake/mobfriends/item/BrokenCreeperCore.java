package com.sake.mobfriends.item;

import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.UUID;

public class BrokenCreeperCore extends Item {
    public BrokenCreeperCore(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
        ItemStack heldItem = pPlayer.getItemInHand(pUsedHand);
        ItemStack offhandItem = pPlayer.getItemInHand(InteractionHand.OFF_HAND);

        if (offhandItem.is(Items.TOTEM_OF_UNDYING)) {
            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_CREEPER_CORE.get());
            CompoundTag creeperData = heldItem.get(ModDataComponents.STORED_CREEPER_NBT.get());

            if (creeperData != null) {
                // 【核心修复】在转移数据前，手动将生命值重置为一个正数！
                creeperData.putFloat("Health", 20.0F);
                activeCore.set(ModDataComponents.STORED_CREEPER_NBT.get(), creeperData.copy());
            }

            UUID creeperUUID = heldItem.get(ModDataComponents.CREEPER_UUID.get());
            if (creeperUUID != null) {
                activeCore.set(ModDataComponents.CREEPER_UUID.get(), creeperUUID);
            }

            if (pLevel.isClientSide()) {
                Minecraft.getInstance().gameRenderer.displayItemActivation(activeCore);
            }
            if (!pLevel.isClientSide()) {
                if (!pPlayer.getAbilities().instabuild) {
                    offhandItem.shrink(1);
                }
                pLevel.playSound(null, pPlayer.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
            return InteractionResultHolder.sidedSuccess(activeCore, pLevel.isClientSide());
        }
        return InteractionResultHolder.fail(heldItem);
    }

    @Override
    public Component getName(ItemStack pStack) {
        // .plainCopy() 获取 lang 文件中的原始文本
        // .withStyle(ChatFormatting.DARK_GRAY) 将其设为暗灰色
        return super.getName(pStack).plainCopy().withStyle(ChatFormatting.DARK_GRAY);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        pTooltipComponents.add(Component.translatable("tooltip.mob_friends.broken_zombie_core").withStyle(ChatFormatting.RED));
        pTooltipComponents.add(Component.translatable("tooltip.mob_friends.broken_zombie_core_repair").withStyle(ChatFormatting.GRAY));
    }
}