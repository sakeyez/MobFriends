package com.sake.mobfriends.event;

import com.sake.mobfriends.config.FeedingConfig;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.trading.TradeManager; // 确保导入 TradeManager
import com.sake.mobfriends.util.ModTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent; // 确保导入 AddReloadListenerEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import com.sake.mobfriends.entity.CombatZombie;
import com.sake.mobfriends.item.AbstractCoreItem;
import com.sake.mobfriends.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import java.util.UUID;
import java.util.Set;
import java.util.function.Supplier;


@EventBusSubscriber(modid = com.sake.mobfriends.MobFriends.MOD_ID)
public class ForgeBus {

    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide()) return;

        Supplier<Item> tokenSupplier = null;
        EntityType<?> type = entity.getType();

        if (type.is(ModTags.Entities.ZOMBIES)) {
            tokenSupplier = ModItems.ZOMBIE_TOKEN;
        } else if (type.is(ModTags.Entities.SKELETONS)) {
            tokenSupplier = ModItems.SKELETON_TOKEN;
        }

        if (tokenSupplier != null && FeedingConfig.getFoodBlocks(type).contains(event.getEatenBlockState().getBlock())) {
            ItemEntity itemEntity = new ItemEntity(level, entity.getX(), entity.getY() + 0.5, entity.getZ(), new ItemStack(tokenSupplier.get()));
            level.addFreshEntity(itemEntity);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof CombatZombie deadZombie)) {
            return;
        }

        UUID deadZombieUUID = deadZombie.getUUID();

        for (Player player : event.getEntity().level().players()) {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(ModItems.ACTIVE_ZOMBIE_CORE.get())) {
                    UUID coreUUID = AbstractCoreItem.getZombieUUID(stack);
                    if (deadZombieUUID.equals(coreUUID)) {
                        ItemStack brokenCore = new ItemStack(ModItems.BROKEN_ZOMBIE_CORE.get());
                        AbstractCoreItem.setZombieUUID(brokenCore, deadZombieUUID);
                        inventory.setItem(i, brokenCore);
                        // 找到了就没必要继续循环了
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }
        EntityType<?> type = mob.getType();

        Set<Block> foodBlocks = FeedingConfig.getFoodBlocks(type);

        if (!foodBlocks.isEmpty()) {
            mob.goalSelector.addGoal(1, new EatBlockFoodGoal(
                    mob,
                    1.0D,
                    6,
                    foodBlocks::contains
            ));
        }
    }

    /**
     * 监听数据包重载事件，并将我们的 TradeManager 注册进去。
     * 这是确保交易文件 (trades.json) 被加载的关键。
     */
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new TradeManager());
        // 我们使用一个新的日志消息来确认这个方法被调用了
        com.sake.mobfriends.MobFriends.LOGGER.info("TradeManager successfully registered via Forge event bus.");
    }
}