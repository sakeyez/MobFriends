// 文件路径: src/main/java/com/sake/mobfriends/entity/AbstractWarriorEntity.java

package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractWarriorEntity extends TamableAnimal {

    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HAPPINESS = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEVEL_CAP = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_DAMAGE_REDUCTION = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.FLOAT);

    private final Set<ResourceLocation> eatenDiningFoods = new HashSet<>();
    private final Set<ResourceLocation> eatenRitualFoods = new HashSet<>();

    public EatBlockFoodGoal eatBlockFoodGoal;

    public AbstractWarriorEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LEVEL, 0);
        builder.define(DATA_HAPPINESS, 0);
        builder.define(DATA_LEVEL_CAP, this.getInitialLevelCap());
        builder.define(DATA_DAMAGE_REDUCTION, 0.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("WarriorLevel", this.getWarriorLevel());
        compound.putInt("WarriorHappiness", this.getHappiness());
        compound.putInt("WarriorLevelCap", this.getLevelCap());
        compound.putFloat("DamageReduction", this.getDamageReduction());

        ListTag eatenList = new ListTag();
        for (ResourceLocation foodId : this.eatenDiningFoods) {
            eatenList.add(StringTag.valueOf(foodId.toString()));
        }
        compound.put("EatenDiningFoods", eatenList);

        ListTag ritualList = new ListTag();
        for (ResourceLocation foodId : this.eatenRitualFoods) {
            ritualList.add(StringTag.valueOf(foodId.toString()));
        }
        compound.put("EatenRitualFoods", ritualList);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setWarriorLevel(Math.max(0, compound.getInt("WarriorLevel")));
        this.setHappiness(Math.max(0, compound.getInt("WarriorHappiness")));
        this.setLevelCap(compound.contains("WarriorLevelCap") ? compound.getInt("WarriorLevelCap") : this.getInitialLevelCap());
        this.setDamageReduction(compound.getFloat("DamageReduction"));

        this.eatenDiningFoods.clear();
        ListTag eatenList = compound.getList("EatenDiningFoods", 8);
        for (int i = 0; i < eatenList.size(); i++) {
            this.eatenDiningFoods.add(ResourceLocation.parse(eatenList.getString(i)));
        }

        this.eatenRitualFoods.clear();
        ListTag ritualList = compound.getList("EatenRitualFoods", 8);
        for (int i = 0; i < ritualList.size(); i++) {
            this.eatenRitualFoods.add(ResourceLocation.parse(ritualList.getString(i)));
        }

        if (!this.level().isClientSide) {
            applyLevelBasedStats();
        }
    }

    /**
     * 【核心修复】重写 hurt 方法来统一处理免伤。
     * 这比使用事件监听器更稳定，且符合原版逻辑。
     */
    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        float reduction = this.getDamageReduction();
        // 计算免伤后的实际伤害
        float finalDamage = pAmount * (1.0F - reduction);
        return super.hurt(pSource, finalDamage);
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (this.isOwnedBy(player) && !player.isShiftKeyDown()) {
            if (this.level().isClientSide) return InteractionResult.CONSUME;

            if (isLevelCapped()) {
                player.sendSystemMessage(Component.translatable("message.mob_friends.level_cap_stuck", this.getName()));
                return InteractionResult.FAIL;
            }

            FoodProperties foodProperties = heldItem.get(DataComponents.FOOD);
            if (foodProperties != null) {
                float baseValue = foodProperties.nutrition() + foodProperties.saturation();
                float bonusMultiplier = 1.0f + 0.1f * eatenDiningFoods.size();
                int happinessGain = (int) (baseValue * bonusMultiplier);

                ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

                if (heldItem.is(ModTags.Items.DINING_FOODS)) {
                    if (!eatenDiningFoods.contains(foodId)) {
                        eatenDiningFoods.add(foodId);
                    }
                }

                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                this.addHappiness(happinessGain, player);
                return InteractionResult.SUCCESS;
            }
        }

        return super.mobInteract(player, hand);
    }

    public void handleRitualBlockEaten(BlockState eatenBlockState) {
        if (this.level().isClientSide) return;

        Block eatenBlock = eatenBlockState.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(eatenBlock);

        Set<Block> allRitualBlocks = new HashSet<>();
        allRitualBlocks.addAll(getTier1RitualBlocks());
        allRitualBlocks.addAll(getTier2RitualBlocks());

        if (allRitualBlocks.contains(eatenBlock) && !this.eatenRitualFoods.contains(blockId)) {
            this.eatenRitualFoods.add(blockId);
            this.setLevelCap(this.getLevelCap() + this.getLevelCapIncrease());

            // 通知子类吃下了特定的仪式食物，以解锁技能
            this.onRitualFoodEaten(blockId);

            if (getOwner() instanceof Player player) {
                player.sendSystemMessage(Component.translatable("message.mob_friends.level_cap_increased", this.getName(), this.getLevelCap()));
            }
            this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D)+0.5D, 40, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    public void addHappiness(int amount, @Nullable Player feedbackPlayer) {
        if (!this.level().isClientSide) {
            this.setHappiness(this.getHappiness() + amount);
            boolean leveledUp = checkLevelUp(feedbackPlayer);

            if (!leveledUp && feedbackPlayer != null) {
                if (isLevelCapped()) {
                    feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.ate_food_max_level", this.getName(), amount, this.getWarriorLevel()));
                } else {
                    feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.ate_food", this.getName(), amount, this.getWarriorLevel(), this.getHappiness(), this.getHappinessForNextLevel()));
                }
            }
        }
    }

    private boolean checkLevelUp(@Nullable Player feedbackPlayer) {
        boolean hasLeveledUp = false;
        if (isLevelCapped()) return false;

        int happinessNeeded = getHappinessForNextLevel();
        while (this.getHappiness() >= happinessNeeded) {
            hasLeveledUp = true;
            this.setHappiness(this.getHappiness() - happinessNeeded);
            int currentLevel = this.getWarriorLevel() + 1;
            this.setWarriorLevel(currentLevel); // 这会触发属性和免伤的重新计算
            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
            if (feedbackPlayer != null) {
                feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.level_up", this.getName(), currentLevel));
            }
            if (isLevelCapped()) {
                break;
            }
            happinessNeeded = getHappinessForNextLevel();
        }
        return hasLeveledUp;
    }

    public int getHappinessForNextLevel() {
        int baseRequirement = 100;
        double ritualMultiplier = 1.0 + 0.5 * this.eatenRitualFoods.size();
        return (int) (baseRequirement * ritualMultiplier);
    }

    public boolean isLevelCapped() {
        return this.getWarriorLevel() >= this.getLevelCap();
    }

    public void applyLevelBasedStats() {
        if (this.level().isClientSide()) return;
        int level = this.getWarriorLevel();

        getAttribute(Attributes.MAX_HEALTH).setBaseValue(getInitialHealth() + getHealthPerLevel() * level);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(getInitialSpeed() + getSpeedPerLevel() * level);

        double baseAttack = getInitialAttack() + getAttackPerLevel() * level;
        double multiplier = getInitialAttackMultiplier() + getAttackMultiplierPerLevel() * level;
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * multiplier);

        this.heal(this.getMaxHealth());
    }

    // --- 抽象方法，强制子类实现 ---
    protected abstract int getInitialLevelCap();
    protected abstract int getLevelCapIncrease();
    protected abstract double getInitialHealth();
    protected abstract double getHealthPerLevel();
    protected abstract double getInitialAttack();
    protected abstract double getAttackPerLevel();
    protected abstract double getInitialAttackMultiplier();
    protected abstract double getAttackMultiplierPerLevel();
    protected abstract double getInitialSpeed();
    protected abstract double getSpeedPerLevel();
    protected abstract float getDamageReductionPerLevel();
    protected abstract void onRitualFoodEaten(ResourceLocation foodId);
    protected abstract Set<Block> getTier1RitualBlocks();
    protected abstract Set<Block> getTier2RitualBlocks();

    /**
     * 获取由技能等提供的额外固定免伤值。
     * 子类可以重写此方法以添加技能带来的免伤。
     */
    protected float getBonusDamageReduction() {
        return 0.0f;
    }

    // --- Getters 和 Setters ---
    public float getDamageReduction() {
        return this.entityData.get(DATA_DAMAGE_REDUCTION);
    }

    public void setDamageReduction(float reduction) {
        this.entityData.set(DATA_DAMAGE_REDUCTION, reduction);
    }

    public int getWarriorLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    /**
     * 【核心修复】修正 setWarriorLevel 方法，不再使用 @Override 和 super
     */
    public void setWarriorLevel(int level) {
        // 1. 设置等级数据
        this.entityData.set(DATA_LEVEL, Math.max(0, level));

        // 2. 在服务器端，重新计算所有属性和免伤
        if (!this.level().isClientSide) {
            // 重新应用血量、攻击、速度等属性
            applyLevelBasedStats();
            // 重新计算并存储总免伤（等级成长免伤 + 技能额外免伤）
            float totalReduction = Math.min(1.0f, (getDamageReductionPerLevel() * level) + getBonusDamageReduction());
            this.setDamageReduction(totalReduction);
        }
    }

    public int getHappiness() {
        return this.entityData.get(DATA_HAPPINESS);
    }

    public void setHappiness(int happiness) {
        this.entityData.set(DATA_HAPPINESS, happiness);
    }

    public int getLevelCap() {
        return this.entityData.get(DATA_LEVEL_CAP);
    }

    public void setLevelCap(int cap) {
        this.entityData.set(DATA_LEVEL_CAP, cap);
    }
}