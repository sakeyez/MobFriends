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
    // 【新增】追踪等级上限的数据
    private static final EntityDataAccessor<Integer> DATA_LEVEL_CAP = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.INT);


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
        // 【新增】初始化等级上限为10
        builder.define(DATA_LEVEL_CAP, 10);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("WarriorLevel", this.getWarriorLevel());
        compound.putInt("WarriorHappiness", this.getHappiness());
        // 【新增】保存等级上限
        compound.putInt("WarriorLevelCap", this.getLevelCap());


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
        // 【新增】读取等级上限，如果不存在则默认为10
        this.setLevelCap(compound.contains("WarriorLevelCap") ? compound.getInt("WarriorLevelCap") : 10);


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
     * 【重构】由事件处理器调用，当战士吃掉一个方块后触发。
     */
    public void handleRitualBlockEaten(BlockState eatenBlockState) {
        if (this.level().isClientSide) return;

        Block eatenBlock = eatenBlockState.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(eatenBlock);

        // 合并所有仪式食物
        Set<Block> allRitualBlocks = new HashSet<>();
        allRitualBlocks.addAll(getTier1RitualBlocks());
        allRitualBlocks.addAll(getTier2RitualBlocks());

        // 如果吃的是一种新的仪式食物
        if (allRitualBlocks.contains(eatenBlock) && !this.eatenRitualFoods.contains(blockId)) {
            this.eatenRitualFoods.add(blockId);
            // 等级上限+10
            this.setLevelCap(this.getLevelCap() + 10);

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
        int currentLevel = this.getWarriorLevel();
        // 如果已经达到上限，则不升级
        if (isLevelCapped()) return false;

        int happinessNeeded = getHappinessForNextLevel();
        while (this.getHappiness() >= happinessNeeded) {
            hasLeveledUp = true;
            this.setHappiness(this.getHappiness() - happinessNeeded);
            currentLevel++;
            this.setWarriorLevel(currentLevel);
            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
            if (feedbackPlayer != null) {
                feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.level_up", this.getName(), currentLevel));
            }
            // 如果升级后达到上限，停止继续升级
            if (isLevelCapped()) {
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
        if (level < 40) return 150; // 为新的等级添加幸福感需求
        return Integer.MAX_VALUE;
    }

    /**
     * 【修改】现在使用动态等级上限
     */
    public boolean isLevelCapped() {
        return this.getWarriorLevel() >= this.getLevelCap();
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
        }
    }

    public int getHappiness() {
        return this.entityData.get(DATA_HAPPINESS);
    }

    public void setHappiness(int happiness) {
        this.entityData.set(DATA_HAPPINESS, happiness);
    }

    // 【新增】等级上限的Getter和Setter
    public int getLevelCap() {
        return this.entityData.get(DATA_LEVEL_CAP);
    }

    public void setLevelCap(int cap) {
        this.entityData.set(DATA_LEVEL_CAP, cap);
    }
}