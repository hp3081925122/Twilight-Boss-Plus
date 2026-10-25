package com.twilightbossplus.client.vfx;

import com.twilightbossplus.yeti.YetiFx;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class YetiPose {
    private static final int RECOVER = 10;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private YetiPose() {
    }

    static void record(ClientLevel level, int type, CompoundTag d) {
        if (type != YetiFx.POSE) {
            return;
        }
        int id = d.getIntOr("id", 0);
        Entity entity = level.getEntity(id);
        if (entity == null) {
            return;
        }
        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        int windup = Math.max(1, d.getIntOr("windup", 0));
        STATES.put(id, new State(d.getIntOr("kind", 0), entity.tickCount, windup, Math.max(windup, d.getIntOr("d", 0))));
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
