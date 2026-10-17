package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.MinoFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class MinoFxPacket {
    public static final int CHARGE_WARN = 0;
    public static final int STUN = 1;
    public static final int WAVE = 2;
    public static final int PILLAR_WARN = 3;
    public static final int PILLAR_RISE = 4;
    public static final int PILLAR_BREAK = 5;
    public static final int SPORE_BURST = 6;
    public static final int HORN = 7;
    public static final int ROAR = 8;
    public static final int CHARGE_END = 9;

    private final int type;
    private final int entityId;
    private final float[] data;

    public MinoFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(MinoFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static MinoFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 256);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new MinoFxPacket(type, entityId, data);
    }

    public static void handle(MinoFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinoFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
