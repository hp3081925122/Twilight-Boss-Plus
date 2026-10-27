package com.twilightbossplus.queen.skill;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class AuroraMirrors extends QueenSkill {
    private static final int RISE = 16;
    private static final int STEP = 6;
    private static final int WARN = 16;
    private static final int FIRE = 6;
    private static final int GAP = 10;
    private static final int RECOVER = 14;
    private static final double RADIUS = 9.0;
    private static final double HEIGHT = 1.1;
    private static final int[][] HEXAGRAM = {{-1, 0}, {0, 2}, {2, 4}, {4, 0}, {-1, 1}, {1, 3}, {3, 5}, {5, 1}};
    private static final int[][] HEXAGON = {{-1, 0}, {0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 0}};

    private final List<Beam> beams = new ArrayList<>();
    private Vec3 center = Vec3.ZERO;
    private Vec3 spot = Vec3.ZERO;
    private int finish;

    public AuroraMirrors(QueenBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "aurora_mirrors";
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        Vec3 around = this.mob.position();
        if (target != null) {
            around = target.position();
        }
        this.center = this.brain.ground(around.add(0.0, 0.5, 0.0));
        this.spot = this.center.add(0.0, 7.0, 0.0);
        QueenFx.pose(this.brain, this.mob, QueenFx.POSE_RAISE, RISE, RISE + 20);
        this.sound(TFSounds.SNOW_QUEEN_AMBIENT.get(), this.mob.position(), 3.0F, 0.5F);
    }

    private void plan() {
        double offset = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
        Vec3[] mirrors = new Vec3[6];
        for (int i = 0; i < 6; i++) {
            double a = offset + Mth.TWO_PI * i / 6.0;
            double x = this.center.x + Math.cos(a) * RADIUS;
            double z = this.center.z + Math.sin(a) * RADIUS;
            mirrors[i] = new Vec3(x, this.brain.groundY(x, z, this.center.y + 2.0) + HEIGHT, z);
        }
        Vec3 crown = this.spot.add(0.0, 1.5, 0.0);
        int start = 20;
        int cycles = SnowQueenConfig.MIRROR_CYCLES.get();
        for (int c = 0; c < cycles; c++) {
            int[][] order = HEXAGRAM;
            if (c % 2 == 1) {
                order = HEXAGON;
            }
            for (int j = 0; j < order.length; j++) {
                Vec3 from = crown;
                if (order[j][0] >= 0) {
                    from = mirrors[order[j][0]];
                }
                this.beams.add(new Beam(from, mirrors[order[j][1]], start + j * STEP, c % 2));
            }
            start += (order.length - 1) * STEP + WARN + FIRE + GAP;
        }
        this.finish = start - GAP + RECOVER;
        QueenFx.mirrors(this.brain, this.center, RADIUS, offset, HEIGHT, this.finish);
        this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, this.center, 3.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        if (this.age < RISE) {
            this.brain.fly(this.spot, 0.5, 0.3);
            this.faceTarget(15.0F);
            return true;
        }
        this.brain.fly(this.spot, 0.2, 0.2);
        int local = this.age - RISE;
        if (local == 0) {
            this.plan();
        }
        double multiplier = SnowQueenConfig.AURORA_MIRRORS.multiplier.get();
        for (Beam beam : this.beams) {
            if (local == beam.start) {
                QueenFx.mirrorBeam(this.brain, beam.from, beam.to, WARN, FIRE, beam.tint);
                this.sound(SoundEvents.AMETHYST_BLOCK_CHIME, beam.to, 1.6F, 0.8F + beam.tint * 0.3F);
            }
            if (local == beam.start + WARN) {
                this.sound(SoundEvents.BEACON_DEACTIVATE, beam.to, 1.4F, 1.6F);
                this.brain.faceToward(beam.to, 30.0F);
            }
            if (local >= beam.start + WARN && local < beam.start + WARN + FIRE) {
                for (LivingEntity victim : this.brain.victimsOnSegment(beam.from, beam.to, 0.55)) {
                    if (beam.hit.add(victim.getId())) {
                        this.brain.frost(victim, multiplier, beam.from, 0.3, 0.2);
                    }
                }
            }
        }
        return local < this.finish;
    }

    private static final class Beam {
        final Vec3 from;
        final Vec3 to;
        final int start;
        final int tint;
        final Set<Integer> hit = new HashSet<>();

        Beam(Vec3 from, Vec3 to, int start, int tint) {
            this.from = from;
            this.to = to;
            this.start = start;
            this.tint = tint;
        }
    }
}
