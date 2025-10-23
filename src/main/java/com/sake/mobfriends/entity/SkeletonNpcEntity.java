package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.SkeletonFishingGoal;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.OptionalInt;

public class SkeletonNpcEntity extends Skeleton implements Merchant {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    public SkeletonNpcEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Skeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SkeletonFishingGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel, @NotNull DifficultyInstance pDifficulty, @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        SpawnGroupData data = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        return data;
    }

    // --- 【最终修正】交互逻辑 ---
    @Override
    protected @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        ItemStack playerStack = pPlayer.getItemInHand(pHand);

        if (pPlayer.isShiftKeyDown()) {
            EquipmentSlot slot = null;
            SoundEvent sound = SoundEvents.ITEM_PICKUP;

            if (playerStack.getItem() instanceof ArmorItem armorItem) {
                slot = armorItem.getEquipmentSlot();
                sound = armorItem.getEquipSound().value();
            } else if (playerStack.getItem() instanceof FishingRodItem) {
                slot = EquipmentSlot.MAINHAND;
            }

            if (slot != null) {
                if (!this.level().isClientSide()) {
                    ItemStack currentItem = this.getItemBySlot(slot);
                    this.setItemSlot(slot, playerStack.copy());
                    if (!pPlayer.getAbilities().instabuild) {
                        pPlayer.setItemInHand(pHand, currentItem);
                    }
                    this.playSound(sound, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }

        // 交易逻辑
        if (this.isAlive() && this.getTradingPlayer() == null) {
            if (!this.level().isClientSide()) {
                this.startTrading(pPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(pPlayer, pHand);
    }

    // --- 交易逻辑实现保持不变 ---
    public void startTrading(Player pPlayer) { this.setTradingPlayer(pPlayer); this.openTradingScreen(pPlayer, this.getDisplayName()); }
    public void openTradingScreen(Player pPlayer, Component pDisplayName) { OptionalInt o = pPlayer.openMenu(new SimpleMenuProvider((i, v, p) -> new MerchantMenu(i, v, this), pDisplayName)); if (o.isPresent()) { MerchantOffers offers = this.getOffers(); if (!offers.isEmpty()) { pPlayer.sendMerchantOffers(o.getAsInt(), offers, 1, this.getVillagerXp(), this.showProgressBar(), this.canRestock()); } } }
    @Override public void setTradingPlayer(@Nullable Player p) { this.tradingPlayer = p; }
    @Nullable @Override public Player getTradingPlayer() { return this.tradingPlayer; }
    @Override public MerchantOffers getOffers() { if (this.offers == null) { this.offers = TradeManager.getOffersFor(BuiltInRegistries.ENTITY_TYPE.getKey(this.getType())); } return this.offers; }
    @Override public void overrideOffers(@NotNull MerchantOffers o) { this.offers = o; }
    @Override public void notifyTrade(@NotNull MerchantOffer o) { o.increaseUses(); this.ambientSoundTime = -this.getAmbientSoundInterval(); this.playSound(SoundEvents.VILLAGER_YES, 1.0F, this.getVoicePitch()); }
    @Override public void notifyTradeUpdated(@NotNull ItemStack s) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int x) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return this.level().isClientSide(); }
    @Override public boolean canRestock() { return false; }
}