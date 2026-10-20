package com.twilightbossplus.knight.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.knight.skill.EchoSlash;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SlashEcho implements BossHazard {
    private final Vec3 center;
    private final float angle;
    private final float sweep;
    private final float radius;
    private final double multiplier;
    private int delay;

    public SlashEcho(Vec3 center, float angle, float sweep, float radius, int delay, double multiplier) {
        this.center = center;
        this.angle = angle;
        this.sweep = sweep;
        this.radius = radius;
        this.delay = delay;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.delay--;
        if (this.delay > 0) {
            return true;
        }
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.55F);
        for (LivingEntity victim : EchoSlash.sector(brain, this.center, this.angle, this.sweep, this.radius)) {
            if (brain.damage(victim, this.multiplier)) {
                brain.knockFrom(victim, this.center, 0.3, 0.15);
            }
        }
        return false;
    }
}
