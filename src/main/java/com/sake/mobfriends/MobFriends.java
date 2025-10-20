package com.sake.mobfriends;

import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.init.ModTriggers; // 确保导入
import com.sake.mobfriends.network.NpcPacketHandler;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MobFriends.MOD_ID)
public class MobFriends {
    public static final String MOD_ID = "mob_friends";
    public static final Logger LOGGER = LogManager.getLogger();

    public MobFriends(IEventBus modEventBus) {
        // --- MOD 事件总线注册 ---
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // 【核心修复】将你的触发器注册到 Mod 事件总线
        ModTriggers.TRIGGERS.register(modEventBus);

        // --- FORGE 事件总线注册 ---
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // ...
    }

    private void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        NpcPacketHandler.register(event);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // 【核心修复】删除了整个 ModEventBusEvents 静态内部类
    // 因为我们现在使用了更稳定可靠的 DeferredRegister 方式
}