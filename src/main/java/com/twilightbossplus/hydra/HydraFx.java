package com.twilightbossplus.hydra;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class HydraFx {
    public static final int WARN = 400;
    public static final int SHELL = 401;
    public static final int BURST = 402;
    public static final int POOL = 403;
    public static final int BREATH = 404;
    public static final int FAN = 405;
    public static final int BITE = 406;
    public static final int STOMP = 408;
    public static final int WAVE = 409;
    public static final int ROAR = 410;
    public static final int AURA = 411;
    public static final int REGROW = 412;
    public static final int CROWN = 413;

    private HydraFx() {
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

    public static void shell(BossBrain<?> brain, Vec3 origin, Vec3 velocity, double gravity, int flight, Vec3 landing, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "o", origin);
        BossBrain.putVec(data, "v", velocity);
        BossBrain.putVec(data, "l", landing);
        data.putDouble("g", gravity);
        data.putInt("t", flight);
        data.putFloat("r", (float) radius);
        brain.send(SHELL, data);
    }

    public static void burst(BossBrain<?> brain, Vec3 center, double radius) {
        brain.send(BURST, at(center, radius));
    }

    public static void pool(BossBrain<?> brain, Vec3 center, double radius, int duration) {
        CompoundTag data = at(center, radius);
        data.putInt("d", duration);
        brain.send(POOL, data);
    }

    public static void breath(BossBrain<?> brain, Entity hydra, int head, int windup, int duration, double length) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", hydra.getId());
        data.putInt("head", head);
        data.putInt("windup", windup);
        data.putInt("d", duration);
        data.putFloat("len", (float) length);
        brain.send(BREATH, data);
    }

    public static void fan(BossBrain<?> brain, Vec3 center, double inner, double outer, float startYaw, float sweep, int duration) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("in", (float) inner);
        data.putFloat("out", (float) outer);
        data.putFloat("yaw", startYaw);
        data.putFloat("sweep", sweep);
        data.putInt("d", duration);
        brain.send(FAN, data);
    }

    public static void bite(BossBrain<?> brain, Vec3 center, double radius, int duration) {
        CompoundTag data = at(center, radius);
        data.putInt("d", duration);
        brain.send(BITE, data);
    }

    public static void stomp(BossBrain<?> brain, Vec3 center, double radius) {
        brain.send(STOMP, at(center, radius));
    }

    public static void wave(BossBrain<?> brain, Vec3 center, double speed, double max, double thickness, int delay) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("speed", (float) speed);
        data.putFloat("max", (float) max);
        data.putFloat("th", (float) thickness);
        data.putInt("delay", delay);
        brain.send(WAVE, data);
    }

    public static void roar(BossBrain<?> brain, Entity hydra, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", hydra.getId());
        data.putInt("d", duration);
        brain.send(ROAR, data);
    }

    public static void aura(BossBrain<?> brain, Entity hydra, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", hydra.getId());
        data.putInt("d", duration);
        brain.send(AURA, data);
    }

    public static void crown(BossBrain<?> brain, Entity hydra, int windup, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", hydra.getId());
        data.putInt("windup", windup);
        data.putInt("d", duration);
        brain.send(CROWN, data);
    }

    public static void regrow(BossBrain<?> brain, Entity hydra, int head) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", hydra.getId());
        data.putInt("head", head);
        brain.send(REGROW, data);
    }
}
