package com.sake.mobfriends.client;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.gui.ZombieChestScreen;
import com.sake.mobfriends.client.gui.CombatCoreTooltip;
import com.sake.mobfriends.client.gui.CombatCoreTooltipRenderer;
import com.sake.mobfriends.client.model.CombatBlazeModel;
import com.sake.mobfriends.client.model.CombatCreeperModel;
import com.sake.mobfriends.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.client.model.CreeperNpcModel;
import com.sake.mobfriends.init.ModEntities;
import com.sake.mobfriends.init.ModMenuTypes;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MobFriends.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {


    // 僵尸战士的盔甲层 (保持不变)
    public static final ModelLayerLocation COMBAT_ZOMBIE_INNER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_zombie_inner_armor"), "main");
    public static final ModelLayerLocation COMBAT_ZOMBIE_OUTER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_zombie_outer_armor"), "main");

    //
    public static final ModelLayerLocation COMBAT_WITHER_INNER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_wither_inner_armor"), "main");
    public static final ModelLayerLocation COMBAT_WITHER_OUTER_ARMOR_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_wither_outer_armor"), "main");


    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMBAT_ZOMBIE.get(), CombatZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBAT_WITHER.get(), CombatWitherRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBAT_CREEPER.get(), CombatCreeperRenderer::new);
        event.registerEntityRenderer(ModEntities.COMBAT_BLAZE.get(), CombatBlazeRenderer::new);
        event.registerEntityRenderer(ModEntities.ZOMBIE_NPC.get(), ZombieNpcRenderer::new); // <-- 修改
        event.registerEntityRenderer(ModEntities.SKELETON_NPC.get(), SkeletonNpcRenderer::new); // <-- 修改
        event.registerEntityRenderer(ModEntities.CREEPER_NPC.get(), CreeperNpcRenderer::new);
        event.registerEntityRenderer(ModEntities.ENDERMAN_NPC.get(), EndermanRenderer::new);
        event.registerEntityRenderer(ModEntities.SLIME_NPC.get(), SlimeRenderer::new);
        event.registerEntityRenderer(ModEntities.BLAZE_NPC.get(), BlazeRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(final EntityRenderersEvent.RegisterLayerDefinitions event) {
        // 注册僵尸战士的模型层

        event.registerLayerDefinition(CombatZombieModel.LAYER_LOCATION, CombatZombieModel::createBodyLayer);
        event.registerLayerDefinition(COMBAT_ZOMBIE_INNER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0f), 64, 32));
        event.registerLayerDefinition(COMBAT_ZOMBIE_OUTER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0f), 64, 32));

        //凋零骷髅战士
        event.registerLayerDefinition(COMBAT_WITHER_INNER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F), 0.0f), 64, 32));
        event.registerLayerDefinition(COMBAT_WITHER_OUTER_ARMOR_LAYER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0f), 64, 32));
        event.registerLayerDefinition(CombatCreeperModel.LAYER_LOCATION, CombatCreeperModel::createBodyLayer);
        event.registerLayerDefinition(CombatBlazeModel.LAYER_LOCATION, CombatBlazeModel::createBodyLayer);

        event.registerLayerDefinition(CreeperNpcModel.LAYER_LOCATION, CreeperNpcModel::createBodyLayer);
    }



    // (GUI和Tooltip的注册保持不变)
    @SubscribeEvent
    public static void onRegisterMenuScreens(final RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ZOMBIE_CHEST_MENU.get(), ZombieChestScreen::new);
    }
    @SubscribeEvent
    public static void onRegisterTooltipFactories(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(CombatCoreTooltip.class, CombatCoreTooltipRenderer::new);
    }
}