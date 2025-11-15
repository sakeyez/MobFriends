package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, MobFriends.MOD_ID);

    private static final FoodProperties SOUL_BAOZI_FOOD = new FoodProperties.Builder().nutrition(5)
            .saturationModifier(0.6F)
            .build();
    // --- 核心物品 (Cores) ---
    public static final Supplier<Item> ZOMBIE_CORE = ITEMS.register("zombie_core",
            () -> new ZombieCore(new Item.Properties().stacksTo(1))); // <-- 修正
    public static final Supplier<Item> ACTIVE_ZOMBIE_CORE = ITEMS.register("active_zombie_core",
            () -> new ActiveZombieCore(new Item.Properties().stacksTo(1))); // <-- 修正
    public static final Supplier<Item> BROKEN_ZOMBIE_CORE = ITEMS.register("broken_zombie_core",
            () -> new BrokenZombieCore(new Item.Properties().stacksTo(1))); // <-- 修正

    public static final Supplier<Item> WITHER_CORE = ITEMS.register("wither_core",
            () -> new WitherCore(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> ACTIVE_WITHER_CORE = ITEMS.register("active_wither_core",
            () -> new ActiveWitherCore(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> BROKEN_WITHER_CORE = ITEMS.register("broken_wither_core",
            () -> new BrokenWitherCore(new Item.Properties().stacksTo(1)));


    public static final Supplier<Item> CREEPER_CORE = ITEMS.register("creeper_core",
            () -> new CreeperCore(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> ACTIVE_CREEPER_CORE = ITEMS.register("active_creeper_core",
            () -> new ActiveCreeperCore(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> BROKEN_CREEPER_CORE = ITEMS.register("broken_creeper_core",
            () -> new BrokenCreeperCore(new Item.Properties().stacksTo(1)));


    public static final Supplier<Item> BLAZE_CORE = ITEMS.register("blaze_core",
            () -> new BlazeCore(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> ACTIVE_BLAZE_CORE = ITEMS.register("active_blaze_core",
            () -> new ActiveBlazeCore(new Item.Properties().stacksTo(1)));
    public static final Supplier<Item> BROKEN_BLAZE_CORE = ITEMS.register("broken_blaze_core",
            () -> new BrokenBlazeCore(new Item.Properties().stacksTo(1)));

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

    // 货币
    public static final Supplier<Item> COIN = ITEMS.register("coin",
            () -> new Item(new Item.Properties()));
    public static final Supplier<Item> POWDER = ITEMS.register("powder",
            () -> new Item(new Item.Properties()));

    //灵魂包子
    public static final Supplier<Item> SOUL_BAOZI = ITEMS.register("soul_baozi",
            () -> new Item(new Item.Properties().food(SOUL_BAOZI_FOOD)));
    //澎湃灵魂包
    public static final Supplier<Item> UPGRADED_SOUL_BAOZI = ITEMS.register("upgraded_soul_baozi",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(10).saturationModifier(1.2f).build()
            ).rarity(Rarity.RARE))); // 让它显示为稀有
    // 喷香烤包子
    public static final Supplier<Item> UPGRADED_SAMSA = ITEMS.register("upgraded_samsa",
            () -> new Item(new Item.Properties().food(
                    new FoodProperties.Builder().nutrition(10).saturationModifier(1.2f).build()
            ).rarity(Rarity.RARE))); // 让它显示为稀有


    public static final Supplier<Item> ZOMBIE_BLOCK = ITEMS.register("zombie_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ZOMBIE_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> SKELETON_BLOCK = ITEMS.register("skeleton_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SKELETON_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> CREEPER_BLOCK = ITEMS.register("creeper_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.CREEPER_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> ENDERMAN_BLOCK = ITEMS.register("enderman_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.ENDERMAN_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> SLIME_BLOCK = ITEMS.register("slime_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SLIME_BLOCK.get(), new Item.Properties()));

    public static final Supplier<Item> BLAZE_BLOCK = ITEMS.register("blaze_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.BLAZE_BLOCK.get(), new Item.Properties()));
    // 刷怪蛋

    public static final Supplier<Item> ZOMBIE_NPC_SPAWN_EGG = ITEMS.register("zombie_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ZOMBIE_NPC, 0x00af49, 0x7f4b39, new Item.Properties()));
    public static final Supplier<Item> SKELETON_NPC_SPAWN_EGG = ITEMS.register("skeleton_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SKELETON_NPC, 0xc1c1c1, 0x738b8b, new Item.Properties()));
    public static final Supplier<Item> CREEPER_NPC_SPAWN_EGG = ITEMS.register("creeper_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.CREEPER_NPC, 0x0da70b, 0x000000, new Item.Properties()));
    public static final Supplier<Item> ENDERMAN_NPC_SPAWN_EGG = ITEMS.register("enderman_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.ENDERMAN_NPC, 0x161616, 0xd47af7, new Item.Properties()));
    public static final Supplier<Item> SLIME_NPC_SPAWN_EGG = ITEMS.register("slime_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.SLIME_NPC, 0x7fcc51, 0x568239, new Item.Properties()));
    public static final Supplier<Item> BLAZE_NPC_SPAWN_EGG = ITEMS.register("blaze_npc_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.BLAZE_NPC, 0xf6b201, 0xfff3a5, new Item.Properties()));
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}