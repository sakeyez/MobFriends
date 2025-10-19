package com.sake.mobfriends.network;

import com.sake.mobfriends.MobFriends;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NpcPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MobFriends.MOD_ID).versioned(PROTOCOL_VERSION);

        // 在这里注册您的数据包
        // registrar.playBidirectional(...);
    }
}