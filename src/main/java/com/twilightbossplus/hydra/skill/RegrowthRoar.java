package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.RingWave;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraColors;
import com.twilightbossplus.hydra.HydraFx;
import com.twilightbossplus.hydra.HydraHeads;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.HydraHeadContainer;
import twilightforest.init.TFSounds;

public final class RegrowthRoar extends HydraSkill {
    private static final int ROAR_START = 10;
    private static final int IMPACT = 40;
    private static final int REGROW_STEP = 5;
    private static final double WAVE_SPEED = 0.6;
    private static final double WAVE_MAX = 20.0;
    private static final double WAVE_THICKNESS = 1.4;
    private static final int SECOND_WAVE = 14;

    private final List<Integer> regrowing = new ArrayList<>();

    public RegrowthRoar(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "RegrowthRoar";
    }

    @Override
    public void start() {
        this.heads.addAll(this.brain.aliveHeads(0, 1, 2, 3, 4, 5, 6));
        for (int i = 0; i < HydraBrain.HEADS; i++) {
            HydraHeadContainer head = this.brain.head(i);
            if (!head.isActive()) {
                head.setRespawnCounter(2 + this.regrowing.size() * REGROW_STEP);
                this.regrowing.add(i);
            }
        }
        TwilightBossPlus.debug("Hydra {} regrows heads {}", this.hydra.getId(), this.regrowing);
        this.command(HydraHeads.ROAR_START, ROAR_START);
        HydraFx.roar(this.brain, this.hydra, IMPACT + 10);
        HydraFx.aura(this.brain, this.hydra, 160);
        HydraFx.warn(this.brain, this.center(), 6.0, IMPACT, HydraColors.RAGE);
    }

    @Override
    protected boolean tick() {
        for (int k = 0; k < this.regrowing.size(); k++) {
            if (this.age == 3 + k * REGROW_STEP) {
                HydraFx.regrow(this.brain, this.hydra, this.regrowing.get(k));
            }
        }
        if (this.age == ROAR_START) {
            this.command(HydraHeads.ROAR_RAWR, IMPACT - ROAR_START + 10);
            this.sound(TFSounds.HYDRA_ROAR.get(), this.hydra.position(), 4.0F, 0.6F);
        }
        if (this.age == IMPACT || this.age == IMPACT + SECOND_WAVE) {
            Vec3 center = this.center();
            this.brain.addHazard(new RingWave(center, WAVE_SPEED, WAVE_MAX, WAVE_THICKNESS, 0, HydraConfig.REGROWTH_ROAR.multiplier.get()));
            HydraFx.wave(this.brain, center, WAVE_SPEED, WAVE_MAX, WAVE_THICKNESS, 0);
            HydraFx.stomp(this.brain, center, 4.0);
            this.brain.shake(center, 1.2F, 28.0F, 16);
            this.sound(SoundEvents.GENERIC_EXPLODE, center, 3.0F, 0.5F);
        }
        return this.age < IMPACT + SECOND_WAVE + 10;
    }
}
