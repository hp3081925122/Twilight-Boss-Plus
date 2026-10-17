package com.twilightbossplus.entity;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.network.UrGhastFxPacket;
import com.twilightbossplus.registry.ModEntities;
import com.twilightbossplus.urghast.UrGhastControllerHolder;
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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

public class PrimalTear extends Entity {
    private static final EntityDataAccessor<Vector3f> START = SynchedEntityData.defineId(PrimalTear.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> END = SynchedEntityData.defineId(PrimalTear.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> FALL_TICKS = SynchedEntityData.defineId(PrimalTear.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerId;
    private float damage;
    private float popDamage;
    private boolean done;

    public PrimalTear(EntityType<? extends PrimalTear> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public PrimalTear(Level level, UrGhast owner, Vec3 start, Vec3 end, int fallTicks, float damage, float popDamage) {
        this(ModEntities.PRIMAL_TEAR.get(), level);
        this.ownerId = owner.getUUID();
        this.damage = damage;
        this.popDamage = popDamage;
        this.entityData.set(START, new Vector3f((float) start.x, (float) start.y, (float) start.z));
        this.entityData.set(END, new Vector3f((float) end.x, (float) end.y, (float) end.z));
        this.entityData.set(FALL_TICKS, Math.max(1, fallTicks));
        this.setPos(start.x, start.y, start.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(START, new Vector3f());
        this.entityData.define(END, new Vector3f());
        this.entityData.define(FALL_TICKS, 50);
    }

    public Vec3 getStart() {
        Vector3f value = this.entityData.get(START);
        return new Vec3(value.x(), value.y(), value.z());
    }

    public Vec3 getEnd() {
        Vector3f value = this.entityData.get(END);
        return new Vec3(value.x(), value.y(), value.z());
    }

    public int getFallTicks() {
        return this.entityData.get(FALL_TICKS);
    }

    public float progress(float partialTick) {
        return Mth.clamp((this.tickCount + partialTick) / this.getFallTicks(), 0.0F, 1.0F);
    }

    public Vec3 positionAt(float progress) {
        Vec3 start = this.getStart();
        Vec3 end = this.getEnd();
        double fall = progress * progress;
        return new Vec3(Mth.lerp(progress, start.x, end.x), Mth.lerp(fall, start.y, end.y), Mth.lerp(progress, start.z, end.z));
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        super.tick();
        Vec3 next = this.positionAt(this.progress(0.0F));
        if (this.level().isClientSide()) {
            this.setPos(next.x, next.y, next.z);
            return;
        }
        if (this.done) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        Entity owner = null;
        if (this.ownerId != null) {
            owner = level.getEntity(this.ownerId);
        }
        if (!(owner instanceof UrGhast ghast) || !ghast.isAlive()) {
            this.discard();
            return;
        }
        AABB box = this.getBoundingBox().move(next.subtract(this.position()));
        boolean blocked = this.tickCount > 4 && !level.noCollision(this, box.deflate(0.3D));
        this.setPos(next.x, next.y, next.z);
        if (blocked || this.tickCount >= this.getFallTicks()) {
            this.impact(level, ghast);
        }
    }

    private void impact(ServerLevel level, UrGhast ghast) {
        this.done = true;
        double radius = UrGhastConfig.TEAR_RADIUS.get();
        double knockback = Math.min(1.0D, UrGhastConfig.TEAR_KNOCKBACK.get());
        AABB area = new AABB(this.getX() - radius, this.getY() - 2.0D, this.getZ() - radius, this.getX() + radius, this.getY() + radius, this.getZ() + radius);
        int hits = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity instanceof CarminiteGhastguard || !entity.isAlive()) {
                continue;
            }
            double dx = entity.getX() - this.getX();
            double dz = entity.getZ() - this.getZ();
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (entity.hurt(this.damageSources().mobProjectile(this, ghast), this.damage)) {
                hits++;
            }
            if (knockback > 0.0D && dx * dx + dz * dz > 1.0E-4D) {
                entity.knockback(knockback, -dx, -dz);
            }
        }
        ModNetwork.sendTracking(this, new UrGhastFxPacket(UrGhastFxPacket.SPLASH, -1, (float) this.getX(), (float) this.getY(), (float) this.getZ(), (float) radius, 0.0F, -1.0F));
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 3.0F, 0.5F);
        ((UrGhastControllerHolder) ghast).twilightbossplus$getController().addPool(this.getX(), this.getY(), this.getZ());
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[UrGhast {}] primal tear landed at {} {} {}, hit {} entities", ghast.getId(), this.getX(), this.getY(), this.getZ(), hits);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.done || this.isRemoved() || !(source.getEntity() instanceof Player player)) {
            return false;
        }
        this.done = true;
        Entity owner = null;
        if (this.ownerId != null) {
            owner = ((ServerLevel) this.level()).getEntity(this.ownerId);
        }
        int ownerId = -1;
        if (owner instanceof UrGhast ghast && ghast.isAlive()) {
            ownerId = ghast.getId();
            ghast.hurt(this.damageSources().indirectMagic(this, player), this.popDamage);
        }
        ModNetwork.sendTracking(this, new UrGhastFxPacket(UrGhastFxPacket.SPLASH, -1, (float) this.getX(), (float) this.getY(), (float) this.getZ(), 1.5F, 1.0F, ownerId));
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.6F);
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[PrimalTear {}] shot down by {}, boss takes {}", this.getId(), player.getName().getString(), this.popDamage);
        }
        this.discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.done;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerId = tag.getUUID("Owner");
        }
        this.damage = tag.getFloat("Damage");
        this.popDamage = tag.getFloat("PopDamage");
        this.entityData.set(START, new Vector3f(tag.getFloat("StartX"), tag.getFloat("StartY"), tag.getFloat("StartZ")));
        this.entityData.set(END, new Vector3f(tag.getFloat("EndX"), tag.getFloat("EndY"), tag.getFloat("EndZ")));
        this.entityData.set(FALL_TICKS, Math.max(1, tag.getInt("FallTicks")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putFloat("Damage", this.damage);
        tag.putFloat("PopDamage", this.popDamage);
        Vector3f start = this.entityData.get(START);
        Vector3f end = this.entityData.get(END);
        tag.putFloat("StartX", start.x());
        tag.putFloat("StartY", start.y());
        tag.putFloat("StartZ", start.z());
        tag.putFloat("EndX", end.x());
        tag.putFloat("EndY", end.y());
        tag.putFloat("EndZ", end.z());
        tag.putInt("FallTicks", this.getFallTicks());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
