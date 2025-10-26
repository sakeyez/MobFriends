package com.sake.mobfriends.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sake.mobfriends.client.ClientSetup;
import com.sake.mobfriends.client.model.CombatWitherModel;
import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers; // <-- 导入
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
        // 【核心修改】使用原版的 WITHER_SKELETON 模型层！
        super(context, new CombatWitherModel(context.bakeLayer(ModelLayers.WITHER_SKELETON)), 0.5F);

        // 【核心修改】添加盔甲和物品层，并使用我们为凋零战士定义的专属盔甲层
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_WITHER_INNER_ARMOR_LAYER)),
                new HumanoidModel<>(context.bakeLayer(ClientSetup.COMBAT_WITHER_OUTER_ARMOR_LAYER)),
                Minecraft.getInstance().getModelManager()
        ));
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CombatWither entity) {
        return TEXTURE_LOCATION;
    }

    @Override
    protected void scale(CombatWither wither, PoseStack poseStack, float partialTickTime) {
        float scale = wither.getScale();
        // 凋零骷髅本身就比普通生物高，所以我们可能需要调整基础缩放值
        poseStack.scale(scale, scale, scale);
    }
}