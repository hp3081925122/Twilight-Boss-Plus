package com.twilightbossplus.entity;

import com.twilightbossplus.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;

public class VenomGlob extends ThrowableProjectile {
    public static final float GRAVITY = 0.03F;
    public static final float DRAG = 0.99F;
    private static final int MAX_AGE = 120;

    private float damage;

    public VenomGlob(EntityType<? extends VenomGlob> type, Level level) {
        super(type, level);
    }

    public VenomGlob(Level level, LivingEntity owner, float damage) {
        super(ModEntities.VENOM_GLOB.get(), owner, level);
        this.damage = damage;
    }

    public static Vec3 solveVelocity(Vec3 from, Vec3 to, int flightTicks) {
        double sum = (1.0D - Math.pow(DRAG, flightTicks)) / (1.0D - DRAG);
        double vx = (to.x - from.x) / sum;
        double vz = (to.z - from.z) / sum;
        double vy = (to.y - from.y + GRAVITY / (1.0D - DRAG) * (flightTicks - sum)) / sum;
        return new Vec3(vx, vy, vz);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected float getGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount > MAX_AGE) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        Entity owner = this.getOwner();
        if (owner != null) {
            if (entity == owner) {
                return false;
            }
            if (entity instanceof PartEntity<?> part && part.getParent() == owner) {
                return false;
            }
        }
        if (entity instanceof VenomGlob || entity instanceof VenomPool) {
            return false;
        }
        return super.canHitEntity(entity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) {
            return;
        }
        LivingEntity owner = null;
        if (this.getOwner() instanceof LivingEntity living) {
            owner = living;
        }
        result.getEntity().hurt(this.damageSources().mobProjectile(this, owner), this.damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        Vec3 hit = result.getLocation();
        double groundY = hit.y;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(hit.y + 0.5D);
        for (int y = startY; y >= startY - 6; y--) {
            cursor.set(Math.floor(hit.x), y - 1, Math.floor(hit.z));
            VoxelShape shape = this.level().getBlockState(cursor).getCollisionShape(this.level(), cursor);
            if (!shape.isEmpty()) {
                groundY = shape.max(Direction.Axis.Y) + cursor.getY();
                break;
            }
        }
        VenomPool pool = ModEntities.VENOM_POOL.get().create(this.level());
        if (pool != null) {
            pool.moveTo(hit.x, groundY, hit.z, 0.0F, 0.0F);
            this.level().addFreshEntity(pool);
        }
        this.level().playSound(null, hit.x, hit.y, hit.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2F, 0.6F + this.random.nextFloat() * 0.2F);
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
    }
}
