package com.twilightbossplus.knight.skill;

import com.twilightbossplus.common.BossSkill;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightFx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;

public abstract class KnightSkill extends BossSkill<KnightPhantom, KnightBrain> {
    protected final List<KnightPhantom> crew = new ArrayList<>();
    protected LivingEntity victim;

    protected KnightSkill(KnightBrain brain, LivingEntity target) {
        super(brain);
        this.victim = target;
    }

    @Override
    public void start() {
    }

    protected void enlist(KnightPhantom knight) {
        if (this.crew.contains(knight)) {
            return;
        }
        this.crew.add(knight);
        KnightBrain.of(knight).claim(this);
        KnightFx.reveal(this.brain, knight);
    }

    protected void dismiss(KnightPhantom knight) {
        this.crew.remove(knight);
        KnightBrain other = KnightBrain.of(knight);
        if (other.controller() == this) {
            other.release();
        }
    }

    public void keep() {
        this.crew.removeIf(knight -> !knight.isAlive() || knight.isRemoved() || KnightBrain.of(knight).controller() != this);
        for (KnightPhantom knight : this.crew) {
            KnightBrain.of(knight).stamp();
        }
        LivingEntity current = this.brain.target();
        if (current != null && (this.victim == null || !this.victim.isAlive())) {
            this.victim = current;
        }
    }

    @Override
    public void end() {
        for (KnightPhantom knight : new ArrayList<>(this.crew)) {
            this.dismiss(knight);
        }
        this.crew.clear();
    }

    protected boolean valid() {
        return this.victim != null && this.victim.isAlive() && !this.victim.isRemoved();
    }

    protected boolean active(KnightPhantom knight) {
        return knight.isAlive() && this.crew.contains(knight);
    }

    protected static void steer(KnightPhantom knight, Vec3 dest, double speed) {
        Vec3 delta = dest.subtract(knight.position());
        double length = delta.length();
        Vec3 motion = delta;
        if (length > speed) {
            motion = delta.scale(speed / length);
        }
        knight.setDeltaMovement(motion);
        knight.getMoveControl().setWantedPosition(dest.x, dest.y + 64.0, dest.z, 0.0);
        knight.noPhysics = true;
    }

    protected static void look(KnightPhantom knight, Vec3 point) {
        double dx = point.x - knight.getX();
        double dz = point.z - knight.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        knight.setYRot(yaw);
        knight.yBodyRot = yaw;
        knight.yHeadRot = yaw;
    }

    protected void sound(Holder<SoundEvent> sound, Vec3 at, float volume, float pitch) {
        this.sound(sound.value(), at, volume, pitch);
    }

    protected void sound(SoundEvent sound, Vec3 at, float volume, float pitch) {
        this.brain.level().playSound(null, at.x, at.y, at.z, sound, this.mob.getSoundSource(), volume, pitch);
    }

    protected Vec3 floor(Vec3 pos) {
        return this.brain.ground(pos);
    }

    protected static float angle(Vec3 from, Vec3 to) {
        return (float) Math.atan2(to.z - from.z, to.x - from.x);
    }

    protected static Vec3 horizontal(Vec3 from, Vec3 to) {
        Vec3 dir = new Vec3(to.x - from.x, 0.0, to.z - from.z);
        if (dir.lengthSqr() < 1.0E-6) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return dir.normalize();
    }

    protected static boolean inSector(LivingEntity entity, Vec3 center, float angle, float sweep) {
        if (sweep >= Mth.TWO_PI - 0.01F) {
            return true;
        }
        float actual = (float) Math.atan2(entity.getZ() - center.z, entity.getX() - center.x);
        float diff = Mth.wrapDegrees((float) Math.toDegrees(actual - angle));
        return Math.abs(diff) <= Math.toDegrees(sweep * 0.5F) + 10.0F;
    }
}
