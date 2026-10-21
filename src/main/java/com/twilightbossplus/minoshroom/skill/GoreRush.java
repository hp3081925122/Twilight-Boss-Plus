package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class GoreRush extends MinoSkill {
    private static final double WIDTH = 2.6;
    private static final double SPEED = 0.95;
    private static final double MIN_LENGTH = 8.0;
    private static final double MAX_LENGTH = 22.0;
    private static final int MAX_DASH = 40;

    private final int windup;
    private final Set<Integer> hit = new HashSet<>();
    private int remaining;
    private Vec3 direction = Vec3.ZERO;
    private double length;
    private double travelled;
    private boolean dashing;
    private int timer;

    public GoreRush(MinoBrain brain, int windup, int charges) {
        super(brain);
        this.windup = windup;
        this.remaining = charges;
    }

    @Override
    public String name() {
        return "GoreRush";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        this.aim();
    }

    private boolean aim() {
        LivingEntity target = this.target();
        if (target == null) {
            this.remaining = 0;
            return false;
        }
        Vec3 from = this.mino.position();
        Vec3 offset = new Vec3(target.getX() - from.x, 0.0, target.getZ() - from.z);
        double distance = offset.length();
        if (distance < 1.0E-3) {
            offset = this.forward();
            distance = 1.0;
        }
        this.direction = offset.normalize();
        this.length = Mth.clamp(distance + 5.0, MIN_LENGTH, MAX_LENGTH);
        this.travelled = 0.0;
        this.dashing = false;
        this.timer = 0;
        this.hit.clear();
        this.remaining--;
        this.faceDirection(this.direction);
        MinoFx.track(this.brain, this.mino, from, from.add(this.direction.scale(this.length)), WIDTH, this.windup, MinoFx.TRACK_HOOF);
        this.sound(TFSounds.MINOSHROOM_AMBIENT.get(), from, 2.0F, 0.7F);
        return true;
    }

    @Override
    protected boolean tick() {
        if (this.direction == Vec3.ZERO) {
            return false;
        }
        this.timer++;
        if (!this.dashing) {
            this.holdStill();
            this.faceDirection(this.direction);
            if (this.timer % 5 == 0) {
                MinoFx.dust(this.brain, this.brain.ground(this.mino.position()), 0.8);
                this.sound(TFSounds.MINOSHROOM_STEP.get(), this.mino.position(), 1.5F, 0.6F);
            }
            if (this.timer >= this.windup) {
                this.dashing = true;
                this.timer = 0;
                this.mino.setCharging(true);
                this.mino.setSprinting(true);
                this.sound(TFSounds.MINOSHROOM_ATTACK.get(), this.mino.position(), 2.0F, 0.8F);
            }
            return true;
        }
        Vec3 motion = this.mino.getDeltaMovement();
        this.mino.setDeltaMovement(this.direction.x * SPEED, motion.y, this.direction.z * SPEED);
        this.faceDirection(this.direction);
        this.travelled += SPEED;
        for (LivingEntity victim : this.brain.victims(this.mino.getBoundingBox().inflate(0.8, 0.3, 0.8))) {
            if (!this.hit.add(victim.getId())) {
                continue;
            }
            if (this.shieldBlocked(victim)) {
                if (victim instanceof Player player) {
                    player.disableShield();
                }
                this.brain.knock(victim, this.direction.x, this.direction.z, 0.9, 0.3);
                this.sound(SoundEvents.SHIELD_BREAK, victim.position(), 1.2F, 0.8F);
                continue;
            }
            if (this.brain.damage(victim, MinoConfig.GORE_RUSH.multiplier.get())) {
                this.brain.knock(victim, this.direction.x, this.direction.z, 1.0, 0.45);
                this.brain.shake(victim.position(), 0.6F, 12.0F, 8);
            }
        }
        if (this.timer % 4 == 0) {
            MinoFx.dust(this.brain, this.brain.ground(this.mino.position()), 1.0);
        }
        boolean wall = this.timer > 3 && this.mino.horizontalCollision;
        if (wall) {
            return this.impact();
        }
        if (this.travelled >= this.length || this.timer > MAX_DASH) {
            this.stopDash();
            if (this.remaining > 0) {
                return this.aim();
            }
            return false;
        }
        return true;
    }

    private boolean impact() {
        this.stopDash();
        Vec3 at = this.mino.position().add(this.direction.scale(1.2));
        TwilightBossPlus.debug("Minoshroom {} rammed a wall and is stunned", this.mino.getId());
        MinoFx.slam(this.brain, this.brain.ground(at), 2.2);
        this.brain.shake(at, 1.0F, 18.0F, 12);
        this.sound(SoundEvents.ANVIL_LAND, at, 1.6F, 0.5F);
        this.sound(TFSounds.MINOSHROOM_HURT.get(), at, 2.0F, 0.6F);
        Vec3 motion = this.mino.getDeltaMovement();
        this.mino.setDeltaMovement(-this.direction.x * 0.4, motion.y + 0.25, -this.direction.z * 0.4);
        this.brain.requestStun();
        return false;
    }

    private void stopDash() {
        this.mino.setCharging(false);
        this.mino.setSprinting(false);
        Vec3 motion = this.mino.getDeltaMovement();
        this.mino.setDeltaMovement(motion.x * 0.2, motion.y, motion.z * 0.2);
    }

    private boolean shieldBlocked(LivingEntity victim) {
        if (!victim.isBlocking()) {
            return false;
        }
        Vec3 look = victim.getViewVector(1.0F);
        Vec3 toMino = new Vec3(this.mino.getX() - victim.getX(), 0.0, this.mino.getZ() - victim.getZ()).normalize();
        return look.x * toMino.x + look.z * toMino.z > 0.0;
    }

    @Override
    public void end() {
        this.mino.setCharging(false);
        this.mino.setSprinting(false);
    }
}
