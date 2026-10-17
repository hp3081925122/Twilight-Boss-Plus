package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AlphaYetiConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BASE_ATTACK;
    public static final ForgeConfigSpec.DoubleValue PHASE2_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue PHASE3_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue PHASE_ROAR;
    public static final ForgeConfigSpec.DoubleValue THROW_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue LANDING_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LANDING_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue FALLING_ICE_CAP;
    public static final ForgeConfigSpec.IntValue PHASE3_TIRED_TICKS;
    public static final ForgeConfigSpec.BooleanValue BREATH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BREATH_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue BREATH_COOLDOWN;
    public static final ForgeConfigSpec.IntValue BREATH_INHALE;
    public static final ForgeConfigSpec.IntValue BREATH_TICKS;
    public static final ForgeConfigSpec.DoubleValue BREATH_RANGE;
    public static final ForgeConfigSpec.DoubleValue BREATH_ANGLE;
    public static final ForgeConfigSpec.IntValue BREATH_FREEZE;
    public static final ForgeConfigSpec.DoubleValue BREATH_SWEEP;
    public static final ForgeConfigSpec.BooleanValue RIFT_ENABLED;
    public static final ForgeConfigSpec.DoubleValue RIFT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RIFT_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue RIFT_COOLDOWN;
    public static final ForgeConfigSpec.IntValue RIFT_WINDUP;
    public static final ForgeConfigSpec.DoubleValue RIFT_LENGTH;
    public static final ForgeConfigSpec.IntValue RIFT_PHASE1_LINES;
    public static final ForgeConfigSpec.IntValue RIFT_PHASE2_LINES;
    public static final ForgeConfigSpec.DoubleValue RIFT_RANGE;
    public static final ForgeConfigSpec.BooleanValue BOULDER_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BOULDER_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOULDER_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue BOULDER_COOLDOWN;
    public static final ForgeConfigSpec.IntValue BOULDER_WINDUP;
    public static final ForgeConfigSpec.DoubleValue BOULDER_SPEED;
    public static final ForgeConfigSpec.IntValue BOULDER_MAX_TICKS;
    public static final ForgeConfigSpec.IntValue BOULDER_HITS;
    public static final ForgeConfigSpec.DoubleValue BOULDER_SLUSH_RADIUS;
    public static final ForgeConfigSpec.IntValue BOULDER_SLUSH_TICKS;
    public static final ForgeConfigSpec.BooleanValue ROAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ROAR_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ROAR_COOLDOWN;
    public static final ForgeConfigSpec.IntValue ROAR_TICKS;
    public static final ForgeConfigSpec.IntValue ROAR_WARNING;
    public static final ForgeConfigSpec.IntValue ROAR_PHASE2_COUNT;
    public static final ForgeConfigSpec.IntValue ROAR_PHASE3_COUNT;
    public static final ForgeConfigSpec.DoubleValue ROAR_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ROAR_RANGE;
    public static final ForgeConfigSpec.BooleanValue WAVE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WAVE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WAVE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue WAVE_SPEED;
    public static final ForgeConfigSpec.DoubleValue WAVE_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("alpha_yeti");
        ENABLED = builder
            .comment("是否启用雪怪首领增强", "Enable Alpha Yeti rework")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率", "Attack damage multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.1D, 100.0D);
        BASE_ATTACK = builder
            .comment("技能伤害的基础攻击力", "Base attack damage used by skills")
            .defineInRange("base_attack", 8.0D, 0.0D, 1000.0D);
        PHASE2_THRESHOLD = builder
            .comment("进入二阶段的血量比例", "Health ratio to enter phase 2")
            .defineInRange("phase2_threshold", 0.6D, 0.0D, 1.0D);
        PHASE3_THRESHOLD = builder
            .comment("进入三阶段的血量比例", "Health ratio to enter phase 3")
            .defineInRange("phase3_threshold", 0.3D, 0.0D, 1.0D);
        PHASE_ROAR = builder
            .comment("转阶段时是否强制释放崩落怒吼", "Force an Avalanche Roar on phase change")
            .define("phase_roar", true);
        builder.push("vanilla");
        THROW_STRENGTH = builder
            .comment("抓投抛出速度倍率（再乘以 1 - 击退抗性）", "Grab throw velocity multiplier (also scaled by 1 - knockback resistance)")
            .defineInRange("throw_strength", 0.7D, 0.0D, 4.0D);
        LANDING_MULTIPLIER = builder
            .comment("暴走落地伤害倍率", "Rampage landing damage multiplier")
            .defineInRange("landing_multiplier", 0.6D, 0.0D, 100.0D);
        LANDING_KNOCKBACK = builder
            .comment("暴走落地击退强度", "Rampage landing knockback strength")
            .defineInRange("landing_knockback", 0.4D, 0.0D, 1.0D);
        FALLING_ICE_CAP = builder
            .comment("雪怪附近落冰对玩家伤害上限（攻击力倍率，0 为不限制）", "Falling ice damage cap near the Alpha Yeti, as attack multiplier (0 = no cap)")
            .defineInRange("falling_ice_cap_multiplier", 1.5D, 0.0D, 100.0D);
        PHASE3_TIRED_TICKS = builder
            .comment("三阶段疲惫时间（tick）", "Tired duration in phase 3 (ticks)")
            .defineInRange("phase3_tired_ticks", 60, 1, 1200);
        builder.pop();
        builder.push("frost_breath");
        BREATH_ENABLED = builder
            .comment("是否启用霜息", "Enable Frost Breath")
            .define("enabled", true);
        BREATH_MULTIPLIER = builder
            .comment("每 10 tick 的伤害倍率", "Damage multiplier every 10 ticks")
            .defineInRange("multiplier", 0.3D, 0.0D, 100.0D);
        BREATH_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 240, 1, 12000);
        BREATH_INHALE = builder
            .comment("吸气时间（tick）", "Inhale time in ticks")
            .defineInRange("inhale_ticks", 20, 1, 200);
        BREATH_TICKS = builder
            .comment("喷吐时间（tick）", "Breath duration in ticks")
            .defineInRange("breath_ticks", 40, 1, 400);
        BREATH_RANGE = builder
            .comment("喷吐距离", "Breath length")
            .defineInRange("range", 9.0D, 1.0D, 32.0D);
        BREATH_ANGLE = builder
            .comment("扇形总角度", "Cone total angle in degrees")
            .defineInRange("angle", 55.0D, 5.0D, 180.0D);
        BREATH_FREEZE = builder
            .comment("每次命中增加的冰冻值", "Frozen ticks added per hit")
            .defineInRange("freeze_per_hit", 60, 0, 1000);
        BREATH_SWEEP = builder
            .comment("三阶段左右扫动的角度", "Phase 3 sweep angle to each side")
            .defineInRange("phase3_sweep", 40.0D, 0.0D, 90.0D);
        builder.pop();
        builder.push("ice_rift");
        RIFT_ENABLED = builder
            .comment("是否启用冰裂重击", "Enable Ice Rift")
            .define("enabled", true);
        RIFT_MULTIPLIER = builder
            .comment("伤害倍率", "Damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        RIFT_KNOCKBACK = builder
            .comment("击退强度", "Knockback strength")
            .defineInRange("knockback", 0.5D, 0.0D, 1.0D);
        RIFT_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 180, 1, 12000);
        RIFT_WINDUP = builder
            .comment("举臂前摇（tick）", "Wind-up time in ticks")
            .defineInRange("windup_ticks", 18, 1, 200);
        RIFT_LENGTH = builder
            .comment("冰缝长度", "Rift length")
            .defineInRange("length", 10.0D, 2.0D, 32.0D);
        RIFT_PHASE1_LINES = builder
            .comment("一阶段冰缝数量", "Rift count in phase 1")
            .defineInRange("phase1_lines", 5, 1, 16);
        RIFT_PHASE2_LINES = builder
            .comment("二阶段起冰缝数量", "Rift count from phase 2")
            .defineInRange("phase2_lines", 7, 1, 16);
        RIFT_RANGE = builder
            .comment("触发距离", "Trigger range")
            .defineInRange("trigger_range", 6.0D, 1.0D, 32.0D);
        builder.pop();
        builder.push("boulder");
        BOULDER_ENABLED = builder
            .comment("是否启用雪球巨石", "Enable Snow Boulder")
            .define("enabled", true);
        BOULDER_MULTIPLIER = builder
            .comment("伤害倍率", "Damage multiplier")
            .defineInRange("multiplier", 1.2D, 0.0D, 100.0D);
        BOULDER_KNOCKBACK = builder
            .comment("击退强度", "Knockback strength")
            .defineInRange("knockback", 0.8D, 0.0D, 1.0D);
        BOULDER_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 280, 1, 12000);
        BOULDER_WINDUP = builder
            .comment("捧雪前摇（tick）", "Wind-up time in ticks")
            .defineInRange("windup_ticks", 24, 1, 200);
        BOULDER_SPEED = builder
            .comment("滚动速度（格/tick）", "Rolling speed in blocks per tick")
            .defineInRange("speed", 0.55D, 0.05D, 3.0D);
        BOULDER_MAX_TICKS = builder
            .comment("最长滚动时间（tick）", "Max rolling time in ticks")
            .defineInRange("max_ticks", 80, 1, 1200);
        BOULDER_HITS = builder
            .comment("打碎需要的玩家攻击次数", "Player hits needed to break it")
            .defineInRange("hits_to_break", 3, 1, 100);
        BOULDER_SLUSH_RADIUS = builder
            .comment("雪泥半径", "Slush radius")
            .defineInRange("slush_radius", 3.0D, 0.0D, 16.0D);
        BOULDER_SLUSH_TICKS = builder
            .comment("雪泥持续时间（tick）", "Slush duration in ticks")
            .defineInRange("slush_ticks", 100, 0, 2400);
        builder.pop();
        builder.push("avalanche_roar");
        ROAR_ENABLED = builder
            .comment("是否启用崩落怒吼", "Enable Avalanche Roar")
            .define("enabled", true);
        ROAR_MULTIPLIER = builder
            .comment("每根冰锥的伤害倍率", "Damage multiplier per icicle")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        ROAR_COOLDOWN = builder
            .comment("冷却（tick）", "Cooldown in ticks")
            .defineInRange("cooldown_ticks", 480, 1, 12000);
        ROAR_TICKS = builder
            .comment("怒吼时间（tick）", "Roar duration in ticks")
            .defineInRange("roar_ticks", 30, 1, 200);
        ROAR_WARNING = builder
            .comment("冰锥预警时间（tick）", "Icicle warning time in ticks")
            .defineInRange("warning_ticks", 30, 5, 200);
        ROAR_PHASE2_COUNT = builder
            .comment("二阶段每名玩家的冰锥数量", "Icicles per player in phase 2")
            .defineInRange("phase2_count", 3, 0, 16);
        ROAR_PHASE3_COUNT = builder
            .comment("三阶段每名玩家的冰锥数量", "Icicles per player in phase 3")
            .defineInRange("phase3_count", 4, 0, 16);
        ROAR_RADIUS = builder
            .comment("冰锥落地伤害半径", "Icicle impact radius")
            .defineInRange("radius", 1.6D, 0.5D, 8.0D);
        ROAR_RANGE = builder
            .comment("影响玩家的范围", "Range to target players")
            .defineInRange("range", 24.0D, 4.0D, 64.0D);
        builder.pop();
        builder.push("snow_wave");
        WAVE_ENABLED = builder
            .comment("是否启用三阶段暴走雪浪", "Enable phase 3 rampage Snow Wave")
            .define("enabled", true);
        WAVE_MULTIPLIER = builder
            .comment("伤害倍率", "Damage multiplier")
            .defineInRange("multiplier", 0.5D, 0.0D, 100.0D);
        WAVE_KNOCKBACK = builder
            .comment("击退强度", "Knockback strength")
            .defineInRange("knockback", 0.4D, 0.0D, 1.0D);
        WAVE_SPEED = builder
            .comment("扩散速度（格/tick）", "Expansion speed in blocks per tick")
            .defineInRange("speed", 0.5D, 0.05D, 3.0D);
        WAVE_RADIUS = builder
            .comment("最大半径", "Max radius")
            .defineInRange("radius", 8.0D, 1.0D, 32.0D);
        builder.pop();
        builder.pop();
        SPEC = builder.build();
    }

    private AlphaYetiConfig() {
    }
}
