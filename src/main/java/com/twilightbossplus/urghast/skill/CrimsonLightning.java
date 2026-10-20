package com.twilightbossplus.urghast.skill;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBrain;
import com.twilightbossplus.urghast.UrGhastFx;
import com.twilightbossplus.urghast.hazard.LightningStrike;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class CrimsonLightning extends UrGhastSkill {
    private static final int GAP = 10;
    private static final int DELAY = 18;
    private static final double RADIUS = 2.2;
    private static final double LEAD = 8.0;

    private final int strikes;
    private int fired;
    private Vec3 last;
    private Vec3 velocity = Vec3.ZERO;

    public CrimsonLightning(UrGhastBrain brain, LivingEntity target) {
        super(brain, target);
        this.strikes = UrGhastConfig.LIGHTNING_STRIKES.get();
        this.last = target.position();
    }

    @Override
    public String name() {
        return "crimson_lightning";
    }

    @Override
    public void start() {
        this.mob.setCharging(true);
        this.sound(TFSounds.UR_GHAST_WARN.get(), this.brain.center(), 10.0F, 0.4F);
    }

    @Override
    protected boolean tick() {
        if (!this.valid()) {
            return false;
        }
        this.hover();
        Vec3 now = this.victim.position();
        Vec3 step = now.subtract(this.last);
        if (step.lengthSqr() > 4.0) {
            step = Vec3.ZERO;
        }
        this.velocity = this.velocity.scale(0.6).add(step.scale(0.4));
        this.last = now;
        this.stare(this.aim(this.victim));
        if (this.age % GAP != 0 || this.fired >= this.strikes) {
            return this.fired < this.strikes;
        }
        RandomSource random = this.mob.getRandom();
        Vec3 spot = now;
        if (this.fired % 2 == 0) {
            spot = now.add(this.velocity.x * LEAD, 0.0, this.velocity.z * LEAD);
        }
        spot = this.brain.ground(new Vec3(spot.x, now.y, spot.z));
        Vec3 sky = this.brain.center().add((random.nextDouble() - 0.5) * this.mob.getBbWidth() * 0.6, -this.mob.getBbHeight() * 0.35, (random.nextDouble() - 0.5) * this.mob.getBbWidth() * 0.6);
        this.brain.addHazard(new LightningStrike(spot, sky, RADIUS, DELAY, UrGhastConfig.CRIMSON_LIGHTNING.multiplier.get()));
        UrGhastFx.boltWarn(this.brain, spot, RADIUS, DELAY);
        this.fired++;
        return true;
    }
}
