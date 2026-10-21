package com.twilightbossplus.queen.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.queen.QueenBrain;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.SnowQueen;

public abstract class QueenSkill extends BossSkill<SnowQueen, QueenBrain> {
    protected QueenSkill(QueenBrain brain) {
        super(brain);
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    protected void sound(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        this.sound(sound.value(), at, volume, pitch);
    }

    protected void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
        this.brain.level().playSound(null, at.x, at.y, at.z, sound, this.mob.getSoundSource(), volume, pitch);
    }

    protected void faceTarget(float step) {
        LivingEntity target = this.target();
        if (target != null) {
            this.brain.faceToward(target.position(), step);
        }
    }

    protected Vec3 above(Vec3 point, double height) {
        double floor = Math.max(point.y, this.brain.groundY(point.x, point.z, point.y + 3.0));
        return new Vec3(point.x, floor + height, point.z);
    }
}
