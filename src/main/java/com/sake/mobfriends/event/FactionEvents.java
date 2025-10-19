package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.factions.FactionHandler;
import com.sake.mobfriends.init.ModTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.Set;

@EventBusSubscriber(modid = MobFriends.MOD_ID)
public class FactionEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) {
            return;
        }

        String faction = FactionHandler.getFaction(event.getEntity());
        if (faction == null) {
            return;
        }

        if (FactionHandler.isFriendly(player, faction)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerPickupItem(ItemEntityPickupEvent event) {
        // 【核心移植 - 最终修正】
        // 根据您提供的 PlayerEvent.java 源代码，使用 getPlayer() 是获取玩家的最可靠方法。
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 从事件中获取物品实体，再从中获取物品堆栈
        String faction = FactionHandler.getFactionByToken(event.getItemEntity().getItem().getItem());
        if (faction == null) {
            return;
        }

        Set<String> friendlyFactions = ModAttachments.getFriendlyFactions(serverPlayer);

        if (friendlyFactions.add(faction)) {
            ModTriggers.BECAME_FRIENDLY_WITH_FACTION.get().trigger(serverPlayer, faction);
        }
    }
}