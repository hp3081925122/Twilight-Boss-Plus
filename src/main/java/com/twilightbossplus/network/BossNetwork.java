package com.twilightbossplus.network;

import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BossNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TwilightBossPlus.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private BossNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(VfxPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VfxPacket::encode)
                .decoder(VfxPacket::new)
                .consumerMainThread(VfxPacket::handle)
                .add();
    }

    public static void sendAround(Entity entity, VfxPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    }
}
