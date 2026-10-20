package com.twilightbossplus.naga.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.naga.NagaFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class VenomGlob implements BossHazard {
    public static final double SPLASH_RADIUS = 2.6;

    private final Vec3 origin;
    private final Vec3 velocity;
    private final double gravity;
    private final int flight;
    private final Vec3 landing;
    private final double splashMultiplier;
    private final double poolMultiplier;
    private final int poolTicks;
    private int age;

    public VenomGlob(Vec3 origin, Vec3 velocity, double gravity, int flight, Vec3 landing, double splashMultiplier, double poolMultiplier, int poolTicks) {
        this.origin = origin;
        this.velocity = velocity;
        this.gravity = gravity;
        this.flight = flight;
        this.landing = landing;
        this.splashMultiplier = splashMultiplier;
        this.poolMultiplier = poolMultiplier;
        this.poolTicks = poolTicks;
    }

    public void announce(BossBrain<?> brain) {
        NagaFx.glob(brain, this.origin, this.velocity, this.gravity, this.flight, this.landing, SPLASH_RADIUS);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age < this.flight) {
            return true;
        }
        for (LivingEntity victim : brain.victimsAround(this.landing, SPLASH_RADIUS, 2.5)) {
            if (brain.damage(victim, this.splashMultiplier)) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), brain.mob());
            }
        }
        brain.level().playSound(null, this.landing.x, this.landing.y, this.landing.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.5F, 0.6F);
        VenomPool pool = new VenomPool(this.landing, SPLASH_RADIUS, this.poolTicks, this.poolMultiplier);
        brain.addHazard(pool);
        pool.announce(brain);
        return false;
    }
}
