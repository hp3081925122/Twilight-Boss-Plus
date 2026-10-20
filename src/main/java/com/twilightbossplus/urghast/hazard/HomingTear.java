package com.twilightbossplus.urghast.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.urghast.UrGhastFx;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class HomingTear implements BossHazard {
    public static final double RADIUS = 1.8;

    private final int id;
    private final double speed;
    private final double turn;
    private final int life;
    private final double multiplier;
    @Nullable
    private final LivingEntity target;
    private Vec3 pos;
    private Vec3 dir;
    private int age;

    public HomingTear(int id, Vec3 pos, Vec3 dir, double speed, double turn, int life, @Nullable LivingEntity target, double multiplier) {
        this.id = id;
        this.pos = pos;
        this.dir = dir.normalize();
        this.speed = speed;
        this.turn = turn;
        this.life = life;
        this.target = target;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.target != null && this.target.isAlive()) {
            Vec3 want = this.target.position().add(0.0, this.target.getBbHeight() * 0.5, 0.0).subtract(this.pos);
            if (want.lengthSqr() > 1.0E-6) {
                Vec3 steered = this.dir.lerp(want.normalize(), this.turn);
                if (steered.lengthSqr() > 1.0E-6) {
                    this.dir = steered.normalize();
                }
            }
        }
        Vec3 next = this.pos.add(this.dir.scale(this.speed));
        HitResult block = brain.level().clip(new ClipContext(this.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, brain.mob()));
        if (block.getType() != HitResult.Type.MISS) {
            this.burst(brain, block.getLocation());
            return false;
        }
        if (!brain.victimsOnSegment(this.pos, next, 0.6).isEmpty()) {
            this.burst(brain, next);
            return false;
        }
        this.pos = next;
        if (this.age >= this.life) {
            this.burst(brain, this.pos);
            return false;
        }
        return true;
    }

    private void burst(BossBrain<?> brain, Vec3 at) {
        for (LivingEntity victim : brain.victimsAround(at, RADIUS, RADIUS + 1.0)) {
            if (brain.damage(victim, this.multiplier)) {
                brain.knockFrom(victim, at, 0.3, 0.2);
            }
        }
        UrGhastFx.tearBurst(brain, this.id, at, RADIUS);
        brain.level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.4F, 0.7F);
    }
}
