package com.twilightbossplus.knight.skill;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightColors;
import com.twilightbossplus.knight.KnightFx;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.init.TFSounds;

public final class ChainHook extends KnightSkill {
    public static final double RANGE = 18.0;
    private static final int WINDUP = 14;
    private static final double SPEED = 1.4;
    private static final int PULL = 18;
    private static final int REEL = 22;
    private static final int RECOVER = 8;

    private final KnightPhantom hooker;
    private int stage;
    private int timer;
    private Vec3 hook = Vec3.ZERO;
    private Vec3 dir = Vec3.ZERO;
    private double travelled;
    @Nullable
    private LivingEntity caught;

    public ChainHook(KnightBrain brain, KnightPhantom hooker, LivingEntity target) {
        super(brain, target);
        this.hooker = hooker;
    }

    @Override
    public String name() {
        return "ChainHook";
    }

    @Override
    public void start() {
        this.enlist(this.hooker);
        KnightFx.gather(this.brain, this.hooker, WINDUP, KnightColors.CHAIN);
        this.sound(SoundEvents.CHAIN_PLACE, this.hooker.position(), 1.5F, 0.6F);
    }

    private Vec3 hand() {
        return this.hooker.position().add(0.0, 1.6, 0.0);
    }

    @Override
    protected boolean tick() {
        if (!this.active(this.hooker)) {
            return false;
        }
        this.timer++;
        if (this.stage == 0) {
            steer(this.hooker, this.hooker.position(), 0.0);
            if (!this.valid()) {
                return false;
            }
            look(this.hooker, this.victim.position());
            if (this.timer >= WINDUP) {
                this.hook = this.hand();
                Vec3 motion = this.victim.getDeltaMovement();
                Vec3 aim = this.victim.position().add(motion.x * 4.0, this.victim.getBbHeight() * 0.5, motion.z * 4.0);
                this.dir = aim.subtract(this.hook).normalize();
                this.travelled = 0.0;
                this.stage = 1;
                this.timer = 0;
                KnightFx.hook(this.brain, this.hooker, this.hook, this.dir, SPEED, RANGE);
                this.sound(TFSounds.KNIGHT_PHANTOM_THROW_PICK.get(), this.hook, 2.0F, 0.6F);
            }
            return true;
        }
        if (this.stage == 1) {
            steer(this.hooker, this.hooker.position(), 0.0);
            Vec3 next = this.hook.add(this.dir.scale(SPEED));
            HitResult block = this.brain.level().clip(new ClipContext(this.hook, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.hooker));
            if (block.getType() != HitResult.Type.MISS) {
                next = block.getLocation();
            }
            List<LivingEntity> victims = this.brain.victimsOnSegment(this.hook, next, 0.8);
            this.hook = next;
            this.travelled += SPEED;
            if (!victims.isEmpty()) {
                this.caught = victims.get(0);
                this.stage = 2;
                this.timer = 0;
                KnightFx.tether(this.brain, this.hooker, this.caught.getId(), this.hook, PULL + RECOVER);
                this.sound(SoundEvents.CHAIN_HIT, this.hook, 2.0F, 0.7F);
                return true;
            }
            if (block.getType() != HitResult.Type.MISS || this.travelled >= RANGE) {
                this.stage = 3;
                this.timer = 0;
                KnightFx.tether(this.brain, this.hooker, -1, this.hook, REEL + RECOVER);
                this.sound(SoundEvents.CHAIN_HIT, this.hook, 1.5F, 0.5F);
            }
            return true;
        }
        if (this.stage == 2) {
            return this.pull();
        }
        if (this.stage == 3) {
            return this.reel();
        }
        steer(this.hooker, this.hooker.position(), 0.0);
        return this.timer < RECOVER;
    }

    private boolean pull() {
        LivingEntity target = this.caught;
        if (target == null || !target.isAlive()) {
            return false;
        }
        KnightBrain owner = KnightBrain.of(this.hooker);
        Vec3 toKnight = this.hooker.position().subtract(target.position());
        double distance = toKnight.length();
        double scale = owner.knockScale(target);
        if (scale > 0.0 && distance > 1.5) {
            Vec3 pull = toKnight.normalize().scale(Math.min(0.55 * scale, distance * 0.5));
            target.setDeltaMovement(pull.x, Math.max(0.05, pull.y * 0.5 + 0.08), pull.z);
            target.hurtMarked = true;
        }
        double meet = 0.25;
        if (scale <= 0.0) {
            meet = 0.9;
        }
        steer(this.hooker, target.position(), meet);
        look(this.hooker, target.position());
        if (distance < 2.6 || this.timer >= PULL) {
            Vec3 at = target.position();
            if (distance < 4.5) {
                if (owner.damage(target, KnightConfig.CHAIN_HOOK.multiplier.get())) {
                    owner.knockFrom(target, this.hooker.position(), 0.45, 0.3);
                }
            }
            KnightFx.impact(this.brain, at, 2.2, true);
            this.brain.shake(at, 0.3F, 12.0F, 8);
            this.sound(SoundEvents.ANVIL_LAND, at, 1.0F, 0.6F);
            this.stage = 4;
            this.timer = 0;
        }
        return true;
    }

    private boolean reel() {
        steer(this.hooker, this.hook, 1.0);
        look(this.hooker, this.hook);
        if (this.hooker.position().distanceTo(this.hook) < 1.4 || this.timer >= REEL) {
            Vec3 at = this.floor(this.hook);
            KnightBrain owner = KnightBrain.of(this.hooker);
            for (LivingEntity victim : this.brain.victimsAround(at, 3.0, 3.0)) {
                if (owner.damage(victim, KnightConfig.HOOK_SLAM_MULTIPLIER.get())) {
                    owner.knockFrom(victim, at, 0.45, 0.3);
                }
            }
            KnightFx.impact(this.brain, at, 3.0, true);
            this.brain.shake(at, 0.4F, 14.0F, 10);
            this.sound(SoundEvents.ANVIL_LAND, at, 1.2F, 0.5F);
            this.stage = 4;
            this.timer = 0;
        }
        return true;
    }
}
