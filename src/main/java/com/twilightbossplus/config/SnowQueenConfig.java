package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SnowQueenConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BASE_ATTACK;
    public static final ForgeConfigSpec.DoubleValue PHASE2_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue PHASE3_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue PHASE_AURORA;
    public static final ForgeConfigSpec.DoubleValue BEAM_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHIELD_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DROP_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SHIELD_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue REFRACT_ANGLE;
    public static final ForgeConfigSpec.BooleanValue SIGIL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SIGIL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SIGIL_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue SIGIL_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SIGIL_WINDUP;
    public static final ForgeConfigSpec.DoubleValue SIGIL_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SIGIL_WIDTH;
    public static final ForgeConfigSpec.DoubleValue SIGIL_RANGE;
    public static final ForgeConfigSpec.IntValue SIGIL_SECOND_DELAY;
    public static final ForgeConfigSpec.BooleanValue LANCE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue LANCE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LANCE_LIFT;
    public static final ForgeConfigSpec.IntValue LANCE_COOLDOWN;
    public static final ForgeConfigSpec.IntValue LANCE_WARNING;
    public static final ForgeConfigSpec.IntValue LANCE_INTERVAL;
    public static final ForgeConfigSpec.IntValue LANCE_PHASE1_COUNT;
    public static final ForgeConfigSpec.IntValue LANCE_PHASE3_COUNT;
    public static final ForgeConfigSpec.DoubleValue LANCE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue LANCE_RANGE;
    public static final ForgeConfigSpec.BooleanValue AURORA_ENABLED;
    public static final ForgeConfigSpec.DoubleValue AURORA_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue AURORA_COOLDOWN;
    public static final ForgeConfigSpec.IntValue AURORA_WARNING;
    public static final ForgeConfigSpec.IntValue AURORA_ACTIVE;
    public static final ForgeConfigSpec.IntValue AURORA_COUNT;
    public static final ForgeConfigSpec.DoubleValue AURORA_LENGTH;
    public static final ForgeConfigSpec.DoubleValue AURORA_WIDTH;
    public static final ForgeConfigSpec.DoubleValue AURORA_DRIFT;
    public static final ForgeConfigSpec.DoubleValue AURORA_RANGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("snow_queen");
        ENABLED = builder
            .comment("是否启用冰雪女王增强", "Enable Snow Queen rework")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率", "Attack damage multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.1D, 100.0D);
        BASE_ATTACK = builder
            .comment("技能伤害的基础攻击力", "Base attack damage used by skills")
            .defineInRange("base_attack", 7.0D, 0.0D, 1000.0D);
        PHASE2_THRESHOLD = builder
            .comment("进入二阶段的血量比例", "Health ratio to enter phase 2")
            .defineInRange("phase2_threshold", 0.6D, 0.0D, 1.0D);
        PHASE3_THRESHOLD = builder
            .comment("进入三阶段的血量比例", "Health ratio to enter phase 3")
            .defineInRange("phase3_threshold", 0.3D, 0.0D, 1.0D);
        PHASE_AURORA = builder
            .comment("转阶段时是否强制释放极光帷幕", "Force an Aurora Curtain on phase change")
            .define("phase_aurora", true);
        builder.push("vanilla");
        BEAM_MULTIPLIER = builder
            .comment("冰息每次命中的伤害倍率", "Ice beam damage multiplier per hit")
            .defineInRange("beam_multiplier", 0.5D, 0.0D, 100.0D);
        SHIELD_MULTIPLIER = builder
            .comment("冰盾碰撞伤害倍率", "Ice shield contact damage multiplier")
            .defineInRange("shield_multiplier", 0.8D, 0.0D, 100.0D);
        DROP_MULTIPLIER = builder
            .comment("坠落阶段冰盾碰撞伤害倍率", "Ice shield damage multiplier while dropping")
            .defineInRange("drop_multiplier", 1.2D, 0.0D, 100.0D);
        SHIELD_KNOCKBACK = builder
            .comment("冰盾击退强度", "Ice shield knockback strength")
            .defineInRange("shield_knockback", 0.4D, 0.0D, 1.0D);
        REFRACT_ANGLE = builder
            .comment("三阶段折射光束的偏转角（0 为关闭）", "Phase 3 refracted beam angle (0 = disabled)")
            .defineInRange("refract_angle", 20.0D, 0.0D, 60.0D);
        builder.pop();
        builder.push("snowflake_sigil");
        SIGIL_ENABLED = builder
            .comment("是否启用霜华六芒", "Enable Snowflake Sigil")
            .define("enabled", true);
        SIGIL_MULTIPLIER = builder
            .comment("伤害倍率", "Damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        SIGIL_KNOCKBACK = builder
            .comment("击退强度", "Knockback strength")
            .defineInRange("knockback", 0.4D, 0.0D, 1.0D);
        SIGIL_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 200, 1, 12000);
        SIGIL_WINDUP = builder
            .comment("蓄力时间（tick）", "Wind-up time in ticks")
            .defineInRange("windup_ticks", 20, 1, 200);
        SIGIL_RADIUS = builder
            .comment("印记半径", "Sigil radius")
            .defineInRange("radius", 7.0D, 2.0D, 24.0D);
        SIGIL_WIDTH = builder
            .comment("花臂宽度", "Arm width")
            .defineInRange("arm_width", 1.0D, 0.2D, 4.0D);
        SIGIL_RANGE = builder
            .comment("触发的水平距离", "Horizontal trigger range")
            .defineInRange("trigger_range", 6.0D, 1.0D, 32.0D);
        SIGIL_SECOND_DELAY = builder
            .comment("二阶段第二层印记的延迟（tick）", "Delay of the second layer from phase 2 (ticks)")
            .defineInRange("second_delay", 15, 1, 200);
        builder.pop();
        builder.push("crystal_lance");
        LANCE_ENABLED = builder
            .comment("是否启用棱晶突刺", "Enable Crystal Lance")
            .define("enabled", true);
        LANCE_MULTIPLIER = builder
            .comment("每根伤害倍率", "Damage multiplier per spike")
            .defineInRange("multiplier", 0.8D, 0.0D, 100.0D);
        LANCE_LIFT = builder
            .comment("上抛速度", "Upward launch speed")
            .defineInRange("lift", 0.3D, 0.0D, 1.0D);
        LANCE_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 160, 1, 12000);
        LANCE_WARNING = builder
            .comment("标记预警时间（tick）", "Warning time in ticks")
            .defineInRange("warning_ticks", 14, 1, 200);
        LANCE_INTERVAL = builder
            .comment("连续突刺间隔（tick）", "Interval between spikes in ticks")
            .defineInRange("interval_ticks", 10, 1, 200);
        LANCE_PHASE1_COUNT = builder
            .comment("一二阶段每名玩家的突刺次数", "Spikes per player in phase 1 and 2")
            .defineInRange("phase1_count", 3, 1, 16);
        LANCE_PHASE3_COUNT = builder
            .comment("三阶段每名玩家的突刺次数", "Spikes per player in phase 3")
            .defineInRange("phase3_count", 5, 1, 16);
        LANCE_RADIUS = builder
            .comment("突刺伤害半径", "Spike damage radius")
            .defineInRange("radius", 1.2D, 0.3D, 6.0D);
        LANCE_RANGE = builder
            .comment("触发距离", "Trigger range")
            .defineInRange("range", 16.0D, 2.0D, 64.0D);
        builder.pop();
        builder.push("aurora_curtain");
        AURORA_ENABLED = builder
            .comment("是否启用极光帷幕", "Enable Aurora Curtain")
            .define("enabled", true);
        AURORA_MULTIPLIER = builder
            .comment("每 10 tick 的伤害倍率", "Damage multiplier every 10 ticks")
            .defineInRange("multiplier", 0.25D, 0.0D, 100.0D);
        AURORA_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 400, 1, 12000);
        AURORA_WARNING = builder
            .comment("预警时间（tick）", "Warning time in ticks")
            .defineInRange("warning_ticks", 30, 1, 200);
        AURORA_ACTIVE = builder
            .comment("生效时间（tick）", "Active time in ticks")
            .defineInRange("active_ticks", 60, 1, 1200);
        AURORA_COUNT = builder
            .comment("帷幕数量", "Curtain count")
            .defineInRange("count", 3, 1, 12);
        AURORA_LENGTH = builder
            .comment("帷幕长度", "Curtain length")
            .defineInRange("length", 14.0D, 2.0D, 48.0D);
        AURORA_WIDTH = builder
            .comment("帷幕宽度", "Curtain width")
            .defineInRange("width", 1.2D, 0.2D, 6.0D);
        AURORA_DRIFT = builder
            .comment("生效后横移速度（格/tick）", "Drift speed while active (blocks per tick)")
            .defineInRange("drift", 0.06D, 0.0D, 1.0D);
        AURORA_RANGE = builder
            .comment("影响玩家的范围", "Range to target players")
            .defineInRange("range", 24.0D, 4.0D, 64.0D);
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private SnowQueenConfig() {
    }
}
