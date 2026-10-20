package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraHeads;
import com.twilightbossplus.hydra.hazard.MagmaShell;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class MagmaVolley extends HydraSkill {
    private static final int WINDUP = 20;
    private static final int INTERVAL = 8;
    private static final int FLIGHT = 26;
    private static final double RADIUS = 2.4;
    private static final double SPREAD = 2.8;
    private static final float LOB_PITCH = -30.0F;

    private int shells;
    private int finish;
    private Vec3 lastAim = Vec3.ZERO;

    public MagmaVolley(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "MagmaVolley";
    }

    @Override
    public void start() {
        List<Integer> alive = this.brain.aliveHeads(0, 1, 2, 3, 4, 5, 6);
        int count = 2;
        if (this.brain.enraged()) {
            count = 3;
        }
        RandomSource random = this.hydra.getRandom();
        while (this.heads.size() < count && !alive.isEmpty()) {
            this.heads.add(alive.remove(random.nextInt(alive.size())));
        }
        this.shells = HydraConfig.VOLLEY_SHELLS.get();
        this.finish = WINDUP + this.shells * INTERVAL + this.heads.size() * 3;
        LivingEntity target = this.target();
        if (target != null) {
            this.lastAim = target.position();
        }
        this.command(HydraHeads.MORTAR_BEGINNING, WINDUP);
        this.sound(TFSounds.HYDRA_GROWL.get(), this.hydra.position(), 2.0F, 0.8F);
    }

    @Override
    protected boolean tick() {
        if (this.heads.isEmpty()) {
            return false;
        }
        LivingEntity target = this.target();
        if (target != null) {
            this.lastAim = target.position();
        }
        for (int index : this.heads) {
            this.brain.aim(index, yaw(this.brain.headPos(index), this.lastAim), LOB_PITCH);
        }
        if (this.age == WINDUP) {
            this.command(HydraHeads.MORTAR_SHOOTING, this.finish - WINDUP + 4);
        }
        if (this.age >= WINDUP && target != null) {
            RandomSource random = this.hydra.getRandom();
            for (int k = 0; k < this.heads.size(); k++) {
                int local = this.age - WINDUP - k * 3;
                if (local < 0 || local % INTERVAL != 0 || local / INTERVAL >= this.shells) {
                    continue;
                }
                int index = this.heads.get(k);
                Vec3 aim = this.lead(target, FLIGHT * 0.6);
                if (local > 0) {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double dist = SPREAD * (0.4 + random.nextDouble() * 0.6);
                    aim = aim.add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                }
                MagmaShell shell = new MagmaShell(this.brain.mouth(index), this.brain.ground(aim), FLIGHT, RADIUS,
                        HydraConfig.MAGMA_VOLLEY.multiplier.get(), HydraConfig.POOL_MULTIPLIER.get(), HydraConfig.POOL_TICKS.get());
                this.brain.addHazard(shell);
                shell.announce(this.brain);
                this.sound(TFSounds.HYDRA_SHOOT.get(), this.brain.mouth(index), 2.5F, 0.9F + random.nextFloat() * 0.2F);
            }
        }
        if (this.age >= this.finish) {
            this.release(HydraHeads.MORTAR_ENDING, 15);
            return false;
        }
        return true;
    }

    @Override
    public void end() {
        if (this.age < this.finish) {
            this.release(HydraHeads.MORTAR_ENDING, 15);
        }
    }
}
