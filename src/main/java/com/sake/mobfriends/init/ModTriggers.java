package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.advancements.BecomeFriendlyTrigger;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Supplier;

public class ModTriggers {
    // 核心修复 #1: 使用正确的 ResourceLocation.fromNamespaceAndPath 方法
    public static final DeferredRegister<BecomeFriendlyTrigger> TRIGGERS = DeferredRegister.create(
            ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "triggers"), MobFriends.MOD_ID
    );

    public static final Supplier<BecomeFriendlyTrigger> BECAME_FRIENDLY_WITH_FACTION = TRIGGERS.register(
            "become_friendly_with_faction", BecomeFriendlyTrigger::new
    );

    public static void register(RegisterEvent event) {
        // 核心修复 #2: 在这里也使用正确的方法
        CriteriaTriggers.register(
                ResourceLocation.fromNamespaceAndPath(MobFriends.MOD_ID, "become_friendly_with_faction").toString(),
                BECAME_FRIENDLY_WITH_FACTION.get()
        );
    }
}