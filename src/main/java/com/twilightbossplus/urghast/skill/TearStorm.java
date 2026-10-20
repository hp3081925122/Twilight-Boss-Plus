package com.twilightbossplus.urghast.skill;

import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.UrGhastBrain;
import com.twilightbossplus.urghast.UrGhastFx;
import com.twilightbossplus.urghast.hazard.RainCurtain;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.monster.CarminiteGhastling;

public final class TearStorm extends UrGhastSkill {
    private static final double RANGE = 40.0;
    private static final double LENGTH = 16.0;
    private static final double GAP_WIDTH = 3.0;
    private static final double OFFSET = 9.0;
    private static final double SPEED = 0.22;
    private static final int LIFE = 82;
    private static final int CLOUD = 34;

    private final Map<Integer, Integer> nextCurtain = new HashMap<>();

    public TearStorm(UrGhastBrain brain) {
        super(brain, null);
    }

    @Override
    public String name() {
        return "tear_storm";
    }

    @Override
    protected boolean tick() {
        if (!this.mob.isInTantrum()) {
            return false;
        }
        this.hover();
        if (this.age % (CLOUD - 4) == 0) {
            UrGhastFx.storm(this.brain, this.mob, CLOUD);
        }
        int interval = UrGhastConfig.STORM_INTERVAL.get();
        if (this.brain.enraged()) {
            interval = Math.max(10, (int) (interval * 0.7));
        }
        RandomSource random = this.mob.getRandom();
        for (Player player : this.brain.players(RANGE)) {
            Integer next = this.nextCurtain.get(player.getId());
            if (next == null) {
                next = 10 + random.nextInt(20);
            }
            if (this.age < next) {
                this.nextCurtain.put(player.getId(), next);
                continue;
            }
            this.nextCurtain.put(player.getId(), this.age + interval);
            double angle = random.nextDouble() * Math.PI * 2.0;
            Vec3 motion = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            Vec3 start = this.brain.ground(player.position().subtract(motion.scale(OFFSET)));
            double gap = (random.nextDouble() - 0.5) * (LENGTH - GAP_WIDTH * 2.0);
            this.brain.addHazard(new RainCurtain(start, motion, LENGTH, gap, GAP_WIDTH, SPEED, LIFE, UrGhastConfig.TEAR_STORM.multiplier.get()));
            UrGhastFx.curtain(this.brain, start, motion, LENGTH, gap, GAP_WIDTH, SPEED, LIFE);
        }
        if (this.age % 10 == 0) {
            AABB below = this.mob.getBoundingBox().move(0.0, -16.0, 0.0).inflate(0.0, 16.0, 0.0);
            for (CarminiteGhastling ghastling : this.brain.level().getEntitiesOfClass(CarminiteGhastling.class, below)) {
                ghastling.push(0.0, 1.0, 0.0);
            }
        }
        return true;
    }
}
