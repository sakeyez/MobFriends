package com.sake.mobfriends.client.model;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.CombatZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

public class CombatZombieModel<T extends CombatZombie> extends HumanoidModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "combat_zombie"), "main");

    public CombatZombieModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /**
     * 【最终动画修复 - 整体协调版】
     */
    @Override
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);

        if (pEntity.isInSittingPose()) {
            this.rightArm.xRot = -0.75F;
            this.leftArm.xRot = -0.75F;
            this.rightLeg.xRot = -1.4F;//右腿抬起
            this.rightLeg.yRot = 0.4F;//右腿分开
            this.leftLeg.xRot = -1.4F ;//左腿抬起
            this.leftLeg.yRot = -0.4F;//左腿分开
        }
    }
}