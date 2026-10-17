package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class UrGhastConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BASE_ATTACK;
    public static final ForgeConfigSpec.DoubleValue PHASE2_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue PHASE3_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue PHASE_TANTRUM;
    public static final ForgeConfigSpec.DoubleValue FIREBALL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FIREBALL_SPREAD;
    public static final ForgeConfigSpec.IntValue FIREBALL_PHASE1_COUNT;
    public static final ForgeConfigSpec.IntValue FIREBALL_PHASE2_COUNT;
    public static final ForgeConfigSpec.DoubleValue TANTRUM_FIRST_RATIO;
    public static final ForgeConfigSpec.DoubleValue TANTRUM_RATIO;
    public static final ForgeConfigSpec.DoubleValue TANTRUM_DAMAGE_DIVISOR;
    public static final ForgeConfigSpec.IntValue TANTRUM_NO_TRAP_TICKS;
    public static final ForgeConfigSpec.BooleanValue TRAP_SCALES_WITH_HEALTH;
    public static final ForgeConfigSpec.DoubleValue ABSORB_HEAL_RATIO;
    public static final ForgeConfigSpec.BooleanValue REFLECT_SCALES_WITH_HEALTH;
    public static final ForgeConfigSpec.DoubleValue RAIN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RAIN_RADIUS;
    public static final ForgeConfigSpec.DoubleValue RAIN_PHASE3_SCALE;
    public static final ForgeConfigSpec.DoubleValue RAIN_DRIFT_SPEED;
    public static final ForgeConfigSpec.DoubleValue RAIN_DRIFT_MAX;
    public static final ForgeConfigSpec.IntValue RAIN_SLOW_TICKS;
    public static final ForgeConfigSpec.BooleanValue GAZE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GAZE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue GAZE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue GAZE_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue GAZE_RANGE;
    public static final ForgeConfigSpec.IntValue GAZE_SLOW_TICKS;
    public static final ForgeConfigSpec.BooleanValue BROOD_ENABLED;
    public static final ForgeConfigSpec.IntValue BROOD_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue BROOD_MAX_GHASTLINGS;
    public static final ForgeConfigSpec.DoubleValue BROOD_SPEED;
    public static final ForgeConfigSpec.IntValue BROOD_MAX_TICKS;
    public static final ForgeConfigSpec.DoubleValue BROOD_ATTACK_PER_STACK;
    public static final ForgeConfigSpec.IntValue BROOD_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue BROOD_STACK_TICKS;
    public static final ForgeConfigSpec.BooleanValue TEAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue TEAR_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TEAR_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue TEAR_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue TEAR_CHARGE_TICKS;
    public static final ForgeConfigSpec.IntValue TEAR_FALL_TICKS;
    public static final ForgeConfigSpec.DoubleValue TEAR_RADIUS;
    public static final ForgeConfigSpec.DoubleValue TEAR_POP_DAMAGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue TEAR_POOL_RADIUS;
    public static final ForgeConfigSpec.IntValue TEAR_POOL_TICKS;
    public static final ForgeConfigSpec.DoubleValue TEAR_POOL_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("ur_ghast");
        ENABLED = builder
            .comment("启用暮初恶魂增强，关闭后保持暮色原版", "Enable the Ur-Ghast rework; disable to keep vanilla Twilight Forest behavior")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率，技能伤害 = 攻击力 * 技能倍率", "Attack damage multiplier; skill damage = attack damage * skill multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.0D, 100.0D);
        BASE_ATTACK = builder
            .comment("基础攻击力（原版没有攻击属性，火球直击为 16）", "Base attack damage (vanilla has no attack attribute; a fireball direct hit deals 16)")
            .defineInRange("base_attack", 16.0D, 0.0D, 1000.0D);
        PHASE2_THRESHOLD = builder
            .comment("进入二阶段的血量百分比", "Health ratio to enter phase 2")
            .defineInRange("phase2_threshold", 0.6D, 0.0D, 1.0D);
        PHASE3_THRESHOLD = builder
            .comment("进入三阶段的血量百分比", "Health ratio to enter phase 3")
            .defineInRange("phase3_threshold", 0.3D, 0.0D, 1.0D);
        PHASE_TANTRUM = builder
            .comment("进入二、三阶段时强制进入一次暴怒", "Force a tantrum when entering phase 2 and phase 3")
            .define("phase_tantrum", true);

        builder.push("fireball");
        FIREBALL_MULTIPLIER = builder
            .comment("火球直击伤害倍率", "Fireball direct hit damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        FIREBALL_SPREAD = builder
            .comment("扇形中相邻两颗火球的水平夹角（度）", "Horizontal angle between neighbouring fireballs in the fan, in degrees")
            .defineInRange("spread_degrees", 12.0D, 0.0D, 60.0D);
        FIREBALL_PHASE1_COUNT = builder
            .comment("一阶段每轮火球数", "Fireballs per volley in phase 1")
            .defineInRange("phase1_count", 3, 1, 9);
        FIREBALL_PHASE2_COUNT = builder
            .comment("二阶段起每轮火球数", "Fireballs per volley from phase 2")
            .defineInRange("phase2_count", 5, 1, 9);
        builder.pop();

        builder.push("tantrum");
        TANTRUM_FIRST_RATIO = builder
            .comment("首次进入暴怒需要受到的伤害（最大生命的比例，原版 10 / 250）", "Damage needed for the first tantrum, as a ratio of max health (vanilla 10 / 250)")
            .defineInRange("first_ratio", 0.04D, 0.001D, 1.0D);
        TANTRUM_RATIO = builder
            .comment("之后每次切换暴怒需要受到的伤害（最大生命的比例，原版 18 / 250）", "Damage needed for each later tantrum toggle, as a ratio of max health (vanilla 18 / 250)")
            .defineInRange("ratio", 0.072D, 0.001D, 1.0D);
        TANTRUM_DAMAGE_DIVISOR = builder
            .comment("暴怒时受到伤害的除数", "Damage divisor while in a tantrum")
            .defineInRange("damage_divisor", 10.0D, 1.0D, 100.0D);
        TANTRUM_NO_TRAP_TICKS = builder
            .comment("没有可用陷阱时暴怒自动结束的时长（tick），0 为不自动结束", "Tantrum auto-ends after this many ticks when no usable trap exists; 0 disables")
            .defineInRange("no_trap_ticks", 400, 0, 12000);
        TRAP_SCALES_WITH_HEALTH = builder
            .comment("恶魂陷阱的伤害乘以血量倍率", "Scale ghast trap damage by the health multiplier")
            .define("trap_scales_with_health", true);
        ABSORB_HEAL_RATIO = builder
            .comment("吸收一只恶灵回复的生命（最大生命的比例，原版 2 / 250）", "Healing per absorbed ghastling, as a ratio of max health (vanilla 2 / 250)")
            .defineInRange("absorb_heal_ratio", 0.008D, 0.0D, 1.0D);
        REFLECT_SCALES_WITH_HEALTH = builder
            .comment("玩家反弹火球的伤害乘以血量倍率", "Scale reflected fireball damage by the health multiplier")
            .define("reflect_scales_with_health", true);
        builder.pop();

        builder.push("tear_rain");
        RAIN_MULTIPLIER = builder
            .comment("泪雨每次伤害倍率（每 10 tick 一次）", "Tear rain damage multiplier per hit (every 10 ticks)")
            .defineInRange("multiplier", 0.1875D, 0.0D, 100.0D);
        RAIN_RADIUS = builder
            .comment("泪雨区半径", "Tear rain radius")
            .defineInRange("radius", 7.0D, 1.0D, 32.0D);
        RAIN_PHASE3_SCALE = builder
            .comment("三阶段泪雨半径倍率", "Tear rain radius scale in phase 3")
            .defineInRange("phase3_radius_scale", 1.4D, 1.0D, 4.0D);
        RAIN_DRIFT_SPEED = builder
            .comment("二阶段起泪雨中心每 tick 向玩家漂移的距离，0 为不漂移", "Blocks per tick the rain centre drifts toward a player from phase 2; 0 disables")
            .defineInRange("drift_speed", 0.15D, 0.0D, 2.0D);
        RAIN_DRIFT_MAX = builder
            .comment("泪雨中心最多偏离 Boss 投影的距离", "Max distance the rain centre may drift from the boss projection")
            .defineInRange("drift_max", 10.0D, 0.0D, 48.0D);
        RAIN_SLOW_TICKS = builder
            .comment("泪雨施加缓慢 I 的时长（tick）", "Slowness I duration from tear rain, in ticks")
            .defineInRange("slow_ticks", 40, 0, 1200);
        builder.pop();

        builder.push("gaze");
        GAZE_ENABLED = builder
            .comment("启用凝视烙印", "Enable Sorrowful Gaze")
            .define("enabled", true);
        GAZE_MULTIPLIER = builder
            .comment("凝视烙印伤害倍率", "Sorrowful Gaze damage multiplier")
            .defineInRange("multiplier", 0.9D, 0.0D, 100.0D);
        GAZE_COOLDOWN_TICKS = builder
            .comment("凝视烙印冷却（tick）", "Sorrowful Gaze cooldown in ticks")
            .defineInRange("cooldown_ticks", 300, 1, 12000);
        GAZE_DURATION_TICKS = builder
            .comment("注视持续时间（tick）", "Gaze duration in ticks")
            .defineInRange("duration_ticks", 50, 5, 400);
        GAZE_RANGE = builder
            .comment("凝视烙印的最大距离", "Max range of Sorrowful Gaze")
            .defineInRange("range", 48.0D, 4.0D, 128.0D);
        GAZE_SLOW_TICKS = builder
            .comment("命中后缓慢 II 的时长（tick）", "Slowness II duration on hit, in ticks")
            .defineInRange("slow_ticks", 60, 0, 1200);
        builder.pop();

        builder.push("brood_call");
        BROOD_ENABLED = builder
            .comment("启用母巢召回", "Enable Brood Call")
            .define("enabled", true);
        BROOD_COOLDOWN_TICKS = builder
            .comment("母巢召回冷却（tick）", "Brood Call cooldown in ticks")
            .defineInRange("cooldown_ticks", 500, 1, 12000);
        BROOD_MAX_GHASTLINGS = builder
            .comment("一次最多召回的恶灵数量", "Max ghastlings recalled at once")
            .defineInRange("max_ghastlings", 8, 1, 32);
        BROOD_SPEED = builder
            .comment("被召回恶灵每 tick 的飞行距离", "Blocks per tick a recalled ghastling flies")
            .defineInRange("speed", 0.35D, 0.05D, 2.0D);
        BROOD_MAX_TICKS = builder
            .comment("召回最长持续时间（tick）", "Max recall duration in ticks")
            .defineInRange("max_ticks", 100, 10, 1200);
        BROOD_ATTACK_PER_STACK = builder
            .comment("每层怨泣增加的攻击力比例", "Attack bonus ratio per Grief stack")
            .defineInRange("attack_per_stack", 0.08D, 0.0D, 10.0D);
        BROOD_MAX_STACKS = builder
            .comment("怨泣层数上限", "Max Grief stacks")
            .defineInRange("max_stacks", 5, 0, 50);
        BROOD_STACK_TICKS = builder
            .comment("没有新层数时怨泣清空的时间（tick）", "Ticks without a new stack before Grief resets")
            .defineInRange("stack_ticks", 400, 1, 12000);
        builder.pop();

        builder.push("primal_tear");
        TEAR_ENABLED = builder
            .comment("启用原初泪核", "Enable Primal Tear")
            .define("enabled", true);
        TEAR_MULTIPLIER = builder
            .comment("泪核落地伤害倍率", "Primal Tear impact damage multiplier")
            .defineInRange("multiplier", 1.5D, 0.0D, 100.0D);
        TEAR_KNOCKBACK = builder
            .comment("泪核落地击退强度（受击退抗性影响）", "Primal Tear impact knockback (affected by knockback resistance)")
            .defineInRange("knockback", 0.6D, 0.0D, 1.0D);
        TEAR_COOLDOWN_TICKS = builder
            .comment("原初泪核冷却（tick）", "Primal Tear cooldown in ticks")
            .defineInRange("cooldown_ticks", 440, 1, 12000);
        TEAR_CHARGE_TICKS = builder
            .comment("凝聚泪核的时间（tick）", "Primal Tear charge time in ticks")
            .defineInRange("charge_ticks", 30, 5, 200);
        TEAR_FALL_TICKS = builder
            .comment("泪核下落到落点的时间（tick）", "Ticks for the Primal Tear to reach its landing point")
            .defineInRange("fall_ticks", 50, 10, 400);
        TEAR_RADIUS = builder
            .comment("泪核落地伤害半径", "Primal Tear impact radius")
            .defineInRange("radius", 5.0D, 1.0D, 16.0D);
        TEAR_POP_DAMAGE_RATIO = builder
            .comment("泪核被击落时 Boss 受到的伤害（最大生命的比例）", "Damage dealt to the boss when the tear is shot down, as a ratio of max health")
            .defineInRange("pop_damage_ratio", 0.03D, 0.0D, 1.0D);
        TEAR_POOL_RADIUS = builder
            .comment("悲泣之池半径", "Sorrow pool radius")
            .defineInRange("pool_radius", 4.0D, 0.0D, 16.0D);
        TEAR_POOL_TICKS = builder
            .comment("悲泣之池持续时间（tick）", "Sorrow pool duration in ticks")
            .defineInRange("pool_ticks", 160, 0, 2400);
        TEAR_POOL_MULTIPLIER = builder
            .comment("悲泣之池每 20 tick 的伤害倍率", "Sorrow pool damage multiplier every 20 ticks")
            .defineInRange("pool_multiplier", 0.1D, 0.0D, 100.0D);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private UrGhastConfig() {
    }
}
