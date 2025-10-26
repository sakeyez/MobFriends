package com.sake.mobfriends.client.model;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.CombatBlaze;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

public class CombatBlazeModel extends HumanoidModel<CombatBlaze> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_blaze"), "main");

    public CombatBlazeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // 烈焰人也暂时使用人形模型
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}