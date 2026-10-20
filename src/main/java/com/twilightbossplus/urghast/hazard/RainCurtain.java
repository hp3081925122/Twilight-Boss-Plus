package com.twilightbossplus.urghast.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RainCurtain implements BossHazard {
    public static final double HEIGHT = 6.0;
    private static final double THICKNESS = 0.7;
    private static final int HIT_COOLDOWN = 10;

    private final Vec3 start;
    private final Vec3 motion;
    private final Vec3 side;
    private final double length;
    private final double gap;
    private final double gapWidth;
    private final double speed;
    private final int life;
    private final double multiplier;
    private final Map<Integer, Integer> hits = new HashMap<>();
    private int age;

    public RainCurtain(Vec3 start, Vec3 motion, double length, double gap, double gapWidth, double speed, int life, double multiplier) {
        this.start = start;
        this.motion = new Vec3(motion.x, 0.0, motion.z).normalize();
        this.side = new Vec3(-this.motion.z, 0.0, this.motion.x);
        this.length = length;
        this.gap = gap;
        this.gapWidth = gapWidth;
        this.speed = speed;
        this.life = life;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        Vec3 center = this.start.add(this.motion.scale(this.age * this.speed));
        double half = this.length * 0.5;
        AABB box = new AABB(center.subtract(half, 3.0, half), center.add(half, HEIGHT, half));
        for (LivingEntity victim : brain.victims(box)) {
            Vec3 rel = victim.position().subtract(center);
            double across = rel.dot(this.motion);
            double along = rel.dot(this.side);
            if (Math.abs(across) > THICKNESS + victim.getBbWidth() * 0.5 || Math.abs(along) > half) {
                continue;
            }
            if (Math.abs(along - this.gap) < this.gapWidth * 0.5) {
                continue;
            }
            if (rel.y < -3.0 || rel.y > HEIGHT) {
                continue;
            }
            Integer last = this.hits.get(victim.getId());
            if (last != null && this.age - last < HIT_COOLDOWN) {
                continue;
            }
            this.hits.put(victim.getId(), this.age);
            if (brain.damage(victim, this.multiplier)) {
                brain.knock(victim, this.motion.x, this.motion.z, 0.15, 0.05);
            }
        }
        return this.age < this.life;
    }
}
