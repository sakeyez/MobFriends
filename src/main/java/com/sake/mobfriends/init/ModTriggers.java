package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.advancements.BecomeFriendlyTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModTriggers {

    // 1. 创建一个用于 CriterionTrigger 的 DeferredRegister
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, MobFriends.MOD_ID);

    // 2. 注册你的自定义触发器
    // "become_friendly_with_faction" 是你的触发器 ID，它必须和你的 advancement json 文件以及 BecomeFriendlyTrigger.java 中定义的一致
    public static final DeferredHolder<CriterionTrigger<?>, BecomeFriendlyTrigger> BECAME_FRIENDLY_WITH_FACTION = TRIGGERS.register("become_friendly_with_faction", BecomeFriendlyTrigger::new);

    /**
     * 这个方法不再需要，因为注册工作已经由上面的 TRIGGERS.register() 完成。
     * 你可以选择删除它，或者保留一个空方法。
     */
    public static void register() {
        // No-op
    }
}