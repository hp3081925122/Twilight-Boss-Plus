package com.twilightbossplus.queen.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.queen.QueenBrain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RimeTrail implements BossHazard {
    private final double radius;
    private final double multiplier;
    private final List<Vec3> nodes = new ArrayList<>();
    private final List<Integer> blooms = new ArrayList<>();
    private final Set<Integer> hit = new HashSet<>();
    private int age;
    private boolean closed;

    public RimeTrail(double radius, double multiplier) {
        this.radius = radius;
        this.multiplier = multiplier;
    }

    public void add(Vec3 pos, int delay) {
        this.nodes.add(pos);
        this.blooms.add(this.age + delay);
    }

    public void close() {
        this.closed = true;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        boolean pending = false;
        for (int i = 0; i < this.nodes.size(); i++) {
            int bloom = this.blooms.get(i);
            if (bloom > this.age) {
                pending = true;
                continue;
            }
            if (bloom != this.age) {
                continue;
            }
            Vec3 at = this.nodes.get(i);
            for (LivingEntity victim : brain.victimsAround(at, this.radius, 2.5)) {
                if (!this.hit.add(victim.getId())) {
                    continue;
                }
                if (brain instanceof QueenBrain queen) {
                    queen.frost(victim, this.multiplier, at, 0.25, 0.45);
                }
            }
            if (i % 3 == 0) {
                brain.level().playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.4F, 1.1F + (i % 5) * 0.08F);
            }
        }
        return pending || !this.closed;
    }
}
