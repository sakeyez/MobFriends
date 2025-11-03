package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
// 【修改】不再需要导入旧的技能AI文件
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CombatZombie extends AbstractWarriorEntity {

    // 【新增】技能计时器字段
    private int healSkillCooldown = 0;
    private int teleportSkillCooldown = 0;

    // --- 其他字段保持不变 ---
    private static final Set<Block> TIER_1_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "crystal_lamb_chop")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    private static final Set<Block> TIER_2_BLOCKS = Stream.of(
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "sweet_and_sour_ender_pearls"),
            ResourceLocation.fromNamespaceAndPath("kaleidoscope_cookery", "golden_salad")
    ).map(BuiltInRegistries.BLOCK::get).filter(block -> block != Blocks.AIR).collect(Collectors.toSet());

    private static final EntityDataAccessor<Boolean> DATA_HAS_TELEPORT_SKILL = SynchedEntityData.defineId(CombatZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_HEAL_SKILL = SynchedEntityData.defineId(CombatZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BONUS_REDUCTION = SynchedEntityData.defineId(CombatZombie.class, EntityDataSerializers.BOOLEAN);

    public CombatZombie(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /**
     * 【新增】重写实体的 tick() 方法。
     * 这个方法每游戏刻都会执行，是实现可靠计时器的最佳位置。
     */
    @Override
    public void tick() {
        super.tick();

        // 确保逻辑只在服务器端运行，避免客户端执行不必要的操作
        if (this.level().isClientSide()) {
            return;
        }

        // --- 黄金沙拉技能：计时器逻辑 ---
        if (this.hasHealSkill()) {
            // 条件：处于非满血状态
            if (this.getHealth() < this.getMaxHealth()) {
                if (this.healSkillCooldown > 0) {
                    this.healSkillCooldown--; // 计时器倒数
                } else {
                    // 时间到！触发技能效果
                    triggerHealSkill();
                    // 重置计时器为20秒
                    this.healSkillCooldown = 400; // 20 seconds * 20 ticks/second
                }
            }
        }

        // --- 珍珠咕咾肉技能：计时器逻辑 ---
        if (this.hasTeleportSkill()) {
            LivingEntity target = this.getTarget();
            // 条件：进入战斗状态 (有存活的目标)
            if (target != null && target.isAlive()) {
                if (this.teleportSkillCooldown > 0) {
                    this.teleportSkillCooldown--; // 计时器倒数
                } else {
                    // 时间到！检查距离并尝试传送
                    double distanceSq = this.distanceToSqr(target);
                    if (distanceSq > 36.0D) { // 距离大于6格时传送
                        teleportNearTarget(target);
                    }
                    // 重置计时器为10秒
                    this.teleportSkillCooldown = 200; // 10 seconds * 20 ticks/second
                }
            } else {
                // 如果脱离战斗，立即重置传送计时器，为下一次战斗做准备
                this.teleportSkillCooldown = 0;
            }
        }
    }

    /**
     * 【新增】触发治疗技能效果的辅助方法。
     */
    private void triggerHealSkill() {
        // 1. 净化：清除所有负面效果
        new ArrayList<>(this.getActiveEffects()).stream()
                .filter(effect -> !effect.getEffect().value().isBeneficial())
                .forEach(effect -> this.removeEffect(effect.getEffect()));

        // 2. 治疗：恢复20点生命值 (10颗心)
        this.heal(20.0F);

        // 3. 护盾：获得伤害吸收I，持续10秒
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
    }

    /**
     * 【新增】传送到目标附近的辅助方法。
     */
    private void teleportNearTarget(LivingEntity target) {
        Vec3 targetPos = target.position();
        for (int i = 0; i < 10; ++i) { // 尝试10次找到一个可传送的位置
            double x = targetPos.x + (this.getRandom().nextDouble() - 0.5D) * 8.0D;
            double y = targetPos.y + (this.getRandom().nextInt(5) - 2);
            double z = targetPos.z + (this.getRandom().nextDouble() - 0.5D) * 8.0D;
            if (this.randomTeleport(x, y, z, true)) {
                // 传送成功后播放音效，增加表现力
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, this.getSoundSource(), 1.0F, 1.0F);
                return; // 传送成功后立即退出
            }
        }
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_TELEPORT_SKILL, false);
        builder.define(DATA_HAS_HEAL_SKILL, false);
        builder.define(DATA_HAS_BONUS_REDUCTION, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HasTeleportSkill", this.hasTeleportSkill());
        compound.putBoolean("HasHealSkill", this.hasHealSkill());
        compound.putBoolean("HasBonusReduction", this.entityData.get(DATA_HAS_BONUS_REDUCTION));
        // 保存计时器状态，以便在游戏重载后能继续计时
        compound.putInt("HealCooldown", this.healSkillCooldown);
        compound.putInt("TeleportCooldown", this.teleportSkillCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DATA_HAS_TELEPORT_SKILL, compound.getBoolean("HasTeleportSkill"));
        this.entityData.set(DATA_HAS_HEAL_SKILL, compound.getBoolean("HasHealSkill"));
        this.entityData.set(DATA_HAS_BONUS_REDUCTION, compound.getBoolean("HasBonusReduction"));
        // 读取计时器状态
        this.healSkillCooldown = compound.getInt("HealCooldown");
        this.teleportSkillCooldown = compound.getInt("TeleportCooldown");
    }

    /**
     * 【修改】清理了AI目标，移除了旧的技能AI。
     */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));

        Set<Block> ritualFoodBlocks = new HashSet<>();
        ritualFoodBlocks.addAll(getTier1RitualBlocks());
        ritualFoodBlocks.addAll(getTier2RitualBlocks());
        if (!ritualFoodBlocks.isEmpty()) {
            this.eatBlockFoodGoal = new EatBlockFoodGoal(this, 1.2D, 16, ritualFoodBlocks::contains);
            this.goalSelector.addGoal(3, this.eatBlockFoodGoal);
        }

        this.goalSelector.addGoal(4, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0D, 10.0F, 2.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
    }

    // --- 其他所有方法保持不变 ---

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5D);
    }
    @Override
    protected void onRitualFoodEaten(ResourceLocation foodId) {
        if (foodId.getPath().equals("crystal_lamb_chop")) {
            this.entityData.set(DATA_HAS_BONUS_REDUCTION, true);
            this.setWarriorLevel(this.getWarriorLevel());
        } else if (foodId.getPath().equals("sweet_and_sour_ender_pearls")) {
            this.entityData.set(DATA_HAS_TELEPORT_SKILL, true);
        } else if (foodId.getPath().equals("golden_salad")) {
            this.entityData.set(DATA_HAS_HEAL_SKILL, true);
        }
    }
    @Override
    protected float getBonusDamageReduction() {
        return this.entityData.get(DATA_HAS_BONUS_REDUCTION) ? 0.2f : 0.0f;
    }
    public boolean hasTeleportSkill() {
        return this.entityData.get(DATA_HAS_TELEPORT_SKILL);
    }
    public boolean hasHealSkill() {
        return this.entityData.get(DATA_HAS_HEAL_SKILL);
    }
    @Override protected int getInitialLevelCap() { return 15; }
    @Override protected int getLevelCapIncrease() { return 5; }
    @Override protected double getInitialHealth() { return 30.0; }
    @Override protected double getHealthPerLevel() { return 3.0; }
    @Override protected double getInitialAttack() { return 1.0; }
    @Override protected double getAttackPerLevel() { return 0.1; }
    @Override protected double getInitialAttackMultiplier() { return 1.0; }
    @Override protected double getAttackMultiplierPerLevel() { return 0.02; }
    @Override protected double getInitialSpeed() { return 0.23; }
    @Override protected double getSpeedPerLevel() { return 0.005; }
    @Override protected float getDamageReductionPerLevel() { return 0.0066f; }
    @Override protected Set<Block> getTier1RitualBlocks() { return TIER_1_BLOCKS; }
    @Override protected Set<Block> getTier2RitualBlocks() { return TIER_2_BLOCKS; }
    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && this.isOwnedBy(player)) {
            ItemAttributeModifiers modifiers = heldItem.get(DataComponents.ATTRIBUTE_MODIFIERS);
            boolean hasAttackDamage = false;
            if (modifiers != null) {
                for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                    if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                        hasAttackDamage = true;
                        break;
                    }
                }
            }

            boolean isEquippable = heldItem.getItem() instanceof ArmorItem ||
                    heldItem.getItem() instanceof ShieldItem ||
                    heldItem.is(Items.TOTEM_OF_UNDYING) ||
                    hasAttackDamage;

            if (!heldItem.isEmpty() && isEquippable) {
                // 【核心修复】在这里添加对不死图腾的特殊判断
                EquipmentSlot slot = getEquipmentSlotForItem(heldItem);
                if (heldItem.is(Items.TOTEM_OF_UNDYING)) {
                    slot = EquipmentSlot.OFFHAND; // 强制指定为副手
                }

                if (!this.level().isClientSide) {
                    ItemStack currentItem = this.getItemBySlot(slot);
                    this.setItemSlot(slot, heldItem.copy());
                    if (!player.getAbilities().instabuild) {
                        player.setItemInHand(hand, currentItem);
                    }
                    this.playSound((SoundEvent) SoundEvents.ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }

        return super.mobInteract(player, hand);
    }
    @Override public boolean isFood(@NotNull ItemStack pStack) { return false; }
    @Nullable @Override public AgeableMob getBreedOffspring(@NotNull ServerLevel serverLevel, @NotNull AgeableMob ageableMob) { return null; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ZOMBIE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(@NotNull DamageSource pDamageSource) { return SoundEvents.ZOMBIE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_DEATH; }
    @Override protected void playStepSound(@NotNull BlockPos pPos, @NotNull BlockState pState) { this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F); }
}