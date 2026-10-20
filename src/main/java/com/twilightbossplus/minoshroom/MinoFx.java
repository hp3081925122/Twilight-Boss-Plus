package com.twilightbossplus.minoshroom;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MinoFx {
    public static final int WARN = 300;
    public static final int TRACK = 301;
    public static final int POP = 302;
    public static final int SLAM = 303;
    public static final int DUST = 304;
    public static final int RUBBLE = 305;
    public static final int SPORE_CLOUD = 306;
    public static final int SPORE_CAP = 307;
    public static final int WHIRL = 308;
    public static final int SNARE = 309;
    public static final int ROOT = 310;
    public static final int SPROUT = 311;
    public static final int STUN = 312;
    public static final int AXE_CHARGE = 313;
    public static final int STOMP_WAVE = 314;
    public static final int AURA = 315;
    public static final int CLEAVE = 316;

    public static final int TRACK_FISSURE = 0;
    public static final int TRACK_HOOF = 1;
    public static final int CLEAVE_SWEEP = 0;
    public static final int CLEAVE_CHOP = 1;

    private MinoFx() {
    }

    private static CompoundTag at(Vec3 center, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        return data;
    }

    public static void warn(BossBrain<?> brain, Vec3 center, double radius, int duration, int color) {
        CompoundTag data = at(center, radius);
        data.putInt("d", duration);
        data.putInt("color", color);
        brain.send(WARN, data);
    }

    public static void track(BossBrain<?> brain, Entity entity, Vec3 from, Vec3 to, double width, int duration, int style) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        BossBrain.putVec(data, "a", brain.ground(from));
        BossBrain.putVec(data, "b", new Vec3(to.x, brain.groundY(to.x, to.z, from.y), to.z));
        data.putFloat("w", (float) width);
        data.putInt("d", duration);
        data.putInt("style", style);
        brain.send(TRACK, data);
    }

    public static void pop(BossBrain<?> brain, Vec3 center, double radius) {
        brain.send(POP, at(center, radius));
    }

    public static void slam(BossBrain<?> brain, Vec3 center, double radius) {
        brain.send(SLAM, at(center, radius));
    }

    public static void dust(BossBrain<?> brain, Vec3 center, double radius) {
        brain.send(DUST, at(center, radius));
    }

    public static void rubble(BossBrain<?> brain, Vec3 center, double radius, int fuse) {
        CompoundTag data = at(center, radius);
        data.putInt("d", fuse);
        brain.send(RUBBLE, data);
    }

    public static void sporeCloud(BossBrain<?> brain, Vec3 center, double radius, int duration) {
        CompoundTag data = at(center, radius);
        data.putInt("d", duration);
        brain.send(SPORE_CLOUD, data);
    }

    public static void sporeCap(BossBrain<?> brain, Entity entity, double height, double size, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("h", (float) height);
        data.putFloat("s", (float) size);
        data.putInt("d", duration);
        brain.send(SPORE_CAP, data);
    }

    public static void whirl(BossBrain<?> brain, Entity entity, double radius, int windup, int duration, int pulse) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("r", (float) radius);
        data.putInt("windup", windup);
        data.putInt("d", duration);
        data.putInt("pulse", pulse);
        brain.send(WHIRL, data);
    }

    public static void snare(BossBrain<?> brain, Vec3 center, double radius, int duration) {
        CompoundTag data = at(center, radius);
        data.putInt("d", duration);
        brain.send(SNARE, data);
    }

    public static void root(BossBrain<?> brain, Entity victim, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", victim.getId());
        data.putInt("d", duration);
        brain.send(ROOT, data);
    }

    public static void sprout(BossBrain<?> brain, Vec3 center, double radius) {
        brain.send(SPROUT, at(center, radius));
    }

    public static void stun(BossBrain<?> brain, Entity entity, double height, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("h", (float) height);
        data.putInt("d", duration);
        brain.send(STUN, data);
    }

    public static void axeCharge(BossBrain<?> brain, Entity entity, double height, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("h", (float) height);
        data.putInt("d", duration);
        brain.send(AXE_CHARGE, data);
    }

    public static void stompWave(BossBrain<?> brain, Vec3 center, double speed, double max, double thickness, int delay, int color) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("speed", (float) speed);
        data.putFloat("max", (float) max);
        data.putFloat("th", (float) thickness);
        data.putInt("delay", delay);
        data.putInt("color", color);
        brain.send(STOMP_WAVE, data);
    }

    public static void aura(BossBrain<?> brain, Entity entity, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putInt("d", duration);
        brain.send(AURA, data);
    }

    public static void cleave(BossBrain<?> brain, Entity entity, double reach, double half, int windup, int style) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("reach", (float) reach);
        data.putFloat("half", (float) half);
        data.putInt("windup", windup);
        data.putInt("style", style);
        brain.send(CLEAVE, data);
    }
}
