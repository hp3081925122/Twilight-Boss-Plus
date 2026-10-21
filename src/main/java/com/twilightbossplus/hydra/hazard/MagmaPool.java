package com.twilightbossplus.hydra.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.hydra.HydraFx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MagmaPool implements BossHazard {
    private static final int PULSE = 10;

    private final Vec3 center;
    private final double radius;
    private final int duration;
    private final double multiplier;
    private int age;

    public MagmaPool(Vec3 center, double radius, int duration, double multiplier) {
        this.center = center;
        this.radius = radius;
        this.duration = duration;
        this.multiplier = multiplier;
    }

    public void announce(BossBrain<?> brain) {
        HydraFx.pool(brain, this.center, this.radius, this.duration);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age % PULSE == 0) {
            for (LivingEntity victim : brain.victimsAround(this.center, this.radius, 1.2)) {
                if (brain.damage(victim, this.multiplier)) {
                    victim.setRemainingFireTicks(2 * 20);
                }
            }
        }
        return this.age < this.duration;
    }
}
