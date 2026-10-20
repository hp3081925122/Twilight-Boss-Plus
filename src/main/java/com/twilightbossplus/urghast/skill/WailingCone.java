package com.twilightbossplus.urghast.skill;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBrain;
import com.twilightbossplus.urghast.UrGhastFx;
import com.twilightbossplus.urghast.hazard.ConeWave;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class WailingCone extends UrGhastSkill {
    public static final double RANGE = 34.0;
    private static final int INHALE = 20;
    private static final int WAVES = 3;
    private static final int GAP = 8;
    private static final float HALF = (float) Math.toRadians(18.0);
    private static final double SPEED = 1.2;
    private static final double MAX = 36.0;
    private static final double THICKNESS = 1.5;

    private Vec3 dir = Vec3.ZERO;
    private int waves;

    public WailingCone(UrGhastBrain brain, LivingEntity target) {
        super(brain, target);
    }

    @Override
    public String name() {
        return "wailing_cone";
    }

    @Override
    public void start() {
        this.mob.setCharging(true);
        UrGhastFx.inhale(this.brain, this.mob, this.brain.mouth(), INHALE);
        this.sound(TFSounds.UR_GHAST_WARN.get(), this.brain.center(), 8.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        if (!this.valid()) {
            return false;
        }
        this.hover();
        if (this.age < INHALE) {
            this.stare(this.aim(this.victim));
            return true;
        }
        if (this.age == INHALE) {
            this.dir = this.aim(this.victim).subtract(this.brain.mouth()).normalize();
        }
        this.stare(this.brain.mouth().add(this.dir.scale(10.0)));
        if ((this.age - INHALE) % GAP == 0 && this.waves < WAVES) {
            Vec3 origin = this.brain.mouth();
            this.brain.addHazard(new ConeWave(origin, this.dir, HALF, SPEED, MAX, THICKNESS, 0, UrGhastConfig.WAILING_CONE.multiplier.get(), UrGhastConfig.WAIL_SLOWNESS_TICKS.get()));
            UrGhastFx.wail(this.brain, origin, this.dir, HALF, SPEED, MAX, 0);
            this.sound(TFSounds.UR_GHAST_TANTRUM.get(), origin, 10.0F, 0.8F + this.waves * 0.15F);
            this.waves++;
        }
        return this.waves < WAVES;
    }
}
