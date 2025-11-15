package com.sake.mobfriends.entity;

import com.sake.mobfriends.MobFriends; // 【【【新增 IMPORT】】】
import com.sake.mobfriends.block.WorkstationBlockEntity; // 【【【新增 IMPORT】】】
import com.sake.mobfriends.entity.ai.DepositItemsGoal;
import com.sake.mobfriends.entity.ai.ReturnToWorkstationGoal;
import com.sake.mobfriends.entity.ai.ZombieFarmerGoal;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.BlockPos; // 【【【新增 IMPORT】】】
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity; // 【【【新增 IMPORT】】】
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nullable;
import java.util.OptionalInt;


// 【修改】实现新接口
public class ZombieNpcEntity extends Zombie implements Merchant, MenuProvider, IWorkstationNPC {

    @Nullable private MerchantOffers offers;
    @Nullable private Player tradingPlayer;
    private final SimpleContainer inventory = new SimpleContainer(27);

    // 【【【新增字段】】】
    @Nullable private BlockPos workstationPos = null;

    public ZombieNpcEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level); this.setCanPickUpLoot(false);
    }

    public static @NotNull AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    // (registerGoals 保持不变)
    @Override protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ZombieFarmerGoal(this, 0.8D));
        this.goalSelector.addGoal(2, new DepositItemsGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new ReturnToWorkstationGoal(this, 16.0D, 1.0D));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    // 【修改】添加NBT
    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        c.put("Inventory", this.inventory.createTag(this.level().registryAccess()));
        // 【新增】保存工作站位置
        if (this.workstationPos != null) {
            c.putLong("WorkstationPos", this.workstationPos.asLong());
        }
    }

    // 【修改】添加NBT
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        this.inventory.fromTag(c.getList("Inventory", 10), this.level().registryAccess());
        // 【新增】加载工作站位置
        if (c.contains("WorkstationPos")) {
            this.workstationPos = BlockPos.of(c.getLong("WorkstationPos"));
        } else {
            this.workstationPos = null;
        }
    }

    // (finalizeSpawn 保持不变)
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
            } else if (playerStack.getItem() instanceof HoeItem) {
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
            } else {
                if (!this.level().isClientSide) {
                    pPlayer.openMenu(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }

        if (this.isAlive() && this.getTradingPlayer() == null && !this.isBaby()) {
            // 【【【修复：添加主手检查】】】
            if (pHand == InteractionHand.MAIN_HAND) {
                if (!this.level().isClientSide()) {
                    this.startTrading(pPlayer);
                }
            }

        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    // (GUI 和 交易逻辑)
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
        this.openTradingScreen(p, this.getDisplayName());
    }
    public void openTradingScreen(Player pPlayer, Component pDisplayName) {
        OptionalInt o = pPlayer.openMenu(new SimpleMenuProvider((i, v, p) -> new MerchantMenu(i, v, this), pDisplayName));
        if (o.isPresent()) {
            MerchantOffers offers = this.getOffers();
            if (!offers.isEmpty()) {
                pPlayer.sendMerchantOffers(o.getAsInt(), offers, 1, this.getVillagerXp(), this.showProgressBar(), this.canRestock());
            }
        }
    }
    @Override
    public void setTradingPlayer(@Nullable Player p) {
        this.tradingPlayer = p;
    }
    @Nullable @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    // 【【【修改：重写 GETOFFERS】】】
    @Override
    public MerchantOffers getOffers() {
        if (this.level().isClientSide()) {
            return new MerchantOffers(); // 客户端不需要交易
        }

        // 1. 检查NPC是否已绑定
        if (this.workstationPos == null) {
            MobFriends.LOGGER.warn("NPC {} 尚未绑定工作站，无法提供交易。", this.getUUID());
            return new MerchantOffers();
        }

        // 2. 尝试获取工作站方块实体
        BlockEntity be = this.level().getBlockEntity(this.workstationPos);
        if (!(be instanceof WorkstationBlockEntity workstation)) {
            MobFriends.LOGGER.warn("NPC {} 无法在 {} 找到其工作站实体。", this.getUUID(), this.workstationPos);
            return new MerchantOffers();
        }

        // 3. 从工作站获取等级 (BE中是 0-4)
        // 我们需要 1-5，所以 +1
        int currentLevel = workstation.getNpcLevel() + 1; // getnpcLevel() 是你在 BE 中创建的方法

        // 4. 从TradeManager获取对应等级的交易
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        // 【注意】这里使用了新的、需要等级的方法
        return TradeManager.getOffersFor(entityId, currentLevel);
    }

    @Override
    public void overrideOffers(@NotNull MerchantOffers o) {
        this.offers = o; // 尽管我们不再使用 this.offers，但最好还是实现它
    }
    @Override
    public void notifyTrade(@NotNull MerchantOffer o) {
        o.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        // 【【【修复】】】
        this.playSound(this.getNotifyTradeSound(), 1.0F, 1.0F);
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        // 【【【修复】】】
        return SoundEvents.ZOMBIE_AMBIENT;
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
    public boolean isClientSide() {
        return this.level().isClientSide();
    }
    @Override
    public boolean canRestock() {
        return false;
    }

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