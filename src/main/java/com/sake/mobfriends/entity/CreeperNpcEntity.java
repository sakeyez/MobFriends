package com.sake.mobfriends.entity;

// 【新增】导入
import com.sake.mobfriends.entity.ai.CreeperEngineerGoal;
import com.sake.mobfriends.entity.ai.DepositItemsGoal;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

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
import net.minecraft.world.entity.monster.Creeper;
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

// 【修改】实现 MenuProvider 来提供背包GUI
public class CreeperNpcEntity extends Creeper implements Merchant, MenuProvider {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【新增】背包，和僵尸农夫一样
    private final SimpleContainer inventory = new SimpleContainer(27);

    public CreeperNpcEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    // 【修改】重写 registerGoals
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));

        // --- 新增AI ---
        this.goalSelector.addGoal(1, new CreeperEngineerGoal(this, 0.8D));
        // 苦力怕工程师没有产出物品，所以不需要 DepositItemsGoal
        // 如果你未来的设计（比如精密构件）产出了物品，再取消下面这行的注释
        // this.goalSelector.addGoal(2, new DepositItemsGoal(this, 0.8D));

        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // 【新增】背包相关方法 (复制自 ZombieNpcEntity)
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        c.put("Inventory", this.inventory.createTag(this.level().registryAccess()));
        // 【【【修改点：移除】】】
        // if (this.offers != null) {
        //     c.put("Offers", this.offers.save(this.level().registryAccess()));
        // }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        this.inventory.fromTag(c.getList("Inventory", 10), this.level().registryAccess());
        // 【【【修改点：移除】】】
        // if (c.contains("Offers", 10)) {
        //     this.offers = MerchantOffers.load(this.level().registryAccess(), c.getCompound("Offers")).orElse(null);
        // }
    }

    // --- 【修改】交互逻辑 ---
    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        // 【新增】潜行右键打开背包GUI
        if (pPlayer.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                pPlayer.openMenu(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 交易逻辑
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

    // 【新增】MenuProvider 实现
    @Override
    public @NotNull Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int c, @NotNull Inventory i, @NotNull Player p) {
        // 复用僵尸的背包菜单
        return new ZombieChestMenu(c, i, this.inventory);
    }

    // --- 交易逻辑 (保持不变) ---
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

    public boolean canRestock() { return false; }
}