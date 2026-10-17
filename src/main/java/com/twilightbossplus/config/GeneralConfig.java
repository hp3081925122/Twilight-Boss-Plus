package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");
        DEBUG_LOG = builder
            .comment("输出 Boss 技能与阶段切换的调试日志", "Log boss skill and phase debug messages")
            .define("debug_log", false);
        builder.pop();
        SPEC = builder.build();
    }

    private GeneralConfig() {
    }
}
