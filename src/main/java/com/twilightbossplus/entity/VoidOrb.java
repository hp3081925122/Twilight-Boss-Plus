package com.twilightbossplus.entity;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.network.LichFxPacket;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.registry.ModEntities;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.monster.LichMinion;
import twilightforest.entity.projectile.LichBolt;

public class VoidOrb extends Entity {
    public static final int WINDUP = 15;
    private static final float MAX_TURN = 4.0F * Mth.DEG_TO_RAD;

    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID targetId;
    private float damage;
    private int lifetime;

    public VoidOrb(EntityType<? extends VoidOrb> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public VoidOrb(Level level, Lich owner, LivingEntity target, float damage) {
        this(ModEntities.VOID_ORB.get(), level);
        this.ownerId = owner.getUUID();
        this.targetId = target.getUUID();
        this.damage = damage;
        this.lifetime = LichConfig.VOID_ORB_LIFETIME.get();
        Vec3 hand = handPosition(owner);
        this.setPos(hand.x, hand.y, hand.z);
    }

    private static Vec3 handPosition(LivingEntity owner) {
        float yaw = owner.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(owner.getX() + Mth.cos(yaw) * 0.65D - Mth.sin(yaw) * 0.4D, owner.getY() + owner.getBbHeight() + 0.35D, owner.getZ() + Mth.sin(yaw) * 0.65D + Mth.cos(yaw) * 0.4D);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        super.tick();
        if (this.level().isClientSide()) {
            this.setPos(this.position().add(this.getDeltaMovement()));
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Entity owner = null;
        if (this.ownerId != null) {
            owner = level.getEntity(this.ownerId);
        }
        Entity target = null;
        if (this.targetId != null) {
            target = level.getEntity(this.targetId);
        }
        if (!(owner instanceof Lich lich) || !lich.isAlive() || !(target instanceof LivingEntity living) || !living.isAlive()) {
            this.pop(false);
            return;
        }
        if (this.tickCount < WINDUP) {
            Vec3 hand = handPosition(lich);
            this.setDeltaMovement(Vec3.ZERO);
            this.setPos(hand.x, hand.y, hand.z);
            return;
        }
        double speed = LichConfig.VOID_ORB_SPEED.get();
        Vec3 desired = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D).subtract(this.position()).normalize();
        Vec3 current = this.getDeltaMovement();
        if (this.tickCount == WINDUP || current.lengthSqr() < 1.0E-6D) {
            current = desired;
            this.playSound(SoundEvents.ENDER_EYE_LAUNCH, 1.5F, 0.5F);
        } else {
            current = current.normalize();
            double dot = Mth.clamp(current.dot(desired), -1.0D, 1.0D);
            double angle = Math.acos(dot);
            if (angle > MAX_TURN && angle > 1.0E-4D) {
                double t = MAX_TURN / angle;
                double sin = Math.sin(angle);
                current = current.scale(Math.sin((1.0D - t) * angle) / sin).add(desired.scale(Math.sin(t * angle) / sin)).normalize();
            } else {
                current = desired;
            }
        }
        Vec3 motion = current.scale(speed);
        this.setDeltaMovement(motion);
        AABB next = this.getBoundingBox().move(motion);
        if (!level.noCollision(this, next.deflate(0.1D))) {
            this.pop(true);
            return;
        }
        this.setPos(this.position().add(motion));

        for (LichBolt bolt : level.getEntitiesOfClass(LichBolt.class, this.getBoundingBox().inflate(0.4D))) {
            if (!(bolt.getOwner() instanceof Lich)) {
                bolt.discard();
                this.pop(true);
                return;
            }
        }
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.1D))) {
            if (hit instanceof Lich || hit instanceof LichMinion || !hit.isAlive()) {
                continue;
            }
            hit.hurt(this.damageSources().indirectMagic(this, lich), this.damage);
            this.pop(true);
            return;
        }
        if (this.tickCount > WINDUP + this.lifetime) {
            this.pop(true);
        }
    }

    private void pop(boolean visible) {
        if (visible) {
            ModNetwork.sendTracking(this, new LichFxPacket(LichFxPacket.ORB_POP, this.getId(), (float) this.getX(), (float) this.getY(), (float) this.getZ()));
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
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
        if (tag.hasUUID("Target")) {
            this.targetId = tag.getUUID("Target");
        }
        this.damage = tag.getFloat("Damage");
        this.lifetime = tag.getInt("Lifetime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        if (this.targetId != null) {
            tag.putUUID("Target", this.targetId);
        }
        tag.putFloat("Damage", this.damage);
        tag.putInt("Lifetime", this.lifetime);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
