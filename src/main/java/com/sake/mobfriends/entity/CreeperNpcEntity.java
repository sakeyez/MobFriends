package com.sake.mobfriends.entity;

import com.sake.mobfriends.MobFriends; // 【新增】
import com.sake.mobfriends.block.WorkstationBlockEntity; // 【新增】
import com.sake.mobfriends.entity.ai.CreeperEngineerGoal;
import com.sake.mobfriends.entity.ai.DepositItemsGoal;
import com.sake.mobfriends.entity.ai.ReturnToWorkstationGoal;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import net.minecraft.core.BlockPos; // 【新增】
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
import net.minecraft.world.level.block.entity.BlockEntity; // 【新增】
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.OptionalInt;

// 【修改】实现新接口
public class CreeperNpcEntity extends Creeper implements Merchant, MenuProvider, IWorkstationNPC {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【【【新增字段】】】
    @Nullable private BlockPos workstationPos = null;

    private final SimpleContainer inventory = new SimpleContainer(27);

    public CreeperNpcEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CreeperEngineerGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new ReturnToWorkstationGoal(this, 16.0D, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
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
        // 移除旧的 offers 保存
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
        // 移除旧的 offers 加载
    }

    // (mobInteract 保持不变)
    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        if (pPlayer.isShiftKeyDown()) {
            if (!this.level().isClientSide) {
                pPlayer.openMenu(this);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

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

    @Override
    public @NotNull Component getDisplayName() {
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int c, @NotNull Inventory i, @NotNull Player p) {
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
    public void notifyTradeUpdated(@NotNull ItemStack pStack) {}

    @Override
    public int getVillagerXp() { return 0; }

    @Override
    public void overrideXp(int pXp) {}

    @Override
    public boolean showProgressBar() { return false; }



    @Override
    public boolean isClientSide() { return this.level().isClientSide(); }

    public boolean canRestock() { return false; }

    @Override
    public void notifyTrade(@NotNull MerchantOffer pOffer) {
        pOffer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        // 【【【修复】】】
        this.playSound(this.getNotifyTradeSound(), 1.0F, this.getVoicePitch());
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        // 【【【修复】】】(使用 "Hurt" 声音，因为它很短)
        return SoundEvents.CREEPER_HURT;
    }

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