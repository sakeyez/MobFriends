package com.sake.mobfriends.entity;

import com.sake.mobfriends.entity.ai.DepositItemsGoal;
import com.sake.mobfriends.entity.ai.ZombieFarmerGoal;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;

public class ZombieNpcEntity extends Zombie implements Merchant, MenuProvider {


    @Nullable private MerchantOffers offers;
    @Nullable private Player tradingPlayer;
    private final SimpleContainer inventory = new SimpleContainer(27);
    public ZombieNpcEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level); this.setCanPickUpLoot(false);
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.3D);
    }
    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ZombieFarmerGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new DepositItemsGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this)); }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        c.put("Inventory", this.inventory.createTag(this.level().registryAccess()));
    }
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c); this.inventory.fromTag(c.getList("Inventory", 10), this.level().registryAccess()); }

    @Nullable @Override
    public SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor l, @NotNull DifficultyInstance d, @NotNull MobSpawnType r, @Nullable SpawnGroupData s) {
        SpawnGroupData data = super.finalizeSpawn(l, d, r, s);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        hoe.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
        this.setItemSlot(EquipmentSlot.MAINHAND, hoe);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.inventory.addItem(new ItemStack(Items.WHEAT_SEEDS, 64));
        this.inventory.addItem(new ItemStack(Items.POTATO, 64));
        this.inventory.addItem(new ItemStack(Items.CARROT, 64)); return data;
    }


    @Override
    protected @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        ItemStack playerStack = pPlayer.getItemInHand(pHand);

        if (pPlayer.isShiftKeyDown()) {
            EquipmentSlot slot = null; // 初始化为null
            SoundEvent sound = SoundEvents.ITEM_PICKUP; // 默认声音

            // 规则1：如果是盔甲
            if (playerStack.getItem() instanceof ArmorItem armorItem) {
                slot = armorItem.getEquipmentSlot();
                sound = armorItem.getEquipSound().value();
            }
            // 规则2：如果是锄头
            else if (playerStack.getItem() instanceof HoeItem) {
                slot = EquipmentSlot.MAINHAND;
            }

            if (slot != null) {
                // 执行装备交换
                if (!this.level().isClientSide()) {
                    ItemStack currentItem = this.getItemBySlot(slot);
                    this.setItemSlot(slot, playerStack.copy());
                    if (!pPlayer.getAbilities().instabuild) {
                        pPlayer.setItemInHand(pHand, currentItem);
                    }
                    this.playSound(sound, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            } else {
                // 如果不是有效装备，则打开背包GUI
                if (!this.level().isClientSide) {
                    pPlayer.openMenu(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }

        // 交易逻辑
        if (this.isAlive() && this.getTradingPlayer() == null && !this.isBaby()) {
            if (!this.level().isClientSide) {
                this.startTrading(pPlayer);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        return super.mobInteract(pPlayer, pHand);
    }

    // --- GUI 和交易逻辑保持不变 ---
    @Override
    public @NotNull Component getDisplayName() {
        return this.getName();
    }
    @Nullable @Override
    public AbstractContainerMenu createMenu(int c, @NotNull Inventory i, @NotNull Player p) {
        return new ZombieChestMenu(c, i, this.inventory);
    }
    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }
    public void startTrading(Player p) {
        this.setTradingPlayer(p);
    }
    @Override
    public void setTradingPlayer(@Nullable Player p) {
        this.tradingPlayer = p;
    }
    @Nullable @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }
    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = TradeManager.getOffersFor(BuiltInRegistries.ENTITY_TYPE.getKey(this.getType()));
        }
        return this.offers;
    }
    @Override
    public void overrideOffers(@NotNull MerchantOffers o) {
        this.offers = o;
    }
    @Override
    public void notifyTrade(@NotNull MerchantOffer o) {
        o.increaseUses(); this.ambientSoundTime = -this.getAmbientSoundInterval();
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
    }
    @Override
    public void notifyTradeUpdated(@NotNull ItemStack s) {

    }
    @Override
    public int getVillagerXp() {
        return 0;
    }
    @Override
    public void overrideXp(int x) {

    }
    @Override
    public boolean showProgressBar() {
        return false;
    }
    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }
    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }
    @Override
    public boolean canRestock() {
        return false;
    }
}