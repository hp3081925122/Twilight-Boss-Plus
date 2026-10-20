package com.twilightbossplus.naga;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class NagaFx {
    public static final int WARN = 100;
    public static final int LUNGE = 101;
    public static final int GLOB = 102;
    public static final int POOL = 103;
    public static final int BREATH = 104;
    public static final int QUAKE = 105;
    public static final int LASH = 106;
    public static final int BURROW = 107;
    public static final int ERUPT = 108;
    public static final int HISS = 109;
    public static final int AURA = 110;
    public static final int BITE = 111;

    private NagaFx() {
    }

    public static void warn(BossBrain<?> brain, Vec3 center, double radius, int duration, int color) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putInt("d", duration);
        data.putInt("color", color);
        brain.send(WARN, data);
    }

    public static void lunge(BossBrain<?> brain, Vec3 from, Vec3 to, double width, int duration) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "a", from);
        BossBrain.putVec(data, "b", to);
        data.putFloat("w", (float) width);
        data.putInt("d", duration);
        brain.send(LUNGE, data);
    }

    public static void glob(BossBrain<?> brain, Vec3 origin, Vec3 velocity, double gravity, int flight, Vec3 landing, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "o", origin);
        BossBrain.putVec(data, "v", velocity);
        BossBrain.putVec(data, "l", landing);
        data.putDouble("g", gravity);
        data.putInt("t", flight);
        data.putFloat("r", (float) radius);
        brain.send(GLOB, data);
    }

    public static void pool(BossBrain<?> brain, Vec3 center, double radius, int duration) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putInt("d", duration);
        brain.send(POOL, data);
    }

    public static void breath(BossBrain<?> brain, Entity naga, double length, double half, int windup, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", naga.getId());
        data.putFloat("len", (float) length);
        data.putFloat("half", (float) half);
        data.putInt("windup", windup);
        data.putInt("d", duration);
        brain.send(BREATH, data);
    }

    public static void quake(BossBrain<?> brain, Vec3 center, double radius, double speed, double max, int[] delays) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("speed", (float) speed);
        data.putFloat("max", (float) max);
        data.putIntArray("delays", delays);
        brain.send(QUAKE, data);
    }

    public static void lash(BossBrain<?> brain, Vec3 pivot, double radius, float yaw) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", pivot);
        data.putFloat("r", (float) radius);
        data.putFloat("yaw", yaw);
        brain.send(LASH, data);
    }

    public static void burrow(BossBrain<?> brain, Entity naga, boolean on) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", naga.getId());
        data.putBoolean("on", on);
        brain.send(BURROW, data);
    }

    public static void erupt(BossBrain<?> brain, Vec3 center, double radius, boolean big) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putBoolean("big", big);
        brain.send(ERUPT, data);
    }

    public static void hiss(BossBrain<?> brain, Vec3 center, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        brain.send(HISS, data);
    }

    public static void aura(BossBrain<?> brain, Entity naga, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", naga.getId());
        data.putInt("d", duration);
        brain.send(AURA, data);
    }

    public static void bite(BossBrain<?> brain, Entity naga, double reach, double half, int windup, int interval, int snaps) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", naga.getId());
        data.putFloat("reach", (float) reach);
        data.putFloat("half", (float) half);
        data.putInt("windup", windup);
        data.putInt("interval", interval);
        data.putInt("snaps", snaps);
        brain.send(BITE, data);
    }
}
