package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.mobfriends.entity.SkeletonNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 【最终修复版】
 * 1. 继承 HumanoidMobRenderer (模仿 CombatWitherRenderer)
 * 2. 在构造函数中加载 ModelLayers.SKELETON 和默认盔甲层
 * 3. render 方法不再需要 @Override
 * 4. render 方法使用正确的实体类型 SkeletonNpcEntity
 * 5. render 方法使用你验证过的正确平移值 -0.8D (来自 CombatWitherRenderer)
 */
public class SkeletonNpcRenderer extends HumanoidMobRenderer<SkeletonNpcEntity, SkeletonModel<SkeletonNpcEntity>> {

    private static final ResourceLocation SKELETON_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/skeleton/skeleton.png");

    public SkeletonNpcRenderer(EntityRendererProvider.Context pContext) {
        // 【修改】加载原版骷髅模型
        super(pContext, new SkeletonModel<>(pContext.bakeLayer(ModelLayers.SKELETON)), 0.5F);

        // 【新增】添加原版盔甲层，模仿 CombatWitherRenderer
        this.addLayer(new HumanoidArmorLayer<>(this,
                new SkeletonModel<>(pContext.bakeLayer(ModelLayers.SKELETON_INNER_ARMOR)),
                new SkeletonModel<>(pContext.bakeLayer(ModelLayers.SKELETON_OUTER_ARMOR)),
                Minecraft.getInstance().getModelManager()
        ));
    }

    // 【修改】移除 @Override，并使用正确的实体类型
    public void render(SkeletonNpcEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {

        if (pEntity.isPassenger()) {
            // 【修改】使用 -0.8D，这个值来自你的 CombatWitherRenderer
            pPoseStack.translate(0.0D, -0.8D, 0.0D);
        }

        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    // 【修改】使用正确的实体类型
    public @NotNull ResourceLocation getTextureLocation(@NotNull SkeletonNpcEntity pEntity) {
        return SKELETON_LOCATION;
    }
}