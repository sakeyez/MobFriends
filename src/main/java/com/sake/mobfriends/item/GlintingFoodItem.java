package com.sake.mobfriends.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// 这是一个通用的“发光食物”类
public class GlintingFoodItem extends Item {
    public GlintingFoodItem(Properties properties) {
        super(properties);
    }

    // 核心：重写 isFoil 方法，让它始终返回 true
    @Override
    public boolean isFoil(ItemStack pStack) {
        return true; // 强制物品发光
    }
}