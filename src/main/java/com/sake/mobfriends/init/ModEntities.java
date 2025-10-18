package com.sake.mobfriends.init;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// NeoForge 1.21.1 移植要点:
// 1. 注册表类型: 实体类型的注册表是 BuiltInRegistries.ENTITY_TYPE。
// 2. EntityType.Builder: 使用 Builder 模式来创建实体类型。
//    - of(): 指定实体的构造函数引用 (例如 Combat_Zombie::new)。
//    - mobCategory(): 定义实体的分类 (例如 MONSTER, CREATURE)。
//    - dimensions(): 设置实体的碰撞箱大小。
//    - build(): 最后调用 build() 并传入注册名。
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MobFriends.MOD_ID);

    // --- 战斗型生物 ---
    public static final Supplier<EntityType<CombatZombie>> COMBAT_ZOMBIE = ENTITY_TYPES.register("combat_zombie",
            () -> EntityType.Builder.of(CombatZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F) // 尺寸参考原版僵尸
                    .build("combat_zombie"));

    public static final Supplier<EntityType<CombatWither>> COMBAT_WITHER = ENTITY_TYPES.register("combat_wither",
            () -> EntityType.Builder.of(CombatWither::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F) // 尺寸参考原版凋灵骷髅
                    .build("combat_wither"));

    // --- NPC 生物 ---
    public static final Supplier<EntityType<ZombieNpcEntity>> ZOMBIE_NPC = ENTITY_TYPES.register("zombie_npc",
            () -> EntityType.Builder.of(ZombieNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build("zombie_npc"));
    public static final Supplier<EntityType<SkeletonNpcEntity>> SKELETON_NPC = ENTITY_TYPES.register("skeleton_npc",
            () -> EntityType.Builder.of(SkeletonNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.99F)
                    .build("skeleton_npc"));
    public static final Supplier<EntityType<CreeperNpcEntity>> CREEPER_NPC = ENTITY_TYPES.register("creeper_npc",
            () -> EntityType.Builder.of(CreeperNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.7F)
                    .build("creeper_npc"));
    public static final Supplier<EntityType<EndermanNpcEntity>> ENDERMAN_NPC = ENTITY_TYPES.register("enderman_npc",
            () -> EntityType.Builder.of(EndermanNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 2.9F)
                    .build("enderman_npc"));
    public static final Supplier<EntityType<SlimeNpcEntity>> SLIME_NPC = ENTITY_TYPES.register("slime_npc",
            () -> EntityType.Builder.of(SlimeNpcEntity::new, MobCategory.CREATURE)
                    .sized(2.04F, 2.04F) // 尺寸参考大史莱姆
                    .build("slime_npc"));
    public static final Supplier<EntityType<BlazeNpcEntity>> BLAZE_NPC = ENTITY_TYPES.register("blaze_npc",
            () -> EntityType.Builder.of(BlazeNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("blaze_npc"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}