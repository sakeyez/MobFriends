package com.sake.mobfriends.client.model;

import com.mojang.blaze3d.vertex.PoseStack; // 【新增】
import com.mojang.blaze3d.vertex.VertexConsumer; // 【新增】
import com.sake.mobfriends.MobFriends;
import net.minecraft.client.model.CreeperModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Creeper;

public class CreeperNpcModel<T extends Creeper> extends CreeperModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "creeper_npc"), "main");

    // 【修改】父类 CreeperModel 会自动处理所有部件
    // 我们不需要在这里单独声明它们

    public CreeperNpcModel(ModelPart root) {
        super(root);
    }

    // 【【【核心修改：完全替换为你的 Blockbench 模型定义】】】
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 你的 Blockbench 模型中，所有东西都在 "Body" 下面
        // 我们需要把它们拆分，以匹配 CreeperModel 的结构 (head, body, legs 都是 root 的子项)

        // 1. 定义身体 (Body)
        // 你的模型 Body PartPose Y=24, Box Y=-18.0F (即 Y=6)
        // 这与原版 Creeper Y=6 匹配
        partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        // 2. 定义头部 (Head) 和你的护目镜 (Goggles)
        // 你的模型 Head PartPose Y=-18.0F (相对于Body) (即 Y=6)
        // 这也与原版 Creeper Y=6 匹配
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(16, 50).addBox(-3.0F, -6.0F, -5.0F, 6.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 39).addBox(1.0F, -7.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(4, 43).addBox(-3.0F, -7.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 44).addBox(1.0F, -4.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(42, 47).addBox(-3.0F, -4.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(5, 58).addBox(-4.0F, -6.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(18, 58).addBox(3.0F, -6.0F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(43, 1).addBox(-5.0F, -6.0F, -5.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                        .texOffs(43, 1).addBox(4.0F, -6.0F, -5.0F, 1.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                        .texOffs(1, 1).addBox(-4.0F, -7.0F, -5.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(1, 1).addBox(-4.0F, -4.0F, -5.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(1, 1).addBox(3.0F, -4.0F, -5.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(56, 35).addBox(-5.0F, -5.0F, -5.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 29).addBox(4.0F, -5.0F, -5.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(1, 5).addBox(3.0F, -7.0F, -5.0F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 17).addBox(1.0F, -8.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(44, 24).addBox(-3.0F, -8.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(55, 24).addBox(-1.0F, -7.0F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 6.0F, 0.0F));

        // 2b. 添加 Head 的子部件 (来自你的 "cube_r1")
        head.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(44, 0).addBox(-5.0F, -1.0F, -1.0F, -1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.0F, -5.0F, 10.0F, 0.0F, -1.5708F, 0.0F));

        // 3. 定义四肢 (Legs)
        // 你的模型 Leg PartPose Y=-6.0F (相对于Body) (即 Y=18)
        // 这也与原版 Creeper Y=18 匹配
        CubeListBuilder legBuilder = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, new CubeDeformation(0.0F));

        // 你的 leg2 对应 right_front_leg
        partdefinition.addOrReplaceChild("right_front_leg", legBuilder, PartPose.offset(-2.0F, 18.0F, -4.0F));
        // 你的 leg3 对应 left_front_leg
        partdefinition.addOrReplaceChild("left_front_leg", legBuilder, PartPose.offset(2.0F, 18.0F, -4.0F));
        // 你的 leg0 对应 right_hind_leg
        partdefinition.addOrReplaceChild("right_hind_leg", legBuilder, PartPose.offset(-2.0F, 18.0F, 4.0F));
        // 你的 leg1 对应 left_hind_leg
        partdefinition.addOrReplaceChild("left_hind_leg", legBuilder, PartPose.offset(2.0F, 18.0F, 4.0F));

        return LayerDefinition.create(meshdefinition, 64, 64); // 使用你的 64x64 贴图尺寸
    }

    // setupAnim 会被父类 CreeperModel 自动处理，我们不需要重写它！
    // @Override
    // public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
    //     super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);
    // }

    // renderToBuffer 也不需要，父类会处理
}