package com.twilightbossplus.yeti.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.yeti.YetiBrain;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class IceShard implements BossHazard {
    private final Vec3 dir;
    private final double speed;
    private final double range;
    private final double multiplier;
    private Vec3 pos;
    private double travelled;

    public IceShard(Vec3 start, Vec3 dir, double speed, double range, double multiplier) {
        this.pos = start;
        this.dir = dir.normalize();
        this.speed = speed;
        this.range = range;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        Vec3 next = this.pos.add(this.dir.scale(this.speed));
        double y = brain.groundY(next.x, next.z, this.pos.y);
        if (y > this.pos.y + 1.1) {
            return false;
        }
        this.pos = new Vec3(next.x, y, next.z);
        this.travelled += this.speed;
        for (LivingEntity victim : brain.victimsAround(this.pos, 0.9, 1.5)) {
            if (brain instanceof YetiBrain yeti) {
                yeti.frost(victim, this.multiplier, this.pos.subtract(this.dir), 0.3, 0.2);
            }
            return false;
        }
        return this.travelled < this.range;
    }
}
