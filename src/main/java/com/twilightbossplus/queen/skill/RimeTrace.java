package com.twilightbossplus.queen.skill;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import com.twilightbossplus.queen.hazard.RimeTrail;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class RimeTrace extends QueenSkill {
    private static final int RISE = 14;
    private static final int RECOVER = 10;
    private static final int BLOOM = 16;
    private static final double RADIUS = 1.3;

    private int draw = 50;
    private double speed = 0.24;
    private Vec3 spot = Vec3.ZERO;
    private Vec3 point = Vec3.ZERO;
    @Nullable
    private RimeTrail trail;

    public RimeTrace(QueenBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "rime_trace";
    }

    @Override
    public void start() {
        super.start();
        if (this.brain.enraged()) {
            this.draw = 60;
            this.speed = 0.27;
        }
        this.spot = this.above(this.mob.position(), 5.5);
        QueenFx.pose(this.brain, this.mob, QueenFx.POSE_BREATH, RISE, RISE + this.draw);
        this.sound(TFSounds.SNOW_QUEEN_AMBIENT.get(), this.mob.position(), 2.0F, 0.8F);
    }

    @Override
    protected boolean tick() {
        LivingEntity target = this.target();
        if (this.age < RISE) {
            this.brain.fly(this.spot, 0.4, 0.3);
            this.faceTarget(15.0F);
            return true;
        }
        this.brain.brake();
        int local = this.age - RISE;
        if (local == 0) {
            Vec3 from = this.mob.position();
            Vec3 aim = from;
            if (target != null) {
                aim = target.position();
            }
            Vec3 start = from.lerp(aim, 0.4);
            this.point = this.brain.ground(new Vec3(start.x, Math.max(from.y - 2.0, aim.y + 1.0), start.z));
            this.trail = new RimeTrail(RADIUS, SnowQueenConfig.RIME_TRACE.multiplier.get());
            this.brain.addHazard(this.trail);
            QueenFx.trace(this.brain, this.mob, this.draw);
            this.sound(SoundEvents.POWDER_SNOW_STEP, this.point, 2.0F, 0.6F);
        }
        if (local < this.draw && this.trail != null) {
            if (target != null) {
                Vec3 flat = new Vec3(target.getX() - this.point.x, 0.0, target.getZ() - this.point.z);
                double distance = flat.length();
                if (distance > 1.0E-3) {
                    Vec3 step = flat.scale(Math.min(this.speed, distance) / distance);
                    double reference = Math.max(this.point.y, target.getY()) + 1.0;
                    double x = this.point.x + step.x;
                    double z = this.point.z + step.z;
                    this.point = new Vec3(x, this.brain.groundY(x, z, reference), z);
                }
            }
            this.brain.faceToward(this.point, 12.0F);
            if (local % 2 == 0) {
                this.trail.add(this.point, BLOOM);
                QueenFx.traceNode(this.brain, this.mob, this.point, BLOOM, RADIUS);
            }
            if (local % 8 == 0) {
                this.sound(SoundEvents.POWDER_SNOW_STEP, this.point, 1.4F, 0.8F);
            }
        }
        if (local == this.draw && this.trail != null) {
            this.trail.close();
        }
        return local < this.draw + RECOVER;
    }

    @Override
    public void end() {
        if (this.trail != null) {
            this.trail.close();
        }
    }
}
