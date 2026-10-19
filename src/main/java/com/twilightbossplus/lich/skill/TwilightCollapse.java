package com.twilightbossplus.lich.skill;

import com.twilightbossplus.common.RingWave;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.network.VfxPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class TwilightCollapse extends LichSkill {
    private static final int RISE = 20;
    private static final int PULL = 50;
    private static final int AFTER = 20;
    private static final double HOVER = 3.5;
    private static final double PULL_RADIUS = 14.0;
    private static final double PULL_STRENGTH = 0.09;
    private static final double BLAST_RADIUS = 6.0;
    private static final double WAVE_SPEED = 0.6;
    private static final double WAVE_RADIUS = 16.0;
    private static final double WAVE_THICKNESS = 1.4;

    private Vec3 center = Vec3.ZERO;

    public TwilightCollapse(LichBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "TwilightCollapse";
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
        this.center = this.brain.ground(this.lich.position());
        this.lich.setNoGravity(true);
        this.brain.vortex(this.center, PULL_RADIUS, RISE + PULL, LichColors.VOID);
        this.brain.circle(this.center, BLAST_RADIUS, RISE + PULL, LichColors.DANGER);
        this.brain.orb(this.lich, 1.1, 1.8, RISE + PULL, LichColors.VOID);
        this.sound(TFSounds.LICH_AMBIENT.get(), this.lich.position(), 3.0F, 0.4F);
        this.sound(SoundEvents.BEACON_POWER_SELECT, this.lich.position(), 3.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        if (this.age < RISE) {
            double wanted = this.center.y + HOVER;
            this.lich.setDeltaMovement(0.0, (wanted - this.lich.getY()) * 0.2, 0.0);
        } else {
            this.lich.setDeltaMovement(0.0, 0.0, 0.0);
        }
        this.lich.getNavigation().stop();
        LivingEntity target = this.target();
        if (target != null) {
            this.lookAt(target);
        }
        if (this.age >= RISE && this.age < RISE + PULL) {
            for (LivingEntity victim : this.brain.victimsAround(this.center, PULL_RADIUS, 6.0)) {
                double dx = this.center.x - victim.getX();
                double dz = this.center.z - victim.getZ();
                if (dx * dx + dz * dz < 1.0) {
                    continue;
                }
                this.brain.knock(victim, dx, dz, PULL_STRENGTH, 0.0);
            }
            if (this.age % 10 == 0) {
                this.sound(SoundEvents.PORTAL_AMBIENT, this.center, 2.0F, 0.5F + (this.age - RISE) / (float) PULL);
            }
        }
        if (this.age == RISE + PULL) {
            for (LivingEntity victim : this.brain.victimsAround(this.center, BLAST_RADIUS, 6.0)) {
                if (this.brain.damage(victim, LichConfig.TWILIGHT_COLLAPSE.multiplier.get())) {
                    this.brain.knockFrom(victim, this.center, 1.2, 0.5);
                }
            }
            this.brain.addHazard(new RingWave(this.center, WAVE_SPEED, WAVE_RADIUS, WAVE_THICKNESS, 4, LichConfig.COLLAPSE_WAVE_MULTIPLIER.get()));
            this.brain.ring(this.center, WAVE_SPEED, WAVE_RADIUS, WAVE_THICKNESS, 4, LichColors.VOID);
            this.brain.burst(this.center, BLAST_RADIUS, VfxPacket.BURST_SOUL, LichColors.VOID);
            this.brain.pillar(this.center, 2.4, 14.0, 18, LichColors.VOID);
            this.brain.shake(this.center, 1.6F, 32.0F, 20);
            this.cast();
            this.sound(SoundEvents.GENERIC_EXPLODE, this.center, 3.0F, 0.6F);
            this.sound(SoundEvents.WITHER_SHOOT, this.center, 2.0F, 0.5F);
        }
        return this.age < RISE + PULL + AFTER;
    }

    @Override
    public void end() {
        this.lich.setNoGravity(false);
    }
}
