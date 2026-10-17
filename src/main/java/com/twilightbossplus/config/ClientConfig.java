package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VFX_ENABLED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("vfx");
        VFX_ENABLED = builder
            .comment("显示本模组的 Boss 技能特效", "Render boss skill effects from this mod")
            .define("enabled", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
