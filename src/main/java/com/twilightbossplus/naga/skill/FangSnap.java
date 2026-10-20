package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class FangSnap extends NagaSkill {
    public static final double TRIGGER_RANGE = 5.0;
    private static final double REACH = 4.6;
    private static final double HALF_ANGLE = 55.0;
    private static final int WINDUP = 10;
    private static final int INTERVAL = 9;

    private final int snaps;
    private int done;

    public FangSnap(NagaBrain brain) {
        super(brain);
        this.snaps = NagaConfig.FANG_SNAPS.get();
    }

    @Override
    public String name() {
        return "fang_snap";
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        if (target != null) {
            this.face(target.position(), 360.0F);
        }
        NagaFx.bite(this.brain, this.naga, REACH, HALF_ANGLE, WINDUP, INTERVAL, this.snaps);
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 2.5F, 1.6F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (target != null) {
            this.face(target.position(), 9.0F);
        }
        if (this.done < this.snaps && this.age == WINDUP + this.done * INTERVAL) {
            this.snap();
            this.done++;
        }
        return this.age < WINDUP + (this.snaps - 1) * INTERVAL + 8;
    }

    private void snap() {
        Vec3 origin = this.naga.position();
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.naga.getYRot());
        double cos = Math.cos(Math.toRadians(HALF_ANGLE));
        for (LivingEntity victim : this.brain.victimsAround(origin, REACH, 3.0)) {
            double dx = victim.getX() - origin.x;
            double dz = victim.getZ() - origin.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.8 && (dx * forward.x + dz * forward.z) / distance < cos) {
                continue;
            }
            if (this.brain.damage(victim, NagaConfig.FANG_SNAP.multiplier.get())) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0), this.naga);
                this.brain.knock(victim, dx, dz, 0.35, 0.15);
            }
        }
        this.naga.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.6F + this.done * 0.15F);
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 1.5F, 1.3F);
    }
}
