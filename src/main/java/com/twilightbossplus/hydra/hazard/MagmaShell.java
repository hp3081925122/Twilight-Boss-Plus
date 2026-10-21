package com.twilightbossplus.hydra.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.hydra.HydraFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MagmaShell implements BossHazard {
    public static final double GRAVITY = 0.06;

    private final Vec3 origin;
    private final Vec3 velocity;
    private final int flight;
    private final Vec3 landing;
    private final double radius;
    private final double multiplier;
    private final double poolMultiplier;
    private final int poolTicks;
    private int age;

    public MagmaShell(Vec3 origin, Vec3 landing, int flight, double radius, double multiplier, double poolMultiplier, int poolTicks) {
        this.origin = origin;
        this.landing = landing;
        this.flight = Math.max(1, flight);
        this.radius = radius;
        this.multiplier = multiplier;
        this.poolMultiplier = poolMultiplier;
        this.poolTicks = poolTicks;
        double t = this.flight;
        this.velocity = new Vec3((landing.x - origin.x) / t, (landing.y - origin.y + 0.5 * GRAVITY * t * t) / t, (landing.z - origin.z) / t);
    }

    public void announce(BossBrain<?> brain) {
        HydraFx.shell(brain, this.origin, this.velocity, GRAVITY, this.flight, this.landing, this.radius);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age < this.flight) {
            return true;
        }
        for (LivingEntity victim : brain.victimsAround(this.landing, this.radius, 2.5)) {
            if (brain.damage(victim, this.multiplier)) {
                victim.setRemainingFireTicks(3 * 20);
                brain.knockFrom(victim, this.landing, 0.35, 0.25);
            }
        }
        brain.level().playSound(null, this.landing.x, this.landing.y, this.landing.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.4F);
        HydraFx.burst(brain, this.landing, this.radius);
        brain.shake(this.landing, 0.35F, 10.0F, 6);
        if (this.poolTicks > 0) {
            MagmaPool pool = new MagmaPool(this.landing, this.radius * 0.85, this.poolTicks, this.poolMultiplier);
            brain.addHazard(pool);
            pool.announce(brain);
        }
        return false;
    }
}
