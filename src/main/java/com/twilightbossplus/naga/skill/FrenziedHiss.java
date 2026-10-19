package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaColors;
import com.twilightbossplus.network.VfxPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class FrenziedHiss extends NagaSkill {
    private static final int BLAST_TICK = 12;
    private static final int DURATION = 32;
    private static final double RADIUS = 10.0;

    public FrenziedHiss(NagaBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "frenzied_hiss";
    }

    @Override
    public void start() {
        super.start();
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 5.0F, 0.55F);
        CompoundTag aura = new CompoundTag();
        aura.putInt("id", this.naga.getId());
        aura.putInt("duration", 120);
        this.brain.send(VfxPacket.AURA, aura);
        Vec3 pos = this.naga.position();
        this.brain.circle(new Vec3(pos.x, this.brain.groundY(pos.x, pos.z, pos.y), pos.z), RADIUS, BLAST_TICK, NagaColors.RAGE);
    }

    @Override
    public boolean invulnerable() {
        return true;
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age == BLAST_TICK) {
            Vec3 pos = this.naga.position();
            Vec3 center = new Vec3(pos.x, this.brain.groundY(pos.x, pos.z, pos.y), pos.z);
            for (LivingEntity victim : this.brain.victimsAround(center, RADIUS, 4.0)) {
                this.brain.damage(victim, NagaConfig.FRENZIED_HISS.multiplier.get());
                this.brain.knockFrom(victim, center, 0.9, 0.4);
            }
            this.brain.ring(center, 1.1, RADIUS, 1.6, 0, NagaColors.RAGE);
            this.brain.shake(center, 1.4F, 28.0F, 18);
            this.naga.playSound(TFSounds.NAGA_HISS.get(), 5.0F, 0.5F);
        }
        return this.age < DURATION;
    }
}
