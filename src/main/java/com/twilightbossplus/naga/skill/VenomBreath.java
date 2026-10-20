package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class VenomBreath extends NagaSkill {
    private static final int WINDUP = 16;
    private static final int DURATION = 56;
    private static final int PULSE = 8;
    private static final double LENGTH = 10.0;
    private static final double HALF_ANGLE = 28.0;
    private static final float TURN_SPEED = 2.5F;

    public VenomBreath(NagaBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "venom_breath";
    }

    @Override
    public void start() {
        super.start();
        NagaFx.breath(this.brain, this.naga, LENGTH, HALF_ANGLE, WINDUP, WINDUP + DURATION);
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 3.0F, 1.2F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (this.age < WINDUP) {
            if (target != null) {
                this.face(target.position(), 12.0F);
            }
            return true;
        }
        if (target != null) {
            this.face(target.position(), TURN_SPEED);
        }
        if ((this.age - WINDUP) % 10 == 0) {
            this.naga.playSound(SoundEvents.FIRE_EXTINGUISH, 1.5F, 0.5F);
        }
        if ((this.age - WINDUP) % PULSE == 0) {
            this.pulse();
        }
        return this.age < WINDUP + DURATION;
    }

    private void pulse() {
        Vec3 origin = this.naga.getEyePosition();
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.naga.getYRot());
        double cos = Math.cos(Math.toRadians(HALF_ANGLE));
        AABB box = new AABB(origin, origin).inflate(LENGTH, 4.0, LENGTH);
        for (LivingEntity victim : this.brain.victims(box)) {
            Vec3 to = new Vec3(victim.getX() - origin.x, 0.0, victim.getZ() - origin.z);
            double distance = to.length();
            if (distance > LENGTH || distance < 1.0E-3) {
                continue;
            }
            if ((to.x * forward.x + to.z * forward.z) / distance < cos) {
                continue;
            }
            Vec3 aim = victim.getEyePosition();
            if (this.naga.level().clip(new ClipContext(origin, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.naga)).getType() != HitResult.Type.MISS) {
                continue;
            }
            if (this.brain.damage(victim, NagaConfig.VENOM_BREATH.multiplier.get())) {
                victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), this.naga);
            }
        }
    }
}
