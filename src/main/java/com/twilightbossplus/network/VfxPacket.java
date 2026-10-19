package com.twilightbossplus.network;

import com.twilightbossplus.client.vfx.ClientVfx;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class VfxPacket {
    public static final int LINE = 0;
    public static final int CIRCLE = 1;
    public static final int GLOB = 2;
    public static final int POOL = 3;
    public static final int RING = 4;
    public static final int CONE = 5;
    public static final int BURROW = 6;
    public static final int BURST = 7;
    public static final int SHAKE = 8;
    public static final int AURA = 9;
    public static final int BEAM = 10;
    public static final int PILLAR = 11;
    public static final int TETHER = 12;
    public static final int SIGIL = 13;
    public static final int VORTEX = 14;
    public static final int SLASH = 15;
    public static final int ORB = 16;
    public static final int WALL = 17;

    public static final int BURST_VENOM = 0;
    public static final int BURST_DIRT = 1;
    public static final int BURST_SWEEP = 2;
    public static final int BURST_SLAM = 3;
    public static final int BURST_SOUL = 4;

    public final int type;
    public final CompoundTag data;

    public VfxPacket(int type, CompoundTag data) {
        this.type = type;
        this.data = data;
    }

    public VfxPacket(FriendlyByteBuf buf) {
        this.type = buf.readVarInt();
        CompoundTag tag = buf.readNbt();
        if (tag == null) {
            tag = new CompoundTag();
        }
        this.data = tag;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.type);
        buf.writeNbt(this.data);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientVfx.handle(this.type, this.data));
    }
}
