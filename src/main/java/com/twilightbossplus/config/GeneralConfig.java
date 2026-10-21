package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GeneralConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DEBUG_LOG;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("general");
        DEBUG_LOG = b.comment("输出 Boss 技能与阶段切换的调试日志", "Log boss skill and phase debug messages").define("debug_log", false);
        b.pop();
        SPEC = b.build();
    }

    private GeneralConfig() {
    }
}
