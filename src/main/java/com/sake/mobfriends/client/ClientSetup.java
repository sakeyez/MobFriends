package com.sake.mobfriends.client;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.model.CombatWitherModel;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.client.renderer.CombatWitherRenderer;
import com.sake.mobfriends.client.renderer.CombatZombieRenderer;
import com.sake.mobfriends.init.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MobFriends.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMBAT_ZOMBIE.get(), CombatZombieRenderer::new);

        // --- 新增代码 ---
        // 注册凋灵战士的渲染器
        event.registerEntityRenderer(ModEntities.COMBAT_WITHER.get(), CombatWitherRenderer::new);
        // --- 新增代码结束 ---

        // 其他NPC的渲染器可以在这里继续添加
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CombatZombieModel.LAYER_LOCATION, CombatZombieModel::createBodyLayer);

        // --- 新增代码 ---
        // 注册凋灵战士的模型图层定义
        event.registerLayerDefinition(CombatWitherModel.LAYER_LOCATION, CombatWitherModel::createBodyLayer);
        // --- 新增代码结束 ---
    }
}