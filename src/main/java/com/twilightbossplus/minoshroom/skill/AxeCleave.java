package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class AxeCleave extends MinoSkill {
    public static final double TRIGGER_RANGE = 4.5;
    private static final double SWEEP_REACH = 4.2;
    private static final double SWEEP_HALF = 70.0;
    private static final double CHOP_REACH = 4.8;
    private static final double CHOP_HALF = 28.0;
    private static final int WINDUP = 10;
    private static final int GAP = 12;

    public AxeCleave(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "AxeCleave";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        if (target != null) {
            this.face(target.position(), 360.0F);
        }
        MinoFx.cleave(this.brain, this.mino, SWEEP_REACH, SWEEP_HALF, WINDUP, MinoFx.CLEAVE_SWEEP);
        this.sound(TFSounds.MINOSHROOM_AMBIENT.get(), this.mino.position(), 1.6F, 1.2F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (target != null && this.age != WINDUP && this.age != WINDUP + GAP) {
            this.face(target.position(), 12.0F);
        }
        if (this.age == WINDUP - 3 || this.age == WINDUP + GAP - 3) {
            this.swing();
        }
        if (this.age == WINDUP) {
            this.strike(SWEEP_REACH, SWEEP_HALF, MinoConfig.AXE_CLEAVE.multiplier.get(), 0.5, 0.2);
            this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, this.mino.position(), 1.6F, 0.6F);
        }
        if (this.age == WINDUP + 1) {
            MinoFx.cleave(this.brain, this.mino, CHOP_REACH, CHOP_HALF, GAP - 1, MinoFx.CLEAVE_CHOP);
        }
        if (this.age == WINDUP + GAP) {
            this.strike(CHOP_REACH, CHOP_HALF, MinoConfig.CHOP_MULTIPLIER.get(), 0.2, 0.5);
            Vec3 point = this.brain.ground(this.mino.position().add(this.forward().scale(CHOP_REACH * 0.7)));
            MinoFx.pop(this.brain, point, 1.6);
            this.brain.shake(point, 0.7F, 14.0F, 8);
            this.sound(TFSounds.MINOSHROOM_SLAM.get(), point, 1.6F, 1.1F);
        }
        return this.age < WINDUP + GAP + 8;
    }

    private void strike(double reach, double half, double multiplier, double horizontal, double vertical) {
        Vec3 origin = this.mino.position();
        Vec3 facing = this.forward();
        double cos = Math.cos(Math.toRadians(half));
        for (LivingEntity victim : this.brain.victimsAround(origin, reach, 3.0)) {
            double dx = victim.getX() - origin.x;
            double dz = victim.getZ() - origin.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > 0.8 && (dx * facing.x + dz * facing.z) / distance < cos) {
                continue;
            }
            if (this.brain.damage(victim, multiplier)) {
                this.brain.knock(victim, dx, dz, horizontal, vertical);
            }
        }
    }
}
