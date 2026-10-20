package com.twilightbossplus.minoshroom.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.MinoBrain;
import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MyceliumSnare extends MinoSkill {
    private static final int WINDUP = 16;
    private static final double RADIUS = 1.8;

    private Vec3 center = Vec3.ZERO;

    public MyceliumSnare(MinoBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "MyceliumSnare";
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.target();
        if (target == null) {
            return;
        }
        this.center = this.brain.ground(target.position());
        MinoFx.snare(this.brain, this.center, RADIUS, WINDUP);
        this.swing();
        this.sound(SoundEvents.ROOTED_DIRT_PLACE, this.center, 2.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        if (this.center == Vec3.ZERO) {
            return false;
        }
        this.holdStill();
        this.face(this.center, 20.0F);
        if (this.age < WINDUP) {
            return true;
        }
        int root = MinoConfig.ROOT_TICKS.get();
        int caught = 0;
        for (LivingEntity victim : this.brain.victimsAround(this.center, RADIUS + 0.4, 2.5)) {
            caught++;
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, root, 6, false, true), this.mino);
            victim.setDeltaMovement(0.0, Math.min(0.0, victim.getDeltaMovement().y), 0.0);
            victim.hurtMarked = true;
            this.brain.damage(victim, MinoConfig.MYCELIUM_SNARE.multiplier.get());
            MinoFx.root(this.brain, victim, root);
        }
        MinoFx.sprout(this.brain, this.center, RADIUS);
        this.sound(SoundEvents.ROOTED_DIRT_BREAK, this.center, 2.0F, 0.6F);
        TwilightBossPlus.debug("Minoshroom {} mycelium snare caught {} targets", this.mino.getId(), caught);
        if (caught > 0) {
            this.brain.requestRush();
        }
        return false;
    }
}
