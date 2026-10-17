package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.KnightFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class KnightFxPacket {
    public static final int TRACK = 0;
    public static final int LINE = 1;
    public static final int AXE_GLOW = 2;
    public static final int PICK_RING = 3;
    public static final int ENCIRCLE = 4;
    public static final int THRUST = 5;
    public static final int ECHO_SPAWN = 6;
    public static final int ECHO_DASH = 7;
    public static final int ECHO_END = 8;
    public static final int OATH = 9;
    public static final int REGROUP = 10;
    public static final int ENCIRCLE_END = 11;

    private final int type;
    private final int entityId;
    private final float[] data;

    public KnightFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(KnightFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static KnightFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 1024);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new KnightFxPacket(type, entityId, data);
    }

    public static void handle(KnightFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> KnightFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
