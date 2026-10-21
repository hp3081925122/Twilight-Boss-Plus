package com.twilightbossplus.network;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.vfx.ClientVfx;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class VfxPacket implements CustomPacketPayload {
    public static final int SHAKE = 8;
    public static final Type<VfxPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TwilightBossPlus.MODID, "vfx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VfxPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, packet -> packet.type,
            ByteBufCodecs.COMPOUND_TAG, packet -> packet.data,
            VfxPacket::new);

    public final int type;
    public final CompoundTag data;

    public VfxPacket(int type, CompoundTag data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public Type<VfxPacket> type() {
        return TYPE;
    }

    public static void handle(VfxPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientVfx.handle(packet.type, packet.data));
    }
}
