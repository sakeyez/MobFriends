package com.sake.mobfriends;

import com.sake.mobfriends.advancements.BecomeFriendlyTrigger;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.init.ModTriggers;
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
        // 移除了之前所有错误的触发器注册尝试
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // --- FORGE 事件总线注册 ---
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 确保这个方法是空的，或者只包含它应该处理的任务
    }

    private void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        NpcPacketHandler.register(event);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // --- 【核心修复】使用 RegisterEvent 来处理注册 ---
    // 这是一个静态内部类，专门用于监听 MOD 事件总线上的事件
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {

        // 这个方法会监听 RegisterEvent 事件
        @SubscribeEvent
        public static void onRegister(final RegisterEvent event) {
            // 检查当前是否为进度触发器（Criterion Trigger）的注册事件
            if (event.getRegistryKey().equals(Registries.TRIGGER_TYPE)) {
                // 如果是，就注册我们的自定义触发器
                // 这是 NeoForge 推荐的、用于非 DeferredRegister 内容的注册方式
                event.register(Registries.TRIGGER_TYPE, helper -> {
                    helper.register(BecomeFriendlyTrigger.ID, ModTriggers.BECAME_FRIENDLY_WITH_FACTION);
                });
            }
        }
    }
}