package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.common.RingWave;
import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoColors;
import com.twilightbossplus.minoshroom.hazard.SporeCloud;
import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class BullRage extends MinoSkill {
    private static final int STOMP = 14;
    private static final int DURATION = 36;
    private static final double WAVE_SPEED = 0.6;
    private static final double WAVE_RADIUS = 14.0;
    private static final double WAVE_THICKNESS = 1.4;
    private static final int[] DELAYS = {0, 10};
    private static final int CLOUDS = 4;
    private static final double CLOUD_DISTANCE = 6.0;

    private Vec3 center = Vec3.ZERO;

    public BullRage(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "BullRage";
    }

    @Override
    public boolean invulnerable() {
        return true;
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        this.center = this.brain.ground(this.mino.position());
        MinoFx.aura(this.brain, this.mino, 120);
        MinoFx.warn(this.brain, this.center, 4.0, STOMP, MinoColors.RAGE);
        this.sound(SoundEvents.RAVAGER_ROAR, this.mino.position(), 3.0F, 0.7F);
        this.sound(TFSounds.MINOSHROOM_AMBIENT.get(), this.mino.position(), 3.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age == STOMP - 4) {
            this.swing();
        }
        if (this.age == STOMP) {
            double multiplier = MinoConfig.BULL_RAGE.multiplier.get();
            for (int delay : DELAYS) {
                this.brain.addHazard(new RingWave(this.center, WAVE_SPEED, WAVE_RADIUS, WAVE_THICKNESS, delay, multiplier));
                MinoFx.stompWave(this.brain, this.center, WAVE_SPEED, WAVE_RADIUS, WAVE_THICKNESS, delay, MinoColors.RAGE);
            }
            MinoFx.slam(this.brain, this.center, 4.0);
            this.brain.shake(this.center, 1.6F, 32.0F, 20);
            this.sound(TFSounds.MINOSHROOM_SLAM.get(), this.center, 3.0F, 0.7F);
            int duration = MinoConfig.SPORE_TICKS.get();
            double sporeMultiplier = MinoConfig.SPORE_BURST.multiplier.get();
            double offset = this.mino.getRandom().nextDouble() * Math.PI;
            for (int i = 0; i < CLOUDS; i++) {
                double angle = offset + i * Math.PI * 2.0 / CLOUDS;
                Vec3 spot = this.brain.ground(this.center.add(Math.cos(angle) * CLOUD_DISTANCE, 0.0, Math.sin(angle) * CLOUD_DISTANCE));
                SporeCloud cloud = new SporeCloud(spot, duration, sporeMultiplier);
                this.brain.addHazard(cloud);
                cloud.announce(this.brain);
            }
        }
        return this.age < DURATION;
    }
}
