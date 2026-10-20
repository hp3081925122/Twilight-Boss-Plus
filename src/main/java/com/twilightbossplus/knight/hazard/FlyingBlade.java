package com.twilightbossplus.knight.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FlyingBlade implements BossHazard {
    private final Vec3 origin;
    private final Vec3 dir;
    private final double speed;
    private final double range;
    private final double multiplier;
    private final Set<Integer> hit = new HashSet<>();
    private double travelled;

    public FlyingBlade(Vec3 origin, Vec3 dir, double speed, double range, double multiplier) {
        this.origin = origin;
        this.dir = dir;
        this.speed = speed;
        this.range = range;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        Vec3 from = this.origin.add(this.dir.scale(this.travelled));
        this.travelled = Math.min(this.range, this.travelled + this.speed);
        Vec3 to = this.origin.add(this.dir.scale(this.travelled));
        for (LivingEntity victim : brain.victimsOnSegment(from, to, 0.7)) {
            if (!this.hit.add(victim.getId())) {
                continue;
            }
            if (brain.damage(victim, this.multiplier)) {
                brain.knock(victim, this.dir.x, this.dir.z, 0.35, 0.15);
            }
        }
        return this.travelled < this.range;
    }
}
