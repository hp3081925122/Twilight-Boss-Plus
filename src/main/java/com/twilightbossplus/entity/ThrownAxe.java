package com.twilightbossplus.entity;

import com.twilightbossplus.minoshroom.MinoshroomBossController;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.registry.ModEntities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.init.TFDamageTypes;

public class ThrownAxe extends Entity {
    public static final Set<Integer> CLIENT_HIDDEN_OWNERS = new HashSet<>();
    public static final int TRAIL_LENGTH = 10;
    private static final double SIDE_RADIUS = 2.5D;
    private static final double RETURN_SPEED = 0.9D;
    private static final double CATCH_RANGE = 1.2D;
    private static final double KNOCKBACK = 0.4D;
    private static final int MAX_AGE = 80;
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(ThrownAxe.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(ThrownAxe.class, EntityDataSerializers.ITEM_STACK);

    public final List<Vec3> trail = new ArrayList<>();

    @Nullable
    private UUID ownerId;
    private Vec3 start = Vec3.ZERO;
    private Vec3 apex = Vec3.ZERO;
    private double side = 1.0D;
    private int duration = 20;
    private int flightTicks;
    private boolean directReturn;
    private float damage;
    private final Set<Integer> hitOut = new HashSet<>();
    private final Set<Integer> hitBack = new HashSet<>();

    public ThrownAxe(EntityType<? extends ThrownAxe> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ThrownAxe(Level level, Minoshroom owner, Vec3 target, float damage) {
        this(ModEntities.THROWN_AXE.get(), level);
        this.ownerId = owner.getUUID();
        this.entityData.set(OWNER, owner.getId());
        this.entityData.set(ITEM, owner.getMainHandItem().copy());
        this.start = handPosition(owner);
        this.apex = target;
        if (owner.getRandom().nextBoolean()) {
            this.side = -1.0D;
        }
        double distance = this.start.distanceTo(target);
        this.duration = Mth.clamp((int) (distance * 2.4D), 16, 48);
        this.damage = damage;
        this.setPos(this.start.x, this.start.y, this.start.z);
    }

    public static Vec3 handPosition(LivingEntity owner) {
        Vec3 forward = NagaBossController.directionOf(owner.yBodyRot);
        Vec3 right = NagaBossController.directionOf(owner.yBodyRot + 90.0F);
        return owner.position().add(right.scale(0.7D)).add(forward.scale(0.3D)).add(0.0D, 1.6D, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, -1);
        this.entityData.define(ITEM, ItemStack.EMPTY);
    }

    public int getOwnerEntityId() {
        return this.entityData.get(OWNER);
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            CLIENT_HIDDEN_OWNERS.add(this.getOwnerEntityId());
            this.trail.add(0, this.position());
            while (this.trail.size() > TRAIL_LENGTH) {
                this.trail.remove(this.trail.size() - 1);
            }
            return;
        }
        Entity found = null;
        if (this.ownerId != null) {
            found = ((ServerLevel) this.level()).getEntity(this.ownerId);
        }
        if (!(found instanceof Minoshroom owner) || !owner.isAlive() || this.tickCount > MAX_AGE) {
            this.discard();
            return;
        }
        Vec3 hand = handPosition(owner);
        Vec3 previous = this.position();
        Vec3 next;
        this.flightTicks++;
        float progress = (float) this.flightTicks / this.duration;
        if (this.directReturn) {
            Vec3 toHand = hand.subtract(previous);
            if (toHand.length() <= CATCH_RANGE + RETURN_SPEED) {
                this.discard();
                return;
            }
            next = previous.add(toHand.normalize().scale(RETURN_SPEED));
        } else {
            if (progress >= 1.0F) {
                this.discard();
                return;
            }
            next = this.ellipsePoint(progress, hand);
            if (progress < 0.5F && this.blockedAt(next)) {
                this.directReturn = true;
                this.level().playSound(null, previous.x, previous.y, previous.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.5F, 1.6F);
                next = previous;
            }
        }
        this.setPos(next.x, next.y, next.z);
        Vec3 motion = next.subtract(previous);
        this.setDeltaMovement(motion);
        boolean outbound = progress < 0.5F && !this.directReturn;
        this.hitTargets(owner, motion, outbound);
        if (this.flightTicks % 5 == 0) {
            this.level().playSound(null, next.x, next.y, next.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.9F, 0.6F);
        }
    }

    private Vec3 ellipsePoint(float progress, Vec3 hand) {
        double angle = Math.PI * (1.0D - 2.0D * progress);
        Vec3 axis = this.apex.subtract(this.start);
        double halfLength = axis.length() * 0.5D;
        Vec3 dir = axis.normalize();
        Vec3 sideDir = new Vec3(-dir.z, 0.0D, dir.x).normalize().scale(this.side);
        Vec3 center = this.start.add(axis.scale(0.5D));
        Vec3 point = center.add(dir.scale(halfLength * Math.cos(angle))).add(sideDir.scale(SIDE_RADIUS * Math.sin(angle)));
        if (progress > 0.5F) {
            double weight = (progress - 0.5D) * 2.0D;
            point = point.add(hand.subtract(this.start).scale(weight));
        }
        return point;
    }

    private boolean blockedAt(Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        if (!this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty()) {
            return true;
        }
        AABB box = this.getBoundingBox().move(point.subtract(this.position()));
        return !this.level().getEntitiesOfClass(MushroomPillar.class, box).isEmpty();
    }

    private void hitTargets(Minoshroom owner, Vec3 motion, boolean outbound) {
        Set<Integer> hitSet = this.hitBack;
        if (outbound) {
            hitSet = this.hitOut;
        }
        AABB box = this.getBoundingBox().expandTowards(motion.scale(-1.0D)).inflate(0.3D);
        for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!MinoshroomBossController.canHit(owner, living) || hitSet.contains(living.getId())) {
                continue;
            }
            hitSet.add(living.getId());
            DamageSource source = TFDamageTypes.getIndirectEntityDamageSource(this.level(), TFDamageTypes.THROWN_AXE, this, owner);
            if (living.hurt(source, this.damage) && motion.lengthSqr() > 1.0E-4D) {
                Vec3 push = motion.normalize();
                living.knockback(KNOCKBACK, -push.x, -push.z);
            }
        }
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        CLIENT_HIDDEN_OWNERS.remove(this.getOwnerEntityId());
    }

    public boolean isOwnedBy(Minoshroom owner) {
        return owner.getUUID().equals(this.ownerId);
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
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
