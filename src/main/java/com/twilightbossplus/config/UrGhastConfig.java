package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class UrGhastConfig {
    public static final class Skill {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue cooldown;
        public final ModConfigSpec.DoubleValue multiplier;

        Skill(ModConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 恶魂攻击力 * 倍率", en + " damage multiplier; damage = Ur-Ghast attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
        }
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue BASE_ATTACK;
    public static final ModConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SKILL_KNOCKBACK;
    public static final ModConfigSpec.IntValue SKILL_INTERVAL;
    public static final ModConfigSpec.DoubleValue PHASE_TWO_HEALTH;
    public static final ModConfigSpec.DoubleValue PHASE_THREE_HEALTH;
    public static final ModConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;

    public static final Skill TEAR_VOLLEY;
    public static final ModConfigSpec.IntValue VOLLEY_TEARS;
    public static final Skill WAILING_CONE;
    public static final ModConfigSpec.IntValue WAIL_SLOWNESS_TICKS;
    public static final Skill TEAR_STORM;
    public static final ModConfigSpec.IntValue STORM_INTERVAL;
    public static final Skill TEAR_PRISM;
    public static final ModConfigSpec.IntValue PRISM_CYCLES;
    public static final Skill CRIMSON_LIGHTNING;
    public static final ModConfigSpec.IntValue LIGHTNING_STRIKES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ur_ghast");
        ENABLED = b.comment("启用暮初恶魂增强，关闭后保持暮色原版；开启时原版火球与暴怒泪雨伤害被新技能取代，召唤小恶魂与暴怒减伤保留", "Enable the Ur-Ghast rework; disable to keep vanilla. When enabled the vanilla fireballs and tantrum tear damage are replaced, minion summoning and tantrum damage reduction are kept").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.2, 0.1, 100.0);
        BASE_ATTACK = b.comment("基础攻击力，恶魂本身没有攻击力属性，所有技能伤害以此为基础", "Base attack damage; the Ur-Ghast has no attack attribute so every skill scales from this value").defineInRange("base_attack", 10.0, 0.0, 1000.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率", "Attack damage multiplier").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 40, 0, 1200);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("进入二阶段的血量比例，进入时强制释放泪晶棱阵", "Health ratio to enter phase two; forces Tear Prism").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("进入三阶段的血量比例，进入时强制释放赤泪雷殛", "Health ratio to enter phase three; forces Crimson Lightning").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段技能冷却倍率", "Skill cooldown multiplier in phase three").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        b.pop();

        b.comment("全阶段：泪珠追魂，赤晶泪珠先环绕恶魂，再逐颗射出并缓慢追踪目标，命中或撞墙时溅开", "All phases: Tear Volley, crimson tears orbit the Ur-Ghast and are fired one by one, slowly homing and splashing on impact");
        TEAR_VOLLEY = new Skill(b, "tear_volley", "泪珠追魂", "Tear Volley", 90, 0.8);
        VOLLEY_TEARS = b.comment("一轮泪珠数量，二阶段起额外 +2", "Tears per volley, +2 from phase two").defineInRange("tears", 6, 1, 30);
        b.pop();

        b.comment("全阶段：恸哭锥波，恶魂吸气后朝目标发出三道锥形声浪，命中造成伤害并减速，侧移出锥形范围即可躲避", "All phases: Wailing Cone, three cone-shaped sound waves toward the target that damage and slow; step out of the cone to dodge");
        WAILING_CONE = new Skill(b, "wailing_cone", "恸哭锥波", "Wailing Cone", 140, 0.7);
        WAIL_SLOWNESS_TICKS = b.comment("声浪造成的缓慢时长（tick）", "Slowness duration from a wave in ticks").defineInRange("slowness_ticks", 40, 0, 600);
        b.pop();

        b.comment("暴怒时：泪幕风暴，取代原版暴怒的泪水伤害，恶魂身下形成乌云，带缺口的泪水雨幕从玩家身旁横扫而过，从缺口穿过即可躲避", "During tantrum: Tear Curtain, replaces the vanilla tantrum damage with sweeping walls of tear rain that each have a gap to slip through");
        TEAR_STORM = new Skill(b, "tear_storm", "泪幕风暴", "Tear Curtain", 1, 0.5);
        STORM_INTERVAL = b.comment("每名玩家两道雨幕之间的间隔（tick），三阶段缩短 30%", "Ticks between two curtains per player, 30% shorter in phase three").defineInRange("interval_ticks", 50, 10, 1200);
        b.pop();

        b.comment("二阶段：泪晶棱阵，目标周围升起六颗泪晶，两两对穿连成三道贯穿中心的光束，先闪烁预警再点亮，每轮旋转角度不同，站在光束间隙即可躲避", "Phase 2: Tear Prism, six crystals rise around the target and link into three beams through the center; each cycle flashes a warning first and rotates, stand between the beams to dodge");
        TEAR_PRISM = new Skill(b, "tear_prism", "泪晶棱阵", "Tear Prism", 320, 0.9);
        PRISM_CYCLES = b.comment("光束轮数", "Number of beam cycles").defineInRange("cycles", 4, 1, 20);
        b.pop();

        b.comment("三阶段：赤泪雷殛，赤红雷电追着目标落下，每道雷落下前地面都有预警", "Phase 3: Crimson Lightning, telegraphed crimson bolts chase the target");
        CRIMSON_LIGHTNING = new Skill(b, "crimson_lightning", "赤泪雷殛", "Crimson Lightning", 360, 1.6);
        LIGHTNING_STRIKES = b.comment("雷击次数", "Number of strikes").defineInRange("strikes", 8, 1, 40);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private UrGhastConfig() {
    }
}
