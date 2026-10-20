package com.twilightbossplus.minoshroom.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SporeCloud implements BossHazard {
    public static final double RADIUS = 2.5;
    private static final int PULSE = 10;

    private final Vec3 center;
    private final int duration;
    private final double multiplier;
    private int age;

    public SporeCloud(Vec3 center, int duration, double multiplier) {
        this.center = center;
        this.duration = duration;
        this.multiplier = multiplier;
    }

    public void announce(BossBrain<?> brain) {
        MinoFx.sporeCloud(brain, this.center, RADIUS, this.duration);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age % PULSE == 0) {
            for (LivingEntity victim : brain.victimsAround(this.center, RADIUS, 2.5)) {
                brain.damage(victim, this.multiplier);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), brain.mob());
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), brain.mob());
            }
        }
        return this.age < this.duration;
    }
}
