package com.twilightbossplus.yeti.skill;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class Leap {
    private final Mob mob;
    private final Vec3 from;
    private final Vec3 to;
    private final int flight;
    private final double apex;
    private int age;

    public Leap(Mob mob, Vec3 to, int flight, double apex) {
        this.mob = mob;
        this.from = mob.position();
        this.to = to;
        this.flight = Math.max(4, flight);
        this.apex = apex;
    }

    public Vec3 target() {
        return this.to;
    }

    public boolean step() {
        this.age++;
        this.mob.getNavigation().stop();
        this.mob.fallDistance = 0.0F;
        if (this.age > this.flight / 2 && this.mob.onGround()) {
            return true;
        }
        if (this.age > this.flight) {
            this.mob.setDeltaMovement(0.0, -0.6, 0.0);
            return this.age > this.flight + 12;
        }
        double f = this.age / (double) this.flight;
        Vec3 want = this.from.lerp(this.to, f).add(0.0, 4.0 * this.apex * f * (1.0 - f), 0.0);
        Vec3 motion = want.subtract(this.mob.position());
        if (!Double.isFinite(motion.x) || !Double.isFinite(motion.y) || !Double.isFinite(motion.z)) {
            return true;
        }
        this.mob.setDeltaMovement(motion);
        this.mob.hasImpulse = true;
        return false;
    }
}
