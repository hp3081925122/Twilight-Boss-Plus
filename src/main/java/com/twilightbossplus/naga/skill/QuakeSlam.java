package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaColors;
import com.twilightbossplus.common.RingWave;
import com.twilightbossplus.network.VfxPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class QuakeSlam extends NagaSkill {
    private static final int LEAP_TICK = 10;
    private static final int MAX_AIR = 30;
    private static final double CENTER_RADIUS = 3.5;
    private static final double WAVE_SPEED = 0.55;
    private static final double WAVE_RADIUS = 16.0;
    private static final double WAVE_THICKNESS = 1.4;
    private static final int[] WAVE_DELAYS = {0, 9, 18};

    private int slamTick = -1;

    public QuakeSlam(NagaBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "quake_slam";
    }

    @Override
    public void start() {
        super.start();
        Vec3 pos = this.naga.position();
        this.brain.circle(new Vec3(pos.x, this.brain.groundY(pos.x, pos.z, pos.y), pos.z), CENTER_RADIUS, LEAP_TICK + 14, NagaColors.DANGER);
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 3.0F, 0.7F);
    }

    @Override
    protected boolean tick() {
        if (this.slamTick < 0) {
            this.holdStill();
            LivingEntity target = this.target();
            if (target != null) {
                this.face(target.position(), 10.0F);
            }
            if (this.age == LEAP_TICK) {
                Vec3 motion = this.naga.getDeltaMovement();
                this.naga.setDeltaMovement(motion.x, 0.9, motion.z);
                this.naga.hasImpulse = true;
            }
            boolean landed = this.age > LEAP_TICK + 4 && this.naga.onGround();
            if (landed || this.age > LEAP_TICK + MAX_AIR) {
                this.slam();
            }
            return true;
        }
        this.holdStill();
        return this.age < this.slamTick + 8;
    }

    private void slam() {
        this.slamTick = this.age;
        Vec3 pos = this.naga.position();
        Vec3 center = new Vec3(pos.x, this.brain.groundY(pos.x, pos.z, pos.y), pos.z);
        for (LivingEntity victim : this.brain.victimsAround(center, CENTER_RADIUS, 3.0)) {
            this.brain.damage(victim, NagaConfig.QUAKE_SLAM.multiplier.get());
            this.brain.knockFrom(victim, center, 0.6, 0.45);
        }
        double waveMultiplier = NagaConfig.QUAKE_WAVE_MULTIPLIER.get();
        for (int delay : WAVE_DELAYS) {
            this.brain.addHazard(new RingWave(center, WAVE_SPEED, WAVE_RADIUS, WAVE_THICKNESS, delay, waveMultiplier));
            this.brain.ring(center, WAVE_SPEED, WAVE_RADIUS, WAVE_THICKNESS, delay, NagaColors.EARTH);
        }
        this.brain.burst(center, CENTER_RADIUS, VfxPacket.BURST_SLAM);
        this.brain.shake(center, 1.0F, 24.0F, 14);
        this.naga.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, this.naga.getSoundSource(), 1.4F, 0.6F);
    }
}
