package com.sake.mobfriends.client.renderer.layer;

import com.sake.mobfriends.entity.CombatCreeper;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CombatCreeperPowerLayer extends EnergySwirlLayer<CombatCreeper, CreeperModel<CombatCreeper>> {
    // 【核心修复】使用正确的 fromNamespaceAndPath 方法
    private static final ResourceLocation POWER_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper_armor.png");
    private final CreeperModel<CombatCreeper> model;

    public CombatCreeperPowerLayer(RenderLayerParent<CombatCreeper, CreeperModel<CombatCreeper>> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new CreeperModel<>(pModelSet.bakeLayer(ModelLayers.CREEPER_ARMOR));
    }

    @Override
    protected float xOffset(float p_116683_) {
        return p_116683_ * 0.01F;
    }

    @Override
    protected @NotNull ResourceLocation getTextureLocation() {
        return POWER_LOCATION;
    }

    @Override
    protected @NotNull EntityModel<CombatCreeper> model() {
        return this.model;
    }
}