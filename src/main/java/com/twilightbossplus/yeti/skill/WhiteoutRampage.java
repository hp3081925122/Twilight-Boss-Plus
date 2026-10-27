package com.twilightbossplus.yeti.skill;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiFx;
import com.twilightbossplus.yeti.hazard.IceShard;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class WhiteoutRampage extends YetiSkill {
    private static final int ROAR = 24;
    private static final int CROUCH = 8;
    private static final int FLIGHT = 16;
    private static final int RECOVER = 6;
    private static final double MAX = 20.0;
    private static final int SHARDS = 10;
    private static final double SHARD_SPEED = 0.55;
    private static final double SHARD_RANGE = 12.0;

    private final int leaps;
    private int done;
    private int stage;
    private int stageStart;
    @Nullable
    private Leap leap;
    private boolean finished;

    public WhiteoutRampage(YetiBrain brain) {
        super(brain);
        int count = YetiConfig.RAMPAGE_LEAPS.get();
        if (brain.enraged()) {
            count++;
        }
        this.leaps = count;
    }

    @Override
    public String name() {
        return "whiteout_rampage";
    }

    @Override
    public void start() {
        super.start();
        this.faceTarget(360.0F);
        YetiFx.pose(this.brain, this.mob, YetiFx.POSE_ROAR, ROAR, ROAR);
        YetiFx.roar(this.brain, this.mob, ROAR + this.leaps * (CROUCH + FLIGHT + RECOVER) + 10);
        this.sound(TFSounds.ALPHA_YETI_ROAR.get(), this.mob.position(), 4.0F, 0.6F);
    }

    @Override
    protected boolean tick() {
        int local = this.age - this.stageStart;
        if (this.stage == 0) {
            this.holdStill();
            if (local == ROAR / 2) {
                Vec3 at = this.mob.position();
                for (LivingEntity victim : this.brain.victimsAround(at, 8.0, 4.0)) {
                    this.brain.knockFrom(victim, at, 0.8, 0.3);
                }
                this.brain.shake(at, 0.5F, 24.0F, 12);
            }
            if (local >= ROAR) {
                this.next(1);
            }
            return true;
        }
        if (this.stage == 1) {
            this.holdStill();
            this.faceTarget(30.0F);
            if (local == 0) {
                YetiFx.pose(this.brain, this.mob, YetiFx.POSE_CROUCH, CROUCH, CROUCH + FLIGHT);
            }
            if (local >= CROUCH) {
                LivingEntity target = this.target();
                if (target == null) {
                    this.finished = true;
                    return false;
                }
                this.leap = new Leap(this.mob, this.landing(target, MAX), FLIGHT, 3.5 + this.mob.distanceTo(target) * 0.15);
                this.face(this.leap.target(), 360.0F);
                YetiFx.mark(this.brain, this.leap.target(), AvalanchePounce.RADIUS, FLIGHT);
                YetiFx.leap(this.brain, this.mob, FLIGHT);
                this.next(2);
            }
            return true;
        }
        if (this.stage == 2) {
            if (this.leap == null || this.leap.step()) {
                Vec3 at = this.mob.position();
                this.brain.slam(at, AvalanchePounce.RADIUS, YetiConfig.WHITEOUT_RAMPAGE.multiplier.get());
                double offset = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
                Vec3 base = at;
                for (int i = 0; i < SHARDS; i++) {
                    double a = offset + Math.PI * 2.0 * i / SHARDS;
                    Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
                    this.brain.addHazard(new IceShard(base.add(dir.scale(1.5)), dir, SHARD_SPEED, SHARD_RANGE, YetiConfig.SHARD_MULTIPLIER.get()));
                }
                YetiFx.shards(this.brain, base, SHARDS, offset, SHARD_SPEED, SHARD_RANGE);
                this.next(3);
            }
            return true;
        }
        this.holdStill();
        if (local >= RECOVER) {
            this.done++;
            if (this.done >= this.leaps) {
                this.finished = true;
                return false;
            }
            this.next(1);
        }
        return true;
    }

    private void next(int stage) {
        this.stage = stage;
        this.stageStart = this.age + 1;
    }

    @Override
    public void end() {
        if (this.finished) {
            this.mob.setTired(true);
        }
    }
}
