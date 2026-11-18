package com.twilightbossplus.common;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.network.BossNetwork;
import com.twilightbossplus.network.VfxPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BossBrain<E extends Mob> {
    protected final E mob;
    private final List<BossHazard> hazards = new ArrayList<>();
    private final List<BossHazard> pendingHazards = new ArrayList<>();
    private boolean tickingHazards;

    protected BossBrain(E mob) {
        this.mob = mob;
    }

    public E mob() {
        return this.mob;
    }

    public ServerLevel level() {
        return (ServerLevel) this.mob.level();
    }

    protected abstract double skillKnockback();

    protected abstract boolean isAlly(LivingEntity entity);

    protected void tickHazards() {
        this.tickingHazards = true;
        this.hazards.removeIf(hazard -> !hazard.tick(this));
        this.tickingHazards = false;
        if (!this.pendingHazards.isEmpty()) {
            TwilightBossPlus.debug("Boss {} queued {} hazards spawned during hazard tick", this.mob.getId(), this.pendingHazards.size());
            this.hazards.addAll(this.pendingHazards);
            this.pendingHazards.clear();
        }
    }

    public void addHazard(BossHazard hazard) {
        if (this.tickingHazards) {
            this.pendingHazards.add(hazard);
            return;
        }
        this.hazards.add(hazard);
    }

    protected void clearHazards() {
        this.hazards.clear();
        this.pendingHazards.clear();
    }

    protected void applyModifier(Attribute attribute, UUID id, String name, double amount, boolean keepRatio) {
        AttributeInstance instance = this.mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.getAmount() - amount) < 1.0E-6) {
            return;
        }
        float ratio = this.healthRatio();
        instance.removeModifier(id);
        if (Math.abs(amount) > 1.0E-6) {
            instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (keepRatio) {
            this.mob.setHealth(this.mob.getMaxHealth() * ratio);
        }
    }

    protected void applyTransient(Attribute attribute, UUID id, String name, double amount) {
        AttributeInstance instance = this.mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount > 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    public float healthRatio() {
        return this.mob.getHealth() / Math.max(1.0F, this.mob.getMaxHealth());
    }

    @Nullable
    public LivingEntity target() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return null;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return null;
        }
        return target;
    }

    public double attack() {
        return this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public boolean damage(LivingEntity victim, double multiplier) {
        if (multiplier <= 0.0) {
            return false;
        }
        return victim.hurt(this.mob.damageSources().mobAttack(this.mob), (float) (this.attack() * multiplier));
    }

    public double knockScale(LivingEntity victim) {
        return this.skillKnockback() * Math.max(0.0, 1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
    }

    public void knock(LivingEntity victim, double dirX, double dirZ, double horizontal, double vertical) {
        double scale = this.knockScale(victim);
        if (scale <= 0.0) {
            return;
        }
        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length < 1.0E-4) {
            float angle = this.mob.getRandom().nextFloat() * Mth.TWO_PI;
            dirX = Mth.cos(angle);
            dirZ = Mth.sin(angle);
            length = 1.0;
        }
        victim.push(dirX / length * horizontal * scale, vertical * scale, dirZ / length * horizontal * scale);
        victim.hurtMarked = true;
    }

    public void knockFrom(LivingEntity victim, Vec3 from, double horizontal, double vertical) {
        this.knock(victim, victim.getX() - from.x, victim.getZ() - from.z, horizontal, vertical);
    }

    public List<LivingEntity> victims(AABB box) {
        return this.level().getEntitiesOfClass(LivingEntity.class, box, this::isVictim);
    }

    public List<LivingEntity> victimsAround(Vec3 center, double radius, double height) {
        AABB box = new AABB(center.x - radius, center.y - height, center.z - radius, center.x + radius, center.y + height, center.z + radius);
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : this.victims(box)) {
            double dx = entity.getX() - center.x;
            double dz = entity.getZ() - center.z;
            if (dx * dx + dz * dz <= radius * radius) {
                result.add(entity);
            }
        }
        return result;
    }

    public List<LivingEntity> victimsOnSegment(Vec3 a, Vec3 b, double radius) {
        AABB box = new AABB(a, b).inflate(radius + 1.0);
        Vec3 dir = b.subtract(a);
        double lengthSqr = dir.lengthSqr();
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : this.victims(box)) {
            Vec3 point = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
            double t = 0.0;
            if (lengthSqr > 1.0E-6) {
                t = Mth.clamp(point.subtract(a).dot(dir) / lengthSqr, 0.0, 1.0);
            }
            Vec3 closest = a.add(dir.scale(t));
            double reach = radius + entity.getBbWidth() * 0.5;
            if (closest.distanceToSqr(point) <= reach * reach + entity.getBbHeight() * entity.getBbHeight() * 0.25) {
                result.add(entity);
            }
        }
        return result;
    }

    private boolean isVictim(LivingEntity entity) {
        if (entity == this.mob || !entity.isAlive() || this.isAlly(entity)) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    public double groundY(double x, double z, double reference) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = Mth.floor(reference) + 2;
        for (int y = top; y > top - 12; y--) {
            pos.set(Mth.floor(x), y, Mth.floor(z));
            BlockState state = this.level().getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(this.level(), pos);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        return reference;
    }

    public Vec3 ground(Vec3 pos) {
        return new Vec3(pos.x, this.groundY(pos.x, pos.z, pos.y), pos.z);
    }

    public void send(int type, CompoundTag data) {
        BossNetwork.sendAround(this.mob, new VfxPacket(type, data));
    }

    public static void putVec(CompoundTag tag, String key, Vec3 vec) {
        tag.putDouble(key + "x", vec.x);
        tag.putDouble(key + "y", vec.y);
        tag.putDouble(key + "z", vec.z);
    }

    public void shake(Vec3 center, float strength, float radius, int duration) {
        CompoundTag data = new CompoundTag();
        putVec(data, "c", center);
        data.putFloat("s", strength);
        data.putFloat("r", radius);
        data.putInt("d", duration);
        this.send(VfxPacket.SHAKE, data);
    }
}
