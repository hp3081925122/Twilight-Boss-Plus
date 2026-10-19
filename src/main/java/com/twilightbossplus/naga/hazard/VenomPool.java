package com.twilightbossplus.naga.hazard;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.common.BossHazard;
import com.twilightbossplus.network.VfxPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class VenomPool implements BossHazard {
    private static final int PULSE = 10;

    private final Vec3 center;
    private final double radius;
    private final int duration;
    private final double multiplier;
    private int age;

    public VenomPool(Vec3 center, double radius, int duration, double multiplier) {
        this.center = center;
        this.radius = radius;
        this.duration = duration;
        this.multiplier = multiplier;
    }

    public void announce(BossBrain<?> brain) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", this.center);
        data.putFloat("r", (float) this.radius);
        data.putInt("d", this.duration);
        brain.send(VfxPacket.POOL, data);
    }

    @Override
    public boolean tick(BossBrain<?> brain) {
        this.age++;
        if (this.age % PULSE == 0) {
            for (LivingEntity victim : brain.victimsAround(this.center, this.radius, 1.5)) {
                if (brain.damage(victim, this.multiplier)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0), brain.mob());
                }
            }
        }
        return this.age < this.duration;
    }
}
