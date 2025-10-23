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

import java.util.UUID;
import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MobFriends.MOD_ID);

    public static final Supplier<DataComponentType<UUID>> ZOMBIE_UUID = COMPONENT_TYPES.register("zombie_uuid",
            () -> DataComponentType.<UUID>builder()
                    // 这个用于存档，是正确的
                    .persistent(UUIDUtil.STRING_CODEC)
                    // 最终修正：网络同步的 Codec 也在 UUIDUtil 中！
                    .networkSynchronized(UUIDUtil.STREAM_CODEC)
                    .build());
    // 定义一个可以存储 NBT 标签的数据组件
    public static final Supplier<DataComponentType<CompoundTag>> STORED_ZOMBIE_NBT = COMPONENT_TYPES.register("stored_zombie_nbt",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC) // CompoundTag 自带 Codec
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG) // CompoundTag 自带网络同步 Codec
                    .build());



    public static void register(IEventBus eventBus) {
        COMPONENT_TYPES.register(eventBus);
    }
}