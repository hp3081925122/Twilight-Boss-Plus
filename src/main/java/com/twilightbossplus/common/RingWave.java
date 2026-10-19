package com.twilightbossplus.common;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RingWave implements BossHazard {
    private static final double JUMP_CLEARANCE = 0.75;

    private final Vec3 center;
    private final double speed;
    private final double maxRadius;
    private final double thickness;
    private final double multiplier;
    private final Set<Integer> hit = new HashSet<>();
    private int age;

    public RingWave(Vec3 center, double speed, double maxRadius, double thickness, int delay, double multiplier) {
        this.center = center;
        this.speed = speed;
        this.maxRadius = maxRadius;
        this.thickness = thickness;
        this.multiplier = multiplier;
        this.age = -delay;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age <= 0) {
            return true;
        }
        double radius = this.age * this.speed;
        double inner = Math.max(0.0, radius - this.thickness * 0.5);
        double outer = radius + this.thickness * 0.5;
        for (LivingEntity victim : brain.victimsAround(this.center, outer, 4.0)) {
            double dx = victim.getX() - this.center.x;
            double dz = victim.getZ() - this.center.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < inner || this.hit.contains(victim.getId())) {
                continue;
            }
            double ground = brain.groundY(victim.getX(), victim.getZ(), victim.getY());
            if (victim.getY() - ground > JUMP_CLEARANCE) {
                continue;
            }
            this.hit.add(victim.getId());
            brain.damage(victim, this.multiplier);
            brain.knock(victim, dx, dz, 0.5, 0.35);
        }
        return radius < this.maxRadius;
    }
}
