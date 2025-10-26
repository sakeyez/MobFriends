package com.sake.mobfriends.client.model;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.CombatCreeper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

public class CombatCreeperModel extends HumanoidModel<CombatCreeper> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_creeper"), "main");

    public CombatCreeperModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // 我们暂时使用标准人形模型，所以苦力怕会有人形
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}