// 文件路径: src/main/java/com/sake/mobfriends/client/renderer/CombatZombieRenderer.java

package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.mobfriends.client.ClientSetup;
import com.sake.mobfriends.client.model.CombatZombieModel;
import com.sake.mobfriends.entity.CombatZombie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;

public class CombatZombieRenderer extends HumanoidMobRenderer<CombatZombie, CombatZombieModel<CombatZombie>> {

    // 1. 定义材质文件的路径
    private static final ResourceLocation ZOMBIE_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie/zombie.png");

    public CombatZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new CombatZombieModel<>(context.bakeLayer(CombatZombieModel.LAYER_LOCATION)), 0.5F);

        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_ZOMBIE_INNER_ARMOR_LAYER)),
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_ZOMBIE_OUTER_ARMOR_LAYER)),
                Minecraft.getInstance().getModelManager()
        ));

        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(CombatZombie pEntity, float pEntityYaw, float pPartialTicks, PoseStack pPoseStack, net.minecraft.client.renderer.MultiBufferSource pBuffer, int pPackedLight) {
        // 如果实体正在坐下
        if (pEntity.isInSittingPose() || pEntity.isPassenger()) {
            // 向下平移
            pPoseStack.translate(0.0D, -0.7D, 0.0D);
        }

        // 调用父类的原始render方法，让它在调整过的画布上进行后续渲染
        super.render(pEntity, pEntityYaw, pPartialTicks, pPoseStack, pBuffer, pPackedLight);
    }

    // --- 【这就是解决问题的核心代码】 ---
    // 2. 实现这个方法，返回上面定义的路径
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull CombatZombie entity) {
        return ZOMBIE_LOCATION; //
    }

    @Override
    protected void scale(CombatZombie zombie, PoseStack poseStack, float partialTickTime) {
        float scale = zombie.getScale();
        if (scale != 1.0F) {
            poseStack.scale(scale, scale, scale);
        }
    }
}