package com.sake.mobfriends.client.model;

import com.sake.mobfriends.entity.CombatWither;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;

// 【移植】这个类现在只作为一个类型包装器，不再需要定义自己的模型层
public class CombatWitherModel extends HumanoidModel<CombatWither> {
    public CombatWitherModel(ModelPart root) {
        super(root);
    }
}