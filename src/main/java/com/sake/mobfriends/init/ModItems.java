package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.item.ActiveZombieCore;
import com.sake.mobfriends.item.BrokenZombieCore;
import com.sake.mobfriends.item.ZombieCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, MobFriends.MOD_ID);

    // --- 核心物品 (Cores) ---
    public static final Supplier<Item> ZOMBIE_CORE = ITEMS.register("zombie_core",
            () -> new ZombieCore(new Item.Properties().stacksTo(1))); // <-- 修正
    public static final Supplier<Item> ACTIVE_ZOMBIE_CORE = ITEMS.register("active_zombie_core",
            () -> new ActiveZombieCore(new Item.Properties().stacksTo(1))); // <-- 修正
    public static final Supplier<Item> BROKEN_ZOMBIE_CORE = ITEMS.register("broken_zombie_core",
            () -> new BrokenZombieCore(new Item.Properties().stacksTo(1))); // <-- 修正

    // WitherCore 对应旧版的 WitherWarriorSummonItem (保持不变)
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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}