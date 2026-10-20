package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;
import com.twilightbossplus.minoshroom.hazard.FissureWave;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class AxeFissure extends MinoSkill {
    private static final int WINDUP = 16;
    private static final double STEP = 1.15;
    private static final int STEPS = 12;
    private static final int INTERVAL = 2;

    private Vec3 direction = Vec3.ZERO;

    public AxeFissure(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "AxeFissure";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        if (target == null) {
            return;
        }
        Vec3 offset = new Vec3(target.getX() - this.mino.getX(), 0.0, target.getZ() - this.mino.getZ());
        if (offset.lengthSqr() < 1.0E-4) {
            offset = this.forward();
        }
        this.direction = offset.normalize();
        this.faceDirection(this.direction);
        Vec3 start = this.mino.position().add(this.direction.scale(1.0));
        MinoFx.track(this.brain, this.mino, start, start.add(this.direction.scale(STEP * STEPS)), 2.2, WINDUP, MinoFx.TRACK_FISSURE);
        MinoFx.axeCharge(this.brain, this.mino, this.mino.getBbHeight() + 0.3, WINDUP);
        this.sound(TFSounds.MINOSHROOM_AMBIENT.get(), this.mino.position(), 1.8F, 0.8F);
    }

    @Override
    protected boolean tick() {
        if (this.direction == Vec3.ZERO) {
            return false;
        }
        this.holdStill();
        this.faceDirection(this.direction);
        if (this.age == WINDUP - 4) {
            this.swing();
        }
        if (this.age == WINDUP) {
            Vec3 start = this.mino.position().add(this.direction.scale(0.6));
            this.brain.addHazard(new FissureWave(start, this.direction, STEP, INTERVAL, STEPS, MinoConfig.AXE_FISSURE.multiplier.get()));
            MinoFx.slam(this.brain, this.brain.ground(start), 1.8);
            this.brain.shake(start, 0.8F, 18.0F, 10);
            this.sound(TFSounds.MINOSHROOM_SLAM.get(), start, 2.0F, 0.9F);
        }
        return this.age < WINDUP + 8;
    }
}
