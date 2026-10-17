package com.twilightbossplus.hydra;

import com.twilightbossplus.TwilightBossPlus;
import java.lang.reflect.Method;
import twilightforest.entity.boss.HydraHead;
import twilightforest.entity.boss.HydraHeadContainer;

public final class HydraHeads {
    public static final int IDLE = 0;
    public static final int BITE_BEGINNING = 1;
    public static final int BITE_READY = 2;
    public static final int BITING = 3;
    public static final int BITE_ENDING = 4;
    public static final int FLAME_BEGINNING = 5;
    public static final int FLAMING = 6;
    public static final int FLAME_ENDING = 7;
    public static final int MORTAR_BEGINNING = 8;
    public static final int MORTAR_SHOOTING = 9;
    public static final int MORTAR_ENDING = 10;
    public static final int DYING = 11;
    public static final int DEAD = 12;
    public static final int ATTACK_COOLDOWN = 13;
    public static final int BORN = 14;
    public static final int ROAR_START = 15;
    public static final int ROAR_RAWR = 16;

    private static final String[] NAMES = {
        "IDLE", "BITE_BEGINNING", "BITE_READY", "BITING", "BITE_ENDING", "FLAME_BEGINNING", "FLAMING", "FLAME_ENDING",
        "MORTAR_BEGINNING", "MORTAR_SHOOTING", "MORTAR_ENDING", "DYING", "DEAD", "ATTACK_COOLDOWN", "BORN", "ROAR_START", "ROAR_RAWR"
    };

    private static boolean resolved;
    private static boolean available;
    private static Object[] states;
    private static int[] fromOrdinal;
    private static Method setNextState;

    private HydraHeads() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> stateClass = Class.forName("twilightforest.entity.boss.HydraHeadContainer$State", false, HydraHeadContainer.class.getClassLoader());
            Object[] constants = stateClass.getEnumConstants();
            states = new Object[NAMES.length];
            fromOrdinal = new int[constants.length];
            for (int i = 0; i < constants.length; i++) {
                fromOrdinal[i] = -1;
            }
            for (Object constant : constants) {
                Enum<?> value = (Enum<?>) constant;
                for (int i = 0; i < NAMES.length; i++) {
                    if (NAMES[i].equals(value.name())) {
                        states[i] = constant;
                        fromOrdinal[value.ordinal()] = i;
                    }
                }
            }
            for (int i = 0; i < NAMES.length; i++) {
                if (states[i] == null) {
                    throw new IllegalStateException("Missing hydra head state " + NAMES[i]);
                }
            }
            setNextState = HydraHeadContainer.class.getMethod("setNextState", stateClass);
            available = true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            available = false;
            TwilightBossPlus.LOGGER.error("[TwilightBossPlus] Failed to resolve Hydra head states, new Hydra skills are disabled", e);
        }
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static int stateOf(HydraHead head) {
        resolve();
        if (!available) {
            return -1;
        }
        Enum<?> state = head.getState();
        if (state == null || state.ordinal() >= fromOrdinal.length) {
            return -1;
        }
        return fromOrdinal[state.ordinal()];
    }

    public static boolean force(HydraHeadContainer container, int state) {
        resolve();
        if (!available) {
            return false;
        }
        try {
            setNextState.invoke(container, states[state]);
        } catch (ReflectiveOperationException e) {
            TwilightBossPlus.LOGGER.error("[TwilightBossPlus] Failed to set Hydra head state {}", NAMES[state], e);
            return false;
        }
        container.endCurrentAction();
        return true;
    }

    public static boolean isMortarState(int state) {
        return state == MORTAR_BEGINNING || state == MORTAR_SHOOTING;
    }
}
