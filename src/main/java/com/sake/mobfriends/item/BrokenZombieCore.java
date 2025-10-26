package com.sake.mobfriends.item;

import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag; // <-- 添加导入
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // <-- 添加导入
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class BrokenZombieCore extends AbstractCoreItem {

    public BrokenZombieCore(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, @NotNull InteractionHand pUsedHand) {
        ItemStack heldItem = pPlayer.getItemInHand(pUsedHand); // 这是破损核心
        ItemStack offhandItem = pPlayer.getItemInHand(InteractionHand.OFF_HAND);

        if (offhandItem.is(Items.TOTEM_OF_UNDYING)) {
            ItemStack activeCore = new ItemStack(ModItems.ACTIVE_ZOMBIE_CORE.get());

            // --- 【核心修复 B：转移继承的数据】 ---

            // 1. 从破损核心中获取储存的僵尸完整数据
            CompoundTag zombieData = heldItem.get(ModDataComponents.STORED_ZOMBIE_NBT.get());

            // 2. 如果数据存在，就把它原封不动地转移到新的激活核心里
            if (zombieData != null) {
                activeCore.set(ModDataComponents.STORED_ZOMBIE_NBT.get(), zombieData.copy());
            }

            // 3. 别忘了把UUID也转移过去
            UUID zombieUUID = getZombieUUID(heldItem);
            if (zombieUUID != null) {
                setZombieUUID(activeCore, zombieUUID);
            }

            // --- 后续逻辑保持不变 ---
            if (pLevel.isClientSide()) {
                Minecraft.getInstance().gameRenderer.displayItemActivation(activeCore);
            }

            if (!pLevel.isClientSide()) {
                if (!pPlayer.getAbilities().instabuild) {
                    offhandItem.shrink(1);
                }
                pLevel.playSound(null, pPlayer.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
            }

            // 替换物品
            return InteractionResultHolder.sidedSuccess(activeCore, pLevel.isClientSide());
        }

        return InteractionResultHolder.fail(heldItem);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        pTooltipComponents.add(Component.translatable("tooltip.mob_friends.broken_zombie_core").withStyle(ChatFormatting.RED));
        pTooltipComponents.add(Component.translatable("tooltip.mob_friends.broken_zombie_core_repair").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}