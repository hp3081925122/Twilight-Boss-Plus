package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class Whirlwind extends MinoSkill {
    private static final int WINDUP = 12;
    private static final int SPIN = 50;
    private static final int PULSE = 8;
    private static final double RADIUS = 3.6;
    private static final double CHASE = 0.16;
    private static final float TURN = 40.0F;

    public Whirlwind(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "Whirlwind";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        MinoFx.whirl(this.brain, this.mino, RADIUS, WINDUP, WINDUP + SPIN, PULSE);
        this.sound(TFSounds.MINOSHROOM_AMBIENT.get(), this.mino.position(), 2.0F, 1.1F);
    }

    @Override
    protected boolean tick() {
        LivingEntity target = this.target();
        if (this.age < WINDUP) {
            this.holdStill();
            if (target != null) {
                this.face(target.position(), 20.0F);
            }
            return true;
        }
        this.mino.getNavigation().stop();
        this.setYaw(this.mino.getYRot() + TURN);
        Vec3 motion = this.mino.getDeltaMovement();
        if (target != null) {
            Vec3 offset = new Vec3(target.getX() - this.mino.getX(), 0.0, target.getZ() - this.mino.getZ());
            if (offset.lengthSqr() > 1.0) {
                offset = offset.normalize().scale(CHASE);
                this.mino.setDeltaMovement(offset.x, motion.y, offset.z);
            } else {
                this.mino.setDeltaMovement(0.0, motion.y, 0.0);
            }
        }
        if ((this.age - WINDUP) % PULSE == 0) {
            Vec3 center = this.mino.position();
            for (LivingEntity victim : this.brain.victimsAround(center, RADIUS, 2.5)) {
                if (this.brain.damage(victim, MinoConfig.WHIRLWIND.multiplier.get())) {
                    this.brain.knockFrom(victim, center, 0.55, 0.2);
                }
            }
            this.swing();
            this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, center, 1.4F, 0.6F + this.mino.getRandom().nextFloat() * 0.2F);
        }
        return this.age < WINDUP + SPIN;
    }

    @Override
    public void end() {
        Vec3 motion = this.mino.getDeltaMovement();
        this.mino.setDeltaMovement(0.0, motion.y, 0.0);
    }
}
