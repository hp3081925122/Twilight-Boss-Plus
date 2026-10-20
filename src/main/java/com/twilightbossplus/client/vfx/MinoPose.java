package com.twilightbossplus.client.vfx;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.minoshroom.MinoFx;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class MinoPose {
    public static final int CHOP = 0;
    public static final int SWEEP = 1;
    public static final int WHIRL = 2;
    public static final int PAW = 3;
    private static final int RECOVER = 16;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private MinoPose() {
    }

    static void record(ClientLevel level, int type, CompoundTag d) {
        int kind;
        int windup;
        int duration;
        switch (type) {
            case MinoFx.AXE_CHARGE -> {
                kind = CHOP;
                windup = d.getInt("d");
                duration = windup;
            }
            case MinoFx.CLEAVE -> {
                if (d.getInt("style") == MinoFx.CLEAVE_CHOP) {
                    kind = CHOP;
                } else {
                    kind = SWEEP;
                }
                windup = d.getInt("windup");
                duration = windup;
            }
            case MinoFx.WHIRL -> {
                kind = WHIRL;
                windup = d.getInt("windup");
                duration = d.getInt("d");
            }
            case MinoFx.TRACK -> {
                if (d.getInt("style") != MinoFx.TRACK_HOOF) {
                    return;
                }
                kind = PAW;
                windup = d.getInt("d");
                duration = windup;
            }
            default -> {
                return;
            }
        }
        int id = d.getInt("id");
        Entity entity = level.getEntity(id);
        if (entity == null) {
            return;
        }
        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        STATES.put(id, new State(kind, entity.tickCount, Math.max(1, windup), Math.max(windup, duration)));
        TwilightBossPlus.debug("Minoshroom {} pose {} windup {} duration {}", id, kind, windup, duration);
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

        State(int kind, int start, int windup, int duration) {
            this.kind = kind;
            this.start = start;
            this.windup = windup;
            this.duration = duration;
        }
    }
}
