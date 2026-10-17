package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.HydraFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class HydraFxPacket {
    public static final int TAIL_WARN = 0;
    public static final int SWEEP_WARN = 1;
    public static final int SCORCH = 2;
    public static final int SLAG = 3;
    public static final int SLAG_IMPACT = 4;
    public static final int WAVE = 5;
    public static final int GEYSER = 6;

    private final int type;
    private final int entityId;
    private final float[] data;

    public HydraFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(HydraFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static HydraFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 1024);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new HydraFxPacket(type, entityId, data);
    }

    public static void handle(HydraFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HydraFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
