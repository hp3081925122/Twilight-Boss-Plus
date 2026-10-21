package com.twilightbossplus.queen.skill;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class FrostWaltz extends QueenSkill {
    private static final int DESCEND = 16;
    private static final int SPIN = 36;
    private static final int RECOVER = 10;
    private static final int BLADES = 6;
    private static final double FROM = 1.6;
    private static final double LENGTH = 2.4;
    private static final int HIT_COOLDOWN = 12;

    private final Map<Integer, Integer> lastHit = new HashMap<>();
    private Vec3 spot = Vec3.ZERO;
    private Vec3 center = Vec3.ZERO;
    private double angle;
    private double speed = 0.11;
    private double reach = 6.8;

    public FrostWaltz(QueenBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "frost_waltz";
    }

    @Override
    public void start() {
        super.start();
        if (this.brain.enraged()) {
            this.speed = 0.15;
            this.reach = 7.8;
        }
        LivingEntity target = this.target();
        Vec3 anchor = this.mob.position();
        if (target != null) {
            Vec3 back = new Vec3(this.mob.getX() - target.getX(), 0.0, this.mob.getZ() - target.getZ());
            if (back.lengthSqr() > 1.0E-4) {
                back = back.normalize();
            }
            anchor = target.position().add(back.scale(2.0));
        }
        this.spot = this.above(anchor, 0.6);
        QueenFx.pose(this.brain, this.mob, QueenFx.POSE_WALTZ, DESCEND, DESCEND + SPIN);
        this.sound(TFSounds.SNOW_QUEEN_AMBIENT.get(), this.mob.position(), 2.0F, 1.3F);
    }

    private double radius(int local) {
        float k = Mth.clamp(local / (float) SPIN, 0.0F, 1.0F);
        float tri = 1.0F - Math.abs(2.0F * k - 1.0F);
        float smooth = tri * tri * (3.0F - 2.0F * tri);
        return FROM + (this.reach - FROM) * smooth;
    }

    @Override
    protected boolean tick() {
        if (this.age < DESCEND) {
            this.brain.fly(this.spot, 0.6, 0.35);
            this.faceTarget(20.0F);
            return true;
        }
        if (this.age == DESCEND) {
            this.center = this.mob.position().add(0.0, 0.9, 0.0);
            this.angle = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
            QueenFx.waltz(this.brain, this.mob, this.center, SPIN, FROM, this.reach, BLADES, this.angle, this.speed, LENGTH);
            this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, this.center, 2.0F, 0.6F);
            this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, this.center, 2.0F, 1.4F);
        }
        this.brain.brake();
        int local = this.age - DESCEND;
        if (local < SPIN) {
            this.setYaw(this.mob.getYRot() + 24.0F);
            this.sweep(local);
            if (local % 6 == 0) {
                this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, this.center, 1.2F, 1.2F + local * 0.01F);
            }
        }
        return local < SPIN + RECOVER;
    }

    private void sweep(int local) {
        double r = this.radius(local);
        double inner = Math.max(0.3, r - LENGTH);
        double base = this.angle + this.speed * local;
        for (LivingEntity victim : this.brain.victimsAround(this.center, r + 1.0, 3.0)) {
            if (victim.getY() > this.center.y + 0.6 || victim.getY() + victim.getBbHeight() < this.center.y - 1.0) {
                continue;
            }
            double dx = victim.getX() - this.center.x;
            double dz = victim.getZ() - this.center.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            double pad = victim.getBbWidth() * 0.5;
            if (d < inner - pad || d > r + pad) {
                continue;
            }
            Integer last = this.lastHit.get(victim.getId());
            if (last != null && this.age - last < HIT_COOLDOWN) {
                continue;
            }
            double at = Math.atan2(dz, dx);
            double width = (0.3 + pad) / Math.max(0.6, d);
            boolean touched = false;
            for (int i = 0; i < BLADES; i++) {
                double blade = base + Mth.TWO_PI * i / BLADES;
                if (Math.abs(Mth.wrapDegrees(Math.toDegrees(at - blade))) <= Math.toDegrees(width)) {
                    touched = true;
                    break;
                }
            }
            if (!touched) {
                continue;
            }
            this.lastHit.put(victim.getId(), this.age);
            if (this.brain.frost(victim, SnowQueenConfig.FROST_WALTZ.multiplier.get(), this.center, 0.45, 0.25)) {
                this.sound(TFSounds.SNOW_QUEEN_ATTACK.get(), victim.position(), 1.2F, 1.1F);
            }
        }
    }
}
