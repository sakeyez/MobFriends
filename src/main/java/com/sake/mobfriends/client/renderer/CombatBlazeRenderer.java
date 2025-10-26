package com.sake.mobfriends.client.renderer;

import com.sake.mobfriends.entity.CombatBlaze;
import net.minecraft.client.model.BlazeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CombatBlazeRenderer extends MobRenderer<CombatBlaze, BlazeModel<CombatBlaze>> {
    private static final ResourceLocation BLAZE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/blaze.png");

    public CombatBlazeRenderer(EntityRendererProvider.Context context) {
        // 【核心修改】
        // 1. 父类改为 MobRenderer
        // 2. 模型改为原版的 BlazeModel
        // 3. 模型层使用游戏内置的 ModelLayers.BLAZE
        super(context, new BlazeModel<>(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CombatBlaze entity) {
        return BLAZE_LOCATION;
    }
}