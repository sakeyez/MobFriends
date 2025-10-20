package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.factions.FactionHandler;
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
        // 1. 如果目标不是玩家，或者攻击者是玩家，则忽略此事件
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || event.getEntity() instanceof Player) {
            return;
        }

        // 2. 获取攻击生物所属的派系
        String faction = FactionHandler.getFaction(event.getEntity());
        if (faction == null) {
            // 如果这个生物不属于我们定义的任何派系，就保持它的原版行为
            return;
        }

        // 3. 检查玩家与该派系的关系
        if (FactionHandler.isFriendly(player, faction)) {
            // 如果玩家已经与该派系“友好”（即收集过谢意），则生物永远不会攻击玩家
            event.setCanceled(true);
        } else {
            // 4. 【核心逻辑】如果玩家与该派系还不是朋友，则应用“中立”规则
            // 只有当这个生物被玩家攻击过之后，它才能将玩家设为目标
            if (event.getEntity().getLastHurtByMob() == null || !event.getEntity().getLastHurtByMob().getUUID().equals(player.getUUID())) {
                event.setCanceled(true); // 取消此次目标变更
            }
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

            MobFriends.BECAME_FRIENDLY_WITH_FACTION.trigger(serverPlayer, faction);
        }
    }
}