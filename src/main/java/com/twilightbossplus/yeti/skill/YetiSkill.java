package com.twilightbossplus.yeti.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.yeti.YetiBrain;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.AlphaYeti;

public abstract class YetiSkill extends BossSkill<AlphaYeti, YetiBrain> {
    protected YetiSkill(YetiBrain brain) {
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

    protected Vec3 forward() {
        return Vec3.directionFromRotation(0.0F, this.mob.getYRot());
    }

    protected void faceTarget(float step) {
        LivingEntity target = this.target();
        if (target != null) {
            this.face(target.position(), step);
        }
    }

    protected Vec3 landing(LivingEntity target, double max) {
        Vec3 from = this.mob.position();
        Vec3 flat = new Vec3(target.getX() - from.x, 0.0, target.getZ() - from.z);
        double distance = flat.length();
        if (distance > max) {
            flat = flat.scale(max / distance);
        }
        return this.brain.ground(new Vec3(from.x + flat.x, target.getY(), from.z + flat.z));
    }
}
