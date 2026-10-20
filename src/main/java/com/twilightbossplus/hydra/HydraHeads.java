package com.twilightbossplus.hydra;

import com.twilightbossplus.TwilightBossPlus;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import twilightforest.entity.boss.HydraHeadContainer;

public final class HydraHeads {
    public static final String IDLE = "IDLE";
    public static final String BITE_BEGINNING = "BITE_BEGINNING";
    public static final String BITE_READY = "BITE_READY";
    public static final String BITING = "BITING";
    public static final String BITE_ENDING = "BITE_ENDING";
    public static final String FLAME_BEGINNING = "FLAME_BEGINNING";
    public static final String FLAMING = "FLAMING";
    public static final String FLAME_ENDING = "FLAME_ENDING";
    public static final String MORTAR_BEGINNING = "MORTAR_BEGINNING";
    public static final String MORTAR_SHOOTING = "MORTAR_SHOOTING";
    public static final String MORTAR_ENDING = "MORTAR_ENDING";
    public static final String ROAR_START = "ROAR_START";
    public static final String ROAR_RAWR = "ROAR_RAWR";
    private static final int MAX_HEAD_DAMAGE = 120;
    private static final Map<String, Object> STATES = new HashMap<>();
    private static Method setNextState;
    private static boolean failed;

    private HydraHeads() {
    }

    private static boolean ready() {
        if (setNextState != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        try {
            Class<?> type = Class.forName("twilightforest.entity.boss.HydraHeadContainer$State");
            for (Object constant : type.getEnumConstants()) {
                STATES.put(((Enum<?>) constant).name(), constant);
            }
            setNextState = HydraHeadContainer.class.getMethod("setNextState", type);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            TwilightBossPlus.LOGGER.error("Failed to access Hydra head states", e);
            return false;
        }
    }

    public static boolean alive(HydraHeadContainer head) {
        return head.headEntity != null && head.isActive() && head.getDamageTaken() <= MAX_HEAD_DAMAGE;
    }

    public static boolean command(HydraHeadContainer head, String state, int duration) {
        if (!alive(head) || !ready()) {
            return false;
        }
        Object value = STATES.get(state);
        if (value == null) {
            return false;
        }
        try {
            setNextState.invoke(head, value);
        } catch (ReflectiveOperationException e) {
            TwilightBossPlus.LOGGER.error("Failed to set Hydra head state {}", state, e);
            return false;
        }
        head.endCurrentAction();
        ((HydraHeadControl) head).tbp$duration(duration);
        TwilightBossPlus.debug("Hydra head {} commanded {} for {} ticks", ((HydraHeadControl) head).tbp$headNum(), state, duration);
        return true;
    }
}
