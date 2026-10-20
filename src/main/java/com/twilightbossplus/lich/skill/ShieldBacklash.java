package com.twilightbossplus.lich.skill;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.lich.LichFx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class ShieldBacklash extends LichSkill {
    private static final int WINDUP = 14;
    private static final double RADIUS = 5.5;
    private static final double FINAL_SCALE = 1.5;

    private final boolean finalLayer;
    private final double radius;
    private Vec3 center = Vec3.ZERO;

    public ShieldBacklash(LichBrain brain, boolean finalLayer) {
        super(brain);
        this.finalLayer = finalLayer;
        if (finalLayer) {
            this.radius = RADIUS * FINAL_SCALE;
        } else {
            this.radius = RADIUS;
        }
    }

    @Override
    public String name() {
        return "ShieldBacklash";
    }

    @Override
    public void start() {
        super.start();
        this.center = this.brain.ground(this.lich.position());
        LichFx.sigil(this.brain, this.center, this.radius, WINDUP, LichColors.SHIELD, LichFx.SIGIL_HEXAGRAM);
        LichFx.orb(this.brain, this.lich, 1.2, 1.4, WINDUP, LichColors.SHIELD, LichFx.ORB_SHIELD);
        this.sound(TFSounds.SHIELD_ADD.get(), this.lich.position(), 2.0F, 0.6F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age == WINDUP) {
            double multiplier = LichConfig.SHIELD_BACKLASH.multiplier.get();
            if (this.finalLayer) {
                multiplier *= 1.5;
            }
            for (LivingEntity victim : this.brain.victimsAround(this.center, this.radius, 4.0)) {
                if (this.brain.damage(victim, multiplier)) {
                    this.brain.knockFrom(victim, this.center, 0.9, 0.35);
                }
            }
            LichFx.blast(this.brain, this.center, this.radius, LichColors.SHIELD);
            LichFx.runeWave(this.brain, this.center, 0.9, this.radius, 1.0, 0, LichColors.SHIELD);
            this.brain.shake(this.center, 0.8F, 18.0F, 10);
            this.sound(TFSounds.SHIELD_BREAK.get(), this.lich.position(), 3.0F, 0.5F);
        }
        return this.age < WINDUP + 6;
    }
}
