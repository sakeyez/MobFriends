package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags; // 【修正】需要导入 FeatureFlags
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ModMenuTypes {
    // 【修正】注册表的类型直接使用原版的 BuiltInRegistries.MENU
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, MobFriends.MOD_ID);

    // 【最终修正】使用 new MenuType<>(...) 的标准构造函数来注册
    // 这是当前版本 NeoForge/Minecraft 最稳定和推荐的方式
    public static final Supplier<MenuType<ZombieChestMenu>> ZOMBIE_CHEST_MENU =
            MENUS.register("zombie_chest_menu",
                    () -> new MenuType<>(ZombieChestMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}