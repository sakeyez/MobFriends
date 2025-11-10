// 文件路径: src/main/java/com/sake/mobfriends/entity/AbstractWarriorEntity.java

package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.EatBlockFoodGoal;
import com.sake.mobfriends.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import org.joml.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

    private static final EntityDataAccessor<Integer> DATA_FOOD_HEAL_TIMER = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FOOD_HEAL_AMOUNT = SynchedEntityData.defineId(AbstractWarriorEntity.class, EntityDataSerializers.FLOAT);


    private final Set<ResourceLocation> eatenDiningFoods = new HashSet<>();
    private final Set<ResourceLocation> eatenRitualFoods = new HashSet<>();

    public EatBlockFoodGoal eatBlockFoodGoal;

    public AbstractWarriorEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setOrderedToSit(false);

        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LEVEL, 0);
        builder.define(DATA_HAPPINESS, 0);
        builder.define(DATA_LEVEL_CAP, this.getInitialLevelCap());
        builder.define(DATA_DAMAGE_REDUCTION, 0.0f);
        builder.define(DATA_FOOD_HEAL_TIMER, 0);
        builder.define(DATA_FOOD_HEAL_AMOUNT, 0.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("WarriorLevel", this.getWarriorLevel());
        compound.putInt("WarriorHappiness", this.getHappiness());
        compound.putInt("WarriorLevelCap", this.getLevelCap());
        compound.putFloat("DamageReduction", this.getDamageReduction());
        compound.putInt("FoodHealTimer", this.getFoodHealTimer());
        compound.putFloat("FoodHealAmount", this.getFoodHealAmount());

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

        this.entityData.set(DATA_LEVEL, Math.max(0, compound.getInt("WarriorLevel")));
        this.setHappiness(Math.max(0, compound.getInt("WarriorHappiness")));
        this.setLevelCap(compound.contains("WarriorLevelCap") ? compound.getInt("WarriorLevelCap") : this.getInitialLevelCap());
        this.entityData.set(DATA_DAMAGE_REDUCTION, compound.getFloat("DamageReduction"));
        this.setFoodHealTimer(compound.getInt("FoodHealTimer"));
        this.setFoodHealAmount(compound.getFloat("FoodHealAmount"));

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
            this.setWarriorLevel(this.getWarriorLevel());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        int timer = this.getFoodHealTimer();
        if (timer > 0) {
            if (timer % 10 == 0) {
                this.heal(this.getFoodHealAmount());
            }
            this.setFoodHealTimer(timer - 1);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        float reduction = this.getDamageReduction();
        float finalDamage = pAmount * (1.0F - reduction);
        return super.hurt(pSource, finalDamage);
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (this.isOwnedBy(player)) {

            // --- 逻辑 1: 玩家潜行 (装备/卸装) ---
            if (player.isShiftKeyDown()) {
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

                // 1A: 潜行 + 拿装备 = 穿上/替换
                if (!heldItem.isEmpty() && isEquippable) {
                    EquipmentSlot slot = getEquipmentSlotForItem(heldItem);
                    if (heldItem.is(Items.TOTEM_OF_UNDYING)) {
                        slot = EquipmentSlot.OFFHAND;
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
                // 1B: 潜行 + 空手 = 卸下装备
                else if (heldItem.isEmpty()) {
                    EquipmentSlot slotToEmpty = null;
                    if (!this.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) slotToEmpty = EquipmentSlot.MAINHAND;
                    else if (!this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) slotToEmpty = EquipmentSlot.OFFHAND;
                    else if (!this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) slotToEmpty = EquipmentSlot.HEAD;
                    else if (!this.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) slotToEmpty = EquipmentSlot.CHEST;
                    else if (!this.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) slotToEmpty = EquipmentSlot.LEGS;
                    else if (!this.getItemBySlot(EquipmentSlot.FEET).isEmpty()) slotToEmpty = EquipmentSlot.FEET;

                    if (slotToEmpty != null) {
                        if (!this.level().isClientSide) {
                            ItemStack currentItem = this.getItemBySlot(slotToEmpty);
                            this.setItemSlot(slotToEmpty, ItemStack.EMPTY);
                            player.setItemInHand(hand, currentItem);
                            this.playSound((SoundEvent) SoundEvents.ARMOR_EQUIP_GENERIC, 1.0f, 0.8f);
                        }
                        return InteractionResult.sidedSuccess(this.level().isClientSide());
                    }
                }
            }
            // --- 逻辑 2: 玩家不潜行 (喂食/坐下) ---
            else {
                FoodProperties foodProperties = heldItem.get(DataComponents.FOOD);

                // 2A: 拿食物 = 喂食
                if (foodProperties != null) {
                    if (this.level().isClientSide) return InteractionResult.CONSUME;

                    // 检查黑名单：所有方块食物 和 所有包子
                    boolean isBlacklisted = (heldItem.getItem() instanceof BlockItem) ||
                            heldItem.is(ModTags.Items.THROWABLE_BAOZI);

                    if (isBlacklisted) {
                        return InteractionResult.FAIL; // 阻止喂食
                    }

                    boolean isDiningFood = heldItem.is(ModTags.Items.DINING_FOODS);
                    boolean isAtFinalCap = this.getWarriorLevel() >= this.getFinalLevelCap();
                    boolean isAtIntermediateCap = isLevelCapped() && !isAtFinalCap;

                    // 【你的逻辑 1】: 达到中间上限 (15, 20, 25)
                    if (isAtIntermediateCap && !isDiningFood) {
                        // 此时只吃 dining_food，如果喂了别的，就发送播报并拒食
                        player.sendSystemMessage(Component.translatable("message.mob_friends.needs_feast"));
                        return InteractionResult.FAIL;
                    }

                    // --- 执行喂食 (能到这里，说明食物是允许吃的) ---

                    // 1. 播放吃东西音效
                    this.playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);

                    // 2. 施加治疗buff (所有情况都触发)
                    float totalHeal = foodProperties.nutrition() + foodProperties.saturation();
                    int duration = 160; // 8 秒
                    float healPerTick = totalHeal / (duration / 10.0f); // 0.5秒跳一次，共16次
                    this.setFoodHealTimer(duration);
                    this.setFoodHealAmount(healPerTick);

                    // 3. 处理 Dining Food 计数器和播报 (所有情况都触发)
                    if (isDiningFood) {
                        ResourceLocation foodId = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
                        if (!eatenDiningFoods.contains(foodId)) {
                            eatenDiningFoods.add(foodId);
                            // 发送粉色播报
                            player.sendSystemMessage(Component.translatable("message.mob_friends.new_dining_food").withStyle(ChatFormatting.LIGHT_PURPLE));
                        }
                    }

                    // 4. 处理幸福感 (只在未达到 *任何* 上限时触发)
                    if (!isLevelCapped()) {
                        float baseValue = foodProperties.nutrition() + foodProperties.saturation();
                        float bonusMultiplier = 1.0f + 0.1f * eatenDiningFoods.size();
                        int happinessGain = (int) (baseValue * bonusMultiplier);
                        this.addHappiness(happinessGain, player);
                    }

                    // 5. 消耗物品
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
                // 2B: 空手 = 坐下/起立
                else if (heldItem.isEmpty()) {
                    if (!this.level().isClientSide) {
                        this.setOrderedToSit(!this.isOrderedToSit());
                        this.jumping = false;
                        this.navigation.stop();
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide());
                }
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
            this.onRitualFoodEaten(blockId);

            if (getOwner() instanceof Player player) {
                player.sendSystemMessage(Component.translatable("message.mob_friends.level_cap_increased", this.getName(), this.getLevelCap()));
            }
            this.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D) + 0.5D, 40, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    public void addHappiness(int amount, @Nullable Player feedbackPlayer) {
        if (!this.level().isClientSide) {
            this.setHappiness(this.getHappiness() + amount);
            checkLevelUp(feedbackPlayer);
        }
    }

    /**
     * 【核心修改】移除了升级播报
     */
    private boolean checkLevelUp(@Nullable Player feedbackPlayer) {
        boolean hasLeveledUp = false;
        if (isLevelCapped()) return false;

        int happinessNeeded = getHappinessForNextLevel();
        while (this.getHappiness() >= happinessNeeded) {
            hasLeveledUp = true;
            this.setHappiness(this.getHappiness() - happinessNeeded);
            int currentLevel = this.getWarriorLevel() + 1;
            this.setWarriorLevel(currentLevel);

            this.heal(this.getMaxHealth());
            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);

            // --- 【核心修改】移除了聊天框播报 ---
            // if (feedbackPlayer != null) {
            //     feedbackPlayer.sendSystemMessage(Component.translatable("message.mob_friends.level_up", this.getName(), currentLevel));
            // }

            if (this.level() instanceof ServerLevel serverLevel) {
                Vector3f fromColor = new Vector3f(85 / 255f, 255 / 255f, 85 / 255f);
                Vector3f toColor = new Vector3f(255 / 255f, 215 / 255f, 0 / 255f);
                float scale = 1.2f;
                DustColorTransitionOptions options = new DustColorTransitionOptions(fromColor, toColor, scale);

                serverLevel.sendParticles(options,
                        this.getRandomX(1.0D),
                        this.getRandomY() + 0.5D,
                        this.getRandomZ(1.0D),
                        30, 0.5, 0.5, 0.5, 0.05);
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

    public boolean isAtFinalCap() {
        return this.getWarriorLevel() >= this.getFinalLevelCap();
    }

    public void applyLevelBasedStats() {
        if (this.level().isClientSide()) return;
        int level = this.getWarriorLevel();

        getAttribute(Attributes.MAX_HEALTH).setBaseValue(getInitialHealth() + getHealthPerLevel() * level);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(getInitialSpeed() + getSpeedPerLevel() * level);

        double baseAttack = getInitialAttack() + getAttackPerLevel() * level;
        double multiplier = getInitialAttackMultiplier() + getAttackMultiplierPerLevel() * level;
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(baseAttack * multiplier);
    }

    // --- 抽象方法 ---
    protected abstract int getInitialLevelCap();
    protected abstract int getLevelCapIncrease();
    protected abstract double getInitialHealth();
    protected abstract double getHealthPerLevel();
    protected abstract double getInitialAttack();
    protected abstract int getFinalLevelCap();
    protected abstract double getAttackPerLevel();
    protected abstract double getInitialAttackMultiplier();
    protected abstract double getAttackMultiplierPerLevel();
    protected abstract double getInitialSpeed();
    protected abstract double getSpeedPerLevel();
    protected abstract float getDamageReductionPerLevel();
    protected abstract void onRitualFoodEaten(ResourceLocation foodId);
    protected abstract Set<Block> getTier1RitualBlocks();
    protected abstract Set<Block> getTier2RitualBlocks();
    protected float getBonusDamageReduction() { return 0.0f; }

    // --- Getters / Setters ---
    public float getDamageReduction() { return this.entityData.get(DATA_DAMAGE_REDUCTION); }
    public void setDamageReduction(float reduction) { this.entityData.set(DATA_DAMAGE_REDUCTION, reduction); }
    public int getWarriorLevel() { return this.entityData.get(DATA_LEVEL); }

    public void setWarriorLevel(int level) {
        this.entityData.set(DATA_LEVEL, Math.max(0, level));
        if (!this.level().isClientSide) {
            applyLevelBasedStats();
            float totalReduction = Math.min(1.0f, (getDamageReductionPerLevel() * level) + getBonusDamageReduction());
            this.setDamageReduction(totalReduction);
        }
    }

    public int getHappiness() { return this.entityData.get(DATA_HAPPINESS); }
    public void setHappiness(int happiness) { this.entityData.set(DATA_HAPPINESS, happiness); }
    public int getLevelCap() { return this.entityData.get(DATA_LEVEL_CAP); }
    public void setLevelCap(int cap) { this.entityData.set(DATA_LEVEL_CAP, cap); }
    public int getEatenRitualFoodsCount() { return this.eatenRitualFoods.size(); }

    public int getFoodHealTimer() { return this.entityData.get(DATA_FOOD_HEAL_TIMER); }
    public void setFoodHealTimer(int timer) { this.entityData.set(DATA_FOOD_HEAL_TIMER, timer); }
    public float getFoodHealAmount() { return this.entityData.get(DATA_FOOD_HEAL_AMOUNT); }
    public void setFoodHealAmount(float amount) { this.entityData.set(DATA_FOOD_HEAL_AMOUNT, amount); }
}