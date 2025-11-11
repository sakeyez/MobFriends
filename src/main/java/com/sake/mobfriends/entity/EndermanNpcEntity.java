package com.sake.mobfriends.entity;

import com.sake.mobfriends.trading.TradeManager;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.NotNull;

// 【【【新增导入：DataComponents】】】
import net.minecraft.core.component.DataComponents;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public class EndermanNpcEntity extends EnderMan implements Merchant {


    @Override
    protected void registerGoals() {
        // 添加基础AI

        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D)); // 随机漫步，并躲避水
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F)); // 看向玩家
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this)); // 随机环顾四周
    }
    @Nullable
    private MerchantOffers offers;
    @Nullable
    private Player tradingPlayer;

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
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("TeleportCooldown", this.teleportCooldown);
        if (this.playerToTeleport != null) {
            compound.putUUID("PlayerToTeleport", this.playerToTeleport);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.teleportCooldown = compound.getInt("TeleportCooldown");
        if (compound.hasUUID("PlayerToTeleport")) {
            this.playerToTeleport = compound.getUUID("PlayerToTeleport");
        } else {
            this.playerToTeleport = null;
        }
    }

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

                            // 【【【修复 1： SoundSource.PLAYER -> SoundSource.PLAYERS】】】
                            deathLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }
                this.playerToTeleport = null;
            }
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player pPlayer, @NotNull InteractionHand pHand) {
        ItemStack playerStack = pPlayer.getItemInHand(pHand);

        // 【【【修复 2： playerStack.isEdible() -> playerStack.get(DataComponents.FOOD) != null】】】
        // 1. 检查玩家是否手持食物 且 NPC未处于冷却
        if (playerStack.get(DataComponents.FOOD) != null && this.teleportCooldown == 0) {

            // 2. 仅在服务器端处理逻辑
            if (!this.level().isClientSide()) {

                // 3. 检查玩家是否有死亡点
                if (pPlayer.getLastDeathLocation().isEmpty()) {
                    return InteractionResult.FAIL;
                }

                // 4. 设置传送
                this.teleportCooldown = 10; // 10 ticks = 0.5 秒
                this.playerToTeleport = pPlayer.getUUID();

                // 5. 消耗食物
                if (!pPlayer.getAbilities().instabuild) {
                    playerStack.shrink(1);
                }

                // 6. 在末影人身上播放爱心粒子
                ((ServerLevel)this.level()).sendParticles(
                        ParticleTypes.HEART,
                        this.getRandomX(1.0D),
                        this.getRandomY() + 0.5D,
                        this.getRandomZ(1.0D),
                        7, 0.3, 0.3, 0.3, 0.05
                );

                // 7. 播放声音
                this.playSound(SoundEvents.ENDERMAN_AMBIENT, 1.0F, 1.0F);
            }

            // 8. 返回成功 (无论客户端还是服务端)
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        // 【原版】交易逻辑 (如果上述条件不满足，则尝试交易)
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


    // --- (以下交易逻辑保持不变) ---

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