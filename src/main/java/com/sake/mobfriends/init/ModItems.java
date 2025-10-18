package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// NeoForge 1.21.1 移植要点:
// 1. DeferredRegister: 创建一个针对物品注册表的 DeferredRegister 实例。
//    - 第一个参数是注册表类型，物品是 BuiltInRegistries.ITEM。
//    - 第二个参数是你的模组ID。
// 2. register 方法: 使用 register 方法来定义你的物品。
//    - 第一个参数是物品的注册名 (例如 "zombie_core")。
//    - 第二个参数是一个 Supplier，它返回一个新的 Item 实例。
//    - 我们使用 () -> new Item(new Item.Properties()) 这种 lambda 表达式来提供 Supplier。
// 3. 静态 register 方法: 创建一个公共的静态方法，在主类的构造函数中调用，
//    将 DEFERRED_ITEMS 注册到事件总线。
public class ModItems {
    // 创建一个 DeferredRegister 实例，用于注册物品
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, MobFriends.MOD_ID);

    // --- 核心物品 (Cores) ---
    // ZombieCore 对应旧版的 WarriorSummonItem
    public static final Supplier<Item> ZOMBIE_CORE = ITEMS.register("zombie_core",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BROKEN_ZOMBIE_CORE = ITEMS.register("broken_zombie_core",
            () -> new Item(new Item.Properties()));
    // WitherCore 对应旧版的 WitherWarriorSummonItem
    public static final Supplier<Item> WITHER_CORE = ITEMS.register("wither_core",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BROKEN_WITHER_CORE = ITEMS.register("broken_wither_core",
            () -> new Item(new Item.Properties()));

    // --- 谢意物品 (Tokens) ---
    public static final Supplier<Item> ZOMBIE_TOKEN = ITEMS.register("zombie_token",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SKELETON_TOKEN = ITEMS.register("skeleton_token",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> CREEPER_TOKEN = ITEMS.register("creeper_token",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> ENDERMAN_TOKEN = ITEMS.register("enderman_token",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> SLIME_TOKEN = ITEMS.register("slime_token",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> BLAZE_TOKEN = ITEMS.register("blaze_token",
            () -> new Item(new Item.Properties()));

    // --- 其他物品 ---
    public static final Supplier<Item> COIN = ITEMS.register("coin",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POWDER = ITEMS.register("powder",
            () -> new Item(new Item.Properties()));

    // 公共的注册方法，将在主类中被调用
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}// 物品初始化
