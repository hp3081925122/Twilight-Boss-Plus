package com.twilightbossplus.urghast.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.urghast.UrGhastBrain;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.UrGhast;

public abstract class UrGhastSkill extends BossSkill<UrGhast, UrGhastBrain> {
    @Nullable
    protected LivingEntity victim;

    protected UrGhastSkill(UrGhastBrain brain, @Nullable LivingEntity target) {
        super(brain);
        this.victim = target;
    }

    @Override
    public void start() {
    }

    protected boolean valid() {
        if (this.victim == null || !this.victim.isAlive() || this.victim.isRemoved()) {
            this.victim = this.brain.focus();
        }
        return this.victim != null;
    }

    protected void hover() {
        this.mob.setDeltaMovement(this.mob.getDeltaMovement().scale(0.6));
    }

    protected void stare(Vec3 point) {
        this.mob.getLookControl().setLookAt(point.x, point.y, point.z, 10.0F, 500.0F);
        double dx = point.x - this.mob.getX();
        double dz = point.z - this.mob.getZ();
        if (dx * dx + dz * dz > 1.0E-4) {
            this.setYaw((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        }
    }

    protected void sound(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        this.sound(sound.value(), at, volume, pitch);
    }

    protected void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
        this.brain.level().playSound(null, at.x, at.y, at.z, sound, this.mob.getSoundSource(), volume, pitch);
    }

    protected Vec3 aim(LivingEntity target) {
        return target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
    }
}
