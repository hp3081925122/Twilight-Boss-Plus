package com.twilightbossplus.entity;

import com.twilightbossplus.config.MinoshroomConfig;
import com.twilightbossplus.network.MinoFxPacket;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import twilightforest.entity.boss.Minoshroom;

public class MushroomPillar extends Entity {
    public static final float WIDTH = 1.2F;
    public static final float HEIGHT = 3.0F;
    public static final int RISE_TICKS = 6;
    private static final int HIT_INTERVAL = 10;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MushroomPillar.class, EntityDataSerializers.INT);

    private int hits;
    private int lifetime = 300;
    private int lastHitTick = -HIT_INTERVAL;

    public MushroomPillar(EntityType<? extends MushroomPillar> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public MushroomPillar(Level level, double x, double y, double z, int kind, int lifetime) {
        this(ModEntities.MUSHROOM_PILLAR.get(), level);
        this.setPos(x, y, z);
        this.entityData.set(KIND, kind);
        this.lifetime = lifetime;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, 0);
    }

    public int getKind() {
        return this.entityData.get(KIND);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        this.lifetime--;
        if (this.lifetime <= 0) {
            this.shatter();
        }
    }

    public void shatter() {
        if (this.isRemoved()) {
            return;
        }
        ModNetwork.sendTracking(this, new MinoFxPacket(MinoFxPacket.PILLAR_BREAK, this.getId(), (float) this.getX(), (float) this.getY(), (float) this.getZ(), this.getKind()));
        this.level().playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(), SoundEvents.STEM_BREAK, SoundSource.HOSTILE, 1.6F, 0.7F);
        this.level().playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(), SoundEvents.FUNGUS_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.isRemoved()) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || attacker instanceof Minoshroom) {
            return false;
        }
        if (this.tickCount - this.lastHitTick < HIT_INTERVAL) {
            return false;
        }
        this.lastHitTick = this.tickCount;
        this.hits++;
        if (this.hits >= MinoshroomConfig.PILLAR_HITS_TO_BREAK.get()) {
            this.shatter();
        } else {
            this.level().playSound(null, this.getX(), this.getY() + 1.5D, this.getZ(), SoundEvents.STEM_HIT, SoundSource.HOSTILE, 1.4F, 0.6F + this.hits * 0.15F);
        }
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isRemoved();
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
        this.hits = tag.getInt("Hits");
        this.lifetime = tag.getInt("Lifetime");
        this.entityData.set(KIND, tag.getInt("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Hits", this.hits);
        tag.putInt("Lifetime", this.lifetime);
        tag.putInt("Kind", this.getKind());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
