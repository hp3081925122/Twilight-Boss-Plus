package com.twilightbossplus.entity;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.network.YetiFxPacket;
import com.twilightbossplus.registry.ModEntities;
import com.twilightbossplus.yeti.AlphaYetiControllerHolder;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import twilightforest.entity.boss.AlphaYeti;

public class YetiSnowball extends Entity {
    private static final EntityDataAccessor<Integer> MAX_TICKS = SynchedEntityData.defineId(YetiSnowball.class, EntityDataSerializers.INT);

    @Nullable
    private UUID ownerId;
    private double dirX;
    private double dirZ;
    private float damage;
    private int hitsLeft = 3;
    private boolean done;
    public float rollAngle;
    public float prevRollAngle;

    public YetiSnowball(EntityType<? extends YetiSnowball> type, Level level) {
        super(type, level);
    }

    public YetiSnowball(Level level, AlphaYeti owner, Vec3 start, double dirX, double dirZ, float damage) {
        this(ModEntities.YETI_SNOWBALL.get(), level);
        this.ownerId = owner.getUUID();
        this.dirX = dirX;
        this.dirZ = dirZ;
        this.damage = damage;
        this.hitsLeft = AlphaYetiConfig.BOULDER_HITS.get();
        this.entityData.set(MAX_TICKS, Math.max(1, AlphaYetiConfig.BOULDER_MAX_TICKS.get()));
        this.setPos(start.x, start.y, start.z);
        this.setYRot((float) (Mth.atan2(dirZ, dirX) * Mth.RAD_TO_DEG) - 90.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(MAX_TICKS, 80);
    }

    public float visualSize(float partialTick) {
        float t = Mth.clamp((this.tickCount + partialTick) / this.entityData.get(MAX_TICKS), 0.0F, 1.0F);
        return 1.8F + 0.8F * t;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.prevRollAngle = this.rollAngle;
            double dx = this.getX() - this.xo;
            double dz = this.getZ() - this.zo;
            this.rollAngle += (float) (Math.sqrt(dx * dx + dz * dz) / (this.visualSize(0.0F) * 0.5F) * Mth.RAD_TO_DEG);
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
        if (!(owner instanceof AlphaYeti yeti) || !yeti.isAlive()) {
            this.discard();
            return;
        }
        double speed = AlphaYetiConfig.BOULDER_SPEED.get();
        Vec3 motion = this.getDeltaMovement();
        double vy = (motion.y - 0.08D) * 0.98D;
        this.setDeltaMovement(this.dirX * speed, vy, this.dirZ * speed);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
        }
        if (this.horizontalCollision && this.tickCount > 2) {
            this.shatter(level, yeti, "wall");
            return;
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2D))) {
            if (entity instanceof AlphaYeti || !entity.isAlive() || (entity instanceof Player player && (player.isCreative() || player.isSpectator()))) {
                continue;
            }
            entity.hurt(this.damageSources().mobProjectile(this, yeti), this.damage);
            double knockback = Math.min(1.0D, AlphaYetiConfig.BOULDER_KNOCKBACK.get());
            if (knockback > 0.0D) {
                entity.knockback(knockback, -this.dirX, -this.dirZ);
            }
            this.shatter(level, yeti, "entity " + entity.getName().getString());
            return;
        }
        if (this.tickCount >= this.entityData.get(MAX_TICKS)) {
            this.shatter(level, yeti, "timeout");
        }
    }

    private void shatter(ServerLevel level, AlphaYeti yeti, String reason) {
        this.done = true;
        ModNetwork.sendTracking(this, new YetiFxPacket(YetiFxPacket.BOULDER_BREAK, -1, (float) this.getX(), (float) this.getY(), (float) this.getZ(), this.visualSize(0.0F)));
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SNOW_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 3.0F, 0.6F);
        ((AlphaYetiControllerHolder) yeti).twilightbossplus$getController().addSlush(this.getX(), this.getY() + 0.5D, this.getZ());
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[YetiSnowball {}] shattered by {}", this.getId(), reason);
        }
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || this.done || this.isRemoved() || !(source.getEntity() instanceof Player)) {
            return false;
        }
        this.hitsLeft--;
        this.playSound(SoundEvents.SNOW_HIT, 2.0F, 0.6F);
        if (this.hitsLeft <= 0) {
            Entity owner = null;
            if (this.ownerId != null) {
                owner = ((ServerLevel) this.level()).getEntity(this.ownerId);
            }
            if (owner instanceof AlphaYeti yeti) {
                this.shatter((ServerLevel) this.level(), yeti, "player hits");
            } else {
                this.discard();
            }
        }
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
        this.dirX = tag.getDouble("DirX");
        this.dirZ = tag.getDouble("DirZ");
        this.damage = tag.getFloat("Damage");
        this.hitsLeft = tag.getInt("HitsLeft");
        this.entityData.set(MAX_TICKS, Math.max(1, tag.getInt("MaxTicks")));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        tag.putDouble("DirX", this.dirX);
        tag.putDouble("DirZ", this.dirZ);
        tag.putFloat("Damage", this.damage);
        tag.putInt("HitsLeft", this.hitsLeft);
        tag.putInt("MaxTicks", this.entityData.get(MAX_TICKS));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
