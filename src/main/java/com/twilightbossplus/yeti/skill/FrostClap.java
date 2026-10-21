package com.twilightbossplus.yeti.skill;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class FrostClap extends YetiSkill {
    public static final double TRIGGER = 6.5;
    private static final double REACH = 7.0;
    private static final double HALF = 55.0;
    private static final int WINDUP = 14;

    public FrostClap(YetiBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "frost_clap";
    }

    @Override
    public void start() {
        super.start();
        this.faceTarget(360.0F);
        YetiFx.pose(this.brain, this.mob, YetiFx.POSE_CLAP, WINDUP, WINDUP + 8);
        YetiFx.clap(this.brain, this.mob, REACH, HALF, WINDUP);
        this.sound(TFSounds.ALPHA_YETI_GROWL.get(), this.mob.position(), 2.0F, 0.8F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age < WINDUP - 2) {
            this.faceTarget(10.0F);
        }
        if (this.age == WINDUP) {
            Vec3 origin = this.mob.position().add(0.0, 1.5, 0.0);
            Vec3 facing = this.forward();
            double cos = Math.cos(Math.toRadians(HALF));
            for (LivingEntity victim : this.brain.victimsAround(this.mob.position(), REACH, 4.0)) {
                double dx = victim.getX() - origin.x;
                double dz = victim.getZ() - origin.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 1.2 && (dx * facing.x + dz * facing.z) / distance < cos) {
                    continue;
                }
                this.brain.frost(victim, YetiConfig.FROST_CLAP.multiplier.get(), origin, 0.5, 0.15);
            }
            this.brain.shake(origin, 0.4F, 12.0F, 6);
            this.sound(TFSounds.ALPHA_YETI_ICE.get(), origin, 2.0F, 0.8F);
            this.sound(SoundEvents.POWDER_SNOW_BREAK, origin, 2.0F, 0.6F);
        }
        return this.age < WINDUP + 10;
    }
}
