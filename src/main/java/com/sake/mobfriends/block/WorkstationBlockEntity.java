package com.sake.mobfriends.block;

import com.sake.mobfriends.MobFriends;
import com.sake.mobfriends.entity.IWorkstationNPC;
import com.sake.mobfriends.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.UUID;

public class WorkstationBlockEntity extends BlockEntity {

    // (NBT 键名, 等级常量, 字段... 保持不变)
    private static final String NBT_KEY_LEVEL = "NpcLevel";
    private static final String NBT_KEY_NPC_UUID = "NpcUuid";
    private static final int MAX_LEVEL = 4;
    private static final int[] LEVEL_UP_COSTS = {4, 8, 16, 32};
    private static final String[] LEVEL_NAMES = {"新手", "学徒", "老手", "专家", "大师"};
    private int npcLevel = 0;
    @Nullable
    private UUID npcUuid = null;

    // 【【【新增：粒子计时器】】】
    // (这个值不需要保存到NBT，它只是一个临时的计时器)
    private int rangeParticleTimer = 0;

    // (构造函数, NBT, 数据同步... 保持不变)
    public WorkstationBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WORKSTATION_BLOCK_ENTITY.get(), pPos, pBlockState);
    }
    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putInt(NBT_KEY_LEVEL, this.npcLevel);
        if (this.npcUuid != null) {
            pTag.putUUID(NBT_KEY_NPC_UUID, this.npcUuid);
        }
    }
    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.npcLevel = Math.min(pTag.getInt(NBT_KEY_LEVEL), MAX_LEVEL);
        if (pTag.hasUUID(NBT_KEY_NPC_UUID)) {
            this.npcUuid = pTag.getUUID(NBT_KEY_NPC_UUID);
        } else {
            this.npcUuid = null;
        }
    }
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveWithoutMetadata(pRegistries);
    }


    // --- 【【【新增：Ticking 逻辑】】】 ---

    /**
     * 这个方法由 WorkstationBlock 中的 Ticker 每tick调用一次 (仅限服务器)
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, WorkstationBlockEntity be) {
        // 检查计时器
        if (be.rangeParticleTimer > 0) {

            // 【【【核心修改】】】
            // 我们使用模组运算符 (%) 来检查计时器是否是 20 (1秒) 的倍数。
            // 粒子将在 be.rangeParticleTimer = 100, 80, 60, 40, 20 时生成。
            if (be.rangeParticleTimer % 20 == 0) {
                // 只有在每秒的开头才生成一次粒子
                be.showRange(level, pos);
            }

            // 计时器仍然每 tick 减 1
            be.rangeParticleTimer--;
        }
    }

    public void startShowingRange() {
        // 设置计时器为 100 ticks (5 秒)
        this.rangeParticleTimer = 200;

        // 立即播放一次音效
        if (this.level != null && !this.level.isClientSide()) {
            this.level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 1.2f);
        }
    }




    // (spawnNpc, upgradeLevel, reviveNpc... 保持不变)
    public void spawnNpc(Level level, BlockPos pos, EntityType<?> npcType) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 8, false);
        if (this.npcUuid != null) {
            Entity existingNpc = serverLevel.getEntity(this.npcUuid);
            if (existingNpc != null && existingNpc.isAlive()) {
                existingNpc.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                serverLevel.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0f, 1.0f);
                serverLevel.sendParticles(ParticleTypes.PORTAL, existingNpc.getX(), existingNpc.getY() + 0.5, existingNpc.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                if (player != null) {
                    player.sendSystemMessage(Component.literal("已将你绑定的伙伴传送回来！"));
                }
                return;
            }
            else if (existingNpc == null || !existingNpc.isAlive()) {
                if (player != null) {
                    player.sendSystemMessage(Component.literal("召唤失败：绑定的NPC已死亡。请使用不死图腾右键此方块来复活。"));
                }
                return;
            }
        }
        Entity entity = npcType.create(level);
        if (entity instanceof Mob npcMob) {
            npcMob.setPos(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            npcMob.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
            this.npcUuid = npcMob.getUUID();
            if (npcMob instanceof IWorkstationNPC workstationNPC) {
                workstationNPC.setWorkstationPos(this.getBlockPos());
            } else {
                MobFriends.LOGGER.error("严重错误: {} 没有实现 IWorkstationNPC 接口！绑定失败。", npcMob.getType().toString());
            }
            level.addFreshEntity(npcMob);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.1);
            this.setChangedAndNotify();
        } else {
            MobFriends.LOGGER.error("在 {} 处召唤失败: EntityType {} 不是一个Mob!", pos, BuiltInRegistries.ENTITY_TYPE.getKey(npcType));
        }
    }
    public void upgradeLevel(Player player, ItemStack tokenStack) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (this.npcLevel >= MAX_LEVEL) {
            player.sendSystemMessage(Component.literal("工作站已达到最高等级（" + getLevelName(this.npcLevel) + "）。"));
            return;
        }
        int requiredCost = LEVEL_UP_COSTS[this.npcLevel];
        if (tokenStack.getCount() < requiredCost) {
            player.sendSystemMessage(Component.literal("升级失败：需要 " + requiredCost + " 个谢意才能升级到 " + getLevelName(this.npcLevel + 1) + "。"));
            return;
        }
        if (!player.getAbilities().instabuild) {
            tokenStack.shrink(requiredCost);
        }
        this.npcLevel++;
        this.level.playSound(null, this.worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.2f);
        ((ServerLevel)this.level).sendParticles(ParticleTypes.ENCHANT,
                this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.2, this.worldPosition.getZ() + 0.5,
                30, 0.4, 0.4, 0.4, 0.05);
        player.sendSystemMessage(Component.literal("升级成功！工作站已达到 " + getLevelName(this.npcLevel) + " 等级。"));
        this.setChangedAndNotify();
    }
    public void reviveNpc(Player player, EntityType<?> npcType) {
        if (this.level == null || this.level.isClientSide() || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.npcUuid == null) {
            player.sendSystemMessage(Component.literal("复活失败：此工作站还未召唤过NPC。"));
            return;
        }
        Entity existingNpc = serverLevel.getEntity(this.npcUuid);
        if (existingNpc != null && existingNpc.isAlive()) {
            player.sendSystemMessage(Component.literal("复活失败：你绑定的伙伴还活着！"));
            return;
        }
        this.level.playSound(null, this.worldPosition, SoundEvents.TOTEM_USE, SoundSource.BLOCKS, 1.0f, 0.8f);
        serverLevel.sendParticles(ParticleTypes.SOUL,
                this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                40, 0.5, 0.5, 0.5, 0.05);
        this.npcUuid = null;
        player.sendSystemMessage(Component.literal("你的伙伴已重获新生！(等级保留为 " + getLevelName(this.npcLevel) + ")"));
        this.spawnNpc(this.level, this.worldPosition, npcType);
    }


    /**
     * 方案 4b: 显示范围
     * (此方法保持不变，现在它由 serverTick 调用)
     */
    public void showRange(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            // (我们把声音移到了 startShowingRange, 防止每 tick 都响)

            double radius = 16.5;
            Vec3 center = Vec3.atCenterOf(pos);
            ParticleOptions particle = ParticleTypes.END_ROD;

            Vec3 v000 = center.add(-radius, -radius, -radius);
            Vec3 v100 = center.add( radius, -radius, -radius);
            Vec3 v010 = center.add(-radius,  radius, -radius);
            Vec3 v001 = center.add(-radius, -radius,  radius);
            Vec3 v110 = center.add( radius,  radius, -radius);
            Vec3 v101 = center.add( radius, -radius,  radius);
            Vec3 v011 = center.add(-radius,  radius,  radius);
            Vec3 v111 = center.add( radius,  radius,  radius);

            spawnParticleLine(serverLevel, particle, v000, v100);
            spawnParticleLine(serverLevel, particle, v000, v001);
            spawnParticleLine(serverLevel, particle, v101, v100);
            spawnParticleLine(serverLevel, particle, v101, v001);
            spawnParticleLine(serverLevel, particle, v010, v110);
            spawnParticleLine(serverLevel, particle, v010, v011);
            spawnParticleLine(serverLevel, particle, v111, v110);
            spawnParticleLine(serverLevel, particle, v111, v011);
            spawnParticleLine(serverLevel, particle, v000, v010);
            spawnParticleLine(serverLevel, particle, v100, v110);
            spawnParticleLine(serverLevel, particle, v001, v011);
            spawnParticleLine(serverLevel, particle, v101, v111);
        }
    }

    /**
     * 辅助方法：绘制粒子线
     * (此方法保持不变)
     */
    private void spawnParticleLine(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double dist = delta.length();
        double step = 0.5;

        for (double d = 0; d < dist; d += step) {
            Vec3 p = from.lerp(to, d / dist);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.sendParticles(particle, to.x, to.y, to.z, 1, 0, 0, 0, 0);
    }

    // (setChangedAndNotify, getLevelName, getNpcLevel... 保持不变)
    private void setChangedAndNotify() {
        if (this.level == null) return;
        this.setChanged();
        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
    }
    private String getLevelName(int level) {
        if (level >= 0 && level < LEVEL_NAMES.length) {
            return LEVEL_NAMES[level];
        }
        return "未知";
    }
    public int getNpcLevel() {
        return this.npcLevel;
    }
}