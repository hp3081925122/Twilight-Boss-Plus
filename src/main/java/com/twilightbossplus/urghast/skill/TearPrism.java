package com.twilightbossplus.urghast.skill;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBrain;
import com.twilightbossplus.urghast.UrGhastFx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class TearPrism extends UrGhastSkill {
    public static final double RADIUS = 10.0;
    public static final double BEAM_HEIGHT = 1.1;
    private static final int RISE = 20;
    private static final int FIRE = 8;
    private static final int REST = 4;

    private final int cycles;
    private final int warn;
    private Vec3 center = Vec3.ZERO;
    private double angle;
    private int cycle;
    private int cycleStart;
    private final Set<Integer> hit = new HashSet<>();

    public TearPrism(UrGhastBrain brain, LivingEntity target) {
        super(brain, target);
        this.cycles = UrGhastConfig.PRISM_CYCLES.get();
        if (brain.enraged()) {
            this.warn = 12;
        } else {
            this.warn = 16;
        }
    }

    @Override
    public String name() {
        return "tear_prism";
    }

    @Override
    public void start() {
        this.mob.setCharging(true);
        this.center = this.brain.ground(this.victim.position());
        this.angle = this.mob.getRandom().nextDouble() * Math.PI;
        this.cycleStart = RISE;
        UrGhastFx.prism(this.brain, this.center, RADIUS, this.angle, RISE + this.cycles * (this.warn + FIRE + REST));
        this.sound(TFSounds.UR_GHAST_WARN.get(), this.brain.center(), 8.0F, 0.6F);
        this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, this.center, 3.0F, 0.6F);
    }

    @Override
    protected boolean tick() {
        this.hover();
        this.stare(this.center);
        if (this.age < RISE) {
            return true;
        }
        int local = this.age - this.cycleStart;
        if (local == 0) {
            if (this.cycle > 0) {
                this.angle += Math.PI / 6.0 + this.mob.getRandom().nextDouble() * Math.PI / 6.0;
            }
            this.hit.clear();
            UrGhastFx.prismCycle(this.brain, this.center, RADIUS, this.angle, this.warn, FIRE);
            this.sound(SoundEvents.AMETHYST_BLOCK_CHIME, this.center, 3.0F, 0.5F);
        }
        if (local == this.warn) {
            this.sound(SoundEvents.BEACON_POWER_SELECT, this.center, 3.0F, 1.6F);
        }
        if (local >= this.warn && local < this.warn + FIRE) {
            this.burn();
        }
        if (local >= this.warn + FIRE + REST) {
            this.cycle++;
            this.cycleStart = this.age + 1;
            if (this.cycle >= this.cycles) {
                UrGhastFx.prismShatter(this.brain, this.center, RADIUS);
                this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, this.center, 3.0F, 0.5F);
                return false;
            }
        }
        return true;
    }

    private void burn() {
        double multiplier = UrGhastConfig.TEAR_PRISM.multiplier.get();
        for (int i = 0; i < 3; i++) {
            double a = this.angle + Math.PI / 3.0 * i;
            Vec3 offset = new Vec3(Math.cos(a) * RADIUS, BEAM_HEIGHT, Math.sin(a) * RADIUS);
            Vec3 from = this.center.add(offset);
            Vec3 to = this.center.add(-offset.x, BEAM_HEIGHT, -offset.z);
            for (LivingEntity target : this.brain.victimsOnSegment(from, to, 0.5)) {
                if (!this.hit.add(target.getId())) {
                    continue;
                }
                if (this.brain.damage(target, multiplier)) {
                    this.brain.knockFrom(target, this.center, 0.2, 0.15);
                }
            }
        }
    }
}
