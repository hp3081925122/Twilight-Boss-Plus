package com.twilightbossplus.knight;

import com.twilightbossplus.common.BossBrain;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class KnightFx {
    public static final int REVEAL = 500;
    public static final int SLASH = 501;
    public static final int GATHER = 502;
    public static final int AXE = 503;
    public static final int HOOK = 504;
    public static final int TETHER = 505;
    public static final int IMPACT = 506;
    public static final int NOOSE = 507;
    public static final int NOOSE_BREAK = 508;
    public static final int NOOSE_SNAP = 509;
    public static final int PHALANX = 510;
    public static final int OATH = 511;
    public static final int OATH_POS = 512;
    public static final int OATH_LOCK = 513;
    public static final int OATH_FALL = 514;

    private KnightFx() {
    }

    private static int[] ids(List<? extends Entity> entities) {
        int[] result = new int[entities.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = entities.get(i).getId();
        }
        return result;
    }

    public static void reveal(BossBrain<?> brain, Entity knight) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        brain.send(REVEAL, data);
    }

    public static void slash(BossBrain<?> brain, Entity knight, Vec3 center, float angle, float sweep, float radius, int windup, boolean echo, int direction) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        BossBrain.putVec(data, "c", center);
        data.putFloat("a", angle);
        data.putFloat("sweep", sweep);
        data.putFloat("r", radius);
        data.putInt("windup", windup);
        data.putBoolean("echo", echo);
        data.putInt("dir", direction);
        brain.send(SLASH, data);
    }

    public static void gather(BossBrain<?> brain, Entity knight, int duration, int color) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        data.putInt("d", duration);
        data.putInt("color", color);
        brain.send(GATHER, data);
    }

    public static void axe(BossBrain<?> brain, Entity knight, Vec3 from, Vec3 out, Vec3 far, Vec3 back, int windup, int outTicks, int backTicks, boolean twin) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        BossBrain.putVec(data, "a", from);
        BossBrain.putVec(data, "b", out);
        BossBrain.putVec(data, "c", far);
        BossBrain.putVec(data, "e", back);
        data.putInt("windup", windup);
        data.putInt("out", outTicks);
        data.putInt("back", backTicks);
        data.putBoolean("twin", twin);
        brain.send(AXE, data);
    }

    public static void hook(BossBrain<?> brain, Entity knight, Vec3 from, Vec3 dir, double speed, double range) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        BossBrain.putVec(data, "o", from);
        BossBrain.putVec(data, "v", dir);
        data.putFloat("speed", (float) speed);
        data.putFloat("range", (float) range);
        brain.send(HOOK, data);
    }

    public static void tether(BossBrain<?> brain, Entity knight, int victim, Vec3 anchor, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        data.putInt("victim", victim);
        BossBrain.putVec(data, "p", anchor);
        data.putInt("d", duration);
        brain.send(TETHER, data);
    }

    public static void impact(BossBrain<?> brain, Vec3 center, double radius, boolean heavy) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putBoolean("heavy", heavy);
        brain.send(IMPACT, data);
    }

    public static void noose(BossBrain<?> brain, Vec3 center, List<? extends Entity> knights, int gather, int duration, double from, double to) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putIntArray("ids", ids(knights));
        data.putInt("gather", gather);
        data.putInt("d", duration);
        data.putFloat("from", (float) from);
        data.putFloat("to", (float) to);
        brain.send(NOOSE, data);
    }

    public static void nooseBreak(BossBrain<?> brain, Entity knight) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", knight.getId());
        brain.send(NOOSE_BREAK, data);
    }

    public static void nooseSnap(BossBrain<?> brain, Vec3 center, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        brain.send(NOOSE_SNAP, data);
    }

    public static void phalanx(BossBrain<?> brain, Vec3 center, float angle, List<? extends Entity> knights, float[] offsets, float gap, float back, float length, int gather, int charge) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("a", angle);
        data.putIntArray("ids", ids(knights));
        for (int i = 0; i < offsets.length; i++) {
            data.putFloat("o" + i, offsets[i]);
        }
        data.putInt("n", offsets.length);
        data.putFloat("gap", gap);
        data.putFloat("back", back);
        data.putFloat("len", length);
        data.putInt("gather", gather);
        data.putInt("charge", charge);
        brain.send(PHALANX, data);
    }

    public static void oath(BossBrain<?> brain, List<? extends Entity> knights, Vec3 sword, int gather, int form) {
        CompoundTag data = new CompoundTag();
        data.putIntArray("ids", ids(knights));
        BossBrain.putVec(data, "s", sword);
        data.putInt("gather", gather);
        data.putInt("form", form);
        brain.send(OATH, data);
    }

    public static void oathPos(BossBrain<?> brain, Vec3 sword) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "s", sword);
        brain.send(OATH_POS, data);
    }

    public static void oathLock(BossBrain<?> brain, Vec3 landing, double radius, int lock, int blades, float angle, double range) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "l", landing);
        data.putFloat("r", (float) radius);
        data.putInt("lock", lock);
        data.putInt("blades", blades);
        data.putFloat("a", angle);
        data.putFloat("range", (float) range);
        brain.send(OATH_LOCK, data);
    }

    public static void oathFall(BossBrain<?> brain, Vec3 landing, int fall, double speed) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "l", landing);
        data.putInt("fall", fall);
        data.putFloat("speed", (float) speed);
        brain.send(OATH_FALL, data);
    }
}
