package com.twilightbossplus.client.vfx;

import com.twilightbossplus.queen.QueenFx;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public final class QueenPose {
    private static final int RECOVER = 10;
    private static final Map<Integer, State> STATES = new HashMap<>();

    private QueenPose() {
    }

    static void record(ClientLevel level, int type, CompoundTag d) {
        if (type != QueenFx.POSE) {
            return;
        }
        int id = d.getInt("id");
        Entity entity = level.getEntity(id);
        if (entity == null) {
            return;
        }
        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        int windup = Math.max(1, d.getInt("windup"));
        STATES.put(id, new State(d.getInt("kind"), entity.tickCount, windup, Math.max(windup, d.getInt("d"))));
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
