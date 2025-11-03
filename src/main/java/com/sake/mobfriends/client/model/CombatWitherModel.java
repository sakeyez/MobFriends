// 文件路径: src/main/java/com/sake/mobfriends/client/model/CombatWitherModel.java

package com.sake.mobfriends.client.model;

import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

public class CombatWitherModel extends HumanoidModel<CombatWither> {
    public CombatWitherModel(ModelPart root) {
        super(root);
    }

    /**
     * 【最终动画修复 - 整体协调版】
     * 应用了为僵尸战士微调后的参数
     */
    @Override
    public void setupAnim(CombatWither pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        // 首先，运行父类的默认动画，这会设置好 this.riding 等状态
        super.setupAnim(pEntity, pLimbSwing, pLimbSwingAmount, pAgeInTicks, pNetHeadYaw, pHeadPitch);

        // --- 【核心修改】 ---
        // 当实体被命令坐下(isInSittingPose)或作为乘客(riding)时，应用坐姿动画
        if (this.riding || pEntity.isInSittingPose()) {
            // 手臂自然下垂
            this.rightArm.xRot = -0.75F;
            this.leftArm.xRot = -0.75F;

            // 腿部弯曲并分开，形成坐姿
            this.rightLeg.xRot = -1.4F;      // 右腿抬起
            this.rightLeg.yRot = 0.4F;       // 右腿向外分开
            this.leftLeg.xRot = -1.4F;       // 左腿抬起
            this.leftLeg.yRot = -0.4F;       // 左腿向外分开
        }
    }
}