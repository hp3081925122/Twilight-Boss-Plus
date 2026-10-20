package com.twilightbossplus.urghast.skill;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBrain;
import com.twilightbossplus.urghast.UrGhastFx;
import com.twilightbossplus.urghast.hazard.HomingTear;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class TearVolley extends UrGhastSkill {
    private static final int CHARGE = 24;
    private static final int GAP = 4;
    private static final double SPEED = 0.55;
    private static final double TURN = 0.06;
    private static final int LIFE = 90;

    private final int count;
    private int fired;

    public TearVolley(UrGhastBrain brain, LivingEntity target) {
        super(brain, target);
        int tears = UrGhastConfig.VOLLEY_TEARS.get();
        if (brain.stageTwo()) {
            tears += 2;
        }
        this.count = tears;
    }

    @Override
    public String name() {
        return "tear_volley";
    }

    @Override
    public void start() {
        this.mob.setCharging(true);
        UrGhastFx.orbit(this.brain, this.mob, this.count, CHARGE + this.count * GAP, GAP);
        this.sound(TFSounds.UR_GHAST_WARN.get(), this.brain.center(), 6.0F, 0.7F);
    }

    @Override
    protected boolean tick() {
        if (!this.valid()) {
            return false;
        }
        this.hover();
        Vec3 aim = this.aim(this.victim);
        this.stare(aim);
        if (this.age < CHARGE) {
            return true;
        }
        if ((this.age - CHARGE) % GAP == 0 && this.fired < this.count) {
            this.fire(aim);
        }
        return this.fired < this.count;
    }

    private void fire(Vec3 aim) {
        RandomSource random = this.mob.getRandom();
        double angle = Math.PI * 2.0 * this.fired / this.count;
        Vec3 forward = aim.subtract(this.brain.center()).normalize();
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        right = right.normalize();
        Vec3 upward = right.cross(forward).normalize();
        Vec3 offset = right.scale(Math.cos(angle)).add(upward.scale(Math.sin(angle)));
        Vec3 from = this.brain.mouth().add(offset.scale(this.mob.getBbWidth() * 0.3));
        Vec3 dir = forward.add(offset.scale(0.55 + random.nextDouble() * 0.2)).normalize();
        int id = this.brain.nextTearId();
        this.brain.addHazard(new HomingTear(id, from, dir, SPEED, TURN, LIFE, this.victim, UrGhastConfig.TEAR_VOLLEY.multiplier.get()));
        UrGhastFx.tear(this.brain, id, from, dir, this.victim.getId(), SPEED, TURN, LIFE);
        this.sound(TFSounds.UR_GHAST_SHOOT.get(), from, 3.0F, 1.2F + random.nextFloat() * 0.3F);
        this.fired++;
    }
}
