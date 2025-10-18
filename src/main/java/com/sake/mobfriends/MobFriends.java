package com.sake.mobfriends;

import com.mojang.logging.LogUtils;
import com.sake.mobfriends.event.ForgeBus; // <-- 1. 添加导入
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge; // <-- 2. 添加导入
import org.slf4j.Logger;

@Mod(MobFriends.MOD_ID)
public class MobFriends {
    public static final String MOD_ID = "mob_friends";
    public static final Logger LOGGER = LogUtils.getLogger(); // <-- 3. 将 LOGGER 设为 public static

    public MobFriends(IEventBus modEventBus) {
        LOGGER.info("MobFriends Mod is loading!");

        // --- 原有注册逻辑 (MOD事件总线) ---
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModEntities.register(modEventBus);

        // --- 核心修正点：手动注册 FORGE 事件监听器 ---
        // 这行代码会获取全局的 FORGE 事件总线，
        // 并将我们的 ForgeBus 类注册进去，确保它能收到游戏运行时的所有事件。
        NeoForge.EVENT_BUS.register(ForgeBus.class);
        // --- 修正点结束 ---
    }
}