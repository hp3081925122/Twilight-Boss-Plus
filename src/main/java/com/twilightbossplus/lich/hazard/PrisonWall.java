package com.twilightbossplus.lich.hazard;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PrisonWall implements BossHazard {
    private static final double EDGE = 0.8;
    private static final double HEIGHT = 5.0;
    private static final int HIT_COOLDOWN = 10;

    private final Vec3 center;
    private final double radius;
    private final int warmup;
    private final int duration;
    private final double multiplier;
    private final Set<Integer> prisoners = new HashSet<>();
    private final Map<Integer, Integer> lastHit = new HashMap<>();
    private int age;

    public PrisonWall(Vec3 center, double radius, int warmup, int duration, double multiplier) {
        this.center = center;
        this.radius = radius;
        this.warmup = warmup;
        this.duration = duration;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age < this.warmup) {
            return true;
        }
        if (this.age == this.warmup) {
            for (LivingEntity victim : brain.victimsAround(this.center, this.radius, HEIGHT)) {
                this.prisoners.add(victim.getId());
            }
            TwilightBossPlus.debug("Soul Prison closed with {} prisoners", this.prisoners.size());
        }
        for (LivingEntity victim : brain.victimsAround(this.center, this.radius + EDGE + 1.0, HEIGHT)) {
            if (victim.getY() < this.center.y - 1.5 || victim.getY() > this.center.y + HEIGHT) {
                continue;
            }
            double dx = victim.getX() - this.center.x;
            double dz = victim.getZ() - this.center.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            boolean inside = this.prisoners.contains(victim.getId());
            boolean touching;
            if (inside) {
                touching = distance > this.radius - EDGE;
            } else {
                touching = distance < this.radius + EDGE;
            }
            if (!touching) {
                continue;
            }
            Integer last = this.lastHit.get(victim.getId());
            if (last != null && this.age - last < HIT_COOLDOWN) {
                continue;
            }
            this.lastHit.put(victim.getId(), this.age);
            brain.damage(victim, this.multiplier);
            if (inside) {
                brain.knock(victim, -dx, -dz, 0.7, 0.15);
            } else {
                brain.knock(victim, dx, dz, 0.7, 0.15);
            }
            brain.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 1.5F, 0.6F);
        }
        return this.age < this.warmup + this.duration;
    }
}
