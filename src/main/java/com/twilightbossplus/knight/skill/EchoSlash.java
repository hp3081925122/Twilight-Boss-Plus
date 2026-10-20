package com.twilightbossplus.knight.skill;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightFx;
import com.twilightbossplus.knight.hazard.SlashEcho;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.init.TFSounds;

public final class EchoSlash extends KnightSkill {
    private static final int APPROACH = 18;
    private static final int WINDUP = 9;
    private static final int RECOVER = 5;
    private static final int SLASHES = 3;
    private static final int ECHO_DELAY = 22;
    private static final float RADIUS = 4.0F;
    private static final float SPIN_RADIUS = 4.6F;
    private static final float SWEEP = (float) Math.toRadians(150.0);

    private final KnightPhantom blade;
    private int stage;
    private int timer;
    private int slash;
    private Vec3 center = Vec3.ZERO;
    private float slashAngle;
    private float slashSweep;
    private float slashRadius;

    public EchoSlash(KnightBrain brain, KnightPhantom blade, LivingEntity target) {
        super(brain, target);
        this.blade = blade;
    }

    @Override
    public String name() {
        return "EchoSlash";
    }

    @Override
    public void start() {
        this.enlist(this.blade);
        this.sound(TFSounds.KNIGHT_PHANTOM_AMBIENT.get(), this.blade.position(), 1.5F, 0.7F);
    }

    public static List<LivingEntity> sector(BossBrain<?> brain, Vec3 center, float angle, float sweep, float radius) {
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity entity : brain.victimsAround(center, radius + 0.5, 3.0)) {
            if (KnightSkill.inSector(entity, center, angle, sweep)) {
                result.add(entity);
            }
        }
        return result;
    }

    @Override
    protected boolean tick() {
        if (!this.active(this.blade) || !this.valid()) {
            return false;
        }
        this.timer++;
        Vec3 target = this.victim.position();
        Vec3 dir = horizontal(target, this.blade.position());
        if (this.stage == 0) {
            Vec3 dest = new Vec3(target.x + dir.x * 2.0, target.y, target.z + dir.z * 2.0);
            steer(this.blade, dest, 0.85);
            look(this.blade, target);
            if (this.timer >= APPROACH || (this.timer > 5 && this.blade.position().distanceTo(dest) < 1.0)) {
                this.beginWindup();
            }
            return true;
        }
        if (this.stage == 1) {
            steer(this.blade, this.blade.position(), 0.0);
            look(this.blade, this.center.add(Math.cos(this.slashAngle), 0.0, Math.sin(this.slashAngle)));
            if (this.timer >= WINDUP) {
                this.strike();
                this.stage = 2;
                this.timer = 0;
            }
            return true;
        }
        Vec3 dest = new Vec3(target.x + dir.x * 2.0, target.y, target.z + dir.z * 2.0);
        steer(this.blade, dest, 0.35);
        look(this.blade, target);
        if (this.timer >= RECOVER) {
            if (this.slash >= SLASHES) {
                return false;
            }
            this.beginWindup();
        }
        return true;
    }

    private void beginWindup() {
        this.stage = 1;
        this.timer = 0;
        this.center = this.floor(this.blade.position());
        this.slashAngle = angle(this.center, this.victim.position());
        this.slashSweep = SWEEP;
        this.slashRadius = RADIUS;
        if (this.slash == SLASHES - 1) {
            this.slashSweep = Mth.TWO_PI;
            this.slashRadius = SPIN_RADIUS;
        }
        int direction = 1;
        if (this.slash % 2 == 1) {
            direction = -1;
        }
        KnightFx.slash(this.brain, this.blade, this.center, this.slashAngle, this.slashSweep, this.slashRadius, WINDUP, false, direction);
    }

    private void strike() {
        this.slash++;
        this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, this.center, 1.4F, 0.8F + this.slash * 0.12F);
        KnightBrain owner = KnightBrain.of(this.blade);
        for (LivingEntity victim : sector(this.brain, this.center, this.slashAngle, this.slashSweep, this.slashRadius)) {
            if (owner.damage(victim, KnightConfig.ECHO_SLASH.multiplier.get())) {
                owner.knockFrom(victim, this.center, 0.35, 0.15);
            }
        }
        int direction = 1;
        if (this.slash % 2 == 0) {
            direction = -1;
        }
        owner.addHazard(new SlashEcho(this.center, this.slashAngle, this.slashSweep, this.slashRadius, ECHO_DELAY, KnightConfig.ECHO_MULTIPLIER.get()));
        KnightFx.slash(this.brain, this.blade, this.center, this.slashAngle, this.slashSweep, this.slashRadius, ECHO_DELAY, true, direction);
    }
}
