package com.sake.mobfriends;

import com.sake.mobfriends.advancements.BecomeFriendlyTrigger;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.init.ModCreativeTabs;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.network.NpcPacketHandler;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import com.sake.mobfriends.init.*;
import com.sake.mobfriends.util.FishConversionHelper;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.sake.mobfriends.init.*;
import com.sake.mobfriends.init.ModMenuTypes;

@Mod(MobFriends.MOD_ID)
public class MobFriends {
    public static final String MOD_ID = "mob_friends";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final BecomeFriendlyTrigger BECAME_FRIENDLY_WITH_FACTION = new BecomeFriendlyTrigger();



    public MobFriends(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModEffects.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 在 FMLCommonSetupEvent 事件中调用初始化方法
        // 使用 enqueueWork 是为了确保它在正确的时机和线程上执行
        event.enqueueWork(() -> {
            FishConversionHelper.initialize();
        });
    }

    private void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        NpcPacketHandler.register(event);
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