package com.sake.mobfriends.client.model;

import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public class CombatWitherModel extends HumanoidModel<CombatWither> {
    public CombatWitherModel(ModelPart root) {
        super(root);
    }

    /**
     * 【新增】重写动画设置方法
     */
    @Override
    public void setupAnim(CombatWither pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        // 同样，先调用父类方法处理基础动画
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);

        // 检查坐下状态
        if (pEntity.isInSittingPose()) {
            // 覆盖腿部动画，让它坐下
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.y = 12.0F;
            this.rightLeg.z = 4.0F;

            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.y = 12.0F;
            this.leftLeg.z = 4.0F;
        }
    }
}