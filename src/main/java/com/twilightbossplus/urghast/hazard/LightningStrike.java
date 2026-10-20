package com.twilightbossplus.urghast.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.urghast.UrGhastFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class LightningStrike implements BossHazard {
    private final Vec3 spot;
    private final Vec3 sky;
    private final double radius;
    private final double multiplier;
    private int delay;

    public LightningStrike(Vec3 spot, Vec3 sky, double radius, int delay, double multiplier) {
        this.spot = spot;
        this.sky = sky;
        this.radius = radius;
        this.delay = delay;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.delay--;
        if (this.delay > 0) {
            return true;
        }
        for (LivingEntity victim : brain.victimsAround(this.spot, this.radius, 3.0)) {
            if (brain.damage(victim, this.multiplier)) {
                brain.knockFrom(victim, this.spot, 0.35, 0.3);
            }
        }
        UrGhastFx.bolt(brain, this.spot, this.sky, this.radius);
        brain.shake(this.spot, 0.35F, 18.0F, 8);
        brain.level().playSound(null, this.spot.x, this.spot.y, this.spot.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 2.0F, 0.8F);
        brain.level().playSound(null, this.spot.x, this.spot.y, this.spot.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.2F, 1.3F);
        return false;
    }
}
