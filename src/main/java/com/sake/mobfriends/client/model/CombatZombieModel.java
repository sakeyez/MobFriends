package com.sake.mobfriends.client.model;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.CombatZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

// NeoForge 1.21.1 移植要点:
// 1. 继承: 模型类通常继承自一个基础模型，例如 HumanoidModel 或 EntityModel。
// 2. ModelLayerLocation: 每个模型都需要一个唯一的图层位置，用于在 ClientSetup 中注册。
// 3. createBodyLayer(): 这是一个静态方法，定义了模型的各个部分 (ModelPart) 的形状、大小和位置。
//    这是 1.17+ 版本以来模型系统的核心。
public class CombatZombieModel extends HumanoidModel<CombatZombie> {

    // 定义模型的图层位置
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_zombie"), "main");

    public CombatZombieModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        // MeshDefinition 是构建模型的起点
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 这里可以添加或修改模型的部件
        // 例如，给头部加一个更大的装饰
        // partdefinition.getChild("head").addOrReplaceChild("helmet_extra",
        //         CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.25F)),
        //         PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}