package com.twilightbossplus.common;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public abstract class BossSkill<E extends Mob, B extends BossBrain<E>> {
    protected final B brain;
    protected final E mob;
    protected int age;

    protected BossSkill(B brain) {
        this.brain = brain;
        this.mob = brain.mob();
    }

    public abstract String name();

    public void start() {
        this.mob.getNavigation().stop();
    }

    public final boolean update() {
        boolean running = this.tick();
        this.age++;
        return running;
    }

    protected abstract boolean tick();

    public void end() {
    }

    public boolean invulnerable() {
        return false;
    }

    public boolean blocksMelee() {
        return false;
    }

    protected void holdStill() {
        this.mob.getNavigation().stop();
        Vec3 motion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(0.0, motion.y, 0.0);
    }

    protected void face(Vec3 point, float maxStep) {
        double dx = point.x - this.mob.getX();
        double dz = point.z - this.mob.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float wanted = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float current = this.mob.getYRot();
        float diff = Mth.wrapDegrees(wanted - current);
        float step = Mth.clamp(diff, -maxStep, maxStep);
        this.setYaw(current + step);
    }

    protected void setYaw(float yaw) {
        this.mob.setYRot(yaw);
        this.mob.yBodyRot = yaw;
        this.mob.yHeadRot = yaw;
    }

    protected LivingEntity target() {
        return this.brain.target();
    }
}
