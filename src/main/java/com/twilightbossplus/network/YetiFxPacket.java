package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.YetiFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class YetiFxPacket {
    public static final int CAST = 0;
    public static final int BREATH = 1;
    public static final int RIFT = 2;
    public static final int ICICLE = 3;
    public static final int SLUSH = 4;
    public static final int WAVE = 5;
    public static final int BOULDER_BREAK = 6;

    private final int type;
    private final int entityId;
    private final float[] data;

    public YetiFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(YetiFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static YetiFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 1024);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new YetiFxPacket(type, entityId, data);
    }

    public static void handle(YetiFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> YetiFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
