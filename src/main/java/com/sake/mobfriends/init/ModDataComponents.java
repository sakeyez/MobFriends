package com.sake.mobfriends.init;

import com.mojang.serialization.Codec;
import com.sake.mobfriends.MobFriends;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MobFriends.MOD_ID);

    //僵尸
    public static final Supplier<DataComponentType<UUID>> ZOMBIE_UUID = COMPONENT_TYPES.register("zombie_uuid",
            () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.STRING_CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());
    public static final Supplier<DataComponentType<CompoundTag>> STORED_ZOMBIE_NBT = COMPONENT_TYPES.register("stored_zombie_nbt",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

   //凋零骷髅
    public static final Supplier<DataComponentType<UUID>> WITHER_UUID = COMPONENT_TYPES.register("wither_uuid",
            () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.STRING_CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());
    public static final Supplier<DataComponentType<CompoundTag>> STORED_WITHER_NBT = COMPONENT_TYPES.register("stored_wither_nbt",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    //苦力怕
    public static final Supplier<DataComponentType<UUID>> CREEPER_UUID = COMPONENT_TYPES.register("creeper_uuid",
            () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.STRING_CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());
    public static final Supplier<DataComponentType<CompoundTag>> STORED_CREEPER_NBT = COMPONENT_TYPES.register("stored_creeper_nbt",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    // 烈焰人
    public static final Supplier<DataComponentType<UUID>> BLAZE_UUID = COMPONENT_TYPES.register("blaze_uuid",
            () -> DataComponentType.<UUID>builder()
                    .persistent(UUIDUtil.STRING_CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());
    public static final Supplier<DataComponentType<CompoundTag>> STORED_BLAZE_NBT = COMPONENT_TYPES.register("stored_blaze_nbt",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    public static final Supplier<DataComponentType<Boolean>> IS_UPGRADED = COMPONENT_TYPES.register("is_upgraded",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static void register(IEventBus eventBus) {
        COMPONENT_TYPES.register(eventBus);
    }
}