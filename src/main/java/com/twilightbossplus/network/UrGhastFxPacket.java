package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.UrGhastFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class UrGhastFxPacket {
    public static final int GAZE = 0;
    public static final int GAZE_END = 1;
    public static final int RAIN = 2;
    public static final int RAIN_END = 3;
    public static final int RECALL = 4;
    public static final int RECALL_END = 5;
    public static final int STACKS = 6;
    public static final int TEAR_CHARGE = 7;
    public static final int POOL = 8;
    public static final int SPLASH = 9;

    private final int type;
    private final int entityId;
    private final float[] data;

    public UrGhastFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(UrGhastFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static UrGhastFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 1024);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new UrGhastFxPacket(type, entityId, data);
    }

    public static void handle(UrGhastFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> UrGhastFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
