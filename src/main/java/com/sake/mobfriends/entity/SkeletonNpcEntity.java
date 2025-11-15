package com.sake.mobfriends.entity;

import com.sake.mobfriends.MobFriends; // 【新增】
import com.sake.mobfriends.block.WorkstationBlockEntity; // 【新增】
import com.sake.mobfriends.entity.ai.ReturnToWorkstationGoal;
import com.sake.mobfriends.entity.ai.SkeletonFishingGoal;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.BlockPos; // 【新增】
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag; // 【新增】
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
import net.minecraft.world.level.block.entity.BlockEntity; // 【新增】
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.OptionalInt;

// 【修改】实现新接口
public class SkeletonNpcEntity extends Skeleton implements Merchant, IWorkstationNPC {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【【【新增字段】】】
    @Nullable private BlockPos workstationPos = null;

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
        this.goalSelector.addGoal(2, new ReturnToWorkstationGoal(this, 16.0D, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // 【【【新增NBT】】】
    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        if (this.workstationPos != null) {
            c.putLong("WorkstationPos", this.workstationPos.asLong());
        }
    }

    // 【【【新增NBT】】】
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        if (c.contains("WorkstationPos")) {
            this.workstationPos = BlockPos.of(c.getLong("WorkstationPos"));
        } else {
            this.workstationPos = null;
        }
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor pLevel, @NotNull DifficultyInstance pDifficulty, @NotNull MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData) {
        SpawnGroupData data = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        return data;
    }

    // (mobInteract 保持不变)
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

        if (this.isAlive() && this.getTradingPlayer() == null) {
            if (!this.level().isClientSide()) {
                this.startTrading(pPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(pPlayer, pHand);
    }

    // --- 交易逻辑实现 ---
    public void startTrading(Player pPlayer) { this.setTradingPlayer(pPlayer); this.openTradingScreen(pPlayer, this.getDisplayName()); }
    public void openTradingScreen(Player pPlayer, Component pDisplayName) { OptionalInt o = pPlayer.openMenu(new SimpleMenuProvider((i, v, p) -> new MerchantMenu(i, v, this), pDisplayName)); if (o.isPresent()) { MerchantOffers offers = this.getOffers(); if (!offers.isEmpty()) { pPlayer.sendMerchantOffers(o.getAsInt(), offers, 1, this.getVillagerXp(), this.showProgressBar(), this.canRestock()); } } }
    @Override public void setTradingPlayer(@Nullable Player p) { this.tradingPlayer = p; }
    @Nullable @Override public Player getTradingPlayer() { return this.tradingPlayer; }

    // 【【【修改：重写 GETOFFERS】】】
    @Override
    public MerchantOffers getOffers() {
        if (this.level().isClientSide()) {
            return new MerchantOffers();
        }
        if (this.workstationPos == null) {
            MobFriends.LOGGER.warn("NPC {} 尚未绑定工作站，无法提供交易。", this.getUUID());
            return new MerchantOffers();
        }
        BlockEntity be = this.level().getBlockEntity(this.workstationPos);
        if (!(be instanceof WorkstationBlockEntity workstation)) {
            MobFriends.LOGGER.warn("NPC {} 无法在 {} 找到其工作站实体。", this.getUUID(), this.workstationPos);
            return new MerchantOffers();
        }
        int currentLevel = workstation.getNpcLevel() + 1;
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        return TradeManager.getOffersFor(entityId, currentLevel);
    }

    @Override
    public void notifyTrade(@NotNull MerchantOffer o) {
        o.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        // 【【【修复】】】
        this.playSound(this.getNotifyTradeSound(), 1.0F, this.getVoicePitch());
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        // 【【【修复】】】
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override public void overrideOffers(@NotNull MerchantOffers o) { this.offers = o; }

    @Override public void notifyTradeUpdated(@NotNull ItemStack s) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int x) {}
    @Override public boolean showProgressBar() { return false; }

    @Override public boolean isClientSide() { return this.level().isClientSide(); }
    @Override public boolean canRestock() { return false; }

    // --- 【【【新增：IWorkstationNPC 实现】】】 ---

    @Override
    public void setWorkstationPos(BlockPos pos) {
        this.workstationPos = pos;
    }

    @Override
    @Nullable
    public BlockPos getWorkstationPos() {
        return this.workstationPos;
    }
}