package com.twilightbossplus.lich.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.lich.LichBrain;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Lich;

public abstract class LichSkill extends BossSkill<Lich, LichBrain> {
    protected final Lich lich;

    protected LichSkill(LichBrain brain) {
        super(brain);
        this.lich = brain.lich();
    }

    protected void lookAt(LivingEntity target) {
        this.face(target.position(), 30.0F);
        this.lich.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    protected void cast() {
        this.lich.swing(InteractionHand.MAIN_HAND);
    }

    protected void sound(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        this.sound(sound.value(), at, volume, pitch);
    }

    protected void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
        this.brain.level().playSound(null, at.x, at.y, at.z, sound, this.lich.getSoundSource(), volume, pitch);
    }

    protected Vec3 clip(Vec3 from, Vec3 to) {
        return this.brain.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.lich)).getLocation();
    }

    protected static Vec3 chest(LivingEntity entity) {
        return entity.position().add(0.0, entity.getBbHeight() * 0.55, 0.0);
    }
}
