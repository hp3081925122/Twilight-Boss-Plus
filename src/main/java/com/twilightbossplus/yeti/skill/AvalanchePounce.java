package com.twilightbossplus.yeti.skill;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiFx;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import twilightforest.init.TFSounds;

public final class AvalanchePounce extends YetiSkill {
    public static final double RADIUS = 4.5;
    private static final int FLIGHT = 18;
    private static final int RECOVER = 12;
    private static final double MAX = 22.0;

    private final int crouch;
    @Nullable
    private Leap leap;
    private int landedAt = -1;

    public AvalanchePounce(YetiBrain brain) {
        super(brain);
        if (brain.enraged()) {
            this.crouch = 10;
        } else {
            this.crouch = 14;
        }
    }

    @Override
    public String name() {
        return "avalanche_pounce";
    }

    @Override
    public void start() {
        super.start();
        this.faceTarget(360.0F);
        YetiFx.pose(this.brain, this.mob, YetiFx.POSE_CROUCH, this.crouch, this.crouch + FLIGHT);
        this.sound(TFSounds.ALPHA_YETI_GROWL.get(), this.mob.position(), 2.0F, 0.7F);
    }

    @Override
    protected boolean tick() {
        if (this.age < this.crouch) {
            this.holdStill();
            this.faceTarget(20.0F);
            return true;
        }
        if (this.leap == null) {
            LivingEntity target = this.target();
            if (target == null) {
                return false;
            }
            this.leap = new Leap(this.mob, this.landing(target, MAX), FLIGHT, 4.0 + this.mob.distanceTo(target) * 0.18);
            this.face(this.leap.target(), 360.0F);
            YetiFx.mark(this.brain, this.leap.target(), RADIUS, FLIGHT);
            YetiFx.leap(this.brain, this.mob, FLIGHT);
            this.sound(TFSounds.ALPHA_YETI_ALERT.get(), this.mob.position(), 2.5F, 0.8F);
        }
        if (this.landedAt < 0) {
            if (this.leap.step()) {
                this.landedAt = this.age;
                this.brain.slam(this.mob.position(), RADIUS, YetiConfig.AVALANCHE_POUNCE.multiplier.get());
            }
            return true;
        }
        this.holdStill();
        return this.age < this.landedAt + RECOVER;
    }
}
