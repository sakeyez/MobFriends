package com.sake.mobfriends.item;

import com.sake.mobfriends.init.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.UUID;

public abstract class AbstractCoreItem extends Item {
    // ZOMBIE_UUID_KEY 常量现在不再需要了

    public AbstractCoreItem(Properties properties) {
        super(properties);
    }

    public static void setZombieUUID(ItemStack stack, UUID uuid) {
        if (uuid != null) {
            // 使用 .set() 方法来附加数据组件
            stack.set(ModDataComponents.ZOMBIE_UUID.get(), uuid);
        }
    }

    public static UUID getZombieUUID(ItemStack stack) {
        // 使用 .get() 方法来读取数据组件
        return stack.get(ModDataComponents.ZOMBIE_UUID.get());
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);

        }
    }
