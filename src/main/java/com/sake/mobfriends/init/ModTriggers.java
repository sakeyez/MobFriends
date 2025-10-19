package com.sake.mobfriends.init;

import com.sake.mobfriends.advancements.BecomeFriendlyTrigger;

public class ModTriggers {

    // 只创建实例，不在任何地方主动注册它
    public static final BecomeFriendlyTrigger BECAME_FRIENDLY_WITH_FACTION = new BecomeFriendlyTrigger();

    public static void register() {
        // 这个方法现在完全是空的，也不应该被调用
    }
}