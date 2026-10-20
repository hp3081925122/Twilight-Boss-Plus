package com.twilightbossplus.urghast;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class UrGhastFx {
    public static final int ORBIT = 600;
    public static final int TEAR = 601;
    public static final int TEAR_BURST = 602;
    public static final int WAIL = 603;
    public static final int INHALE = 604;
    public static final int STORM = 605;
    public static final int CURTAIN = 606;
    public static final int PRISM = 607;
    public static final int PRISM_CYCLE = 608;
    public static final int BOLT_WARN = 609;
    public static final int BOLT = 610;
    public static final int PRISM_SHATTER = 611;

    private UrGhastFx() {
    }

    public static void orbit(BossBrain<?> brain, Entity ghast, int count, int duration, int interval) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", ghast.getId());
        data.putInt("n", count);
        data.putInt("d", duration);
        data.putInt("gap", interval);
        brain.send(ORBIT, data);
    }

    public static void tear(BossBrain<?> brain, int tear, Vec3 pos, Vec3 velocity, int target, double speed, double turn, int life) {
        CompoundTag data = new CompoundTag();
        data.putInt("tid", tear);
        BossBrain.putVec(data, "p", pos);
        BossBrain.putVec(data, "v", velocity);
        data.putInt("target", target);
        data.putFloat("speed", (float) speed);
        data.putFloat("turn", (float) turn);
        data.putInt("life", life);
        brain.send(TEAR, data);
    }

    public static void tearBurst(BossBrain<?> brain, int tear, Vec3 pos, double radius) {
        CompoundTag data = new CompoundTag();
        data.putInt("tid", tear);
        BossBrain.putVec(data, "p", pos);
        data.putFloat("r", (float) radius);
        brain.send(TEAR_BURST, data);
    }

    public static void inhale(BossBrain<?> brain, Entity ghast, Vec3 mouth, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", ghast.getId());
        BossBrain.putVec(data, "m", mouth);
        data.putInt("d", duration);
        brain.send(INHALE, data);
    }

    public static void wail(BossBrain<?> brain, Vec3 origin, Vec3 dir, float half, double speed, double max, int delay) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "o", origin);
        BossBrain.putVec(data, "v", dir);
        data.putFloat("half", half);
        data.putFloat("speed", (float) speed);
        data.putFloat("max", (float) max);
        data.putInt("delay", delay);
        brain.send(WAIL, data);
    }

    public static void storm(BossBrain<?> brain, Entity ghast, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", ghast.getId());
        data.putInt("d", duration);
        brain.send(STORM, data);
    }

    public static void curtain(BossBrain<?> brain, Vec3 start, Vec3 motion, double length, double gap, double gapWidth, double speed, int life) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "p", start);
        BossBrain.putVec(data, "v", motion);
        data.putFloat("len", (float) length);
        data.putFloat("gap", (float) gap);
        data.putFloat("gw", (float) gapWidth);
        data.putFloat("speed", (float) speed);
        data.putInt("life", life);
        brain.send(CURTAIN, data);
    }

    public static void prism(BossBrain<?> brain, Vec3 center, double radius, double angle, int duration) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("a", (float) angle);
        data.putInt("d", duration);
        brain.send(PRISM, data);
    }

    public static void prismCycle(BossBrain<?> brain, Vec3 center, double radius, double angle, int warn, int fire) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("a", (float) angle);
        data.putInt("warn", warn);
        data.putInt("fire", fire);
        brain.send(PRISM_CYCLE, data);
    }

    public static void prismShatter(BossBrain<?> brain, Vec3 center, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        brain.send(PRISM_SHATTER, data);
    }

    public static void boltWarn(BossBrain<?> brain, Vec3 spot, double radius, int delay) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", spot);
        data.putFloat("r", (float) radius);
        data.putInt("delay", delay);
        brain.send(BOLT_WARN, data);
    }

    public static void bolt(BossBrain<?> brain, Vec3 spot, Vec3 sky, double radius) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", spot);
        BossBrain.putVec(data, "s", sky);
        data.putFloat("r", (float) radius);
        brain.send(BOLT, data);
    }
}
