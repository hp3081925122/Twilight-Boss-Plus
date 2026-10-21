package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class YetiConfig {
    public static final class Skill {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue cooldown;
        public final ModConfigSpec.DoubleValue multiplier;

        Skill(ModConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 雪怪首领攻击力 * 倍率", en + " damage multiplier; damage = Alpha Yeti attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
        }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BASE_ATTACK;
    public static final ModConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SKILL_KNOCKBACK;
    public static final ModConfigSpec.IntValue SKILL_INTERVAL;
    public static final ModConfigSpec.IntValue FREEZE_TICKS;
    public static final ModConfigSpec.DoubleValue TIRED_DAMAGE_BONUS;
    public static final ModConfigSpec.DoubleValue PHASE_TWO_HEALTH;
    public static final ModConfigSpec.DoubleValue PHASE_THREE_HEALTH;
    public static final ModConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;

    public static final Skill FROST_CLAP;
    public static final Skill AVALANCHE_POUNCE;
    public static final Skill ROLLING_SNOWBALL;
    public static final Skill PERMAFROST_FIELD;
    public static final ModConfigSpec.IntValue FIELD_WAVES;
    public static final Skill WHITEOUT_RAMPAGE;
    public static final ModConfigSpec.DoubleValue RAMPAGE_DAMAGE_RATIO;
    public static final ModConfigSpec.IntValue RAMPAGE_LEAPS;
    public static final ModConfigSpec.DoubleValue SHARD_MULTIPLIER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("alpha_yeti");
        ENABLED = b.comment("启用雪怪首领增强，关闭后保持暮色原版；开启时原版冰弹与原版暴走被新技能取代，抓人投掷与疲惫保留", "Enable the Alpha Yeti rework; disable to keep vanilla. When enabled the vanilla ice bombs and rampage are replaced, grabbing and tiredness are kept").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.3, 0.1, 100.0);
        BASE_ATTACK = b.comment("基础攻击力，雪怪首领原版攻击力只有 1，所有技能伤害以此为基础", "Base attack damage; the vanilla Alpha Yeti only has 1 attack damage so every skill scales from this value").defineInRange("base_attack", 9.0, 0.0, 1000.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率", "Attack damage multiplier").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 30, 0, 1200);
        FREEZE_TICKS = b.comment("冰霜技能每次命中增加的冻结值，原版完全冻结需要 140，穿皮革护甲可免疫", "Freeze ticks added by each frost hit; vanilla needs 140 to be fully frozen, leather armor grants immunity").defineInRange("freeze_ticks", 100, 0, 1000);
        TIRED_DAMAGE_BONUS = b.comment("疲惫时受到的额外伤害比例", "Extra damage ratio taken while tired").defineInRange("tired_damage_bonus", 0.3, 0.0, 10.0);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("进入二阶段的血量比例，进入时强制释放霜晶棋盘", "Health ratio to enter phase two; forces Permafrost Field").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("进入三阶段的血量比例，进入时强制白魇暴走", "Health ratio to enter phase three; forces Whiteout Rampage").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段技能冷却倍率", "Skill cooldown multiplier in phase three").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        b.pop();

        b.comment("全阶段：霜冻合掌，双臂张开后猛然合掌，前方扇形喷出冰霜，造成伤害并冻结", "All phases: Frost Clap, a frontal frost cone that damages and freezes");
        FROST_CLAP = new Skill(b, "frost_clap", "霜冻合掌", "Frost Clap", 80, 1.0);
        b.pop();

        b.comment("全阶段：雪崩扑击，下蹲蓄力后跃向目标，落点出现雪花预警，落地冲击并冻结", "All phases: Avalanche Pounce, leaps at the target with a telegraphed landing that damages and freezes");
        AVALANCHE_POUNCE = new Skill(b, "avalanche_pounce", "雪崩扑击", "Avalanche Pounce", 140, 1.3);
        b.pop();

        b.comment("全阶段：巨雪滚球，搓出雪球滚向目标并越滚越大，侧移即可躲避，三阶段一次三颗", "All phases: Rolling Snowball, a growing snowball rolls at the target; sidestep to dodge, three at once in phase three");
        ROLLING_SNOWBALL = new Skill(b, "rolling_snowball", "巨雪滚球", "Rolling Snowball", 160, 1.4);
        b.pop();

        b.comment("二阶段：霜晶棋盘，地面铺开冰砖棋盘，交替亮起后刺出冰棘，雪怪脚下一格每轮都会刺出", "Phase 2: Permafrost Field, a checkerboard of ice tiles lights up in turns and erupts into spikes; the tile under the yeti erupts every wave");
        PERMAFROST_FIELD = new Skill(b, "permafrost_field", "霜晶棋盘", "Permafrost Field", 300, 1.1);
        FIELD_WAVES = b.comment("冰棘轮数", "Number of spike waves").defineInRange("waves", 3, 1, 12);
        b.pop();

        b.comment("受伤累计触发：白魇暴走，取代原版暴走，咆哮后连续扑击，每次落地放射冰晶，结束后进入原版疲惫", "Triggered by damage taken: Whiteout Rampage, replaces the vanilla rampage with chained pounces that release ice shards, then the yeti gets tired");
        WHITEOUT_RAMPAGE = new Skill(b, "whiteout_rampage", "白魇暴走", "Whiteout Rampage", 1, 1.2);
        RAMPAGE_DAMAGE_RATIO = b.comment("累计受到该比例最大生命的伤害后触发暴走", "Rampage triggers after taking this ratio of max health as damage").defineInRange("damage_ratio", 0.18, 0.01, 1.0);
        RAMPAGE_LEAPS = b.comment("暴走扑击次数，三阶段 +1", "Pounces per rampage, +1 in phase three").defineInRange("leaps", 3, 1, 10);
        SHARD_MULTIPLIER = b.comment("落地冰晶伤害倍率", "Ice shard damage multiplier").defineInRange("shard_multiplier", 0.6, 0.0, 100.0);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private YetiConfig() {
    }
}
