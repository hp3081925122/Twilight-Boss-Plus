package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class HydraConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BASE_ATTACK;
    public static final ForgeConfigSpec.DoubleValue PHASE2_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue PHASE3_THRESHOLD;
    public static final ForgeConfigSpec.IntValue PHASE2_MIN_HEADS;
    public static final ForgeConfigSpec.DoubleValue ROAR_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue SEVER_RATIO;
    public static final ForgeConfigSpec.IntValue RESPAWN_TICKS;
    public static final ForgeConfigSpec.IntValue PHASE3_RESPAWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue BODY_DAMAGE_DIVISOR;
    public static final ForgeConfigSpec.IntValue REGEN_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue REGEN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue REGEN_AMOUNT;
    public static final ForgeConfigSpec.DoubleValue BITE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BITE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue BITE_SHIELD_DAMAGE;
    public static final ForgeConfigSpec.IntValue BITE_SHIELD_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue FLAME_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue FLAME_BURN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FLAME_PHASE3_TRACKING_SCALE;
    public static final ForgeConfigSpec.DoubleValue MORTAR_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue MORTAR_BURN_SECONDS;
    public static final ForgeConfigSpec.BooleanValue MORTAR_MEGA_BLAST;
    public static final ForgeConfigSpec.DoubleValue MORTAR_MEGA_BLAST_POWER;
    public static final ForgeConfigSpec.BooleanValue MORTAR_BREAK_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue COLLISION_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue COLLISION_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue COLLISION_HIT_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue TAIL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue TAIL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TAIL_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue TAIL_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue TAIL_TRIGGER_TICKS;
    public static final ForgeConfigSpec.DoubleValue TAIL_RANGE;
    public static final ForgeConfigSpec.IntValue TAIL_WARNING_TICKS;
    public static final ForgeConfigSpec.BooleanValue SWEEP_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SWEEP_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SWEEP_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue SWEEP_ANGLE;
    public static final ForgeConfigSpec.IntValue SWEEP_TICKS;
    public static final ForgeConfigSpec.IntValue SCORCH_TICKS;
    public static final ForgeConfigSpec.DoubleValue SCORCH_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue SLAG_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SLAG_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SLAG_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue SLAG_COUNT;
    public static final ForgeConfigSpec.DoubleValue SLAG_RADIUS;
    public static final ForgeConfigSpec.IntValue SLAG_WARNING_TICKS;
    public static final ForgeConfigSpec.BooleanValue WAVE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue WAVE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WAVE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue WAVE_RADIUS;
    public static final ForgeConfigSpec.IntValue WAVE_EXPAND_TICKS;
    public static final ForgeConfigSpec.BooleanValue GEYSER_ENABLED;
    public static final ForgeConfigSpec.DoubleValue GEYSER_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue GEYSER_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue GEYSER_RINGS;
    public static final ForgeConfigSpec.DoubleValue GEYSER_RING_SPACING;
    public static final ForgeConfigSpec.IntValue GEYSER_WARNING_TICKS;
    public static final ForgeConfigSpec.IntValue GEYSER_BURN_SECONDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("hydra");
        ENABLED = builder
            .comment("启用九头蛇增强，关闭后保持暮色原版", "Enable the Hydra rework; disable to keep vanilla Twilight Forest behavior")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率，技能伤害 = 攻击力 * 技能倍率", "Attack damage multiplier; skill damage = attack damage * skill multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.1D, 100.0D);
        BASE_ATTACK = builder
            .comment("九头蛇基础攻击力（原版没有攻击力属性，16 对应原版啃咬 48 / 喷火 19 / 迫击炮 18）", "Hydra base attack damage (vanilla has none; 16 matches vanilla bite 48 / flame 19 / mortar 18)")
            .defineInRange("base_attack", 16.0D, 0.0D, 1000.0D);
        PHASE2_THRESHOLD = builder
            .comment("血量低于该比例时进入二阶段", "Enters phase two below this health ratio")
            .defineInRange("phase2_threshold", 0.6D, 0.0D, 1.0D);
        PHASE3_THRESHOLD = builder
            .comment("血量低于该比例时进入三阶段", "Enters phase three below this health ratio")
            .defineInRange("phase3_threshold", 0.3D, 0.0D, 1.0D);
        PHASE2_MIN_HEADS = builder
            .comment("进入二阶段时至少激活的头数", "Minimum active heads when entering phase two")
            .defineInRange("phase2_min_heads", 5, 1, 7);
        ROAR_DAMAGE_REDUCTION = builder
            .comment("阶段咆哮期间的减伤比例", "Damage reduction while roaring on phase change")
            .defineInRange("roar_damage_reduction", 0.5D, 0.0D, 1.0D);

        builder.push("heads");
        SEVER_RATIO = builder
            .comment("单个头累计承受的伤害超过最大生命的该比例时断头（原版 120/360）", "A head is severed after taking more than this ratio of max health (vanilla 120/360)")
            .defineInRange("sever_ratio", 0.333D, 0.01D, 1.0D);
        RESPAWN_TICKS = builder
            .comment("断头后重生所需时间（tick）", "Ticks before a severed head regrows")
            .defineInRange("respawn_ticks", 100, 1, 12000);
        PHASE3_RESPAWN_TICKS = builder
            .comment("三阶段断头重生所需时间（tick）", "Head regrow ticks in phase three")
            .defineInRange("phase3_respawn_ticks", 60, 1, 12000);
        BODY_DAMAGE_DIVISOR = builder
            .comment("没打中张开的嘴时，伤害除以该值", "Damage is divided by this unless an open mouth is hit")
            .defineInRange("body_damage_divisor", 8.0D, 1.0D, 100.0D);
        REGEN_DELAY_TICKS = builder
            .comment("连续未受伤多久后开始回血（tick）", "Ticks without damage before regeneration starts")
            .defineInRange("regen_delay_ticks", 1000, 1, 72000);
        REGEN_INTERVAL_TICKS = builder
            .comment("回血间隔（tick）", "Regeneration interval in ticks")
            .defineInRange("regen_interval_ticks", 5, 1, 1200);
        REGEN_AMOUNT = builder
            .comment("每次回复的生命值", "Health restored per regeneration step")
            .defineInRange("regen_amount", 1.0D, 0.0D, 1000.0D);
        builder.pop();

        builder.push("bite");
        BITE_MULTIPLIER = builder
            .comment("啃咬伤害倍率", "Bite damage multiplier")
            .defineInRange("multiplier", 3.0D, 0.0D, 100.0D);
        BITE_KNOCKBACK = builder
            .comment("啃咬击退强度，受击退抗性影响", "Bite knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 1.0D, 0.0D, 1.2D);
        BITE_SHIELD_DAMAGE = builder
            .comment("啃咬命中举盾玩家时盾牌损失的耐久", "Shield durability lost when a blocking player is bitten")
            .defineInRange("shield_damage", 112, 0, 10000);
        BITE_SHIELD_COOLDOWN = builder
            .comment("啃咬后盾牌冷却（tick）", "Shield cooldown after being bitten, in ticks")
            .defineInRange("shield_cooldown_ticks", 200, 0, 12000);
        builder.pop();

        builder.push("flame");
        FLAME_MULTIPLIER = builder
            .comment("喷火伤害倍率", "Flame breath damage multiplier")
            .defineInRange("multiplier", 1.2D, 0.0D, 100.0D);
        FLAME_BURN_SECONDS = builder
            .comment("喷火点燃时长（秒）", "Flame breath burn duration in seconds")
            .defineInRange("burn_seconds", 3, 0, 60);
        FLAME_PHASE3_TRACKING_SCALE = builder
            .comment("三阶段喷火瞄准速度倍率", "Flame aim tracking speed multiplier in phase three")
            .defineInRange("phase3_tracking_scale", 2.0D, 0.1D, 10.0D);
        builder.pop();

        builder.push("mortar");
        MORTAR_MULTIPLIER = builder
            .comment("迫击炮伤害倍率", "Mortar damage multiplier")
            .defineInRange("multiplier", 1.1D, 0.0D, 100.0D);
        MORTAR_BURN_SECONDS = builder
            .comment("迫击炮点燃时长（秒）", "Mortar burn duration in seconds")
            .defineInRange("burn_seconds", 5, 0, 60);
        MORTAR_MEGA_BLAST = builder
            .comment("看不见目标时迫击炮是否改为碰到方块立即引爆的强力爆破", "Whether mortars become instant heavy blasts when the target is out of sight")
            .define("mortar_mega_blast", true);
        MORTAR_MEGA_BLAST_POWER = builder
            .comment("强力爆破的爆炸威力（原版 4）", "Heavy blast explosion power (vanilla 4)")
            .defineInRange("mega_blast_power", 2.0D, 0.1D, 8.0D);
        MORTAR_BREAK_BLOCKS = builder
            .comment("强力爆破是否破坏方块（还需要 mobGriefing 开启）", "Whether heavy blasts break blocks (also requires mobGriefing)")
            .define("mortar_break_blocks", false);
        builder.pop();

        builder.push("body_collision");
        COLLISION_MULTIPLIER = builder
            .comment("撞上身体或尾巴的伤害倍率", "Damage multiplier when touching the body or tail")
            .defineInRange("multiplier", 0.3D, 0.0D, 100.0D);
        COLLISION_KNOCKBACK = builder
            .comment("身体碰撞击退强度，受击退抗性影响", "Body collision knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 0.8D, 0.0D, 1.2D);
        COLLISION_HIT_INTERVAL = builder
            .comment("同一目标两次碰撞伤害之间的间隔（tick）", "Ticks between collision hits on the same target")
            .defineInRange("hit_interval_ticks", 20, 1, 1200);
        builder.pop();

        builder.push("tail_sweep");
        TAIL_ENABLED = builder
            .comment("启用回身尾扫", "Enable Tail Sweep")
            .define("enabled", true);
        TAIL_MULTIPLIER = builder
            .comment("回身尾扫伤害倍率", "Tail Sweep damage multiplier")
            .defineInRange("multiplier", 1.25D, 0.0D, 100.0D);
        TAIL_KNOCKBACK = builder
            .comment("尾扫击退强度，受击退抗性影响", "Tail Sweep knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 1.2D, 0.0D, 1.2D);
        TAIL_COOLDOWN_TICKS = builder
            .comment("回身尾扫冷却（tick）", "Tail Sweep cooldown in ticks")
            .defineInRange("cooldown_ticks", 240, 1, 12000);
        TAIL_TRIGGER_TICKS = builder
            .comment("目标贴身（6 格内）或在身后累计停留多久后触发（tick）", "Ticks the target must stay close to (within 6 blocks) or behind the body to trigger")
            .defineInRange("trigger_ticks", 40, 1, 1200);
        TAIL_RANGE = builder
            .comment("触发尾扫的最大目标距离", "Maximum target distance to trigger")
            .defineInRange("range", 12.0D, 1.0D, 32.0D);
        TAIL_WARNING_TICKS = builder
            .comment("尾扫预警时间（tick）", "Tail Sweep warning time in ticks")
            .defineInRange("warning_ticks", 20, 1, 200);
        builder.pop();

        builder.push("flame_sweep");
        SWEEP_ENABLED = builder
            .comment("启用三首扫焰", "Enable Triple Flame Sweep")
            .define("enabled", true);
        SWEEP_MULTIPLIER = builder
            .comment("扫焰每次命中的伤害倍率", "Damage multiplier per Flame Sweep hit")
            .defineInRange("multiplier", 0.8D, 0.0D, 100.0D);
        SWEEP_COOLDOWN_TICKS = builder
            .comment("三首扫焰冷却（tick）", "Triple Flame Sweep cooldown in ticks")
            .defineInRange("cooldown_ticks", 360, 1, 12000);
        SWEEP_ANGLE = builder
            .comment("扫射角度（度）", "Sweep angle in degrees")
            .defineInRange("sweep_angle", 100.0D, 10.0D, 270.0D);
        SWEEP_TICKS = builder
            .comment("扫射持续时间（tick，不超过原版喷火的 100）", "Sweep duration in ticks (at most the vanilla flame duration of 100)")
            .defineInRange("sweep_ticks", 60, 10, 100);
        SCORCH_TICKS = builder
            .comment("焦土持续时间（tick）", "Scorched ground duration in ticks")
            .defineInRange("scorch_ticks", 100, 0, 1200);
        SCORCH_MULTIPLIER = builder
            .comment("焦土每 10 tick 的伤害倍率", "Scorched ground damage multiplier per 10 ticks")
            .defineInRange("scorch_multiplier", 0.15D, 0.0D, 100.0D);
        builder.pop();

        builder.push("slag_rain");
        SLAG_ENABLED = builder
            .comment("启用熔渣雨", "Enable Slag Rain")
            .define("enabled", true);
        SLAG_MULTIPLIER = builder
            .comment("熔渣伤害倍率", "Slag damage multiplier")
            .defineInRange("multiplier", 0.9D, 0.0D, 100.0D);
        SLAG_COOLDOWN_TICKS = builder
            .comment("熔渣雨冷却（tick）", "Slag Rain cooldown in ticks")
            .defineInRange("cooldown_ticks", 440, 1, 12000);
        SLAG_COUNT = builder
            .comment("每次喷出的熔渣数量", "Slag blobs per cast")
            .defineInRange("count", 8, 1, 32);
        SLAG_RADIUS = builder
            .comment("每团熔渣的爆炸半径", "Blast radius of each slag blob")
            .defineInRange("radius", 1.8D, 0.5D, 8.0D);
        SLAG_WARNING_TICKS = builder
            .comment("落点预警时间（tick）", "Impact warning time in ticks")
            .defineInRange("warning_ticks", 25, 5, 200);
        builder.pop();

        builder.push("rebirth_wave");
        WAVE_ENABLED = builder
            .comment("启用复生热浪", "Enable Rebirth Heat Wave")
            .define("enabled", true);
        WAVE_MULTIPLIER = builder
            .comment("热浪伤害倍率", "Heat Wave damage multiplier")
            .defineInRange("multiplier", 0.6D, 0.0D, 100.0D);
        WAVE_KNOCKBACK = builder
            .comment("热浪击退强度，受击退抗性影响", "Heat Wave knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 0.8D, 0.0D, 1.2D);
        WAVE_RADIUS = builder
            .comment("热浪最大半径", "Heat Wave max radius")
            .defineInRange("radius", 9.0D, 1.0D, 32.0D);
        WAVE_EXPAND_TICKS = builder
            .comment("热浪扩散到最大半径所需时间（tick）", "Ticks for the Heat Wave to reach its max radius")
            .defineInRange("expand_ticks", 30, 1, 200);
        builder.pop();

        builder.push("lava_geyser");
        GEYSER_ENABLED = builder
            .comment("启用熔岩喷泉", "Enable Lava Geyser")
            .define("enabled", true);
        GEYSER_MULTIPLIER = builder
            .comment("熔岩柱伤害倍率", "Lava column damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        GEYSER_COOLDOWN_TICKS = builder
            .comment("熔岩喷泉冷却（tick）", "Lava Geyser cooldown in ticks")
            .defineInRange("cooldown_ticks", 500, 1, 12000);
        GEYSER_RINGS = builder
            .comment("喷发圈数", "Number of eruption rings")
            .defineInRange("rings", 3, 1, 6);
        GEYSER_RING_SPACING = builder
            .comment("相邻两圈的间距（第一圈距身体 5 格）", "Spacing between rings (the first ring is 5 blocks from the body)")
            .defineInRange("ring_spacing", 4.0D, 2.0D, 12.0D);
        GEYSER_WARNING_TICKS = builder
            .comment("每圈喷发前的预警时间（tick）", "Warning time before each ring erupts, in ticks")
            .defineInRange("warning_ticks", 25, 5, 200);
        GEYSER_BURN_SECONDS = builder
            .comment("熔岩柱点燃时长（秒）", "Lava column burn duration in seconds")
            .defineInRange("burn_seconds", 4, 0, 60);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private HydraConfig() {
    }
}
