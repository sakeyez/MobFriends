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
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    private final Set<ResourceLocation> eatenDiningFoods = new HashSet<>();
    private final Set<ResourceLocation> eatenRitualFoods = new HashSet<>();

    protected EatBlockFoodGoal eatBlockFoodGoal;

    public AbstractWarriorEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LEVEL, 1);
        builder.define(DATA_HAPPINESS, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("WarriorLevel", this.getWarriorLevel());
        compound.putInt("WarriorHappiness", this.getHappiness());

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
        this.setWarriorLevel(Math.max(1, compound.getInt("WarriorLevel")));
        this.setHappiness(Math.max(0, compound.getInt("WarriorHappiness")));

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
            updateEatGoal();
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (this.isOwnedBy(player) && !player.isShiftKeyDown()) {
            if (this.level().isClientSide) return InteractionResult.CONSUME;

            if (isLevelCapped()) {
                player.sendSystemMessage(Component.translatable("message.mob_friends.level_cap_stuck_block", this.getName()));
                return InteractionResult.FAIL;
            }

            FoodProperties foodProperties = heldItem.get(DataComponents.FOOD);
            if (foodProperties != null) {
                int nutrition = foodProperties.nutrition();
                float happinessGain = nutrition;
                ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());

                if (heldItem.is(ModTags.Items.DINING_FOODS)) {
                    if (!eatenDiningFoods.contains(foodId)) {
                        float bonusMultiplier = 1.0f + 0.1f * eatenDiningFoods.size();
                        happinessGain *= bonusMultiplier;
                        eatenDiningFoods.add(foodId);
                        player.sendSystemMessage(Component.translatable("message.mob_friends.ate_dining_food", this.getName(), (int) happinessGain));
                    }
                }

                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                this.addHappiness((int) happinessGain, player);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /**
     * 由事件处理器调用，当战士吃掉一个方块后触发。
     */
    public void handleRitualBlockEaten(BlockState eatenBlockState) {
        if (this.level().isClientSide || !isLevelCapped()) return;

        Block eatenBlock = eatenBlockState.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(eatenBlock);

        // 【核心修复】使用清晰、直接的逻辑来判断方块是否正确
        boolean isCorrectRitualBlock = false;
        if (getWarriorLevel() == 10) {
            isCorrectRitualBlock = getTier1RitualBlocks().contains(eatenBlock);
        } else if (getWarriorLevel() == 20) {
            isCorrectRitualBlock = getTier2RitualBlocks().contains(eatenBlock);
        }

        // 只有当吃掉的是正确的、且之前没吃过的仪式方块时，才继续处理
        if (isCorrectRitualBlock && !this.eatenRitualFoods.contains(blockId)) {
            this.eatenRitualFoods.add(blockId);

            if (checkRitualCompletion()) {
                // 所有仪式方块都吃完了，突破瓶颈
                this.eatenRitualFoods.clear(); // 清空记录，为下一阶段做准备
                this.setWarriorLevel(this.getWarriorLevel() + 1);
                if (getOwner() instanceof Player player) {
                    player.sendSystemMessage(Component.translatable("message.mob_friends.level_cap_broken", this.getName()));
                }
                this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                // 只吃了一部分
                if (getOwner() instanceof Player player) {
                    player.sendSystemMessage(Component.translatable("message.mob_friends.ate_ritual_food_partial", this.getName()));
                }
                this.playSound(SoundEvents.GENERIC_EAT, 1.0f, 1.2f);
            }
        }
    }

    private boolean checkRitualCompletion() {
        int level = getWarriorLevel();
        if (level == 10) {
            return this.eatenRitualFoods.size() >= getTier1RitualBlocks().size();
        }
        if (level == 20) {
            // --- 【核心修复】 ---
            // 将错误的 getTier1RitualBlocks() 修改为 getTier2RitualBlocks()
            return this.eatenRitualFoods.size() >= getTier2RitualBlocks().size();
        }
        return false;
    }

    protected void updateEatGoal() {
        if (this.eatBlockFoodGoal != null) {
            this.goalSelector.removeGoal(this.eatBlockFoodGoal);
            if (isLevelCapped()) {
                this.goalSelector.addGoal(1, this.eatBlockFoodGoal);
            }
        }
    }

    public void addHappiness(int amount, @Nullable Player feedbackPlayer) {
        if (!this.level().isClientSide) {
            this.setHappiness(this.getHappiness() + amount);
            boolean leveledUp = checkLevelUp(feedbackPlayer);

            if (!leveledUp && feedbackPlayer != null) {
                if (this.getWarriorLevel() >= 30) {
                    feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.ate_food_max_level", this.getName(), amount, this.getWarriorLevel()));
                } else {
                    feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.ate_food", this.getName(), amount, this.getWarriorLevel(), this.getHappiness(), this.getHappinessForNextLevel()));
                }
            }
        }
    }

    private boolean checkLevelUp(@Nullable Player feedbackPlayer) {
        boolean hasLeveledUp = false;
        int currentLevel = this.getWarriorLevel();
        if (currentLevel >= 30) return false;

        int happinessNeeded = getHappinessForNextLevel();
        while (this.getHappiness() >= happinessNeeded) {
            hasLeveledUp = true;
            this.setHappiness(this.getHappiness() - happinessNeeded);
            currentLevel++;
            this.setWarriorLevel(currentLevel);
            this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.8F);
            if (feedbackPlayer != null) {
                feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.level_up", this.getName(), currentLevel));
            }
            if (currentLevel >= 30 || isLevelCapped()) {
                break;
            }
            happinessNeeded = getHappinessForNextLevel();
        }
        return hasLeveledUp;
    }

    public int getHappinessForNextLevel() {
        int level = this.getWarriorLevel();
        if (level < 10) return 30;
        if (level < 20) return 50;
        if (level < 30) return 100;
        return Integer.MAX_VALUE;
    }

    public boolean isLevelCapped() {
        int level = this.getWarriorLevel();
        return level == 10 || level == 20;
    }

    public void applyLevelBasedStats() {
        if (this.level().isClientSide()) return;
        AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);

        if (health != null) health.setBaseValue(getHealthForLevel(this.getWarriorLevel()));
        if (damage != null) damage.setBaseValue(getDamageForLevel(this.getWarriorLevel()));
        if (speed != null) speed.setBaseValue(getSpeedForLevel(this.getWarriorLevel()));
        if (armor != null) armor.setBaseValue(getArmorForLevel(this.getWarriorLevel()));

        this.heal(this.getMaxHealth());
    }

    protected abstract double getHealthForLevel(int level);
    protected abstract double getDamageForLevel(int level);
    protected abstract double getSpeedForLevel(int level);
    protected abstract double getArmorForLevel(int level);
    protected abstract Set<Block> getTier1RitualBlocks();
    protected abstract Set<Block> getTier2RitualBlocks();

    public int getWarriorLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public void setWarriorLevel(int level) {
        this.entityData.set(DATA_LEVEL, Math.max(1, level));
        if (!this.level().isClientSide) {
            applyLevelBasedStats();
            updateEatGoal();
        }
    }

    public int getHappiness() {
        return this.entityData.get(DATA_HAPPINESS);
    }

    public void setHappiness(int happiness) {
        this.entityData.set(DATA_HAPPINESS, happiness);
    }
}