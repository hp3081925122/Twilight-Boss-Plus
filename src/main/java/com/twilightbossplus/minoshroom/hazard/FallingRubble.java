package com.twilightbossplus.minoshroom.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FallingRubble implements BossHazard {
    public static final double RADIUS = 1.6;
    public static final int FUSE = 22;

    private final Vec3 center;
    private final double multiplier;
    private int age;

    public FallingRubble(Vec3 center, double multiplier) {
        this.center = center;
        this.multiplier = multiplier;
    }

    public void announce(BossBrain<?> brain) {
        MinoFx.rubble(brain, this.center, RADIUS, FUSE);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age < FUSE) {
            return true;
        }
        for (LivingEntity victim : brain.victimsAround(this.center, RADIUS, 3.0)) {
            if (brain.damage(victim, this.multiplier)) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), brain.mob());
            }
        }
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.STONE_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        brain.level().playSound(null, this.center.x, this.center.y, this.center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.5F, 1.6F);
        return false;
    }
}
