package com.twilightbossplus.entity;

import com.twilightbossplus.config.MinoshroomConfig;
import com.twilightbossplus.minoshroom.MinoshroomBossController;
import com.twilightbossplus.network.MinoFxPacket;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import twilightforest.entity.boss.Minoshroom;

public class Puffball extends Entity {
    public static final double GRAVITY = 0.04D;
    private static final int MAX_AGE = 200;
    private static final EntityDataAccessor<Integer> LANDED_AT = SynchedEntityData.defineId(Puffball.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(Puffball.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(Puffball.class, EntityDataSerializers.FLOAT);

    @Nullable
    private UUID ownerId;
    private float damage;
    private int fuseLeft = -1;

    public Puffball(EntityType<? extends Puffball> type, Level level) {
        super(type, level);
    }

    public Puffball(Level level, Minoshroom owner, Vec3 from, Vec3 velocity, float damage, int fuse) {
        this(ModEntities.PUFFBALL.get(), level);
        this.setPos(from.x, from.y, from.z);
        this.setDeltaMovement(velocity);
        this.ownerId = owner.getUUID();
        this.damage = damage;
        this.entityData.set(FUSE, fuse);
        this.entityData.set(RADIUS, MinoshroomConfig.PUFFBALL_RADIUS.get().floatValue());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LANDED_AT, -1);
        this.entityData.define(FUSE, 30);
        this.entityData.define(RADIUS, 2.5F);
    }

    public int getLandedAt() {
        return this.entityData.get(LANDED_AT);
    }

    public int getFuse() {
        return this.entityData.get(FUSE);
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getLandedAt() < 0) {
            Vec3 velocity = this.getDeltaMovement();
            this.move(MoverType.SELF, velocity);
            this.setDeltaMovement(velocity.x, velocity.y - GRAVITY, velocity.z);
            if (!this.level().isClientSide() && (this.onGround() || this.horizontalCollision)) {
                this.setDeltaMovement(Vec3.ZERO);
                this.entityData.set(LANDED_AT, this.tickCount);
                this.fuseLeft = this.getFuse();
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FUNGUS_PLACE, SoundSource.HOSTILE, 1.2F, 0.6F);
            }
        }
        if (this.level().isClientSide()) {
            return;
        }
        if (this.fuseLeft > 0) {
            this.fuseLeft--;
            if (this.fuseLeft == 0) {
                this.burst(true);
                return;
            }
        }
        if (this.tickCount > MAX_AGE) {
            this.burst(true);
        }
    }

    private void burst(boolean big) {
        if (this.isRemoved()) {
            return;
        }
        double radius = this.getRadius();
        Vec3 center = this.position().add(0.0D, 0.35D, 0.0D);
        if (big) {
            Entity owner = null;
            if (this.ownerId != null) {
                owner = ((ServerLevel) this.level()).getEntity(this.ownerId);
            }
            LivingEntity attacker = null;
            if (owner instanceof LivingEntity living) {
                attacker = living;
            }
            int slowness = MinoshroomConfig.PUFFBALL_SLOWNESS_TICKS.get();
            for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius + 1.0D))) {
                if (attacker != null && !MinoshroomBossController.canHit(attacker, living)) {
                    continue;
                }
                if (attacker == null && living instanceof Minoshroom) {
                    continue;
                }
                if (living.getBoundingBox().distanceToSqr(center) > radius * radius) {
                    continue;
                }
                if (living.hurt(this.damageSources().mobProjectile(this, attacker), this.damage) && slowness > 0) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowness, 0), this);
                }
            }
            ModNetwork.sendTracking(this, new MinoFxPacket(MinoFxPacket.SPORE_BURST, this.getId(), (float) center.x, (float) center.y, (float) center.z, (float) radius, 1.0F));
            this.level().playSound(null, center.x, center.y, center.z, SoundEvents.FUNGUS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
            this.level().playSound(null, center.x, center.y, center.z, SoundEvents.PUFFER_FISH_BLOW_UP, SoundSource.HOSTILE, 1.6F, 0.6F);
        } else {
            ModNetwork.sendTracking(this, new MinoFxPacket(MinoFxPacket.SPORE_BURST, this.getId(), (float) center.x, (float) center.y, (float) center.z, (float) radius * 0.4F, 0.0F));
            this.level().playSound(null, center.x, center.y, center.z, SoundEvents.FUNGUS_HIT, SoundSource.HOSTILE, 1.4F, 1.2F);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.isRemoved()) {
            return false;
        }
        if (source.getEntity() instanceof Minoshroom) {
            return false;
        }
        this.burst(false);
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
        this.damage = tag.getFloat("Damage");
        this.fuseLeft = tag.getInt("FuseLeft");
        this.entityData.set(LANDED_AT, tag.getInt("LandedAt"));
        this.entityData.set(FUSE, tag.getInt("Fuse"));
        if (tag.contains("Radius")) {
            this.entityData.set(RADIUS, tag.getFloat("Radius"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putFloat("Damage", this.damage);
        tag.putInt("FuseLeft", this.fuseLeft);
        tag.putInt("LandedAt", this.getLandedAt());
        tag.putInt("Fuse", this.getFuse());
        tag.putFloat("Radius", this.getRadius());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
