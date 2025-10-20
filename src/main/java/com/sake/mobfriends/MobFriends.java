package com.sake.mobfriends;

import com.sake.mobfriends.advancements.BecomeFriendlyTrigger;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.network.NpcPacketHandler;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries; // 确保这个 import 存在
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

import java.util.Optional;

@Mod(MobFriends.MOD_ID)
public class MobFriends {
    public static final String MOD_ID = "mob_friends";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final BecomeFriendlyTrigger BECAME_FRIENDLY_WITH_FACTION = new BecomeFriendlyTrigger();

    public MobFriends(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);

        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        NpcPacketHandler.register(event);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // ======================= 【终极诊断代码】 =======================
        LOGGER.info("===============================================================");
        LOGGER.info("====== [成就系统最终诊断] 服务器启动，开始检查触发器... ======");

        // 【核心修正】使用 Registries.TRIGGER_TYPE 而不是 BuiltInRegistries.TRIGGER_TYPE
        Optional<CriterionTrigger<?>> trigger = event.getServer().registryAccess().registryOrThrow(Registries.TRIGGER_TYPE).getOptional(BecomeFriendlyTrigger.ID);

        if (trigger.isPresent()) {
            LOGGER.info("====== [诊断成功] 触发器 '{}' 已成功注册在游戏中!", BecomeFriendlyTrigger.ID);
        } else {
            LOGGER.error("====== [诊断失败] 触发器 '{}' 未在游戏中注册! 这是成就系统不工作的根本原因!", BecomeFriendlyTrigger.ID);
        }
        LOGGER.info("===============================================================");
        // =================================================================
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onRegister(final RegisterEvent event) {
            if (event.getRegistryKey().equals(Registries.TRIGGER_TYPE)) {
                event.register(Registries.TRIGGER_TYPE, helper -> {
                    helper.register(BecomeFriendlyTrigger.ID, MobFriends.BECAME_FRIENDLY_WITH_FACTION);
                });
            }
        }
    }
}