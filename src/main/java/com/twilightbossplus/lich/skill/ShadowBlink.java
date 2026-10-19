package com.twilightbossplus.lich.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.network.VfxPacket;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class ShadowBlink extends LichSkill {
    private static final int MARK = 10;
    private static final int SWING = 6;
    private static final int CYCLE = 22;
    private static final double BEHIND = 2.4;
    private static final double REACH = 4.2;
    private static final float HALF_ANGLE = 65.0F;

    private final int blinks;
    private int done;
    private Vec3 destination = Vec3.ZERO;
    private boolean landed;

    public ShadowBlink(LichBrain brain) {
        super(brain);
        int count = LichConfig.BLINK_COUNT.get();
        if (brain.enraged()) {
            count++;
        }
        this.blinks = count;
    }

    @Override
    public String name() {
        return "ShadowBlink";
    }

    @Override
    public boolean blocksMelee() {
        return true;
    }

    @Override
    protected boolean tick() {
        LivingEntity target = this.target();
        if (target == null) {
            return false;
        }
        this.holdStill();
        int cycle = this.age % CYCLE;
        if (cycle == 0) {
            if (this.done >= this.blinks) {
                return false;
            }
            Vec3 look = Vec3.directionFromRotation(0.0F, target.getYRot());
            this.destination = this.brain.ground(target.position().subtract(look.scale(BEHIND)));
            this.landed = false;
            this.brain.circle(this.destination, 1.2, MARK, LichColors.VOID);
            this.sound(TFSounds.LICH_AMBIENT.get(), this.lich.position(), 1.2F, 1.4F);
        }
        if (cycle < MARK) {
            this.lookAt(target);
        }
        if (cycle == MARK) {
            Vec3 from = this.lich.position();
            this.landed = this.lich.randomTeleport(this.destination.x, this.destination.y + 0.5, this.destination.z, false);
            if (!this.landed) {
                TwilightBossPlus.debug("Lich {} shadow blink failed to find a landing spot", this.lich.getId());
                this.done++;
                this.age = this.age - cycle + CYCLE - 1;
                return true;
            }
            this.brain.beam(from.add(0.0, 1.2, 0.0), this.lich.position().add(0.0, 1.2, 0.0), 0.35, 0, 8, LichColors.VOID);
            this.sound(TFSounds.LICH_TELEPORT.get(), from, 1.0F, 1.0F);
            this.sound(TFSounds.LICH_TELEPORT.get(), this.lich.position(), 1.0F, 1.2F);
            this.face(target.position(), 360.0F);
            CompoundTag cone = new CompoundTag();
            cone.putInt("id", this.lich.getId());
            cone.putFloat("len", (float) REACH);
            cone.putFloat("half", HALF_ANGLE);
            cone.putInt("windup", SWING);
            cone.putInt("d", SWING + 2);
            cone.putInt("color", LichColors.DANGER);
            this.brain.send(VfxPacket.CONE, cone);
        }
        if (this.landed && cycle > MARK && cycle < MARK + SWING) {
            this.face(target.position(), 12.0F);
        }
        if (this.landed && cycle == MARK + SWING) {
            this.slash();
            this.done++;
        }
        return true;
    }

    private void slash() {
        float yaw = this.lich.getYRot();
        Vec3 facing = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 origin = this.lich.position();
        Set<Integer> hit = new HashSet<>();
        for (LivingEntity victim : this.brain.victimsAround(origin, REACH, 3.0)) {
            Vec3 offset = victim.position().subtract(origin);
            double length = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            if (length > 0.5) {
                double cos = (offset.x * facing.x + offset.z * facing.z) / length;
                if (cos < Math.cos(Math.toRadians(HALF_ANGLE))) {
                    continue;
                }
            }
            if (hit.add(victim.getId()) && this.brain.damage(victim, LichConfig.SHADOW_BLINK.multiplier.get())) {
                this.brain.knock(victim, offset.x, offset.z, 0.6, 0.25);
            }
        }
        this.cast();
        this.brain.slash(origin.add(0.0, 1.1, 0.0), yaw, REACH, HALF_ANGLE * 2.0F, LichColors.VOID);
        this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, origin, 1.6F, 0.6F);
        this.brain.shake(origin, 0.4F, 12.0F, 6);
    }
}
