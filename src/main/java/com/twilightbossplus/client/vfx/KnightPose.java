package com.twilightbossplus.client.vfx;

import com.twilightbossplus.knight.KnightFx;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class KnightPose {
    public static final int SLASH = 0;
    public static final int RAISE = 1;
    public static final int THROW = 2;
    public static final int LANCE = 3;
    public static final int OATH = 4;
    public static final int CHAINS = 5;
    private static final int RECOVER = 10;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private KnightPose() {
    }

    private static void put(ClientLevel level, int id, int kind, int windup, int duration, int dir) {
        Entity entity = level.getEntity(id);
        if (entity == null) {
            return;
        }
        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        STATES.put(id, new State(kind, entity.tickCount, Math.max(1, windup), Math.max(windup, duration), dir));
    }

    static void record(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case KnightFx.SLASH -> {
                if (!d.getBoolean("echo")) {
                    put(level, d.getInt("id"), SLASH, d.getInt("windup"), d.getInt("windup") + 6, d.getInt("dir"));
                }
            }
            case KnightFx.GATHER -> put(level, d.getInt("id"), RAISE, d.getInt("d"), d.getInt("d"), 1);
            case KnightFx.AXE -> put(level, d.getInt("id"), THROW, d.getInt("windup"), d.getInt("windup") + d.getInt("out") + d.getInt("back"), 1);
            case KnightFx.HOOK -> put(level, d.getInt("id"), THROW, 1, 14, 1);
            case KnightFx.TETHER -> put(level, d.getInt("id"), LANCE, 1, d.getInt("d"), 1);
            case KnightFx.PHALANX -> {
                for (int id : d.getIntArray("ids")) {
                    put(level, id, LANCE, d.getInt("gather"), d.getInt("gather") + d.getInt("charge"), 1);
                }
            }
            case KnightFx.OATH -> {
                for (int id : d.getIntArray("ids")) {
                    put(level, id, OATH, d.getInt("gather"), d.getInt("gather") + d.getInt("form") + 30, 1);
                }
            }
            case KnightFx.NOOSE -> {
                for (int id : d.getIntArray("ids")) {
                    put(level, id, CHAINS, d.getInt("gather"), d.getInt("gather") + d.getInt("d"), 1);
                }
            }
            case KnightFx.NOOSE_BREAK -> STATES.remove(d.getInt("id"));
            default -> {
            }
        }
    }

    @Nullable
    public static State get(Entity entity, float age) {
        State state = STATES.get(entity.getId());
        if (state == null) {
            return null;
        }
        float t = age - state.start;
        if (t < -1.0F || t > state.duration + RECOVER) {
            STATES.remove(entity.getId());
            return null;
        }
        return state;
    }

    public static final class State {
        public final int kind;
        public final int start;
        public final int windup;
        public final int duration;
        public final int dir;

        State(int kind, int start, int windup, int duration, int dir) {
            this.kind = kind;
            this.start = start;
            this.windup = windup;
            this.duration = duration;
            this.dir = dir;
        }
    }
}
