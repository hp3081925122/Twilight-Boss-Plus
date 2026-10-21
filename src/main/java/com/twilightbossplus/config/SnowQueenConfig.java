package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SnowQueenConfig {
    public static final class Skill {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue cooldown;
        public final ModConfigSpec.DoubleValue multiplier;

        Skill(ModConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 冰雪女王攻击力 * 倍率", en + " damage multiplier; damage = Snow Queen attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
        }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SKILL_KNOCKBACK;
    public static final ModConfigSpec.IntValue SKILL_INTERVAL;
    public static final ModConfigSpec.IntValue FREEZE_TICKS;
    public static final ModConfigSpec.DoubleValue HOVER_HEIGHT;
    public static final ModConfigSpec.DoubleValue PHASE_TWO_HEALTH;
    public static final ModConfigSpec.DoubleValue PHASE_THREE_HEALTH;
    public static final ModConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;

    public static final Skill FROST_WALTZ;
    public static final Skill MIRROR_SHELL;
    public static final ModConfigSpec.DoubleValue SHELL_DAMAGE_REDUCTION;
    public static final Skill RIME_TRACE;
    public static final Skill GLACIAL_COFFIN;
    public static final ModConfigSpec.IntValue COFFIN_SLOW_TICKS;
    public static final ModConfigSpec.IntValue COFFIN_MAX_TARGETS;
    public static final Skill CRYSTAL_COURT;
    public static final ModConfigSpec.IntValue COURT_CRYSTALS;
    public static final Skill AURORA_MIRRORS;
    public static final ModConfigSpec.IntValue MIRROR_CYCLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("snow_queen");
        ENABLED = b.comment("启用冰雪女王增强，关闭后保持暮色原版；开启时原版召唤、下坠与冰息循环被新技能取代", "Enable the Snow Queen rework; disable to keep vanilla. When enabled the vanilla summon, drop and breath cycle is replaced").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.3, 0.1, 100.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率，技能伤害以女王攻击力为基础", "Attack damage multiplier; every skill scales from the queen's attack damage").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 30, 0, 1200);
        FREEZE_TICKS = b.comment("冰霜技能每次命中增加的冻结值，原版完全冻结需要 140，穿皮革护甲可免疫", "Freeze ticks added by each frost hit; vanilla needs 140 to be fully frozen, leather armor grants immunity").defineInRange("freeze_ticks", 90, 0, 1000);
        HOVER_HEIGHT = b.comment("空闲时环绕目标的悬浮高度", "Hover height above the target while idle").defineInRange("hover_height", 3.5, 1.0, 16.0);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("进入二阶段的血量比例，进入时强制召唤冰晶廷卫", "Health ratio to enter phase two; forces Crystal Court").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("进入三阶段的血量比例，进入时强制极光折镜", "Health ratio to enter phase three; forces Aurora Mirrors").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段技能冷却倍率", "Skill cooldown multiplier in phase three").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        b.pop();

        b.comment("全阶段近战：霜刃圆舞，俯冲贴地旋转，六片冰刃先外扩再回收，贴身或远离可躲", "All phases, close range: Frost Waltz, six ice blades spiral outwards then back; stay hugged or far away");
        FROST_WALTZ = new Skill(b, "frost_waltz", "霜刃圆舞", "Frost Waltz", 100, 0.9);
        b.pop();

        b.comment("全阶段近战：镜冰反击，展开镜面冰壳并减伤，被攻击时立即碎裂反击，未被攻击则结束时小范围爆开", "All phases, close range: Mirror Shell, a mirror shell reduces damage and shatters into a counter when struck, otherwise bursts weakly at the end");
        MIRROR_SHELL = new Skill(b, "mirror_shell", "镜冰反击", "Mirror Shell", 160, 1.2);
        SHELL_DAMAGE_REDUCTION = b.comment("冰壳期间的减伤比例", "Damage reduction while the shell is up").defineInRange("damage_reduction", 0.6, 0.0, 1.0);
        b.pop();

        b.comment("全阶段：霜痕描摹，升空吐出冰息在地面描线追踪目标，线上霜花按描绘顺序依次绽放", "All phases: Rime Trace, the breath draws a chasing line on the ground and frost flowers bloom along it in order");
        RIME_TRACE = new Skill(b, "rime_trace", "霜痕描摹", "Rime Trace", 140, 1.0);
        b.pop();

        b.comment("全阶段：冰棺封印，在玩家脚下升起六面冰棺，后半段棺壁会把人推回棺内，闭合时封冻并造成伤害", "All phases: Glacial Coffin, a hexagonal coffin rises around players; its walls push back inwards late, closing freezes and damages");
        GLACIAL_COFFIN = new Skill(b, "glacial_coffin", "冰棺封印", "Glacial Coffin", 220, 1.3);
        COFFIN_SLOW_TICKS = b.comment("被封住后的缓慢时长（tick）", "Slowness duration in ticks after being sealed").defineInRange("slow_ticks", 50, 0, 1200);
        COFFIN_MAX_TARGETS = b.comment("一次最多封印的玩家数", "Max players sealed per cast").defineInRange("max_targets", 3, 1, 16);
        b.pop();

        b.comment("二阶段：冰晶廷卫，召唤暮色原版冰晶守卫，并向四周释放极光脉冲", "Phase 2: Crystal Court, summons Twilight Forest ice crystals and releases an aurora pulse");
        CRYSTAL_COURT = new Skill(b, "crystal_court", "冰晶廷卫", "Crystal Court", 500, 0.6);
        COURT_CRYSTALS = b.comment("每次召唤的冰晶数量", "Ice crystals per summon").defineInRange("crystals", 3, 0, 12);
        b.pop();

        b.comment("三阶段：极光折镜，立起六面冰镜，极光在镜间折射，第一轮六芒星中心安全，第二轮外圈六边形内部安全", "Phase 3: Aurora Mirrors, six ice mirrors reflect aurora beams; first a hexagram with a safe center, then the outer hexagon with a safe inside");
        AURORA_MIRRORS = new Skill(b, "aurora_mirrors", "极光折镜", "Aurora Mirrors", 360, 1.4);
        MIRROR_CYCLES = b.comment("折射轮数，六芒星与六边形交替", "Number of reflection cycles, alternating hexagram and hexagon").defineInRange("cycles", 2, 1, 8);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private SnowQueenConfig() {
    }
}
