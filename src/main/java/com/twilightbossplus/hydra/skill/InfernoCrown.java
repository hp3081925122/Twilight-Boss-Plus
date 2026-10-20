package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraColors;
import com.twilightbossplus.hydra.HydraFx;
import com.twilightbossplus.hydra.HydraHeads;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class InfernoCrown extends HydraSkill {
    private static final int WINDUP = 30;
    private static final int DURATION = 80;
    private static final double LENGTH = 20.0;
    private static final double BEAM_RADIUS = 1.2;
    private static final double RING = 9.5;
    private static final double RING_SWING = 2.5;
    private static final float SPIN = 2.2F;
    private static final int PULSE = 5;

    private final float[] angles = new float[HydraBrain.HEADS];
    private float spin = SPIN;
    private Vec3 center = Vec3.ZERO;
    private boolean finished;

    public InfernoCrown(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "InfernoCrown";
    }

    @Override
    public void start() {
        this.heads.addAll(this.brain.aliveHeads(0, 1, 2, 3, 4, 5, 6));
        this.center = this.center();
        int count = this.heads.size();
        if (count == 0) {
            return;
        }
        float base = yaw(this.center, this.brain.headPos(this.heads.get(0)));
        this.heads.sort((a, b) -> Float.compare(Mth.wrapDegrees(yaw(this.center, this.brain.headPos(a)) - base), Mth.wrapDegrees(yaw(this.center, this.brain.headPos(b)) - base)));
        float first = yaw(this.center, this.brain.headPos(this.heads.get(0)));
        for (int i = 0; i < count; i++) {
            this.angles[this.heads.get(i)] = first + 360.0F * i / count;
        }
        if (this.hydra.getRandom().nextBoolean()) {
            this.spin = -SPIN;
        }
        this.aimAll(0.0F);
        this.command(HydraHeads.FLAME_BEGINNING, WINDUP);
        HydraFx.warn(this.brain, this.center, RING + RING_SWING + 1.0, WINDUP, HydraColors.DANGER);
        for (int index : this.heads) {
            HydraFx.breath(this.brain, this.hydra, index, WINDUP, DURATION, LENGTH);
        }
        HydraFx.roar(this.brain, this.hydra, WINDUP);
        HydraFx.crown(this.brain, this.hydra, WINDUP, DURATION);
        this.sound(TFSounds.HYDRA_ROAR.get(), this.hydra.position(), 3.0F, 1.0F);
    }

    private void aimAll(float time) {
        double radius = RING + RING_SWING * Math.sin(time * 0.08);
        for (int index : this.heads) {
            float yaw = this.angles[index] + this.spin * time;
            Vec3 point = this.center.add(flat(yaw).scale(radius));
            point = this.brain.ground(new Vec3(point.x, this.center.y, point.z));
            Vec3 from = this.brain.headPos(index);
            this.brain.aim(index, yaw(from, point), pitch(from, point));
        }
    }

    @Override
    protected boolean tick() {
        this.heads.removeIf(index -> !this.brain.headAlive(index));
        if (this.heads.isEmpty()) {
            return false;
        }
        float time = Math.max(0, this.age - WINDUP);
        this.aimAll(time);
        if (this.age == WINDUP) {
            this.command(HydraHeads.FLAMING, DURATION + 4);
        }
        if (this.age > WINDUP && (this.age - WINDUP) % PULSE == 0) {
            for (int index : this.heads) {
                this.burn(index, LENGTH, BEAM_RADIUS, HydraConfig.INFERNO_CROWN.multiplier.get(), 4);
            }
        }
        if (this.age >= WINDUP + DURATION) {
            this.finished = true;
            this.release(HydraHeads.FLAME_ENDING, 15);
            return false;
        }
        return true;
    }

    @Override
    public void end() {
        if (!this.finished) {
            this.release(HydraHeads.FLAME_ENDING, 15);
        }
    }
}
