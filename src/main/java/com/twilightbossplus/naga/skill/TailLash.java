package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaColors;
import com.twilightbossplus.naga.NagaFx;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class TailLash extends NagaSkill {
    private static final double RADIUS = 5.5;
    private static final double TRIGGER_RANGE = 4.5;
    private static final double HEAD_CLEARANCE = 7.0;
    private static final int WINDUP = 14;

    private Vec3 pivot = Vec3.ZERO;

    public TailLash(NagaBrain brain) {
        super(brain);
    }

    @Nullable
    public static Vec3 tailPivot(NagaBrain brain) {
        List<Entity> segments = brain.activeSegments();
        if (segments.size() < 4) {
            return null;
        }
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        int count = Math.min(3, segments.size());
        for (int i = segments.size() - count; i < segments.size(); i++) {
            Entity segment = segments.get(i);
            x += segment.getX();
            y += segment.getY();
            z += segment.getZ();
        }
        return new Vec3(x / count, y / count, z / count);
    }

    public static boolean shouldTrigger(NagaBrain brain) {
        Vec3 pivot = tailPivot(brain);
        if (pivot == null) {
            return false;
        }
        for (LivingEntity victim : brain.victimsAround(pivot, TRIGGER_RANGE, 3.0)) {
            if (victim.distanceTo(brain.naga()) > HEAD_CLEARANCE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return "tail_lash";
    }

    @Override
    public void start() {
        super.start();
        Vec3 found = tailPivot(this.brain);
        if (found == null) {
            found = this.naga.position();
        }
        this.pivot = new Vec3(found.x, this.brain.groundY(found.x, found.z, found.y), found.z);
        NagaFx.warn(this.brain, this.pivot, RADIUS, WINDUP, NagaColors.WARNING);
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 2.0F, 1.4F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age == WINDUP) {
            for (LivingEntity victim : this.brain.victimsAround(this.pivot, RADIUS, 3.0)) {
                this.brain.damage(victim, NagaConfig.TAIL_LASH.multiplier.get());
                this.brain.knockFrom(victim, this.pivot, 0.7, 0.35);
            }
            NagaFx.lash(this.brain, this.pivot, RADIUS, this.naga.getYRot());
            this.naga.level().playSound(null, this.pivot.x, this.pivot.y, this.pivot.z, SoundEvents.PLAYER_ATTACK_SWEEP, this.naga.getSoundSource(), 2.0F, 0.5F);
        }
        return this.age < WINDUP + 6;
    }
}
