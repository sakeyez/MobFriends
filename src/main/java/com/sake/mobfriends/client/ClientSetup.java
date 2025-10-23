package com.sake.mobfriends.client;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.gui.ZombieChestScreen;
import com.sake.mobfriends.client.gui.ZombieCoreTooltip;
import com.sake.mobfriends.client.gui.ZombieCoreTooltipRenderer;
import com.sake.mobfriends.client.model.CombatWitherModel;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.client.renderer.CombatWitherRenderer;
import com.sake.mobfriends.client.renderer.CombatZombieRenderer;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModMenuTypes;
import net.minecraft.client.renderer.entity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MobFriends.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ZOMBIE_CHEST_MENU.get(), ZombieChestScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMBAT_ZOMBIE.get(), CombatZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBAT_WITHER.get(), CombatWitherRenderer::new);
        event.registerEntityRenderer(ModEntities.ZOMBIE_NPC.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_NPC.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.CREEPER_NPC.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.ENDERMAN_NPC.get(), EndermanRenderer::new);
        event.registerEntityRenderer(ModEntities.SLIME_NPC.get(), SlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLAZE_NPC.get(), BlazeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CombatZombieModel.LAYER_LOCATION, CombatZombieModel::createBodyLayer);
        event.registerLayerDefinition(CombatWitherModel.LAYER_LOCATION, CombatWitherModel::createBodyLayer);
    }

    // --- 【核心修正】 ---
    // 这个方法就是专门用来注册自定义工具提示渲染器的，确保它在这里。
    @SubscribeEvent
    public static void onRegisterTooltipFactories(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ZombieCoreTooltip.class, ZombieCoreTooltipRenderer::new);
    }
}