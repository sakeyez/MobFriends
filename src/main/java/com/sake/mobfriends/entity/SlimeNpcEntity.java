package com.sake.mobfriends.entity;

import com.sake.mobfriends.inventory.ZombieChestMenu;
import com.sake.mobfriends.trading.TradeManager;
import com.sake.mobfriends.world.SlimeInventoryManager; // 【【【新增导入】】】
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel; // 【【【新增导入】】】
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.OptionalInt;

public class SlimeNpcEntity extends Slime implements Merchant, MenuProvider {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【【【修改点 1：移除实例背包】】】
    // private final SimpleContainer inventory = new SimpleContainer(27); // (删除这行)

    // 【【【新增：为客户端AI提供一个空的假背包，防止崩溃】】】
    private static final SimpleContainer CLIENT_DUMMY_INVENTORY = new SimpleContainer(27);

    public SlimeNpcEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        this.moveControl = new SlimeNpcMoveControl(this);
    }

    // (createAttributes 保持不变)
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    // (registerGoals 保持不变)
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SlimeNpcFloatGoal(this));
        this.goalSelector.addGoal(3, new SlimeNpcRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new SlimeNpcKeepOnJumpingGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }
    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        // (背包NBT现在由 SlimeInventoryManager 处理，这里也不需要了)
        // c.put("Inventory", ...);

        // (移除 if (this.offers != null) ...)
    }

    /**
     * 【【【修复：移除 NBT 中对 Offers 的读取】】】
     */
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        // (背包NBT现在由 SlimeInventoryManager 处理，这里也不需要了)
        // this.inventory.fromTag(...);

        // (移除 if (c.contains("Offers", 10)) ...)
    }

    /**
     * 【【【修改：从全局管理器获取背包】】】
     */
    public SimpleContainer getInventory() {
        if (this.level().isClientSide()) {
            return CLIENT_DUMMY_INVENTORY;
        }
        return SlimeInventoryManager.get((ServerLevel) this.level()).getInventory();
    }
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

    // ... (交易相关的代码保持不变) ...

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

    // 【【【修改点 6】】】
    // 实现 MenuProvider 的两个方法

    @Override
    public @NotNull Component getDisplayName() {
        // 重用它自己的实体名称
        return this.getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int c, @NotNull Inventory i, @NotNull Player p) {
        // (此方法只在服务器端调用，所以我们可以安全地转换)
        // 从世界数据中获取那个唯一的共享背包
        SimpleContainer sharedInventory = SlimeInventoryManager.get((ServerLevel) this.level()).getInventory();

        // 使用这个共享背包创建菜单
        return new ZombieChestMenu(c, i, sharedInventory);
    }

    // ... (所有内部AI类 SlimeNpcMoveControl, SlimeNpcRandomDirectionGoal 等保持不变) ...

    static class SlimeNpcMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final Slime slime;
        private boolean isAggressive;

        public SlimeNpcMoveControl(Slime pSlime) {
            super(pSlime);
            this.slime = pSlime;
            this.yRot = 180.0F * pSlime.getYRot() / (float) Math.PI;
        }

        public void setDirection(float pYRot, boolean pAggressive) {
            this.yRot = pYRot;
            this.isAggressive = pAggressive;
        }

        public void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();

            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = MoveControl.Operation.WAIT;
                if (this.mob.onGround()) {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = this.slime.getRandom().nextInt(20) + 10;
                        if (this.isAggressive) {
                            this.jumpDelay /= 3;
                        }

                        this.slime.getJumpControl().jump();

                        if (this.slime.getSize() > 0) {
                            SoundEvent jumpSound = this.slime.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
                            float volume = 0.4F * (float)this.slime.getSize();
                            float pitchBase = this.slime.isTiny() ? 1.4F : 0.8F;
                            float pitch = ((this.slime.getRandom().nextFloat() - this.slime.getRandom().nextFloat()) * 0.2F + 1.0F) * pitchBase;
                            this.slime.playSound(jumpSound, volume, pitch);
                        }
                    } else {
                        this.slime.xxa = 0.0F;
                        this.slime.zza = 0.0F;
                        this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
            }
        }
    }


    static class SlimeNpcRandomDirectionGoal extends Goal {
        private final Slime slime;
        private float chosenDegrees;
        private int nextRandomizeTime;

        public SlimeNpcRandomDirectionGoal(Slime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.slime.getTarget() == null
                    && (this.slime.onGround() || this.slime.isInWater() || this.slime.isInLava())
                    && this.slime.getMoveControl() instanceof SlimeNpcMoveControl;
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = 40 + this.slime.getRandom().nextInt(60);
                this.chosenDegrees = (float)this.slime.getRandom().nextInt(360);
            }
            ((SlimeNpcMoveControl)this.slime.getMoveControl()).setDirection(this.chosenDegrees, false);
        }
    }

    static class SlimeNpcKeepOnJumpingGoal extends Goal {
        private final Slime slime;

        public SlimeNpcKeepOnJumpingGoal(Slime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !this.slime.isPassenger() && this.slime.getMoveControl() instanceof SlimeNpcMoveControl;
        }

        @Override
        public void tick() {
            ((SlimeNpcMoveControl)this.slime.getMoveControl()).setWantedMovement(1.0);
        }
    }

    static class SlimeNpcFloatGoal extends Goal {
        private final Slime slime;

        public SlimeNpcFloatGoal(Slime slime) {
            this.slime = slime;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
            slime.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return (this.slime.isInWater() || this.slime.isInLava())
                    && this.slime.getMoveControl() instanceof SlimeNpcMoveControl;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.slime.getRandom().nextFloat() < 0.8F) {
                this.slime.getJumpControl().jump();
            }
            ((SlimeNpcMoveControl)this.slime.getMoveControl()).setWantedMovement(1.2);
        }
    }
}