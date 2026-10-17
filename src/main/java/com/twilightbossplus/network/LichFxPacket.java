package com.twilightbossplus.network;

import com.twilightbossplus.client.fx.LichFxManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class LichFxPacket {
    public static final int TELEPORT = 0;
    public static final int SIGIL = 1;
    public static final int CHAIN_START = 2;
    public static final int CHAIN_END = 3;
    public static final int SHIELD_BREAK = 4;
    public static final int BOMB_BURST = 5;
    public static final int SLASH = 6;
    public static final int CROWN_START = 7;
    public static final int CROWN_BURST = 8;
    public static final int STAGGER = 9;
    public static final int CANDLE_SNUFF = 10;
    public static final int BEAM = 11;
    public static final int ORB_POP = 12;

    private final int type;
    private final int entityId;
    private final float[] data;

    public LichFxPacket(int type, int entityId, float... data) {
        this.type = type;
        this.entityId = entityId;
        this.data = data;
    }

    public static void encode(LichFxPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.type);
        buf.writeVarInt(packet.entityId);
        buf.writeVarInt(packet.data.length);
        for (float value : packet.data) {
            buf.writeFloat(value);
        }
    }

    public static LichFxPacket decode(FriendlyByteBuf buf) {
        int type = buf.readVarInt();
        int entityId = buf.readVarInt();
        int length = Math.min(buf.readVarInt(), 256);
        float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            data[i] = buf.readFloat();
        }
        return new LichFxPacket(type, entityId, data);
    }

    public static void handle(LichFxPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> LichFxManager.handle(packet.type, packet.entityId, packet.data));
    }
}
