package com.twilightbossplus.queen.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class Coffin implements BossHazard {
    private static final double LOCK = 0.6;

    private final Vec3 center;
    private final double radius;
    private final int close;
    private final double angle;
    private final double multiplier;
    private final int slow;
    private int age;

    public Coffin(Vec3 center, double radius, int close, double angle, double multiplier, int slow) {
        this.center = center;
        this.radius = radius;
        this.close = close;
        this.angle = angle;
        this.multiplier = multiplier;
        this.slow = slow;
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age < this.close) {
            if (this.age >= this.close * LOCK) {
                for (LivingEntity victim : brain.victimsAround(this.center, this.radius + 0.2, 3.5)) {
                    double dx = victim.getX() - this.center.x;
                    double dz = victim.getZ() - this.center.z;
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > this.radius - 0.6) {
                        brain.knock(victim, -dx, -dz, 0.14, 0.0);
                    }
                }
            }
            if (this.age % 6 == 0) {
                brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 1.2F, 0.5F + this.age / (float) this.close);
            }
            return true;
        }
        for (LivingEntity victim : brain.victimsAround(this.center, this.radius, 3.5)) {
            if (!brain.damage(victim, this.multiplier)) {
                continue;
            }
            victim.setDeltaMovement(0.0, Math.min(0.0, victim.getDeltaMovement().y), 0.0);
            victim.hurtMarked = true;
            if (brain instanceof QueenBrain queen) {
                queen.freeze(victim, 2.0);
            }
            if (this.slow > 0) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.slow, 2));
            }
        }
        QueenFx.coffinShatter(brain, this.center, this.radius, this.angle);
        brain.shake(this.center, 0.5F, 14.0F, 8);
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, TFSounds.SNOW_QUEEN_BREAK.get(), SoundSource.HOSTILE, 2.0F, 0.8F);
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        return false;
    }
}
