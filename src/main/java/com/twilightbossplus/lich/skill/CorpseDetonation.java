package com.twilightbossplus.lich.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.lich.LichFx;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.monster.LichMinion;
import twilightforest.init.TFSounds;

public final class CorpseDetonation extends LichSkill {
    private static final double RADIUS = 4.0;

    private final int fuse;
    @Nullable
    private LichMinion marked;

    public CorpseDetonation(LichBrain brain) {
        super(brain);
        this.fuse = LichConfig.CORPSE_FUSE.get();
    }

    @Override
    public String name() {
        return "CorpseDetonation";
    }

    @Override
    public void start() {
        super.start();
        List<LichMinion> minions = this.brain.minions();
        LivingEntity target = this.target();
        double best = Double.MAX_VALUE;
        for (LichMinion minion : minions) {
            double score = minion.distanceToSqr(this.lich);
            if (target != null) {
                score = minion.distanceToSqr(target);
            }
            if (score < best) {
                best = score;
                this.marked = minion;
            }
        }
        if (this.marked == null) {
            return;
        }
        this.marked.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, this.fuse, 3, false, false));
        this.marked.addEffect(new MobEffectInstance(MobEffects.GLOWING, this.fuse, 0, false, false));
        LichFx.chain(this.brain, this.lich, this.marked, this.fuse, LichColors.DRAIN);
        LichFx.mark(this.brain, this.marked, RADIUS, this.fuse);
        this.cast();
        this.sound(TFSounds.LICH_POP_MOB.get(), this.marked.position(), 2.0F, 0.5F);
    }

    @Override
    protected boolean tick() {
        if (this.marked == null) {
            return false;
        }
        this.holdStill();
        this.face(this.marked.position(), 20.0F);
        if (!this.marked.isAlive()) {
            TwilightBossPlus.debug("Lich {} lost the marked minion before detonation", this.lich.getId());
            LichFx.chain(this.brain, this.lich, this.marked, 0, LichColors.DRAIN);
            return false;
        }
        if (this.age % 10 == 0) {
            this.sound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), this.marked.position(), 1.5F, 0.5F + this.age / (float) this.fuse);
        }
        if (this.age >= this.fuse) {
            Vec3 center = this.marked.position();
            for (LivingEntity victim : this.brain.victimsAround(center, RADIUS, 3.0)) {
                if (this.brain.damage(victim, LichConfig.CORPSE_DETONATION.multiplier.get())) {
                    this.brain.knockFrom(victim, center, 0.8, 0.4);
                }
            }
            Vec3 ground = this.brain.ground(center);
            LichFx.blast(this.brain, ground, RADIUS, LichColors.DANGER);
            LichFx.flame(this.brain, ground, 1.4, 4.5, 14, LichColors.DRAIN);
            this.brain.shake(center, 0.9F, 18.0F, 10);
            this.sound(SoundEvents.GENERIC_EXPLODE, center, 1.6F, 0.8F);
            this.sound(TFSounds.MINION_DEATH.get(), center, 1.6F, 0.6F);
            this.marked.discard();
            return false;
        }
        return true;
    }
}
