package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public enum Quality {
        LOW(0.35F),
        MEDIUM(0.65F),
        HIGH(1.0F);

        public final float scale;

        Quality(float scale) {
            this.scale = scale;
        }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.DoubleValue SHAKE_STRENGTH;
    public static final ModConfigSpec.IntValue MAX_SHARDS;
    public static final ModConfigSpec.EnumValue<Quality> QUALITY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("vfx");
        ENABLED = b.comment("显示本模组的 Boss 技能特效与地面预警", "Render boss skill effects and ground warnings").define("enabled", true);
        SCREEN_SHAKE = b.comment("Boss 重击时的镜头震动", "Camera shake on heavy boss impacts").define("screen_shake", true);
        SHAKE_STRENGTH = b.comment("镜头震动强度倍率", "Camera shake strength multiplier").defineInRange("shake_strength", 1.0, 0.0, 3.0);
        MAX_SHARDS = b.comment("本模组自绘碎片/光点同时存在的上限（不使用原版粒子）", "Maximum self-rendered shards and sparks alive at once (vanilla particles are not used)").defineInRange("max_shards", 240, 0, 1500);
        QUALITY = b.comment("特效画质：LOW 碎片约 35%，MEDIUM 约 65%，HIGH 全量", "Effect quality: LOW ~35% shards, MEDIUM ~65%, HIGH full").defineEnum("quality", Quality.HIGH);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
