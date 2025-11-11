package com.sake.mobfriends.entity;

// 【【【新增导入AI】】】
import com.sake.mobfriends.entity.ai.BlazeRefuelGoal;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.registries.BuiltInRegistries;
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
// 【【【新增导入AI】】】
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.OptionalInt;

// (这个文件不再需要实现 MenuProvider 或拥有背包)
public class BlazeNpcEntity extends Blaze implements Merchant {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    public BlazeNpcEntity(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Blaze.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D);
    }

    // 【【【修改：添加AI逻辑】】】
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 添加新的“添火”AI，优先级为1
        this.goalSelector.addGoal(1, new BlazeRefuelGoal(this, 1.0D));
        // 添加一些基础AI
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    // (mobInteract 保持原样，只有交易逻辑)
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

    // --- (以下所有交易逻辑保持不变) ---

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

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
            this.offers = TradeManager.getOffersFor(entityId);
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(@NotNull MerchantOffers pOffers) {
        this.offers = pOffers;
    }

    @Override
    public void notifyTrade(@NotNull MerchantOffer pOffer) {
        pOffer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, this.getVoicePitch());
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
    public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }

    @Override
    public boolean isClientSide() { return this.level().isClientSide(); }

    @Override
    public boolean canRestock() { return false; }
}