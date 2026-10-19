package com.twilightbossplus.lich.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.network.VfxPacket;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import twilightforest.init.TFSounds;

public final class LifeDrain extends LichSkill {
    private static final int WINDUP = 12;
    private static final int CHANNEL = 60;
    private static final int PULSE = 10;
    private static final double MAX_RANGE = 20.0;

    @Nullable
    private LivingEntity victim;
    private boolean linked;

    public LifeDrain(LichBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "LifeDrain";
    }

    @Override
    public void start() {
        super.start();
        this.victim = this.target();
        this.brain.orb(this.lich, 2.4, 0.45, WINDUP + CHANNEL, LichColors.DRAIN);
        this.sound(SoundEvents.EVOKER_PREPARE_ATTACK, this.lich.position(), 1.5F, 0.7F);
    }

    @Override
    protected boolean tick() {
        if (this.victim == null || !this.victim.isAlive()) {
            return false;
        }
        this.holdStill();
        this.lookAt(this.victim);
        if (this.age < WINDUP) {
            return true;
        }
        if (!this.linked) {
            this.linked = true;
            this.brain.tether(this.lich, this.victim, CHANNEL, LichColors.DRAIN);
            this.sound(TFSounds.LICH_POP_MOB.get(), this.lich.position(), 2.0F, 0.6F);
        }
        if (!this.lich.hasLineOfSight(this.victim) || this.lich.distanceTo(this.victim) > MAX_RANGE) {
            TwilightBossPlus.debug("Lich {} life drain broken by line of sight or range", this.lich.getId());
            this.brain.tether(this.lich, this.victim, 0, LichColors.DRAIN);
            this.brain.burst(chest(this.victim), 1.2, VfxPacket.BURST_SOUL, LichColors.DRAIN);
            this.sound(SoundEvents.CHAIN_BREAK, this.victim.position(), 2.0F, 0.6F);
            return false;
        }
        if ((this.age - WINDUP) % PULSE == 0) {
            float before = this.victim.getHealth() + this.victim.getAbsorptionAmount();
            if (this.brain.damage(this.victim, LichConfig.LIFE_DRAIN.multiplier.get())) {
                float dealt = before - this.victim.getHealth() - this.victim.getAbsorptionAmount();
                float heal = (float) (Math.max(0.0F, dealt) * LichConfig.DRAIN_HEAL_RATIO.get());
                if (heal > 0.0F) {
                    this.lich.heal(heal);
                }
                TwilightBossPlus.debug("Lich {} drained {} and healed {}", this.lich.getId(), dealt, heal);
            }
            this.cast();
            this.sound(SoundEvents.SOUL_ESCAPE, this.victim.position(), 1.8F, 0.7F);
        }
        return this.age < WINDUP + CHANNEL;
    }

    @Override
    public void end() {
        if (this.victim != null && this.linked) {
            this.brain.tether(this.lich, this.victim, 0, LichColors.DRAIN);
        }
    }
}
