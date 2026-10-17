package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue VFX_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ForgeConfigSpec.DoubleValue SHAKE_STRENGTH;
    public static final ForgeConfigSpec.IntValue PARTICLE_LIMIT;
    public static final ForgeConfigSpec.EnumValue<Quality> QUALITY;
    public static final ForgeConfigSpec.BooleanValue SCREEN_PULSE;

    public enum Quality {
        LOW(0.35F),
        MEDIUM(0.65F),
        HIGH(1.0F);

        private final float factor;

        Quality(float factor) {
            this.factor = factor;
        }

        public float factor() {
            return this.factor;
        }
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("vfx");
        VFX_ENABLED = builder
            .comment("显示本模组的 Boss 技能特效", "Render boss skill effects from this mod")
            .define("enabled", true);
        SCREEN_SHAKE = builder
            .comment("Boss 重击、爆炸时的镜头震动", "Camera shake on heavy boss impacts and explosions")
            .define("screen_shake", true);
        SHAKE_STRENGTH = builder
            .comment("镜头震动强度倍率", "Camera shake strength multiplier")
            .defineInRange("shake_strength", 1.0D, 0.0D, 3.0D);
        PARTICLE_LIMIT = builder
            .comment("本模组自绘特效粒子的同时存在上限", "Maximum number of custom effect particles alive at once")
            .defineInRange("particle_limit", 2000, 100, 8000);
        QUALITY = builder
            .comment("特效画质：LOW 粒子约 35%，MEDIUM 约 65%，HIGH 全量", "Effect quality: LOW ~35% particles, MEDIUM ~65%, HIGH full")
            .defineEnum("quality", Quality.HIGH);
        SCREEN_PULSE = builder
            .comment("近距离重击时的屏幕边缘泛光", "Screen edge glow pulse on nearby heavy impacts")
            .define("screen_pulse", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
