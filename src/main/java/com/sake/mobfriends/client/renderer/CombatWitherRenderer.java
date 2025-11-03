package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.mobfriends.client.ClientSetup;
import com.sake.mobfriends.client.model.CombatWitherModel;
import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class CombatWitherRenderer extends HumanoidMobRenderer<CombatWither, CombatWitherModel> {

    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/entity/skeleton/wither_skeleton.png");

    public CombatWitherRenderer(EntityRendererProvider.Context context) {
        super(context, new CombatWitherModel(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_WITHER_INNER_ARMOR_LAYER)),
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_WITHER_OUTER_ARMOR_LAYER)),
                Minecraft.getInstance().getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }


    @Override
    public void render(CombatWither pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, net.minecraft.client.renderer.MultiBufferSource pBuffer, int pPackedLight) {
        // 如果实体正在坐下，或者它是一个乘客
        if (pEntity.isInSittingPose() || pEntity.isPassenger()) {
            // 向下平移以防止模型悬空。这个值是从僵尸战士复制过来的。
            // 因为凋零骷髅模型更高，如果效果不完美，可以单独微调这个值（比如 -0.8D）。
            pPoseStack.translate(0.0D, -0.8D, 0.0D);
        }

        // 调用父类的原始render方法，让它在调整过的画布上进行后续渲染
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CombatWither entity) {
        return TEXTURE_LOCATION;
    }

    @Override
    protected void scale(CombatWither wither, PoseStack poseStack, float partialTickTime) {
        float scale = wither.getScale();
        poseStack.scale(scale, scale, scale);
    }
}