package com.sake.mobfriends.factions;

import com.google.common.collect.ImmutableMap;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 派系处理器 - 重构后版本
 * 这个类现在是一个纯粹的工具类，负责定义和查询派系信息。
 * 它不再管理任何动态数据（如玩家友好度），这些数据已由 ModAttachments 处理。
 */
public class FactionHandler {

    // 定义每个派系所包含的生物实体类型标签
    private static final Map<String, TagKey<EntityType<?>>> FACTION_MEMBERS = ImmutableMap.<String, TagKey<EntityType<?>>>builder()
            .put("zombies", ModTags.Entities.ZOMBIES)
            .put("skeletons", ModTags.Entities.SKELETONS)
            .put("creepers", ModTags.Entities.CREEPERS)
            .put("slimes", ModTags.Entities.SLIMES)
            .put("endermen", ModTags.Entities.ENDERMEN)
            .put("blazes", ModTags.Entities.BLAZES)
            .build();

    // 定义与每个派系关联的信物（Token）物品
    private static final Map<String, Supplier<Item>> FACTION_TOKENS = ImmutableMap.<String, Supplier<Item>>builder()
            .put("zombies", ModItems.ZOMBIE_TOKEN)
            .put("skeletons", ModItems.SKELETON_TOKEN)
            .put("creepers", ModItems.CREEPER_TOKEN)
            .put("slimes", ModItems.SLIME_TOKEN)
            .put("endermen", ModItems.ENDERMAN_TOKEN)
            .put("blazes", ModItems.BLAZE_TOKEN)
            .build();

    /**
     * 检查一个玩家是否对某个特定派系友好。
     * @param player 要检查的玩家
     * @param factionName 派系的名称 (例如 "zombies")
     * @return 如果玩家对该派系友好，则返回 true
     */
    public static boolean isFriendly(Player player, String factionName) {
        // 直接从玩家的 Data Attachment 中获取友好的派系列表并进行检查
        return ModAttachments.getFriendlyFactions(player).contains(factionName);
    }

    /**
     * 根据一个生物实体，判断它属于哪个派系。
     * @param entity 要检查的生物
     * @return 如果生物属于任何一个已定义的派系，则返回该派系的名称；否则返回 null
     */
    @Nullable
    public static String getFaction(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        // 遍历所有派系，检查该生物的类型是否匹配派系的成员标签
        for (Map.Entry<String, TagKey<EntityType<?>>> entry : FACTION_MEMBERS.entrySet()) {
            if (type.is(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 根据一个物品，判断它是否是某个派系的信物。
     * @param item 要检查的物品
     * @return 如果该物品是任何一个派系的信物，则返回该派系的名称；否则返回 null
     */
    @Nullable
    public static String getFactionByToken(Item item) {
        // 遍历所有派系信物，检查物品是否匹配
        for (Map.Entry<String, Supplier<Item>> entry : FACTION_TOKENS.entrySet()) {
            if (entry.getValue().get() == item) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 获取所有已定义的派系名称。
     * @return 一个包含所有派系名称的集合
     */
    public static Set<String> getFactionNames() {
        return FACTION_MEMBERS.keySet();
    }
}