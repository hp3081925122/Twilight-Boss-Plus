package com.twilightbossplus.network;

import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(TwilightBossPlus.MOD_ID, "main"),
        () -> VERSION,
        VERSION::equals,
        VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(NagaFxPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(NagaFxPacket::encode)
            .decoder(NagaFxPacket::decode)
            .consumerMainThread(NagaFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(LichFxPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(LichFxPacket::encode)
            .decoder(LichFxPacket::decode)
            .consumerMainThread(LichFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(MinoFxPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MinoFxPacket::encode)
            .decoder(MinoFxPacket::decode)
            .consumerMainThread(MinoFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(HydraFxPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(HydraFxPacket::encode)
            .decoder(HydraFxPacket::decode)
            .consumerMainThread(HydraFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(KnightFxPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(KnightFxPacket::encode)
            .decoder(KnightFxPacket::decode)
            .consumerMainThread(KnightFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(UrGhastFxPacket.class, 5, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(UrGhastFxPacket::encode)
            .decoder(UrGhastFxPacket::decode)
            .consumerMainThread(UrGhastFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(YetiFxPacket.class, 6, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(YetiFxPacket::encode)
            .decoder(YetiFxPacket::decode)
            .consumerMainThread(YetiFxPacket::handle)
            .add();
        CHANNEL.messageBuilder(QueenFxPacket.class, 7, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(QueenFxPacket::encode)
            .decoder(QueenFxPacket::decode)
            .consumerMainThread(QueenFxPacket::handle)
            .add();
    }

    public static void sendTracking(Entity entity, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }

    public static void sendNear(Level level, double x, double y, double z, double radius, Object packet) {
        CHANNEL.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, radius, level.dimension())), packet);
    }
}
