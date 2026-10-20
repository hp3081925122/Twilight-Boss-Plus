package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoColors;
import com.twilightbossplus.minoshroom.hazard.FallingRubble;
import com.twilightbossplus.minoshroom.MinoFx;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class LabyrinthQuake extends MinoSkill {
    private static final int WINDUP = 30;
    private static final double RADIUS = 7.5;
    private static final double CLEARANCE = 0.6;
    private static final int RUBBLE_INTERVAL = 5;
    private static final double RUBBLE_RANGE = 16.0;

    private Vec3 center = Vec3.ZERO;
    private int dropped;

    public LabyrinthQuake(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "LabyrinthQuake";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        this.center = this.brain.ground(this.mino.position());
        MinoFx.warn(this.brain, this.center, RADIUS, WINDUP, MinoColors.DANGER);
        MinoFx.axeCharge(this.brain, this.mino, this.mino.getBbHeight() + 0.3, WINDUP);
        this.sound(TFSounds.MINOSHROOM_AMBIENT.get(), this.mino.position(), 2.5F, 0.5F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (target != null && this.age < WINDUP) {
            this.face(target.position(), 15.0F);
        }
        if (this.age == WINDUP - 5) {
            this.swing();
        }
        if (this.age == WINDUP) {
            for (LivingEntity victim : this.brain.victimsAround(this.center, RADIUS, 3.0)) {
                if (victim.getY() - this.brain.groundY(victim.getX(), victim.getZ(), victim.getY()) > CLEARANCE) {
                    continue;
                }
                if (this.brain.damage(victim, MinoConfig.LABYRINTH_QUAKE.multiplier.get())) {
                    this.brain.knockFrom(victim, this.center, 0.3, 0.55);
                }
            }
            MinoFx.slam(this.brain, this.center, RADIUS * 0.6);
            MinoFx.stompWave(this.brain, this.center, 1.2, RADIUS, 1.2, 0, MinoColors.EARTH);
            this.brain.shake(this.center, 1.4F, 30.0F, 18);
            this.sound(TFSounds.MINOSHROOM_SLAM.get(), this.center, 3.0F, 0.8F);
        }
        int count = MinoConfig.RUBBLE_COUNT.get();
        if (this.age > WINDUP && this.dropped < count && (this.age - WINDUP) % RUBBLE_INTERVAL == 0) {
            this.dropRubble();
        }
        return this.age < WINDUP + count * RUBBLE_INTERVAL + 4;
    }

    private void dropRubble() {
        List<LivingEntity> victims = this.brain.victimsAround(this.center, RUBBLE_RANGE, 6.0);
        Vec3 spot;
        if (!victims.isEmpty() && this.dropped % 2 == 0) {
            LivingEntity victim = victims.get(this.mino.getRandom().nextInt(victims.size()));
            Vec3 motion = victim.getDeltaMovement();
            spot = victim.position().add(motion.x * 8.0, 0.0, motion.z * 8.0);
        } else {
            double angle = this.mino.getRandom().nextDouble() * Math.PI * 2.0;
            double dist = 3.0 + this.mino.getRandom().nextDouble() * (RUBBLE_RANGE * 0.6);
            spot = this.center.add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist);
        }
        FallingRubble rubble = new FallingRubble(this.brain.ground(spot), MinoConfig.RUBBLE_MULTIPLIER.get());
        this.brain.addHazard(rubble);
        rubble.announce(this.brain);
        this.dropped++;
    }
}
