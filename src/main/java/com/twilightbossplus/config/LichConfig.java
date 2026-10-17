package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LichConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SHIELD_STRENGTH;
    public static final ForgeConfigSpec.IntValue MINIONS_TO_SUMMON;
    public static final ForgeConfigSpec.DoubleValue BOLT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOMB_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BOMB_RADIUS;
    public static final ForgeConfigSpec.DoubleValue BOMB_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue ABSORB_CHANNEL_TICKS;
    public static final ForgeConfigSpec.DoubleValue POP_HEAL;
    public static final ForgeConfigSpec.BooleanValue VOID_ORB_ENABLED;
    public static final ForgeConfigSpec.DoubleValue VOID_ORB_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue VOID_ORB_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue VOID_ORB_SPEED;
    public static final ForgeConfigSpec.IntValue VOID_ORB_LIFETIME;
    public static final ForgeConfigSpec.BooleanValue MIRROR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue MIRROR_RADIUS;
    public static final ForgeConfigSpec.IntValue MIRROR_ROUNDS;
    public static final ForgeConfigSpec.BooleanValue CANDLE_ENABLED;
    public static final ForgeConfigSpec.IntValue CANDLE_COUNT;
    public static final ForgeConfigSpec.IntValue CANDLE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue CANDLE_STAGGER_TICKS;
    public static final ForgeConfigSpec.DoubleValue CANDLE_STAGGER_DAMAGE_BONUS;
    public static final ForgeConfigSpec.BooleanValue SIPHON_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SIPHON_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SIPHON_HEAL_RATIO;
    public static final ForgeConfigSpec.IntValue SIPHON_DURATION;
    public static final ForgeConfigSpec.IntValue SIPHON_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue SIPHON_BREAK_RANGE;
    public static final ForgeConfigSpec.BooleanValue BLINK_ENABLED;
    public static final ForgeConfigSpec.DoubleValue BLINK_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue BLINK_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue BLINK_WARNING_TICKS;
    public static final ForgeConfigSpec.DoubleValue BLINK_KNOCKBACK;
    public static final ForgeConfigSpec.BooleanValue CROWN_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CROWN_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue CROWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CROWN_PULL_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue CROWN_PULL_RANGE;
    public static final ForgeConfigSpec.DoubleValue CROWN_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CROWN_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue CROWN_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue CROWN_VULNERABLE_BONUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("lich");
        ENABLED = builder
            .comment("启用巫妖增强，关闭后保持暮色原版", "Enable the Lich rework; disable to keep vanilla Twilight Forest behavior")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率，技能伤害 = 攻击力 * 技能倍率", "Attack damage multiplier; skill damage = attack damage * skill multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.1D, 100.0D);
        SHIELD_STRENGTH = builder
            .comment("新生成巫妖的护盾层数（Boss 条按 6 层显示，所以上限为 6）", "Shield layers of a newly spawned Lich (the boss bar assumes 6, so 6 is the cap)")
            .defineInRange("shield_strength", 6, 1, 6);
        MINIONS_TO_SUMMON = builder
            .comment("新生成巫妖可召唤的仆从总数", "Total minions a newly spawned Lich can summon")
            .defineInRange("minions_to_summon", 9, 1, 64);

        builder.push("vanilla_fix");
        BOLT_MULTIPLIER = builder
            .comment("巫妖法弹伤害倍率（原版固定 6，等于 2.0 倍攻击）", "Lich Bolt damage multiplier (vanilla is a flat 6, equal to 2.0x attack)")
            .defineInRange("bolt_multiplier", 2.0D, 0.0D, 100.0D);
        BOMB_MULTIPLIER = builder
            .comment("巫妖炸弹伤害倍率，替代原版爆炸", "Lich Bomb damage multiplier, replaces the vanilla explosion")
            .defineInRange("bomb_multiplier", 1.5D, 0.0D, 100.0D);
        BOMB_RADIUS = builder
            .comment("巫妖炸弹的伤害半径", "Lich Bomb damage radius")
            .defineInRange("bomb_radius", 2.5D, 0.5D, 16.0D);
        BOMB_KNOCKBACK = builder
            .comment("巫妖炸弹击退强度，受击退抗性影响", "Lich Bomb knockback strength, reduced by knockback resistance")
            .defineInRange("bomb_knockback", 0.6D, 0.0D, 1.2D);
        ABSORB_CHANNEL_TICKS = builder
            .comment("吞噬仆从的引导时长（tick），期间杀死该仆从可打断", "Channel time for absorbing a minion in ticks; killing the minion interrupts it")
            .defineInRange("absorb_channel_ticks", 30, 1, 400);
        POP_HEAL = builder
            .comment("吸收普通怪物时的回血量（原版 2）", "Health restored when popping a nearby mob (vanilla 2)")
            .defineInRange("pop_heal", 2.0D, 0.0D, 1000.0D);
        builder.pop();

        builder.push("void_orb");
        VOID_ORB_ENABLED = builder
            .comment("启用虚蚀法球", "Enable Void Orb")
            .define("enabled", true);
        VOID_ORB_MULTIPLIER = builder
            .comment("虚蚀法球伤害倍率", "Void Orb damage multiplier")
            .defineInRange("multiplier", 1.5D, 0.0D, 100.0D);
        VOID_ORB_COOLDOWN_TICKS = builder
            .comment("虚蚀法球冷却（tick）", "Void Orb cooldown in ticks")
            .defineInRange("cooldown_ticks", 180, 1, 12000);
        VOID_ORB_SPEED = builder
            .comment("虚蚀法球飞行速度（格/tick）", "Void Orb speed in blocks per tick")
            .defineInRange("speed", 0.25D, 0.05D, 2.0D);
        VOID_ORB_LIFETIME = builder
            .comment("虚蚀法球追踪时长（tick）", "Void Orb homing lifetime in ticks")
            .defineInRange("lifetime", 160, 20, 1200);
        builder.pop();

        builder.push("mirror_corridor");
        MIRROR_ENABLED = builder
            .comment("启用镜影回廊（护盾首次降到一半时触发一次）", "Enable Mirror Corridor (triggers once when the shield first drops to half)")
            .define("enabled", true);
        MIRROR_RADIUS = builder
            .comment("镜影回廊站位半径", "Mirror Corridor formation radius")
            .defineInRange("radius", 8.0D, 3.0D, 24.0D);
        MIRROR_ROUNDS = builder
            .comment("镜影回廊齐射轮数", "Mirror Corridor volley rounds")
            .defineInRange("rounds", 2, 1, 6);
        builder.pop();

        builder.push("soul_candle");
        CANDLE_ENABLED = builder
            .comment("启用魂烛阵", "Enable Soul Candles")
            .define("enabled", true);
        CANDLE_COUNT = builder
            .comment("每次点燃的魂烛数量", "Soul Candles lit each time")
            .defineInRange("count", 3, 1, 8);
        CANDLE_COOLDOWN_TICKS = builder
            .comment("魂烛全部熄灭后重新点燃的冷却（tick）", "Ticks before relighting after all candles are out")
            .defineInRange("cooldown_ticks", 600, 20, 12000);
        CANDLE_STAGGER_TICKS = builder
            .comment("魂烛全部被熄灭后巫妖的硬直时长（tick）", "Lich stagger ticks after the player snuffs every candle")
            .defineInRange("stagger_ticks", 40, 0, 400);
        CANDLE_STAGGER_DAMAGE_BONUS = builder
            .comment("硬直期间巫妖额外承受的伤害比例", "Extra damage taken by the Lich while staggered")
            .defineInRange("stagger_damage_bonus", 0.2D, 0.0D, 10.0D);
        builder.pop();

        builder.push("life_siphon");
        SIPHON_ENABLED = builder
            .comment("启用生命虹吸", "Enable Life Siphon")
            .define("enabled", true);
        SIPHON_MULTIPLIER = builder
            .comment("生命虹吸每秒伤害倍率", "Life Siphon damage multiplier per second")
            .defineInRange("multiplier", 0.4D, 0.0D, 100.0D);
        SIPHON_HEAL_RATIO = builder
            .comment("生命虹吸回血 = 实际伤害 * 该比例", "Life Siphon heal = damage dealt * this ratio")
            .defineInRange("heal_ratio", 2.0D, 0.0D, 100.0D);
        SIPHON_DURATION = builder
            .comment("生命虹吸持续时长（tick）", "Life Siphon duration in ticks")
            .defineInRange("duration", 80, 20, 1200);
        SIPHON_COOLDOWN_TICKS = builder
            .comment("生命虹吸冷却（tick）", "Life Siphon cooldown in ticks")
            .defineInRange("cooldown_ticks", 240, 1, 12000);
        SIPHON_BREAK_RANGE = builder
            .comment("目标超过该距离时断链", "The chain breaks when the target is farther than this")
            .defineInRange("break_range", 14.0D, 2.0D, 64.0D);
        builder.pop();

        builder.push("twilight_blink");
        BLINK_ENABLED = builder
            .comment("启用暮光瞬斩", "Enable Twilight Blink")
            .define("enabled", true);
        BLINK_MULTIPLIER = builder
            .comment("暮光瞬斩伤害倍率", "Twilight Blink damage multiplier")
            .defineInRange("multiplier", 1.2D, 0.0D, 100.0D);
        BLINK_COOLDOWN_TICKS = builder
            .comment("暮光瞬斩冷却（tick）", "Twilight Blink cooldown in ticks")
            .defineInRange("cooldown_ticks", 140, 1, 12000);
        BLINK_WARNING_TICKS = builder
            .comment("落点预警时长（tick）", "Landing warning ticks")
            .defineInRange("warning_ticks", 12, 1, 100);
        BLINK_KNOCKBACK = builder
            .comment("暮光瞬斩击退强度，受击退抗性影响", "Twilight Blink knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 0.6D, 0.0D, 1.2D);
        builder.pop();

        builder.push("crown_collapse");
        CROWN_ENABLED = builder
            .comment("启用坍缩王冠", "Enable Crown Collapse")
            .define("enabled", true);
        CROWN_HEALTH_THRESHOLD = builder
            .comment("第三阶段血量低于该比例时解锁", "Unlocks in phase three below this health ratio")
            .defineInRange("health_threshold", 0.3D, 0.0D, 1.0D);
        CROWN_MULTIPLIER = builder
            .comment("坍缩王冠爆发伤害倍率", "Crown Collapse burst damage multiplier")
            .defineInRange("multiplier", 2.0D, 0.0D, 100.0D);
        CROWN_PULL_STRENGTH = builder
            .comment("每 tick 牵引速度，乘以（1 - 击退抗性）", "Pull speed per tick, multiplied by (1 - knockback resistance)")
            .defineInRange("pull_strength", 0.08D, 0.0D, 0.5D);
        CROWN_PULL_RANGE = builder
            .comment("牵引范围", "Pull range")
            .defineInRange("pull_range", 12.0D, 2.0D, 32.0D);
        CROWN_RADIUS = builder
            .comment("爆发伤害半径", "Burst damage radius")
            .defineInRange("radius", 4.0D, 1.0D, 16.0D);
        CROWN_KNOCKBACK = builder
            .comment("爆发击退强度，受击退抗性影响", "Burst knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 1.0D, 0.0D, 1.2D);
        CROWN_COOLDOWN_TICKS = builder
            .comment("坍缩王冠冷却（tick）", "Crown Collapse cooldown in ticks")
            .defineInRange("cooldown_ticks", 500, 1, 12000);
        CROWN_VULNERABLE_BONUS = builder
            .comment("引导期间巫妖额外承受的伤害比例", "Extra damage taken by the Lich while channeling")
            .defineInRange("vulnerable_bonus", 0.25D, 0.0D, 10.0D);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private LichConfig() {
    }
}
