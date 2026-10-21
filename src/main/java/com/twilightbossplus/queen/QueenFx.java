package com.twilightbossplus.queen;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class QueenFx {
    public static final int POSE = 800;
    public static final int WALTZ = 801;
    public static final int SHELL = 802;
    public static final int SHELL_BURST = 803;
    public static final int TRACE = 804;
    public static final int TRACE_NODE = 805;
    public static final int COFFIN = 806;
    public static final int COFFIN_SHATTER = 807;
    public static final int COURT = 808;
    public static final int COURT_RIFT = 809;
    public static final int PULSE = 810;
    public static final int MIRRORS = 811;
    public static final int MIRROR_BEAM = 812;

    public static final int POSE_WALTZ = 0;
    public static final int POSE_GUARD = 1;
    public static final int POSE_BREATH = 2;
    public static final int POSE_RAISE = 3;

    private QueenFx() {
    }

    public static void pose(BossBrain<?> brain, Entity queen, int kind, int windup, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        data.putInt("kind", kind);
        data.putInt("windup", windup);
        data.putInt("d", duration);
        brain.send(POSE, data);
    }

    public static void waltz(BossBrain<?> brain, Entity queen, Vec3 center, int spin, double from, double to, int blades, double angle, double speed, double length) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        BossBrain.putVec(data, "c", center);
        data.putInt("spin", spin);
        data.putFloat("from", (float) from);
        data.putFloat("to", (float) to);
        data.putInt("n", blades);
        data.putFloat("a", (float) angle);
        data.putFloat("speed", (float) speed);
        data.putFloat("len", (float) length);
        brain.send(WALTZ, data);
    }

    public static void shell(BossBrain<?> brain, Entity queen, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        data.putInt("d", duration);
        brain.send(SHELL, data);
    }

    public static void shellBurst(BossBrain<?> brain, Entity queen, Vec3 center, double radius) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        brain.send(SHELL_BURST, data);
    }

    public static void trace(BossBrain<?> brain, Entity queen, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        data.putInt("d", duration);
        brain.send(TRACE, data);
    }

    public static void traceNode(BossBrain<?> brain, Entity queen, Vec3 pos, int delay, double radius) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        BossBrain.putVec(data, "p", pos);
        data.putInt("delay", delay);
        data.putFloat("r", (float) radius);
        brain.send(TRACE_NODE, data);
    }

    public static void coffin(BossBrain<?> brain, Vec3 center, double radius, int close, double angle) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putInt("close", close);
        data.putFloat("a", (float) angle);
        brain.send(COFFIN, data);
    }

    public static void coffinShatter(BossBrain<?> brain, Vec3 center, double radius, double angle) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("a", (float) angle);
        brain.send(COFFIN_SHATTER, data);
    }

    public static void court(BossBrain<?> brain, Entity queen, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", queen.getId());
        data.putInt("d", duration);
        brain.send(COURT, data);
    }

    public static void courtRift(BossBrain<?> brain, Vec3 pos) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "p", pos);
        brain.send(COURT_RIFT, data);
    }

    public static void pulse(BossBrain<?> brain, Vec3 center, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        brain.send(PULSE, data);
    }

    public static void mirrors(BossBrain<?> brain, Vec3 center, double radius, double angle, double height, int life) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("a", (float) angle);
        data.putFloat("h", (float) height);
        data.putInt("life", life);
        brain.send(MIRRORS, data);
    }

    public static void mirrorBeam(BossBrain<?> brain, Vec3 from, Vec3 to, int warn, int fire, int tint) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "a", from);
        BossBrain.putVec(data, "b", to);
        data.putInt("warn", warn);
        data.putInt("fire", fire);
        data.putInt("tint", tint);
        brain.send(MIRROR_BEAM, data);
    }
}
