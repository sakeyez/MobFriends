package com.sake.mobfriends.client.model;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

// NeoForge 1.21.1 移植要点:
// 每个实体模型都需要一个独立的模型类。
// 即使结构与另一个模型完全相同，也必须有自己的 ModelLayerLocation，
// 这样渲染系统才能正确地加载和缓存模型。
public class CombatWitherModel extends HumanoidModel<CombatWither> {

    // 为凋灵战士定义一个唯一的图层位置
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_wither"), "main");

    public CombatWitherModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // 我们暂时使用标准的人形模型结构。
        // 在未来的阶段，可以像旧项目一样为它添加自定义的部件。
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}