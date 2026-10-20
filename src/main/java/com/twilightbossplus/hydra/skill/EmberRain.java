package com.twilightbossplus.hydra.skill;

import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.HydraBrain;
import com.twilightbossplus.hydra.HydraHeads;
import com.twilightbossplus.hydra.hazard.MagmaShell;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class EmberRain extends HydraSkill {
    private static final int WINDUP = 16;
    private static final int SPEW = 40;
    private static final double AREA = 9.0;
    private static final double RADIUS = 2.0;
    private static final float SKY_PITCH = -65.0F;
    private static final int AIMED = 3;
    private static final int POOL_TICKS = 40;

    private int shells;
    private int fired;
    private Vec3 focus = Vec3.ZERO;
    private boolean finished;

    public EmberRain(HydraBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "EmberRain";
    }

    @Override
    public void start() {
        this.heads.addAll(this.brain.aliveHeads(3, 4, 5, 6));
        if (this.heads.isEmpty()) {
            this.heads.addAll(this.brain.aliveHeads(0, 1, 2));
        }
        this.shells = HydraConfig.RAIN_SHELLS.get();
        LivingEntity target = this.target();
        if (target != null) {
            this.focus = target.position();
        }
        this.command(HydraHeads.MORTAR_BEGINNING, WINDUP);
        this.sound(TFSounds.HYDRA_ROAR.get(), this.hydra.position(), 2.5F, 1.3F);
    }

    @Override
    protected boolean tick() {
        this.heads.removeIf(index -> !this.brain.headAlive(index));
        if (this.heads.isEmpty()) {
            return false;
        }
        LivingEntity target = this.target();
        if (target != null) {
            this.focus = target.position();
        }
        for (int index : this.heads) {
            this.brain.aim(index, yaw(this.brain.headPos(index), this.focus), SKY_PITCH);
        }
        if (this.age == WINDUP) {
            this.command(HydraHeads.MORTAR_SHOOTING, SPEW + 4);
        }
        if (this.age >= WINDUP) {
            int due = Math.min(this.shells, (int) Math.ceil((this.age - WINDUP + 1) * this.shells / (double) SPEW));
            RandomSource random = this.hydra.getRandom();
            while (this.fired < due) {
                int index = this.heads.get(this.fired % this.heads.size());
                Vec3 aim = this.focus;
                if (target != null && this.fired < AIMED) {
                    aim = this.lead(target, 20.0);
                } else {
                    double angle = random.nextDouble() * Math.PI * 2.0;
                    double dist = Math.sqrt(random.nextDouble()) * AREA;
                    aim = aim.add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
                }
                int flight = 30 + random.nextInt(12);
                MagmaShell shell = new MagmaShell(this.brain.mouth(index), this.brain.ground(aim), flight, RADIUS,
                        HydraConfig.EMBER_RAIN.multiplier.get(), HydraConfig.POOL_MULTIPLIER.get(), POOL_TICKS);
                this.brain.addHazard(shell);
                shell.announce(this.brain);
                if (this.fired % 2 == 0) {
                    this.sound(TFSounds.HYDRA_SHOOT.get(), this.brain.mouth(index), 2.0F, 1.1F + random.nextFloat() * 0.3F);
                }
                this.fired++;
            }
        }
        if (this.age >= WINDUP + SPEW) {
            this.finished = true;
            this.release(HydraHeads.MORTAR_ENDING, 15);
            return false;
        }
        return true;
    }

    @Override
    public void end() {
        if (!this.finished) {
            this.release(HydraHeads.MORTAR_ENDING, 15);
        }
    }
}
