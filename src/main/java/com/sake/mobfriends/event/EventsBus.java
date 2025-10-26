package com.sake.mobfriends.event;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.*;
import com.sake.mobfriends.init.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

// NeoForge 1.21.1 移植要点:
// 1. @EventBusSubscriber: 这是一个注解，告诉 NeoForge 这个类中包含了事件监听器。
//    - modid: 指定你的模组ID。
//    - bus = EventBusSubscriber.Bus.MOD: 表示这个监听器监听的是 MOD 事件总线。
// 2. @SubscribeEvent: 标记一个方法为事件处理方法。
// 3. EntityAttributeCreationEvent: 这个事件在实体属性需要被创建时触发。
//    - 我们需要在这里为我们的自定义生物添加属性，否则游戏会崩溃。
@EventBusSubscriber(modid = MobFriends.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class EventsBus {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        // 为每个自定义生物注册属性
        event.put(ModEntities.COMBAT_ZOMBIE.get(), CombatZombie.createAttributes().build());
        event.put(ModEntities.COMBAT_WITHER.get(), CombatWither.createAttributes().build());
        event.put(ModEntities.COMBAT_CREEPER.get(), CombatCreeper.createAttributes().build());
        event.put(ModEntities.COMBAT_BLAZE.get(), CombatBlaze.createAttributes().build());
        event.put(ModEntities.ZOMBIE_NPC.get(), ZombieNpcEntity.createAttributes().build());
        event.put(ModEntities.SKELETON_NPC.get(), SkeletonNpcEntity.createAttributes().build());
        event.put(ModEntities.CREEPER_NPC.get(), CreeperNpcEntity.createAttributes().build());
        event.put(ModEntities.ENDERMAN_NPC.get(), EndermanNpcEntity.createAttributes().build());
        event.put(ModEntities.SLIME_NPC.get(), SlimeNpcEntity.createAttributes().build());
        event.put(ModEntities.BLAZE_NPC.get(), BlazeNpcEntity.createAttributes().build());
    }
}