package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.common.RingWave;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraColors;
import com.twilightbossplus.hydra.HydraFx;
import com.twilightbossplus.hydra.HydraHeads;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class MagmaStomp extends HydraSkill {
    public static final double TRIGGER_RANGE = 8.0;
    private static final int ROAR_START = 8;
    private static final int IMPACT = 30;
    private static final double RADIUS = 5.5;
    private static final double WAVE_SPEED = 0.55;
    private static final double WAVE_MAX = 16.0;
    private static final double WAVE_THICKNESS = 1.4;

    public MagmaStomp(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "MagmaStomp";
    }

    @Override
    public void start() {
        this.heads.addAll(this.brain.aliveHeads(0, 1, 2, 3, 4, 5, 6));
        this.command(HydraHeads.ROAR_START, ROAR_START);
        HydraFx.warn(this.brain, this.center(), RADIUS, IMPACT, HydraColors.DANGER);
        HydraFx.roar(this.brain, this.hydra, IMPACT);
    }

    @Override
    protected boolean tick() {
        if (this.age == ROAR_START) {
            this.command(HydraHeads.ROAR_RAWR, IMPACT - ROAR_START + 6);
            this.sound(TFSounds.HYDRA_ROAR.get(), this.hydra.position(), 3.0F, 0.8F);
        }
        if (this.age == IMPACT) {
            Vec3 center = this.center();
            for (LivingEntity victim : this.brain.victimsAround(center, RADIUS, 3.0)) {
                if (this.brain.damage(victim, HydraConfig.MAGMA_STOMP.multiplier.get())) {
                    victim.setSecondsOnFire(3);
                    this.brain.knockFrom(victim, center, 0.8, 0.4);
                }
            }
            int skip = (int) Math.ceil(RADIUS / WAVE_SPEED);
            this.brain.addHazard(new RingWave(center, WAVE_SPEED, WAVE_MAX, WAVE_THICKNESS, -skip, HydraConfig.STOMP_WAVE_MULTIPLIER.get()));
            HydraFx.stomp(this.brain, center, RADIUS);
            HydraFx.wave(this.brain, center, WAVE_SPEED, WAVE_MAX, WAVE_THICKNESS, -skip);
            this.brain.shake(center, 1.0F, 24.0F, 14);
            this.sound(SoundEvents.GENERIC_EXPLODE, center, 2.5F, 0.6F);
        }
        return this.age < IMPACT + 10;
    }
}
