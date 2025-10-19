/*
 * 最终修复说明:
 * 这个问题是Java泛型推断中最棘手的一种。
 *
 * 错误原因: 在 .xmap(HashSet::new, ArrayList::new) 中，第二个参数 `ArrayList::new`
 * 丢失了泛型信息。编译器无法推断出要创建一个 `ArrayList<String>`，而是推断了 `ArrayList<Object>`，
 * 导致类型不匹配。
 *
 * 解决方案: 我们将 `ArrayList::new` 替换为一个更明确的Lambda表达式: `set -> new ArrayList<>(set)`。
 * 这个表达式接收一个类型为 `HashSet<String>` 的 `set`，并返回一个 `new ArrayList<String>(set)`，
 * 从而为编译器提供了足够的信息来正确推断类型。
 *
 * 这将是这个文件需要解决的最后一个编译错误。
 */
package com.sake.mobfriends.attachments;

import com.mojang.serialization.Codec;
import com.sake.mobfriends.MobFriends;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List; // 确保导入 List
import java.util.Set;
import java.util.function.Function; // 确保导入 Function
import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MobFriends.MOD_ID);

    public static final Supplier<AttachmentType<HashSet<String>>> FRIENDLY_FACTIONS = ATTACHMENT_TYPES.register(
            "friendly_factions",
            () -> AttachmentType.builder(() -> new HashSet<String>())
                    // 【最终修正点】明确指定 xmap 的转换类型
                    .serialize(Codec.STRING.listOf().xmap(
                            (Function<List<String>, HashSet<String>>) HashSet::new,
                            (Function<HashSet<String>, List<String>>) ArrayList::new
                    ))
                    .build()
    );

    public static Set<String> getFriendlyFactions(Player player) {
        return player.getData(FRIENDLY_FACTIONS.get());
    }
}