package com.sake.mobfriends.entity;

import com.sake.mobfriends.MobFriends; // 【新增】
import com.sake.mobfriends.block.WorkstationBlockEntity; // 【新增】
import com.sake.mobfriends.entity.ai.ReturnToWorkstationGoal;
import com.sake.mobfriends.inventory.ZombieChestMenu;
import com.sake.mobfriends.trading.TradeManager;
import com.sake.mobfriends.world.SlimeInventoryManager;
import net.minecraft.core.BlockPos; // 【新增】
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.entity.BlockEntity; // 【新增】
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.OptionalInt;

// 【修改】实现新接口
public class SlimeNpcEntity extends Slime implements Merchant, MenuProvider, IWorkstationNPC {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【【【新增字段】】】
    @Nullable private BlockPos workstationPos = null;

    private static final SimpleContainer CLIENT_DUMMY_INVENTORY = new SimpleContainer(27);

    public SlimeNpcEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
        this.moveControl = new SlimeNpcMoveControl(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SlimeNpcFloatGoal(this));
        this.goalSelector.addGoal(2, new ReturnToWorkstationGoal(this, 16.0D, 1.0D));
        this.goalSelector.addGoal(3, new SlimeNpcRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new SlimeNpcKeepOnJumpingGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // 【修改】添加NBT
    @Override
    public void addAdditionalSaveData(CompoundTag c) {
        super.addAdditionalSaveData(c);
        // 【新增】保存工作站位置
        if (this.workstationPos != null) {
            c.putLong("WorkstationPos", this.workstationPos.asLong());
        }
    }

    // 【修改】添加NBT
    @Override
    public void readAdditionalSaveData(CompoundTag c) {
        super.readAdditionalSaveData(c);
        // 【新增】加载工作站位置
        if (c.contains("WorkstationPos")) {
            this.workstationPos = BlockPos.of(c.getLong("WorkstationPos"));
        } else {
            this.workstationPos = null;
        }
    }

    public SimpleContainer getInventory() {
        if (this.level().isClientSide()) {
            return CLIENT_DUMMY_INVENTORY;
        }
        return SlimeInventoryManager.get((ServerLevel) this.level()).getInventory();
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

    // (交易相关的代码)
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
        // 【【【修复】】】(使用小史莱姆的声音，听起来更合适)
        return SoundEvents.SLIME_SQUISH_SMALL;
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

    // (MenuProvider 方法保持不变)
    @Override
    public @NotNull Component getDisplayName() {
        return this.getName();
    }
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int c, @NotNull Inventory i, @NotNull Player p) {
        SimpleContainer sharedInventory = SlimeInventoryManager.get((ServerLevel) this.level()).getInventory();
        return new ZombieChestMenu(c, i, sharedInventory);
    }

    // (内部AI类 保持不变)
    static class SlimeNpcMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final Slime slime;
        private boolean isAggressive;
        public SlimeNpcMoveControl(Slime pSlime) {
            super(pSlime); this.slime = pSlime; this.yRot = 180.0F * pSlime.getYRot() / (float) Math.PI;
        }
        public void setDirection(float pYRot, boolean pAggressive) {
            this.yRot = pYRot; this.isAggressive = pAggressive;
        }
        public void setWantedMovement(double speed) {
            this.speedModifier = speed; this.operation = MoveControl.Operation.MOVE_TO;
        }
        @Override
        public void tick() {
            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot(); this.mob.yBodyRot = this.mob.getYRot();
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
            } else {
                this.operation = MoveControl.Operation.WAIT;
                if (this.mob.onGround()) {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                    if (this.jumpDelay-- <= 0) {
                        this.jumpDelay = this.slime.getRandom().nextInt(20) + 10;
                        if (this.isAggressive) { this.jumpDelay /= 3; }
                        this.slime.getJumpControl().jump();
                        if (this.slime.getSize() > 0) {
                            SoundEvent jumpSound = this.slime.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
                            float volume = 0.4F * (float)this.slime.getSize();
                            float pitchBase = this.slime.isTiny() ? 1.4F : 0.8F;
                            float pitch = ((this.slime.getRandom().nextFloat() - this.slime.getRandom().nextFloat()) * 0.2F + 1.0F) * pitchBase;
                            this.slime.playSound(jumpSound, volume, pitch);
                        }
                    } else {
                        this.slime.xxa = 0.0F; this.slime.zza = 0.0F; this.mob.setSpeed(0.0F);
                    }
                } else {
                    this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                }
            }
        }
    }
    static class SlimeNpcRandomDirectionGoal extends Goal {
        private final Slime slime; private float chosenDegrees; private int nextRandomizeTime;
        public SlimeNpcRandomDirectionGoal(Slime slime) { this.slime = slime; this.setFlags(EnumSet.of(Goal.Flag.LOOK)); }
        @Override
        public boolean canUse() {
            return this.slime.getTarget() == null && (this.slime.onGround() || this.slime.isInWater() || this.slime.isInLava()) && this.slime.getMoveControl() instanceof SlimeNpcMoveControl;
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
        public SlimeNpcKeepOnJumpingGoal(Slime slime) { this.slime = slime; this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE)); }
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
            this.slime = slime; this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE)); slime.getNavigation().setCanFloat(true);
        }
        @Override
        public boolean canUse() {
            return (this.slime.isInWater() || this.slime.isInLava()) && this.slime.getMoveControl() instanceof SlimeNpcMoveControl;
        }
        @Override
        public boolean requiresUpdateEveryTick() { return true; }
        @Override
        public void tick() {
            if (this.slime.getRandom().nextFloat() < 0.8F) {
                this.slime.getJumpControl().jump();
            }
            ((SlimeNpcMoveControl)this.slime.getMoveControl()).setWantedMovement(1.2);
        }
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