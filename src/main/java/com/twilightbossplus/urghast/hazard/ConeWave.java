package com.twilightbossplus.urghast.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ConeWave implements BossHazard {
    private final Vec3 origin;
    private final Vec3 dir;
    private final double cosHalf;
    private final double speed;
    private final double max;
    private final double thickness;
    private final double multiplier;
    private final int slowness;
    private final Set<Integer> hit = new HashSet<>();
    private int age;

    public ConeWave(Vec3 origin, Vec3 dir, double half, double speed, double max, double thickness, int delay, double multiplier, int slowness) {
        this.origin = origin;
        this.dir = dir.normalize();
        this.cosHalf = Math.cos(half);
        this.speed = speed;
        this.max = max;
        this.thickness = thickness;
        this.multiplier = multiplier;
        this.slowness = slowness;
        this.age = -delay;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age <= 0) {
            return true;
        }
        double radius = this.age * this.speed;
        double inner = Math.max(0.0, radius - this.thickness);
        double outer = radius + this.thickness;
        for (LivingEntity victim : brain.victims(new AABB(this.origin, this.origin).inflate(outer))) {
            if (this.hit.contains(victim.getId())) {
                continue;
            }
            Vec3 to = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0).subtract(this.origin);
            double distance = to.length();
            if (distance < inner || distance > outer + victim.getBbWidth() || distance < 1.0E-3) {
                continue;
            }
            if (to.scale(1.0 / distance).dot(this.dir) < this.cosHalf) {
                continue;
            }
            this.hit.add(victim.getId());
            if (brain.damage(victim, this.multiplier)) {
                brain.knock(victim, this.dir.x, this.dir.z, 0.3, 0.1);
                if (this.slowness > 0) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.slowness, 1));
                }
            }
        }
        return radius < this.max;
    }
}
