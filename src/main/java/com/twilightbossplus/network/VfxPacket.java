package com.twilightbossplus.network;

import com.twilightbossplus.client.vfx.ClientVfx;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class VfxPacket {
    public static final int SHAKE = 8;

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
