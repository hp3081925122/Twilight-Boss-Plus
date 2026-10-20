package com.twilightbossplus.lich;

import com.twilightbossplus.common.BossBrain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class LichFx {
    public static final int SIGIL = 200;
    public static final int STAR = 201;
    public static final int BEAM = 202;
    public static final int CHAIN = 203;
    public static final int ORB = 204;
    public static final int MARK = 205;
    public static final int BLAST = 206;
    public static final int FLAME = 207;
    public static final int PRISON = 208;
    public static final int VORTEX = 209;
    public static final int SLASH = 210;
    public static final int BLINK = 211;
    public static final int RUNE_WAVE = 212;

    public static final int SIGIL_HEXAGRAM = 0;
    public static final int SIGIL_PENTAGRAM = 1;
    public static final int SIGIL_SKULL = 2;
    public static final int SIGIL_RIFT = 3;

    public static final int ORB_STAR = 0;
    public static final int ORB_SOUL = 1;
    public static final int ORB_SHIELD = 2;
    public static final int ORB_VOID = 3;

    private LichFx() {
    }

    public static void sigil(BossBrain<?> brain, Vec3 center, double radius, int duration, int color, int kind) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putInt("d", duration);
        data.putInt("color", color);
        data.putInt("kind", kind);
        brain.send(SIGIL, data);
    }

    public static void star(BossBrain<?> brain, Vec3 center, double radius, double height, int fuse) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("h", (float) height);
        data.putInt("d", fuse);
        brain.send(STAR, data);
    }

    public static void beam(BossBrain<?> brain, Vec3 from, Vec3 to, double width, int windup, int duration, int color) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "a", from);
        BossBrain.putVec(data, "b", to);
        data.putFloat("w", (float) width);
        data.putInt("windup", windup);
        data.putInt("d", duration);
        data.putInt("color", color);
        brain.send(BEAM, data);
    }

    public static void chain(BossBrain<?> brain, Entity from, Entity to, int duration, int color) {
        CompoundTag data = new CompoundTag();
        data.putInt("from", from.getId());
        data.putInt("to", to.getId());
        data.putInt("d", duration);
        data.putInt("color", color);
        brain.send(CHAIN, data);
    }

    public static void orb(BossBrain<?> brain, Entity entity, double height, double size, int duration, int color, int style) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("h", (float) height);
        data.putFloat("s", (float) size);
        data.putInt("d", duration);
        data.putInt("color", color);
        data.putInt("style", style);
        brain.send(ORB, data);
    }

    public static void mark(BossBrain<?> brain, Entity entity, double radius, int duration) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("r", (float) radius);
        data.putInt("d", duration);
        brain.send(MARK, data);
    }

    public static void blast(BossBrain<?> brain, Vec3 center, double radius, int color) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putInt("color", color);
        brain.send(BLAST, data);
    }

    public static void flame(BossBrain<?> brain, Vec3 base, double radius, double height, int duration, int color) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", base);
        data.putFloat("r", (float) radius);
        data.putFloat("h", (float) height);
        data.putInt("d", duration);
        data.putInt("color", color);
        brain.send(FLAME, data);
    }

    public static void prison(BossBrain<?> brain, Vec3 center, double radius, double height, int duration) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putFloat("h", (float) height);
        data.putInt("d", duration);
        brain.send(PRISON, data);
    }

    public static void vortex(BossBrain<?> brain, Vec3 center, double radius, int duration) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("r", (float) radius);
        data.putInt("d", duration);
        brain.send(VORTEX, data);
    }

    public static void slash(BossBrain<?> brain, Entity entity, double reach, double half, int windup) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", entity.getId());
        data.putFloat("reach", (float) reach);
        data.putFloat("half", (float) half);
        data.putInt("windup", windup);
        brain.send(SLASH, data);
    }

    public static void blink(BossBrain<?> brain, Vec3 from, Vec3 to) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "a", from);
        BossBrain.putVec(data, "b", to);
        brain.send(BLINK, data);
    }

    public static void runeWave(BossBrain<?> brain, Vec3 center, double speed, double max, double thickness, int delay, int color) {
        CompoundTag data = new CompoundTag();
        BossBrain.putVec(data, "c", center);
        data.putFloat("speed", (float) speed);
        data.putFloat("max", (float) max);
        data.putFloat("th", (float) thickness);
        data.putInt("delay", delay);
        data.putInt("color", color);
        brain.send(RUNE_WAVE, data);
    }
}
