package com.twilightbossplus.knight.skill;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightColors;
import com.twilightbossplus.knight.KnightFx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.init.TFSounds;

public final class RevenantAxe extends KnightSkill {
    private static final int WINDUP = 22;
    private static final double HIT_RADIUS = 1.3;

    private final KnightPhantom thrower;
    private final Set<Integer> hitOut = new HashSet<>();
    private final Set<Integer> hitBack = new HashSet<>();
    private final Set<Integer> hitOutTwin = new HashSet<>();
    private final Set<Integer> hitBackTwin = new HashSet<>();
    private Vec3 from = Vec3.ZERO;
    private Vec3 out = Vec3.ZERO;
    private Vec3 far = Vec3.ZERO;
    private Vec3 back = Vec3.ZERO;
    private Vec3 anchor = Vec3.ZERO;
    private int outTicks;
    private int backTicks;
    private boolean twin;

    public RevenantAxe(KnightBrain brain, KnightPhantom thrower, LivingEntity target) {
        super(brain, target);
        this.thrower = thrower;
    }

    @Override
    public String name() {
        return "RevenantAxe";
    }

    @Override
    public void start() {
        this.enlist(this.thrower);
        this.anchor = this.thrower.position();
        this.twin = this.brain.stageTwo();
        this.from = this.thrower.position().add(0.0, 1.8, 0.0);
        Vec3 aim = this.victim.position().add(0.0, 1.0, 0.0);
        Vec3 dir = horizontal(this.from, aim);
        double distance = Math.sqrt(Math.pow(aim.x - this.from.x, 2) + Math.pow(aim.z - this.from.z, 2));
        this.far = new Vec3(aim.x + dir.x * 5.0, aim.y, aim.z + dir.z * 5.0);
        Vec3 mid = this.from.add(this.far).scale(0.5);
        double side = Mth.clamp(distance * 0.45, 3.0, 8.0);
        if (this.mob.getRandom().nextBoolean()) {
            side = -side;
        }
        Vec3 perp = new Vec3(-dir.z * side, 0.0, dir.x * side);
        this.out = mid.add(perp);
        this.back = mid.subtract(perp);
        this.outTicks = Mth.clamp((int) (distance * 1.1), 14, 26);
        this.backTicks = this.outTicks;
        KnightFx.gather(this.brain, this.thrower, WINDUP, KnightColors.KNIGHTMETAL);
        KnightFx.axe(this.brain, this.thrower, this.from, this.out, this.far, this.back, WINDUP, this.outTicks, this.backTicks, this.twin);
        this.sound(TFSounds.KNIGHT_PHANTOM_AMBIENT.get(), this.anchor, 1.5F, 0.6F);
        look(this.thrower, aim);
    }

    public static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
        double u = 1.0 - t;
        return a.scale(u * u).add(b.scale(2.0 * u * t)).add(c.scale(t * t));
    }

    private Vec3 position(int tick, boolean mirrored) {
        Vec3 first = this.out;
        Vec3 second = this.back;
        if (mirrored) {
            first = this.back;
            second = this.out;
        }
        if (tick <= this.outTicks) {
            return bezier(this.from, first, this.far, tick / (double) this.outTicks);
        }
        return bezier(this.far, second, this.from, Math.min(1.0, (tick - this.outTicks) / (double) this.backTicks));
    }

    @Override
    protected boolean tick() {
        if (!this.active(this.thrower)) {
            return false;
        }
        steer(this.thrower, this.anchor, 0.3);
        if (this.age < WINDUP) {
            if (this.valid()) {
                look(this.thrower, this.victim.position());
            }
            return true;
        }
        int flight = this.age - WINDUP;
        if (flight == 0) {
            this.sound(TFSounds.KNIGHT_PHANTOM_THROW_AXE.get(), this.from, 2.0F, 0.5F);
        }
        if (flight > this.outTicks + this.backTicks) {
            this.sound(SoundEvents.ARMOR_EQUIP_IRON, this.from, 1.5F, 0.6F);
            return false;
        }
        if (flight > 0) {
            this.sweep(flight, false, this.hitOut, this.hitBack);
            if (this.twin) {
                this.sweep(flight, true, this.hitOutTwin, this.hitBackTwin);
            }
        }
        if (flight % 6 == 0) {
            this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, this.position(flight, false), 0.9F, 0.5F);
        }
        Vec3 pos = this.position(flight, false);
        look(this.thrower, pos);
        return true;
    }

    private void sweep(int flight, boolean mirrored, Set<Integer> outSet, Set<Integer> backSet) {
        Vec3 previous = this.position(flight - 1, mirrored);
        Vec3 current = this.position(flight, mirrored);
        Set<Integer> hit = outSet;
        if (flight > this.outTicks) {
            hit = backSet;
        }
        KnightBrain owner = KnightBrain.of(this.thrower);
        Vec3 motion = current.subtract(previous);
        for (LivingEntity victim : this.brain.victimsOnSegment(previous, current, HIT_RADIUS)) {
            if (!hit.add(victim.getId())) {
                continue;
            }
            if (owner.damage(victim, KnightConfig.REVENANT_AXE.multiplier.get())) {
                owner.knock(victim, motion.x, motion.z, 0.35, 0.15);
            }
        }
    }
}
