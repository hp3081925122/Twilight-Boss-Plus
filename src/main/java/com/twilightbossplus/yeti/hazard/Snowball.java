package com.twilightbossplus.yeti.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiFx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Snowball implements BossHazard {
    private final int id;
    private final Vec3 dir;
    private final double speed;
    private final double from;
    private final double to;
    private final int life;
    private final double multiplier;
    private final Set<Integer> hit = new HashSet<>();
    private Vec3 pos;
    private int age;

    public Snowball(int id, Vec3 start, Vec3 dir, double speed, double from, double to, int life, double multiplier) {
        this.id = id;
        this.pos = start;
        this.dir = new Vec3(dir.x, 0.0, dir.z).normalize();
        this.speed = speed;
        this.from = from;
        this.to = to;
        this.life = life;
        this.multiplier = multiplier;
    }

    private double radius() {
        return Mth.lerp(Math.min(1.0, this.age / (double) this.life), this.from, this.to);
    }

    private double growth() {
        return Mth.clamp((this.radius() - this.from) / Math.max(0.01, this.to - this.from), 0.0, 1.0);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        Vec3 next = this.pos.add(this.dir.scale(this.speed));
        double y = brain.groundY(next.x, next.z, this.pos.y + this.radius() * 0.5);
        if (y > this.pos.y + 1.1 + this.radius() * 0.3) {
            this.burst(brain);
            return false;
        }
        this.pos = new Vec3(next.x, y, next.z);
        double r = this.radius();
        Vec3 center = this.pos.add(0.0, r, 0.0);
        for (LivingEntity victim : brain.victimsAround(center, r + 0.4, r + 1.0)) {
            if (!this.hit.add(victim.getId())) {
                continue;
            }
            double grown = this.growth();
            if (brain.damage(victim, this.multiplier * (0.4 + 0.6 * grown))) {
                brain.knock(victim, this.dir.x, this.dir.z, 0.4 + 0.5 * grown, 0.2 + 0.25 * grown);
                if (brain instanceof YetiBrain yeti) {
                    yeti.freeze(victim);
                }
            }
        }
        if (this.age % 6 == 0) {
            brain.level().playSound(null, center.x, center.y, center.z, SoundEvents.SNOW_STEP, SoundSource.HOSTILE, (float) (1.0 + r * 0.4), (float) (0.9 - this.growth() * 0.5));
        }
        if (this.age >= this.life) {
            this.burst(brain);
            return false;
        }
        return true;
    }

    private void burst(BossBrain<?> brain) {
        double r = this.radius();
        Vec3 center = this.pos.add(0.0, r, 0.0);
        for (LivingEntity victim : brain.victimsAround(center, r + 1.2, r + 1.5)) {
            if (brain.damage(victim, this.multiplier * (0.2 + 0.4 * this.growth()))) {
                brain.knockFrom(victim, center, 0.3, 0.2);
                if (brain instanceof YetiBrain yeti) {
                    yeti.freeze(victim);
                }
            }
        }
        YetiFx.snowballBreak(brain, this.id, center, r);
        brain.level().playSound(null, center.x, center.y, center.z, SoundEvents.SNOW_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
        brain.level().playSound(null, center.x, center.y, center.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
    }
}
