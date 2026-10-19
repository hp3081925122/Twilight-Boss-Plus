package com.twilightbossplus.lich.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.network.VfxPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class StarFall implements BossHazard {
    public static final double RADIUS = 2.2;
    private static final double HEIGHT = 7.0;

    private final Vec3 center;
    private final int fuse;
    private final double multiplier;
    private int age;

    public StarFall(Vec3 center, int fuse, double multiplier) {
        this.center = center;
        this.fuse = fuse;
        this.multiplier = multiplier;
    }

    public void announce(BossBrain<?> brain) {
        brain.sigil(this.center, RADIUS, this.fuse, LichColors.TWILIGHT);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age < this.fuse) {
            return true;
        }
        for (LivingEntity victim : brain.victimsAround(this.center, RADIUS, HEIGHT)) {
            if (victim.getY() < this.center.y - 1.0 || victim.getY() > this.center.y + HEIGHT) {
                continue;
            }
            if (brain.damage(victim, this.multiplier)) {
                brain.knockFrom(victim, this.center, 0.2, 0.55);
            }
        }
        brain.pillar(this.center, RADIUS * 0.8, HEIGHT, 14, LichColors.TWILIGHT);
        brain.burst(this.center, RADIUS, VfxPacket.BURST_SOUL, LichColors.TWILIGHT);
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 0.6F);
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.2F, 0.5F);
        return false;
    }
}
