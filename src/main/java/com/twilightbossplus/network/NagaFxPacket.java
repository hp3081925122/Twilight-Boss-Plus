package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.NagaFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class NagaFxPacket {
    public static final int RING = 0;
    public static final int SWEEP = 1;
    public static final int SHED = 2;
    public static final int REND_POINT = 3;
    public static final int BITE = 4;
    public static final int CONSTRICT_START = 5;
    public static final int CONSTRICT_END = 6;
    public static final int WEAK_HIT = 7;

    private final int type;
    private final int entityId;
    private final float[] data;

    public NagaFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(NagaFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static NagaFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 4096);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new NagaFxPacket(type, entityId, data);
    }

    public static void handle(NagaFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NagaFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
