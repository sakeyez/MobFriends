package com.sake.mobfriends.client;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.model.CombatWitherModel;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.client.renderer.CombatWitherRenderer;
import com.sake.mobfriends.client.renderer.CombatZombieRenderer;
import com.sake.mobfriends.init.ModEntities;
import net.minecraft.client.renderer.entity.*; // <-- 修正点：导入更多原版渲染器
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MobFriends.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        // --- 原有代码 ---
        event.registerEntityRenderer(ModEntities.COMBAT_ZOMBIE.get(), CombatZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBAT_WITHER.get(), CombatWitherRenderer::new);

        // --- 修正点：为所有NPC注册渲染器 ---
        // 我们暂时使用原版的渲染器，这样它们就可以被正常渲染了。
        event.registerEntityRenderer(ModEntities.ZOMBIE_NPC.get(), ZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_NPC.get(), SkeletonRenderer::new);
        event.registerEntityRenderer(ModEntities.CREEPER_NPC.get(), CreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.ENDERMAN_NPC.get(), EndermanRenderer::new);
        event.registerEntityRenderer(ModEntities.SLIME_NPC.get(), SlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLAZE_NPC.get(), BlazeRenderer::new);
        // --- 修正点结束 ---
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        // --- 原有代码，无需改动 ---
        event.registerLayerDefinition(CombatZombieModel.LAYER_LOCATION, CombatZombieModel::createBodyLayer);
        event.registerLayerDefinition(CombatWitherModel.LAYER_LOCATION, CombatWitherModel::createBodyLayer);
    }
}