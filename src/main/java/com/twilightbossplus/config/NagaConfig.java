package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class NagaConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue PHASE_TWO_HEALTH;
    public static final ForgeConfigSpec.DoubleValue PHASE_THREE_HEALTH;
    public static final ForgeConfigSpec.IntValue PHASE_TRANSITION_INVULNERABLE_TICKS;
    public static final ForgeConfigSpec.DoubleValue MELEE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue SEGMENT_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STUNLESS_BLOCK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue VENOM_SPIT_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue VENOM_SPIT_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue TAIL_SWEEP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue TAIL_SWEEP_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue EARTH_REND_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue EARTH_REND_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue CONSTRICT_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue CONSTRICT_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue CONSTRICT_BREAK_DAMAGE_RATIO;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("naga");
        ENABLED = builder
            .comment("启用娜迦增强，关闭后保持暮色原版", "Enable the Naga rework; disable to keep vanilla Twilight Forest behavior")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率，技能伤害 = 攻击力 * 技能倍率", "Attack damage multiplier; skill damage = attack damage * skill multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.1D, 100.0D);

        builder.push("phase");
        PHASE_TWO_HEALTH = builder
            .comment("进入二阶段（裂地）的血量比例", "Health ratio to enter phase two (Earth Rend)")
            .defineInRange("phase_two_health", 0.5D, 0.0D, 1.0D);
        PHASE_THREE_HEALTH = builder
            .comment("进入三阶段（绞杀）的血量比例", "Health ratio to enter phase three (Constrict)")
            .defineInRange("phase_three_health", 0.25D, 0.0D, 1.0D);
        PHASE_TRANSITION_INVULNERABLE_TICKS = builder
            .comment("蜕皮转阶段的无敌时长（tick）", "Invulnerable ticks during the shedding phase transition")
            .defineInRange("phase_transition_invulnerable_ticks", 40, 0, 1200);
        builder.pop();

        builder.push("melee");
        MELEE_KNOCKBACK = builder
            .comment("近战击退强度，受击退抗性影响", "Melee knockback strength, reduced by knockback resistance")
            .defineInRange("melee_knockback", 0.8D, 0.0D, 1.2D);
        SEGMENT_DAMAGE_MULTIPLIER = builder
            .comment("身体节碰撞伤害倍率", "Body segment contact damage multiplier")
            .defineInRange("segment_damage_multiplier", 0.4D, 0.0D, 100.0D);
        STUNLESS_BLOCK_MULTIPLIER = builder
            .comment("无眩晕冲锋被盾挡时的伤害倍率", "Damage multiplier when a stunless charge hits a shield")
            .defineInRange("stunless_block_multiplier", 0.8D, 0.0D, 100.0D);
        builder.pop();

        builder.push("venom_spit");
        VENOM_SPIT_MULTIPLIER = builder
            .comment("毒牙喷吐直击伤害倍率", "Venom Spit direct hit damage multiplier")
            .defineInRange("multiplier", 0.6D, 0.0D, 100.0D);
        VENOM_SPIT_COOLDOWN_TICKS = builder
            .comment("毒牙喷吐冷却（tick）", "Venom Spit cooldown in ticks")
            .defineInRange("cooldown_ticks", 160, 1, 12000);
        builder.pop();

        builder.push("tail_sweep");
        TAIL_SWEEP_MULTIPLIER = builder
            .comment("尾刃横扫伤害倍率", "Tail Sweep damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        TAIL_SWEEP_COOLDOWN_TICKS = builder
            .comment("尾刃横扫冷却（tick）", "Tail Sweep cooldown in ticks")
            .defineInRange("cooldown_ticks", 120, 1, 12000);
        builder.pop();

        builder.push("earth_rend");
        EARTH_REND_MULTIPLIER = builder
            .comment("地脉裂袭伤害倍率", "Earth Rend damage multiplier")
            .defineInRange("multiplier", 1.4D, 0.0D, 100.0D);
        EARTH_REND_COOLDOWN_TICKS = builder
            .comment("地脉裂袭冷却（tick）", "Earth Rend cooldown in ticks")
            .defineInRange("cooldown_ticks", 200, 1, 12000);
        builder.pop();

        builder.push("constrict");
        CONSTRICT_MULTIPLIER = builder
            .comment("绞杀之环每秒伤害倍率", "Constrict damage multiplier per second")
            .defineInRange("multiplier", 0.3D, 0.0D, 100.0D);
        CONSTRICT_COOLDOWN_TICKS = builder
            .comment("绞杀之环冷却（tick）", "Constrict cooldown in ticks")
            .defineInRange("cooldown_ticks", 400, 1, 12000);
        CONSTRICT_BREAK_DAMAGE_RATIO = builder
            .comment("击破逆鳞后娜迦承受的最大生命比例伤害", "Fraction of max health dealt to the Naga when the coil is broken")
            .defineInRange("break_damage_ratio", 0.15D, 0.0D, 1.0D);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private NagaConfig() {
    }
}
