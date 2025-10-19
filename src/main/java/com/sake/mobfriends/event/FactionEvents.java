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
    public static void onPlayerPickupItem(ItemEntityPickupEvent.Pre event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        String faction = FactionHandler.getFactionByToken(event.getItemEntity().getItem().getItem());
        if (faction == null) {
            return;
        }

        Set<String> friendlyFactions = ModAttachments.getFriendlyFactions(serverPlayer);

        if (friendlyFactions.add(faction)) {
            // 【移植修正】直接调用触发器的 trigger 方法，不再需要 .get()
            ModTriggers.BECAME_FRIENDLY_WITH_FACTION.trigger(serverPlayer, faction);
        }
    }
}