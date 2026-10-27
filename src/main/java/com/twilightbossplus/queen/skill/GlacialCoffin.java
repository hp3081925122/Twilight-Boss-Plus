package com.twilightbossplus.queen.skill;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import com.twilightbossplus.queen.hazard.Coffin;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class GlacialCoffin extends QueenSkill {
    private static final int WINDUP = 12;
    private static final int RECOVER = 10;
    private static final double RADIUS = 2.4;
    private static final double RANGE = 26.0;

    private int close = 34;

    public GlacialCoffin(QueenBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "glacial_coffin";
    }

    @Override
    public void start() {
        super.start();
        if (this.brain.enraged()) {
            this.close = 28;
        }
        QueenFx.pose(this.brain, this.mob, QueenFx.POSE_RAISE, WINDUP, WINDUP + RECOVER);
        this.sound(TFSounds.SNOW_QUEEN_AMBIENT.get(), this.mob.position(), 2.0F, 0.6F);
    }

    @Override
    protected boolean tick() {
        this.brain.brake();
        this.faceTarget(15.0F);
        if (this.age == WINDUP) {
            List<LivingEntity> victims = new ArrayList<>();
            LivingEntity target = this.target();
            if (target != null && !(target instanceof Player)) {
                victims.add(target);
            }
            List<Player> players = this.brain.players(RANGE);
            players.sort(Comparator.comparingDouble(player -> player.distanceToSqr(this.mob)));
            for (Player player : players) {
                if (victims.size() >= SnowQueenConfig.COFFIN_MAX_TARGETS.get()) {
                    break;
                }
                victims.add(player);
            }
            for (LivingEntity victim : victims) {
                Vec3 center = this.brain.ground(victim.position().add(0.0, 0.5, 0.0));
                double angle = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
                this.brain.addHazard(new Coffin(center, RADIUS, this.close, angle, SnowQueenConfig.GLACIAL_COFFIN.multiplier.get(), SnowQueenConfig.COFFIN_SLOW_TICKS.get()));
                QueenFx.coffin(this.brain, center, RADIUS, this.close, angle);
                this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, center, 2.0F, 0.5F);
            }
        }
        return this.age < WINDUP + RECOVER;
    }
}
