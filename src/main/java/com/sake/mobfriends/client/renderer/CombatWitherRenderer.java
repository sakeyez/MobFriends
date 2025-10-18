package com.sake.mobfriends.client.renderer;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.client.model.CombatWitherModel;
import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

// NeoForge 1.21.1 移植要点:
// 渲染器将实体、模型和材质三者绑定在一起。
// 构造函数中通过 context.bakeLayer(ModelLayerLocation) 获取正确的模型实例。
public class CombatWitherRenderer extends HumanoidMobRenderer<CombatWither, CombatWitherModel> {

    // 定义凋灵战士的材质路径
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(
            MobFriends.MOD_ID, "textures/entity/combat_wither.png");

    public CombatWitherRenderer(EntityRendererProvider.Context context) {
        // 使用 CombatWitherModel 的图层位置来构建模型
        super(context, new CombatWitherModel(context.bakeLayer(CombatWitherModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(CombatWither entity) {
        return TEXTURE_LOCATION;
    }
}