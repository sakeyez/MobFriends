package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.effects.SoulLinkEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MobFriends.MOD_ID);

    // 注册“灵魂链接”效果，颜色为青色
    public static final Supplier<MobEffect> SOUL_LINK = EFFECTS.register("soul_link",
            () -> new SoulLinkEffect(MobEffectCategory.BENEFICIAL, 0x00D1C1));

    // 【修复】确保这个方法是 public static void
    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}