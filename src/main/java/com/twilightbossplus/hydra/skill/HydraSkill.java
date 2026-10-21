package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraHeads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Hydra;

public abstract class HydraSkill extends BossSkill<Hydra, HydraBrain> {
    protected final Hydra hydra;
    protected final List<Integer> heads = new ArrayList<>();

    protected HydraSkill(HydraBrain brain) {
        super(brain);
        this.hydra = brain.hydra();
    }

    @Override
    public void start() {
    }

    protected void command(String state, int duration) {
        this.heads.removeIf(index -> !this.brain.headAlive(index));
        for (int index : this.heads) {
            HydraHeads.command(this.brain.head(index), state, duration);
        }
    }

    protected void release(String state, int duration) {
        this.command(state, duration);
        for (int index : this.heads) {
            this.brain.release(index);
        }
    }

    protected void aimAt(int index, Vec3 point) {
        Vec3 from = this.brain.headPos(index);
        this.brain.aim(index, yaw(from, point), pitch(from, point));
    }

    protected void sound(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        this.sound(sound.value(), at, volume, pitch);
    }

    protected void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
        this.brain.level().playSound(null, at.x, at.y, at.z, sound, this.hydra.getSoundSource(), volume, pitch);
    }

    protected Vec3 center() {
        return this.brain.ground(this.hydra.position());
    }

    protected Vec3 burn(int index, double length, double radius, double multiplier, int fireSeconds) {
        Vec3 from = this.brain.mouth(index);
        Vec3 dir = Vec3.directionFromRotation(this.brain.aimPitch(index), this.brain.aimYaw(index));
        Vec3 to = from.add(dir.scale(length));
        HitResult hit = this.brain.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.hydra));
        if (hit.getType() != HitResult.Type.MISS) {
            to = hit.getLocation();
        }
        for (LivingEntity victim : this.brain.victimsOnSegment(from, to, radius)) {
            if (this.brain.damage(victim, multiplier)) {
                victim.setRemainingFireTicks(fireSeconds * 20);
                this.brain.knock(victim, dir.x, dir.z, 0.25, 0.1);
            }
        }
        return to;
    }

    protected static float yaw(Vec3 from, Vec3 to) {
        return (float) (Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90.0);
    }

    protected static float pitch(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) Math.toDegrees(Math.atan2(from.y - to.y, Math.sqrt(dx * dx + dz * dz)));
    }

    protected static Vec3 flat(float yaw) {
        return Vec3.directionFromRotation(0.0F, yaw);
    }

    protected Vec3 lead(LivingEntity target, double ticks) {
        Vec3 motion = target.getDeltaMovement();
        return target.position().add(motion.x * ticks, 0.0, motion.z * ticks);
    }
}
