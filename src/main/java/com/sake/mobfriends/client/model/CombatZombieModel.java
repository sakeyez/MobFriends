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
import net.minecraft.util.Mth;

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
     * 【新增】重写动画设置方法
     */
    @Override
    public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        // 首先，调用父类的方法，让站立、行走、攻击等基本动画先生效
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);

        // 然后，我们检查实体是否处于坐下状态
        if (pEntity.isInSittingPose()) {
            // 如果是坐着，我们就覆盖腿部的动画
            // 将右腿沿X轴旋转-1.4弧度（大约-80度），并稍微调整Y和Z坐标
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.y = 12.0F;
            this.rightLeg.z = 4.0F;

            // 对左腿做同样的操作
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.y = 12.0F;
            this.leftLeg.z = 4.0F;
        }
    }
}