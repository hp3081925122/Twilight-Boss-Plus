package com.twilightbossplus.queen.skill;

import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class MirrorShell extends QueenSkill {
    private static final int RAISE = 8;
    private static final int RECOVER = 12;

    private int shell = 30;
    private int burstAge = -1;

    public MirrorShell(QueenBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "mirror_shell";
    }

    @Override
    public void start() {
        super.start();
        if (this.brain.enraged()) {
            this.shell = 26;
        }
        QueenFx.pose(this.brain, this.mob, QueenFx.POSE_GUARD, RAISE, RAISE + this.shell);
        QueenFx.shell(this.brain, this.mob, RAISE + this.shell);
        this.sound(SoundEvents.AMETHYST_BLOCK_CHIME, this.mob.position(), 2.0F, 0.7F);
    }

    @Override
    protected boolean tick() {
        this.brain.brake();
        this.faceTarget(10.0F);
        if (this.burstAge >= 0) {
            return this.age < this.burstAge + RECOVER;
        }
        if (this.age == RAISE) {
            this.brain.setShell(true);
            this.sound(SoundEvents.GLASS_PLACE, this.mob.position(), 2.0F, 0.6F);
        }
        if (this.age > RAISE && this.brain.shellStruck()) {
            this.burst(5.5, 1.0, 0.8, 0.35);
            return true;
        }
        if (this.age >= RAISE + this.shell) {
            this.burst(3.5, 0.5, 0.4, 0.2);
        }
        return true;
    }

    private void burst(double radius, double scale, double horizontal, double vertical) {
        this.burstAge = this.age;
        this.brain.setShell(false);
        Vec3 center = this.mob.position().add(0.0, 1.2, 0.0);
        double multiplier = SnowQueenConfig.MIRROR_SHELL.multiplier.get() * scale;
        for (LivingEntity victim : this.brain.victimsAround(center, radius, 3.5)) {
            this.brain.frost(victim, multiplier, center, horizontal, vertical);
        }
        QueenFx.shellBurst(this.brain, this.mob, center, radius);
        this.brain.shake(center, (float) (0.4 * scale + 0.2), 16.0F, 8);
        this.sound(TFSounds.SNOW_QUEEN_BREAK.get(), center, 2.0F, 1.0F);
        this.sound(SoundEvents.GLASS_BREAK, center, 2.0F, 0.7F);
    }

    @Override
    public void end() {
        this.brain.setShell(false);
    }
}
