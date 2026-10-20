package com.twilightbossplus.lich.skill;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.lich.LichFx;
import com.twilightbossplus.lich.hazard.PrisonWall;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SoulPrison extends LichSkill {
    private static final double RADIUS = 6.0;
    private static final double HEIGHT = 5.0;
    private static final int WARMUP = 24;

    private Vec3 center = Vec3.ZERO;

    public SoulPrison(LichBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "SoulPrison";
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        if (target == null) {
            return;
        }
        this.center = this.brain.ground(target.position());
        int duration = LichConfig.PRISON_TICKS.get();
        LichFx.sigil(this.brain, this.center, RADIUS, WARMUP, LichColors.SOUL, LichFx.SIGIL_HEXAGRAM);
        this.brain.addHazard(new PrisonWall(this.center, RADIUS, WARMUP, duration, LichConfig.SOUL_PRISON.multiplier.get()));
        LichFx.orb(this.brain, this.lich, 2.5, 0.55, WARMUP, LichColors.SOUL, LichFx.ORB_SOUL);
        this.sound(SoundEvents.EVOKER_PREPARE_WOLOLO, this.lich.position(), 1.6F, 0.6F);
    }

    @Override
    protected boolean tick() {
        if (this.center == Vec3.ZERO) {
            return false;
        }
        this.holdStill();
        this.face(this.center, 20.0F);
        if (this.age == WARMUP) {
            LichFx.prison(this.brain, this.center, RADIUS, HEIGHT, LichConfig.PRISON_TICKS.get());
            this.cast();
            this.sound(SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, this.center, 2.0F, 0.5F);
            this.brain.shake(this.center, 0.5F, 16.0F, 8);
        }
        return this.age < WARMUP + 4;
    }
}
