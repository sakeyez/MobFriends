package com.sake.mobfriends.attachments;

import com.mojang.serialization.Codec;
import com.sake.mobfriends.MobFriends;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MobFriends.MOD_ID);

    public static final Supplier<AttachmentType<Set<String>>> FRIENDLY_FACTIONS = ATTACHMENT_TYPES.register(
            "friendly_factions",
            () -> {
                // 【核心移植 - 最终修正】
                // 1. 使用 AttachmentType.<Set<String>>builder(...) 明确指定泛型为 Set<String> 接口，解决“不兼容的类型”问题。
                // 2. 使用 () -> new HashSet<>() 的 Lambda 表达式，明确提供一个 Supplier，解决“对builder的引用不明确”的问题。
                // 这样就同时解决了前两次尝试中遇到的所有问题。
                // 参考教程: version-1.21.1.zip/datastorage/attachments.md
                return AttachmentType.<Set<String>>builder(() -> new HashSet<>())
                        .serialize(Codec.STRING.listOf().xmap(HashSet::new, List::copyOf))
                        .build();
            }
    );

    public static Set<String> getFriendlyFactions(Player player) {
        // 在调用 getData 时，必须使用 .get() 来获取 AttachmentType 实例
        return player.getData(FRIENDLY_FACTIONS.get());
    }
}