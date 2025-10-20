package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends; // <-- 【第一处修改】导入主类以使用LOGGER
import com.sake.mobfriends.attachments.ModAttachments;
import com.sake.mobfriends.factions.FactionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;

import java.util.Set;

@EventBusSubscriber(modid = MobFriends.MOD_ID)
public class FactionEvents {

    // onLivingChangeTarget 方法保持不变
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player) || event.getEntity() instanceof Player) {
            return;
        }
        String faction = FactionHandler.getFaction(event.getEntity());
        if (faction == null) {
            return;
        }
        if (FactionHandler.isFriendly(player, faction)) {
            event.setCanceled(true);
        } else {
            if (event.getEntity().getLastHurtByMob() == null || !event.getEntity().getLastHurtByMob().getUUID().equals(player.getUUID())) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 当玩家获得一个成就时触发。
     */
    @SubscribeEvent
    public static void onAdvancementGranted(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ResourceLocation advancementId = event.getAdvancement().id();

        // 检查这个成就是不是我们模组的，并且是不是一个“成为朋友”的成就
        if (advancementId.getNamespace().equals(MobFriends.MOD_ID) && advancementId.getPath().startsWith("friendship/")) {

            // --- 【第二处修改】添加日志 ---
            // 如果代码能执行到这里，说明 if 判断成功了！
            MobFriends.LOGGER.info("检测到友好成就完成！ID: {}", advancementId);

            String faction = advancementId.getPath().substring("friendship/".length());
            Set<String> friendlyFactions = ModAttachments.getFriendlyFactions(serverPlayer);

            // 记录添加前的状态
            boolean alreadyFriendly = friendlyFactions.contains(faction);

            friendlyFactions.add(faction);

            // --- 【第三处修改】添加最终确认日志 ---
            // 这条日志会告诉我们玩家和哪个阵营的关系发生了变化。
            if (!alreadyFriendly) {
                MobFriends.LOGGER.info("玩家 '{}' 已与阵营 '{}' 成为友好关系！", serverPlayer.getName().getString(), faction);
            } else {
                MobFriends.LOGGER.info("玩家 '{}' 与阵营 '{}' 的友好关系已确认。", serverPlayer.getName().getString(), faction);
            }
        }
    }
}