package com.twilightbossplus.network;

import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BossNetwork {
    private static final String VERSION = "1";

    private BossNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(VERSION).playToClient(VfxPacket.TYPE, VfxPacket.STREAM_CODEC, VfxPacket::handle);
        TwilightBossPlus.LOGGER.info("Registered Twilight Boss Plus network payloads");
    }

    public static void sendAround(Entity entity, VfxPacket packet) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, packet);
    }
}
