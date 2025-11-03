package com.sake.mobfriends.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

// 这是一个“被动”效果，它本身不做任何事。
// 真正的逻辑将在 ForgeBus.java 中通过检查实体是否拥有此效果来实现。
public class SoulLinkEffect extends MobEffect {
    public SoulLinkEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);
    }
}