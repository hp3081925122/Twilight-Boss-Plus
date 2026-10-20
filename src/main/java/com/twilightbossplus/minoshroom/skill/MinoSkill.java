package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.minoshroom.MinoBrain;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Minoshroom;

public abstract class MinoSkill extends BossSkill<Minoshroom, MinoBrain> {
    protected final Minoshroom mino;

    protected MinoSkill(MinoBrain brain) {
        super(brain);
        this.mino = brain.mino();
    }

    @Override
    public void start() {
        super.start();
        this.mino.setCharging(false);
        this.mino.setSprinting(false);
    }

    protected void swing() {
        this.mino.swing(InteractionHand.MAIN_HAND);
    }

    protected void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
        this.brain.level().playSound(null, at.x, at.y, at.z, sound, this.mino.getSoundSource(), volume, pitch);
    }

    protected Vec3 forward() {
        return Vec3.directionFromRotation(0.0F, this.mino.getYRot());
    }

    protected void faceDirection(Vec3 dir) {
        this.setYaw((float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0));
    }
}
