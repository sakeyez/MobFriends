package com.sake.mobfriends;

import com.mojang.logging.LogUtils;
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.init.ModEntities;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

// NeoForge 1.21.1 移植要点:
// 1. @Mod 注解: 这是模组的入口点，必须指定模组ID (MOD_ID)。
// 2. 构造函数: 模组的主类构造函数会传入一个 IEventBus 实例。
//    我们需要使用这个 eventBus 来注册我们的 DeferredRegister 实例。
// 3. DeferredRegister: 这是 NeoForge (及现代Forge) 推荐的注册方式，
//    可以安全地在模组加载的任何阶段注册物品、方块等。
@Mod(MobFriends.MOD_ID)
public class MobFriends {
    // 定义模组ID，确保这个ID和你的 mods.toml 文件中的 [[mods]] table 的 modId 一致。
    public static final String MOD_ID = "mob_friends";
    // 创建一个日志记录器，方便调试
    private static final Logger LOGGER = LogUtils.getLogger();

    public MobFriends(IEventBus modEventBus) {
        LOGGER.info("MobFriends Mod is loading!");

        // 将 ModItems 类中定义的 DEFERRED_ITEMS 注册到模组事件总线上
        // 这是让 NeoForge 知道我们的物品需要被注册的关键步骤。
        ModItems.register(modEventBus);

        // 同理，注册我们的创造模式物品栏
        ModCreativeTabs.register(modEventBus);

        ModEntities.register(modEventBus);
    }
}