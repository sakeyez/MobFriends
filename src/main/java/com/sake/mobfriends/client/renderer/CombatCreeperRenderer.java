package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.mobfriends.client.renderer.layer.CombatCreeperPowerLayer;
import com.sake.mobfriends.entity.CombatCreeper;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class CombatCreeperRenderer extends MobRenderer<CombatCreeper, CreeperModel<CombatCreeper>> {
    private static final ResourceLocation CREEPER_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/creeper/creeper.png");

    public CombatCreeperRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperModel<>(context.bakeLayer(ModelLayers.CREEPER)), 0.5F);
        this.addLayer(new CombatCreeperPowerLayer(this, context.getModelSet()));
    }

    @Override
    protected void scale(CombatCreeper pLivingEntity, PoseStack pPoseStack, float pPartialTickTime) {
        float f = pLivingEntity.getSwelling(pPartialTickTime);
        float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
        f = Mth.clamp(f, 0.0F, 1.0F);
        f *= f;
        f *= f;
        float f2 = (1.0F + f * 0.5F) * f1;
        float f3 = (1.0F + f * 0.15F) / f1;
        pPoseStack.scale(f2, f3, f2);
    }

    /**
     * 【核心修复】覆盖此方法以实现白光闪烁效果
     * @return 覆盖层的白色程度 (0.0为无，1.0为纯白)
     */
    @Override
    protected float getWhiteOverlayProgress(CombatCreeper pLivingEntity, float pPartialTicks) {
        float f = pLivingEntity.getSwelling(pPartialTicks);
        // 这段逻辑直接复制自原版CreeperRenderer，用于计算闪烁
        if ((int)(f * 10.0F) % 2 == 0) {
            return 0.0F;
        } else {
            return Mth.clamp(f, 0.5F, 1.0F);
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CombatCreeper entity) {
        return CREEPER_LOCATION;
    }
}