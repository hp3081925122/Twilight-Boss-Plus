package com.twilightbossplus.entity;

import com.twilightbossplus.network.LichFxPacket;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.registry.ModEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import twilightforest.entity.boss.Lich;

public class SoulCandle extends Entity {
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(SoulCandle.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerId;
    private boolean snuffedByPlayer;

    public SoulCandle(EntityType<? extends SoulCandle> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public SoulCandle(Level level, Lich owner) {
        this(ModEntities.SOUL_CANDLE.get(), level);
        this.ownerId = owner.getUUID();
        this.entityData.set(OWNER, owner.getId());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, -1);
    }

    public int getOwnerEntityId() {
        return this.entityData.get(OWNER);
    }

    public boolean isOwnedBy(Lich lich) {
        return lich.getUUID().equals(this.ownerId);
    }

    public boolean wasSnuffedByPlayer() {
        return this.snuffedByPlayer;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        Entity owner = null;
        if (this.ownerId != null) {
            owner = ((ServerLevel) this.level()).getEntity(this.ownerId);
        }
        if (!(owner instanceof Lich lich) || !lich.isAlive() || lich.getPhase() != 2) {
            this.snuff(false);
            return;
        }
        if (this.entityData.get(OWNER) != lich.getId()) {
            this.entityData.set(OWNER, lich.getId());
        }
    }

    private void snuff(boolean byPlayer) {
        if (this.isRemoved()) {
            return;
        }
        this.snuffedByPlayer = byPlayer;
        ModNetwork.sendTracking(this, new LichFxPacket(LichFxPacket.CANDLE_SNUFF, this.getId(), (float) this.getX(), (float) this.getY() + 0.7F, (float) this.getZ()));
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 2.0F, 0.6F);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Lich) {
            return false;
        }
        this.snuff(true);
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
