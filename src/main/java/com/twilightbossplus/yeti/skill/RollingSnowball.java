package com.twilightbossplus.yeti.skill;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiFx;
import com.twilightbossplus.yeti.hazard.Snowball;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class RollingSnowball extends YetiSkill {
    private static final int WINDUP = 20;
    private static final double SPEED = 0.5;
    private static final double FROM = 0.6;
    private static final double TO = 3.8;
    private static final int LIFE = 72;

    public RollingSnowball(YetiBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "rolling_snowball";
    }

    @Override
    public void start() {
        super.start();
        this.faceTarget(360.0F);
        YetiFx.pose(this.brain, this.mob, YetiFx.POSE_SCOOP, WINDUP, WINDUP + 8);
        this.sound(SoundEvents.SNOW_BREAK, this.mob.position(), 2.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age < WINDUP - 4) {
            this.faceTarget(12.0F);
        }
        if (this.age == WINDUP) {
            LivingEntity target = this.target();
            Vec3 dir = this.forward();
            if (target != null) {
                Vec3 flat = new Vec3(target.getX() - this.mob.getX(), 0.0, target.getZ() - this.mob.getZ());
                if (flat.lengthSqr() > 1.0E-4) {
                    dir = flat.normalize();
                }
            }
            int count = 1;
            if (this.brain.enraged()) {
                count = 3;
            }
            for (int i = 0; i < count; i++) {
                double angle = Math.toRadians((i - (count - 1) * 0.5) * 22.0);
                Vec3 d = dir.yRot((float) angle);
                Vec3 start = this.brain.ground(this.mob.position().add(d.scale(this.mob.getBbWidth() * 0.6 + FROM)));
                int id = this.brain.nextBallId();
                this.brain.addHazard(new Snowball(id, start, d, SPEED, FROM, TO, LIFE, YetiConfig.ROLLING_SNOWBALL.multiplier.get()));
                YetiFx.snowball(this.brain, id, start, d, SPEED, FROM, TO, LIFE);
            }
            this.sound(TFSounds.ALPHA_YETI_THROW.get(), this.mob.position(), 2.5F, 0.8F);
        }
        return this.age < WINDUP + 8;
    }
}
