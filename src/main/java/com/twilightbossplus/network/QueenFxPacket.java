package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.QueenFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class QueenFxPacket {
    public static final int CAST = 0;
    public static final int SIGIL = 1;
    public static final int LANCE = 2;
    public static final int AURORA = 3;
    public static final int REFRACT = 4;

    private final int type;
    private final int entityId;
    private final float[] data;

    public QueenFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(QueenFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static QueenFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 1024);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new QueenFxPacket(type, entityId, data);
    }

    public static void handle(QueenFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> QueenFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
