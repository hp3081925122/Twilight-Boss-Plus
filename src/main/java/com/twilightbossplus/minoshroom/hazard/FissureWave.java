package com.twilightbossplus.minoshroom.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.minoshroom.MinoFx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FissureWave implements BossHazard {
    private static final double RADIUS = 1.4;
    private static final double CLEARANCE = 0.9;

    private final Vec3 origin;
    private final Vec3 direction;
    private final double step;
    private final int interval;
    private final int steps;
    private final double multiplier;
    private final Set<Integer> hit = new HashSet<>();
    private int age;
    private int done;

    public FissureWave(Vec3 origin, Vec3 direction, double step, int interval, int steps, double multiplier) {
        this.origin = origin;
        this.direction = direction;
        this.step = step;
        this.interval = interval;
        this.steps = steps;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age % this.interval != 0) {
            return true;
        }
        this.done++;
        Vec3 point = brain.ground(this.origin.add(this.direction.scale(this.step * this.done)));
        for (LivingEntity victim : brain.victimsAround(point, RADIUS, 3.0)) {
            if (this.hit.contains(victim.getId())) {
                continue;
            }
            if (victim.getY() - brain.groundY(victim.getX(), victim.getZ(), victim.getY()) > CLEARANCE) {
                continue;
            }
            this.hit.add(victim.getId());
            if (brain.damage(victim, this.multiplier)) {
                brain.knock(victim, this.direction.x, this.direction.z, 0.25, 0.6);
            }
        }
        MinoFx.pop(brain, point, 1.1);
        if (this.done % 2 == 0) {
            brain.level().playSound(null, point.x, point.y, point.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.6F, 1.4F);
        }
        return this.done < this.steps;
    }
}
