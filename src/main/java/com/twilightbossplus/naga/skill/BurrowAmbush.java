package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.NagaColors;
import com.twilightbossplus.network.VfxPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class BurrowAmbush extends NagaSkill {
    private static final int DIVE_TICKS = 10;
    private static final int MAX_CHASE = 70;
    private static final int WARN_TICKS = 24;
    private static final int RECOVER_TICKS = 12;
    private static final double RADIUS = 3.5;
    private static final double CHASE_SPEED = 1.5;

    private int stage;
    private int stageAge;
    private Vec3 eruption = Vec3.ZERO;
    private boolean hidden;

    public BurrowAmbush(NagaBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "burrow_ambush";
    }

    @Override
    public void start() {
        super.start();
        this.brain.burst(this.naga.position(), 2.5, VfxPacket.BURST_DIRT);
        this.naga.playSound(SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.6F);
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 2.0F, 0.7F);
    }

    @Override
    public boolean invulnerable() {
        return this.hidden;
    }

    @Override
    public boolean blocksMelee() {
        return this.hidden;
    }

    @Override
    protected boolean tick() {
        this.stageAge++;
        LivingEntity target = this.target();
        switch (this.stage) {
            case 0 -> {
                this.holdStill();
                if (this.stageAge == DIVE_TICKS / 2) {
                    this.hide(true);
                }
                if (this.stageAge >= DIVE_TICKS) {
                    this.next();
                }
            }
            case 1 -> {
                if (target == null) {
                    this.lockEruption(this.naga.position());
                    return true;
                }
                if (this.stageAge % 5 == 1) {
                    this.naga.getNavigation().moveTo(target, CHASE_SPEED);
                }
                double dx = target.getX() - this.naga.getX();
                double dz = target.getZ() - this.naga.getZ();
                if (dx * dx + dz * dz < 6.25 || this.stageAge >= MAX_CHASE) {
                    this.lockEruption(target.position());
                }
            }
            case 2 -> {
                this.naga.getNavigation().moveTo(this.eruption.x, this.eruption.y, this.eruption.z, CHASE_SPEED);
                if (this.stageAge >= WARN_TICKS) {
                    this.erupt();
                }
            }
            default -> {
                if (this.stageAge >= RECOVER_TICKS) {
                    return false;
                }
            }
        }
        return true;
    }

    private void next() {
        this.stage++;
        this.stageAge = 0;
    }

    private void lockEruption(Vec3 pos) {
        this.eruption = new Vec3(pos.x, this.brain.groundY(pos.x, pos.z, pos.y), pos.z);
        this.brain.circle(this.eruption, RADIUS, WARN_TICKS, NagaColors.DANGER);
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 3.0F, 0.9F);
        this.next();
    }

    private void erupt() {
        this.naga.getNavigation().stop();
        this.naga.teleportTo(this.eruption.x, this.eruption.y, this.eruption.z);
        this.naga.setDeltaMovement(0.0, 0.85, 0.0);
        this.naga.hasImpulse = true;
        this.hide(false);
        for (LivingEntity victim : this.brain.victimsAround(this.eruption, RADIUS, 3.0)) {
            this.brain.damage(victim, NagaConfig.BURROW_AMBUSH.multiplier.get());
            this.brain.knockFrom(victim, this.eruption, 0.35, 0.75);
        }
        this.brain.burst(this.eruption, RADIUS, VfxPacket.BURST_DIRT);
        this.brain.shake(this.eruption, 1.2F, 20.0F, 12);
        this.naga.level().playSound(null, this.eruption.x, this.eruption.y, this.eruption.z, SoundEvents.GENERIC_EXPLODE, this.naga.getSoundSource(), 1.2F, 0.8F);
        this.naga.playSound(TFSounds.NAGA_HURT.get(), 2.5F, 0.6F);
        this.next();
    }

    private void hide(boolean value) {
        if (this.hidden == value) {
            return;
        }
        this.hidden = value;
        this.naga.setInvisible(value);
        CompoundTag data = new CompoundTag();
        data.putInt("id", this.naga.getId());
        data.putBoolean("on", value);
        this.brain.send(VfxPacket.BURROW, data);
    }

    @Override
    public void end() {
        this.hide(false);
        this.naga.getNavigation().stop();
    }
}
