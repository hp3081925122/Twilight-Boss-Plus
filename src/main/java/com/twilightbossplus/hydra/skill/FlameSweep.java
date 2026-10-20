package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraFx;
import com.twilightbossplus.hydra.HydraHeads;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class FlameSweep extends HydraSkill {
    private static final int WINDUP = 22;
    private static final int SWEEP = 40;
    private static final double LENGTH = 18.0;
    private static final double BEAM_RADIUS = 1.3;
    private static final float HALF_ARC = 55.0F;
    private static final int PULSE = 4;

    private int head = -1;
    private float startYaw;
    private float endYaw;
    private double reach = 10.0;
    private boolean finished;

    public FlameSweep(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "FlameSweep";
    }

    @Override
    public void start() {
        LivingEntity target = this.target();
        List<Integer> front = this.brain.aliveHeads(0, 1, 2);
        if (target == null || front.isEmpty()) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (int index : front) {
            double distance = this.brain.headPos(index).distanceToSqr(target.position());
            if (distance < best) {
                best = distance;
                this.head = index;
            }
        }
        this.heads.add(this.head);
        Vec3 mouth = this.brain.mouth(this.head);
        float base = yaw(mouth, target.position());
        float dir = 1.0F;
        if (this.hydra.getRandom().nextBoolean()) {
            dir = -1.0F;
        }
        this.startYaw = base - dir * HALF_ARC;
        this.endYaw = base + dir * HALF_ARC;
        double dx = target.getX() - mouth.x;
        double dz = target.getZ() - mouth.z;
        this.reach = Mth.clamp(Math.sqrt(dx * dx + dz * dz), 6.0, 15.0);
        this.aimSweep(0.0F);
        this.command(HydraHeads.FLAME_BEGINNING, WINDUP);
        Vec3 ground = this.brain.ground(new Vec3(mouth.x, this.hydra.getY(), mouth.z));
        HydraFx.fan(this.brain, ground, 1.5, this.reach + 3.0, this.startYaw, this.endYaw - this.startYaw, WINDUP + SWEEP);
        HydraFx.breath(this.brain, this.hydra, this.head, WINDUP, SWEEP, LENGTH);
        this.sound(TFSounds.HYDRA_WARN.get(), mouth, 2.0F, 1.1F);
    }

    private void aimSweep(float progress) {
        float yaw = this.startYaw + (this.endYaw - this.startYaw) * progress;
        Vec3 from = this.brain.headPos(this.head);
        Vec3 point = from.add(flat(yaw).scale(this.reach));
        point = this.brain.ground(new Vec3(point.x, this.hydra.getY(), point.z));
        this.brain.aim(this.head, yaw, pitch(from, point));
    }

    @Override
    protected boolean tick() {
        if (this.head < 0 || !this.brain.headAlive(this.head)) {
            return false;
        }
        if (this.age < WINDUP) {
            this.aimSweep(0.0F);
            return true;
        }
        if (this.age == WINDUP) {
            this.command(HydraHeads.FLAMING, SWEEP + 4);
        }
        float progress = Mth.clamp((this.age - WINDUP) / (float) SWEEP, 0.0F, 1.0F);
        this.aimSweep(progress);
        if ((this.age - WINDUP) % PULSE == 0) {
            this.burn(this.head, LENGTH, BEAM_RADIUS, HydraConfig.FLAME_SWEEP.multiplier.get(), 4);
        }
        if (this.age >= WINDUP + SWEEP) {
            this.finished = true;
            this.release(HydraHeads.FLAME_ENDING, 15);
            return false;
        }
        return true;
    }

    @Override
    public void end() {
        if (!this.finished && this.head >= 0) {
            this.release(HydraHeads.FLAME_ENDING, 15);
        }
    }
}
