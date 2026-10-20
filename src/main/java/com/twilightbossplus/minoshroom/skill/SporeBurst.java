package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;
import com.twilightbossplus.minoshroom.hazard.SporeCloud;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SporeBurst extends MinoSkill {
    private static final int WINDUP = 14;
    private static final double SPREAD = 4.0;

    public SporeBurst(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "SporeBurst";
    }

    @Override
    public void start() {
        super.start();
        MinoFx.sporeCap(this.brain, this.mino, this.mino.getBbHeight() + 0.5, 0.7, WINDUP);
        this.sound(SoundEvents.MOOSHROOM_CONVERT, this.mino.position(), 1.6F, 0.6F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (target == null) {
            return false;
        }
        this.face(target.position(), 20.0F);
        if (this.age == WINDUP) {
            int clouds = 3;
            if (this.brain.enraged()) {
                clouds = 5;
            }
            int duration = MinoConfig.SPORE_TICKS.get();
            double multiplier = MinoConfig.SPORE_BURST.multiplier.get();
            for (int i = 0; i < clouds; i++) {
                Vec3 spot = target.position();
                if (i > 0) {
                    double angle = (i / (double) (clouds - 1)) * Math.PI * 2.0 + this.mino.getRandom().nextDouble();
                    double dist = SPREAD * (0.6 + this.mino.getRandom().nextDouble() * 0.4);
                    spot = spot.add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                }
                SporeCloud cloud = new SporeCloud(this.brain.ground(spot), duration, multiplier);
                this.brain.addHazard(cloud);
                cloud.announce(this.brain);
            }
            this.swing();
            this.sound(SoundEvents.PUFFER_FISH_BLOW_OUT, this.mino.position(), 2.0F, 0.5F);
        }
        return this.age < WINDUP + 6;
    }
}
