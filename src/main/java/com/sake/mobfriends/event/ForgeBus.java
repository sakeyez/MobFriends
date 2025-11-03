package com.sake.mobfriends.event;

import com.sake.mobfriends.config.FeedingConfig;
import com.sake.mobfriends.entity.*;
import com.sake.mobfriends.entity.ai.EatBlockFoodGoal; // 【修复二】恢复被误删的导入
import com.sake.mobfriends.init.ModDataComponents;
import com.sake.mobfriends.init.ModEffects;
import com.sake.mobfriends.init.ModItems;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import com.sake.mobfriends.init.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber(modid = com.sake.mobfriends.MobFriends.MOD_ID)
public class ForgeBus {



    @SubscribeEvent
    public static void onMobFinishedEating(MobFinishedEatingEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide()) return;

        if (entity instanceof AbstractWarriorEntity warrior) {
            warrior.handleRitualBlockEaten(event.getEatenBlockState());
            return;
        }

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
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile().level().isClientSide()) {
            return;
        }

        HitResult hitResult = event.getRayTraceResult();
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();

            if (target instanceof AbstractWarriorEntity warrior) {
                if (event.getProjectile() instanceof ThrowableItemProjectile projectile) {

                    // 【最终修改】
                    ItemStack itemStack = projectile.getItem();
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());

                    // 定义所有包子的ID
                    ResourceLocation regularBaoziId = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "baozi");
                    ResourceLocation soulBaoziId = ResourceLocation.fromNamespaceAndPath(com.sake.mobfriends.MobFriends.MOD_ID, "soul_baozi");
                    ResourceLocation samsaBaoziId = ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "samsa");

                    ResourceLocation upgradedSoulBaoziId = ResourceLocation.fromNamespaceAndPath(com.sake.mobfriends.MobFriends.MOD_ID, "upgraded_soul_baozi");
                    ResourceLocation upgradedSamsaId = ResourceLocation.fromNamespaceAndPath(com.sake.mobfriends.MobFriends.MOD_ID, "upgraded_samsa");


                    boolean didApplyEffect = false;

                    // --- 澎湃灵魂包 (升级版) ---
                    if (itemId.equals(upgradedSoulBaoziId)) {
                        warrior.addEffect(new MobEffectInstance((Holder<MobEffect>) ModEffects.SOUL_LINK, 2400, 0));
                        warrior.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3)); // 伤害吸收 IV (等级 3)

                        if (warrior.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SOUL, warrior.getX(), warrior.getY() + warrior.getBbHeight() / 2.0, warrior.getZ(), 50, 0.5, 0.5, 0.5, 0.1);

                        }
                        didApplyEffect = true;
                    }
                    // --- 喷香烤包子 (升级版) ---
                    else if (itemId.equals(upgradedSamsaId)) {
                        warrior.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 1)); // 速度 II (等级 1)
                        warrior.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 3)); // 力量 IV (等级 3)

                        if (warrior.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.LAVA, warrior.getX(), warrior.getY() + warrior.getBbHeight() / 2.0, warrior.getZ(), 40, 0.5, 0.5, 0.5, 0.1);
                        }
                        didApplyEffect = true;
                    }
                    // --- 普通灵魂包 ---
                    else if (itemId.equals(soulBaoziId)) {
                        warrior.heal(15.0f);
                        warrior.addEffect(new MobEffectInstance((Holder<MobEffect>) ModEffects.SOUL_LINK, 2400, 0, false, true, true));
                        if (warrior.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.SOUL, warrior.getX(), warrior.getY() + warrior.getBbHeight() / 2.0, warrior.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                        }
                        didApplyEffect = true;
                    }
                    // --- 普通烤包子 ---
                    else if (itemId.equals(samsaBaoziId)) {
                        warrior.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0)); // 速度 1
                        if (warrior.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.LAVA, warrior.getX(), warrior.getY() + warrior.getBbHeight() / 2.0, warrior.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
                        }
                        didApplyEffect = true;
                    }
                    // --- 普通包子 ---
                    else if (itemId.equals(regularBaoziId)) {
                        warrior.heal(10.0f);
                        if (warrior.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART, warrior.getX(), warrior.getY() + warrior.getBbHeight() / 2.0, warrior.getZ(), 5, 0.3, 0.3, 0.3, 0.05);
                        }
                        didApplyEffect = true;
                    }

                    // --- 如果命中了任意一种包子 ---
                    if (didApplyEffect) {
                        event.setCanceled(true); // 取消事件（阻止伤害和击退）
                        projectile.discard();   // 移除投掷物
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || event.getOriginalDamage() <= 0) {
            return;
        }

        DamageSource source = event.getSource();
        LivingEntity damagedEntity = event.getEntity();
        float damageAmount = event.getOriginalDamage();

        Optional<ResourceKey<DamageType>> optKey = damagedEntity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getResourceKey(source.type());

        if (optKey.isPresent()) {
            ResourceKey<DamageType> key = optKey.get();
            boolean isWitherOrPoison = key.equals(DamageTypes.WITHER) || key.location().getPath().contains("poison");

            if (isWitherOrPoison) {
                final float HEAL_AMOUNT = 1.0F;

                AABB searchArea = new AABB(damagedEntity.blockPosition()).inflate(32.0D);
                List<CombatWither> nearbyWithers = damagedEntity.level().getEntitiesOfClass(CombatWither.class, searchArea);

                for (CombatWither witherSource : nearbyWithers) {
                    if (witherSource.hasLifestealSkill()) {
                        witherSource.heal(HEAL_AMOUNT);
                    }
                }
            }
        }

        if (damagedEntity instanceof Player player) {
            if (source.getEntity() != player) {
                List<AbstractWarriorEntity> linkedWarriors = player.level().getEntitiesOfClass(
                        AbstractWarriorEntity.class,
                        player.getBoundingBox().inflate(64.0D),

                        // 【最终修复】同样，将 SOUL_LINK 变量本身转换为 Holder
                        warrior -> warrior.isOwnedBy(player) && warrior.hasEffect((Holder<MobEffect>) ModEffects.SOUL_LINK) && warrior.isAlive()
                );

                if (!linkedWarriors.isEmpty()) {
                    AbstractWarriorEntity bodyguard = linkedWarriors.get(0);

                    // 【修复】使用 .genericKill() 这一必定存在的伤害源来绕过防御
                    bodyguard.hurt(bodyguard.level().damageSources().genericKill(), damageAmount);

                    if(bodyguard.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                player.getX(), player.getY() + player.getBbHeight() / 2.0, player.getZ(),
                                15, 0.5, 0.5, 0.5, 0.0);
                        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                                bodyguard.getX(), bodyguard.getY() + bodyguard.getBbHeight() / 2.0, bodyguard.getZ(),
                                (int)(damageAmount * 2), 0.3, 0.3, 0.3, 0.2);
                    }

                    // 【修复】错误 3: 使用 setNewDamage(0) 替换 setAmount(0)
                    event.setNewDamage(0);
                    return;
                }
            }
        }

        if (source.getDirectEntity() instanceof SmallFireball &&
                source.getEntity() instanceof CombatBlaze blaze) {
            // 【修复】注意：这里应该用 setNewDamage
            event.setNewDamage(blaze.getFireballDamage());
        }
    }
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }
        EntityType<?> type = mob.getType();

        Set<Block> foodBlocks = FeedingConfig.getFoodBlocks(type);

        // 【修复二】恢复被误删的 EatBlockFoodGoal AI 添加逻辑
        if (!foodBlocks.isEmpty()) {
            mob.goalSelector.addGoal(1, new EatBlockFoodGoal(
                    mob,
                    1.0D,
                    6,
                    foodBlocks::contains
            ));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity deadEntity = event.getEntity();
        if (deadEntity instanceof CombatZombie deadZombie) {
            UUID deadUUID = deadZombie.getUUID();
            for (Player player : deadZombie.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_ZOMBIE_CORE.get())) {
                        UUID coreUUID = stack.get(ModDataComponents.ZOMBIE_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_ZOMBIE_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadZombie.save(data);
                            brokenCore.set(ModDataComponents.STORED_ZOMBIE_NBT.get(), data);
                            brokenCore.set(ModDataComponents.ZOMBIE_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
        else if (deadEntity instanceof CombatWither deadWither) {
            UUID deadUUID = deadWither.getUUID();
            for (Player player : deadWither.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_WITHER_CORE.get())) {
                        UUID coreUUID = stack.get(ModDataComponents.WITHER_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_WITHER_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadWither.save(data);
                            brokenCore.set(ModDataComponents.STORED_WITHER_NBT.get(), data);
                            brokenCore.set(ModDataComponents.WITHER_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
        else if (deadEntity instanceof CombatCreeper deadCreeper) {
            UUID deadUUID = deadCreeper.getUUID();
            for (Player player : deadCreeper.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_CREEPER_CORE.get())) {
                        UUID coreUUID = stack.get(ModDataComponents.CREEPER_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_CREEPER_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadCreeper.save(data);
                            brokenCore.set(ModDataComponents.STORED_CREEPER_NBT.get(), data);
                            brokenCore.set(ModDataComponents.CREEPER_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
        else if (deadEntity instanceof CombatBlaze deadBlaze) {
            UUID deadUUID = deadBlaze.getUUID();
            for (Player player : deadBlaze.level().players()) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.is(ModItems.ACTIVE_BLAZE_CORE.get())) {
                        UUID coreUUID = stack.get(ModDataComponents.BLAZE_UUID.get());
                        if (deadUUID.equals(coreUUID)) {
                            ItemStack brokenCore = new ItemStack(ModItems.BROKEN_BLAZE_CORE.get());
                            CompoundTag data = new CompoundTag();
                            deadBlaze.save(data);
                            brokenCore.set(ModDataComponents.STORED_BLAZE_NBT.get(), data);
                            brokenCore.set(ModDataComponents.BLAZE_UUID.get(), deadUUID);
                            inventory.setItem(i, brokenCore);
                            return;
                        }
                    }
                }
            }
        }
    }
}