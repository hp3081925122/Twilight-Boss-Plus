package com.twilightbossplus.yeti;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class YetiFx {
    public static final int POSE = 700;
    public static final int CLAP = 701;
    public static final int MARK = 702;
    public static final int LANDING = 703;
    public static final int SNOWBALL = 704;
    public static final int SNOWBALL_BREAK = 705;
    public static final int TILES = 706;
    public static final int SPIKES = 707;
    public static final int ROAR = 708;
    public static final int SHARDS = 709;
    public static final int LEAP = 710;

    public static final int POSE_CLAP = 0;
    public static final int POSE_CROUCH = 1;
    public static final int POSE_SCOOP = 2;
    public static final int POSE_SLAM = 3;
    public static final int POSE_ROAR = 4;

    private YetiFx() {
    }

    public static void pose(BossBrain<?> brain, Entity yeti, int kind, int windup, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", yeti.getId());
        data.putInt("kind", kind);
        data.putInt("windup", windup);
        data.putInt("d", duration);
        brain.send(POSE, data);
    }

    public static void clap(BossBrain<?> brain, Entity yeti, double reach, double half, int windup) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", yeti.getId());
        data.putFloat("reach", (float) reach);
        data.putFloat("half", (float) half);
        data.putInt("windup", windup);
        brain.send(CLAP, data);
    }

    public static void mark(BossBrain<?> brain, Vec3 spot, double radius, int delay) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", spot);
        data.putFloat("r", (float) radius);
        data.putInt("delay", delay);
        brain.send(MARK, data);
    }

    public static void leap(BossBrain<?> brain, Entity yeti, int flight) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", yeti.getId());
        data.putInt("d", flight);
        brain.send(LEAP, data);
    }

    public static void landing(BossBrain<?> brain, Vec3 spot, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", spot);
        data.putFloat("r", (float) radius);
        brain.send(LANDING, data);
    }

    public static void snowball(BossBrain<?> brain, int ball, Vec3 start, Vec3 dir, double speed, double from, double to, int life) {
        CompoundTag data = new CompoundTag();
        data.putInt("bid", ball);
        BossBrain.putVec(data, "p", start);
        BossBrain.putVec(data, "v", dir);
        data.putFloat("speed", (float) speed);
        data.putFloat("from", (float) from);
        data.putFloat("to", (float) to);
        data.putInt("life", life);
        brain.send(SNOWBALL, data);
    }

    public static void snowballBreak(BossBrain<?> brain, int ball, Vec3 pos, double radius) {
        CompoundTag data = new CompoundTag();
        data.putInt("bid", ball);
        BossBrain.putVec(data, "p", pos);
        data.putFloat("r", (float) radius);
        brain.send(SNOWBALL_BREAK, data);
    }

    public static void tiles(BossBrain<?> brain, Vec3 center, double size, int half, int parity, int warn) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("size", (float) size);
        data.putInt("half", half);
        data.putInt("parity", parity);
        data.putInt("warn", warn);
        brain.send(TILES, data);
    }

    public static void spikes(BossBrain<?> brain, Vec3 center, double size, int half, int parity) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("size", (float) size);
        data.putInt("half", half);
        data.putInt("parity", parity);
        brain.send(SPIKES, data);
    }

    public static void roar(BossBrain<?> brain, Entity yeti, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", yeti.getId());
        data.putInt("d", duration);
        brain.send(ROAR, data);
    }

    public static void shards(BossBrain<?> brain, Vec3 center, int count, double offset, double speed, double range) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putInt("n", count);
        data.putFloat("a", (float) offset);
        data.putFloat("speed", (float) speed);
        data.putFloat("range", (float) range);
        brain.send(SHARDS, data);
    }
}
