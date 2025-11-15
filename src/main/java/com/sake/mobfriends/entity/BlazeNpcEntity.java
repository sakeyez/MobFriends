package com.sake.mobfriends.entity;

import com.sake.mobfriends.MobFriends; // 【新增】
import com.sake.mobfriends.block.WorkstationBlockEntity; // 【新增】
import com.sake.mobfriends.entity.ai.BlazeRefuelGoal;
import com.sake.mobfriends.entity.ai.ReturnToWorkstationGoal;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.BlockPos; // 【新增】
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag; // 【新增】
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity; // 【新增】
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.util.OptionalInt;

// 【修改】实现新接口
public class BlazeNpcEntity extends Blaze implements Merchant, IWorkstationNPC {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【【【新增字段】】】
    @Nullable private BlockPos workstationPos = null;

    public BlazeNpcEntity(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Blaze.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BlazeRefuelGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new ReturnToWorkstationGoal(this, 16.0D, 1.0D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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

    // (mobInteract 保持不变)
    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        if (this.isAlive() && this.getTradingPlayer() == null) {
            if (pHand == InteractionHand.MAIN_HAND) {
                if (!this.level().isClientSide()) {
                    this.startTrading(pPlayer);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.mobInteract(pPlayer, pHand);
    }

    // --- (交易逻辑) ---
    public void startTrading(Player pPlayer) {
        this.setTradingPlayer(pPlayer);
        this.openTradingScreen(pPlayer, this.getDisplayName());
    }

    public void openTradingScreen(Player pPlayer, Component pDisplayName) {
        OptionalInt optionalint = pPlayer.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, player) -> new MerchantMenu(containerId, playerInventory, this),
                pDisplayName
        ));

        if (optionalint.isPresent()) {
            MerchantOffers merchantoffers = this.getOffers();
            if (!merchantoffers.isEmpty()) {
                pPlayer.sendMerchantOffers(optionalint.getAsInt(), merchantoffers, 1, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
            }
        }
    }

    @Override
    public void setTradingPlayer(@Nullable Player pPlayer) {
        this.tradingPlayer = pPlayer;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

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
    public void overrideOffers(@NotNull MerchantOffers pOffers) {
        this.offers = pOffers;
    }

    @Override
    public void notifyTrade(@NotNull MerchantOffer pOffer) {
        pOffer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        // 【【【修复】】】
        this.playSound(this.getNotifyTradeSound(), 1.0F, this.getVoicePitch());
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        // 【【【修复】】】
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    public void notifyTradeUpdated(@NotNull ItemStack pStack) {}

    @Override
    public int getVillagerXp() { return 0; }

    @Override
    public void overrideXp(int pXp) {}

    @Override
    public boolean showProgressBar() { return false; }



    @Override
    public boolean isClientSide() { return this.level().isClientSide(); }

    @Override
    public boolean canRestock() { return false; }

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