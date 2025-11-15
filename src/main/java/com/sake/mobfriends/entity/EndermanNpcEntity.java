package com.sake.mobfriends.entity;

import com.sake.mobfriends.MobFriends; // 【新增】
import com.sake.mobfriends.block.WorkstationBlockEntity; // 【新增】
import com.sake.mobfriends.entity.ai.ReturnToWorkstationGoal;
import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.BlockPos; // 【新增】
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity; // 【新增】
import org.jetbrains.annotations.NotNull;
import net.minecraft.core.component.DataComponents;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

// 【修改】实现新接口
public class EndermanNpcEntity extends EnderMan implements Merchant, IWorkstationNPC {

    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

    // 【【【新增字段】】】
    @Nullable private BlockPos workstationPos = null;

    private int teleportCooldown = 0;
    @Nullable
    private UUID playerToTeleport = null;

    public EndermanNpcEntity(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return EnderMan.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new ReturnToWorkstationGoal(this, 16.0D, 1.0D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // 【修改】添加NBT
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TeleportCooldown", this.teleportCooldown);
        if (this.playerToTeleport != null) {
            compound.putUUID("PlayerToTeleport", this.playerToTeleport);
        }
        // 【新增】保存工作站位置
        if (this.workstationPos != null) {
            compound.putLong("WorkstationPos", this.workstationPos.asLong());
        }
    }

    // 【修改】添加NBT
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.teleportCooldown = compound.getInt("TeleportCooldown");
        if (compound.hasUUID("PlayerToTeleport")) {
            this.playerToTeleport = compound.getUUID("PlayerToTeleport");
        } else {
            this.playerToTeleport = null;
        }
        // 【新增】加载工作站位置
        if (compound.contains("WorkstationPos")) {
            this.workstationPos = BlockPos.of(compound.getLong("WorkstationPos"));
        } else {
            this.workstationPos = null;
        }
    }

    // (tick 保持不变)
    @Override
    public void tick() {
        super.tick();

        if (this.teleportCooldown > 0 && !this.level().isClientSide()) {
            this.teleportCooldown--;

            if (this.teleportCooldown == 0 && this.playerToTeleport != null) {
                Player player = this.level().getPlayerByUUID(this.playerToTeleport);

                if (player instanceof ServerPlayer serverPlayer && serverPlayer.isAlive()) {

                    Optional<GlobalPos> deathPosOpt = serverPlayer.getLastDeathLocation();

                    if (deathPosOpt.isPresent()) {
                        GlobalPos deathPos = deathPosOpt.get();

                        ServerLevel deathLevel = serverPlayer.server.getLevel(deathPos.dimension());
                        if (deathLevel != null) {
                            BlockPos pos = deathPos.pos();

                            ((ServerLevel)this.level()).sendParticles(
                                    ParticleTypes.PORTAL,
                                    serverPlayer.getX(), serverPlayer.getRandomY(), serverPlayer.getZ(),
                                    50, 0.5, 0.5, 0.5, 0.1
                            );

                            serverPlayer.teleportTo(
                                    deathLevel,
                                    pos.getX() + 0.5,
                                    pos.getY(),
                                    pos.getZ() + 0.5,
                                    serverPlayer.getYRot(),
                                    serverPlayer.getXRot()
                            );

                            deathLevel.sendParticles(
                                    ParticleTypes.PORTAL,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    100, 0.5, 0.5, 0.5, 0.1
                            );

                            deathLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }
                this.playerToTeleport = null;
            }
        }
    }

    // (mobInteract 保持不变)
    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        ItemStack playerStack = pPlayer.getItemInHand(pHand);

        if (playerStack.get(DataComponents.FOOD) != null && this.teleportCooldown == 0) {
            if (!this.level().isClientSide()) {
                if (pPlayer.getLastDeathLocation().isEmpty()) {
                    return InteractionResult.FAIL;
                }
                this.teleportCooldown = 10;
                this.playerToTeleport = pPlayer.getUUID();
                if (!pPlayer.getAbilities().instabuild) {
                    playerStack.shrink(1);
                }
                ((ServerLevel)this.level()).sendParticles(
                        ParticleTypes.HEART,
                        this.getRandomX(1.0D),
                        this.getRandomY() + 0.5D,
                        this.getRandomZ(1.0D),
                        7, 0.3, 0.3, 0.3, 0.05
                );
                this.playSound(SoundEvents.ENDERMAN_AMBIENT, 1.0F, 1.0F);
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
        return SoundEvents.ENDERMAN_AMBIENT;
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