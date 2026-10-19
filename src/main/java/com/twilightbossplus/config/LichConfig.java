package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class LichConfig {
    public static final class Skill {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue cooldown;
        public final ForgeConfigSpec.DoubleValue multiplier;

        Skill(ForgeConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 巫妖攻击力 * 倍率", en + " damage multiplier; damage = Lich attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SKILL_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue SKILL_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue RAGE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;

    public static final Skill SHADOW_CROSSFIRE;
    public static final ForgeConfigSpec.IntValue CROSSFIRE_WINDUP;
    public static final Skill CONSTELLATION;
    public static final ForgeConfigSpec.IntValue CONSTELLATION_STARS;
    public static final Skill SHIELD_BACKLASH;
    public static final Skill CORPSE_DETONATION;
    public static final ForgeConfigSpec.IntValue CORPSE_FUSE;
    public static final Skill LIFE_DRAIN;
    public static final ForgeConfigSpec.DoubleValue DRAIN_HEAL_RATIO;
    public static final Skill SOUL_PRISON;
    public static final ForgeConfigSpec.IntValue PRISON_TICKS;
    public static final Skill SHADOW_BLINK;
    public static final ForgeConfigSpec.IntValue BLINK_COUNT;
    public static final Skill TWILIGHT_COLLAPSE;
    public static final ForgeConfigSpec.DoubleValue COLLAPSE_WAVE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("lich");
        ENABLED = b.comment("启用巫妖增强，关闭后保持暮色原版", "Enable the Lich rework; disable to keep vanilla Twilight Forest behavior").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.5, 0.1, 100.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率，所有技能伤害都以攻击力为基础（原版攻击力 3）", "Attack damage multiplier; every skill scales from attack damage (vanilla attack is 3)").defineInRange("attack_multiplier", 1.5, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 50, 0, 1200);

        b.push("phase");
        b.comment("巫妖的阶段沿用暮色原版：护盾期（影分身）→ 召唤期（亡灵仆从）→ 近战期（金剑）", "Lich phases follow vanilla Twilight Forest: shield (shadow clones) -> minions -> melee (golden sword)");
        RAGE_HEALTH = b.comment("狂怒血量比例，首次低于该比例时释放暮色坍缩", "Health ratio for rage; Twilight Collapse is cast the first time health drops below it").defineInRange("rage_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("狂怒后技能冷却倍率", "Skill cooldown multiplier after rage").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        b.pop();

        b.comment("护盾期：幻影交叉射线，本体与影分身同时锁定目标并齐射暮光射线", "Shield phase: Shadow Crossfire, the Lich and its shadow clones lock on and fire twilight beams together");
        SHADOW_CROSSFIRE = new Skill(b, "shadow_crossfire", "幻影交叉射线", "Shadow Crossfire", 160, 2.0);
        CROSSFIRE_WINDUP = b.comment("射线锁定预警时间（tick）", "Beam lock-on warning in ticks").defineInRange("windup_ticks", 22, 8, 100);
        b.pop();

        b.comment("全阶段：暮光星阵，在目标脚下连续落下星光法阵，延迟后喷发光柱", "All phases: Twilight Constellation, sigils fall under the target and erupt into light pillars");
        CONSTELLATION = new Skill(b, "twilight_constellation", "暮光星阵", "Twilight Constellation", 220, 2.2);
        CONSTELLATION_STARS = b.comment("星阵数量", "Number of sigils").defineInRange("stars", 5, 1, 12);
        b.pop();

        b.comment("护盾期：护盾反震，每次护盾被击碎时向四周释放冲击，最后一层碎裂时范围更大", "Shield phase: Shield Backlash, a shockwave each time a shield layer breaks; larger on the final layer");
        SHIELD_BACKLASH = new Skill(b, "shield_backlash", "护盾反震", "Shield Backlash", 60, 1.2);
        b.pop();

        b.comment("召唤期：尸爆，标记一只亡灵仆从，引信结束后将其引爆", "Minion phase: Corpse Detonation, marks a minion and detonates it after a fuse");
        CORPSE_DETONATION = new Skill(b, "corpse_detonation", "尸爆", "Corpse Detonation", 200, 3.0);
        CORPSE_FUSE = b.comment("尸爆引信时间（tick）", "Corpse Detonation fuse in ticks").defineInRange("fuse_ticks", 40, 10, 200);
        b.pop();

        b.comment("召唤期起：生命汲取，持续吸取目标生命治疗自身，阻断视线即可打断", "Minion phase onward: Life Drain, channels a beam that heals the Lich; breaking line of sight interrupts it");
        LIFE_DRAIN = new Skill(b, "life_drain", "生命汲取", "Life Drain", 260, 0.8);
        DRAIN_HEAL_RATIO = b.comment("实际造成伤害转化为治疗的比例", "Ratio of dealt damage converted into healing").defineInRange("heal_ratio", 1.0, 0.0, 10.0);
        b.pop();

        b.comment("召唤期起：冥狱囚笼，在目标处升起灵魂牢笼，触碰牢笼边缘会受伤并被弹回", "Minion phase onward: Soul Prison, a soul cage around the target; touching its edge hurts and bounces you back");
        SOUL_PRISON = new Skill(b, "soul_prison", "冥狱囚笼", "Soul Prison", 320, 1.0);
        PRISON_TICKS = b.comment("牢笼持续时间（tick）", "Cage duration in ticks").defineInRange("duration_ticks", 80, 20, 600);
        b.pop();

        b.comment("近战期：暗影闪袭，闪现到目标身后挥出横斩，可连续多次", "Melee phase: Shadow Blink, teleports behind the target and slashes, several times in a row");
        SHADOW_BLINK = new Skill(b, "shadow_blink", "暗影闪袭", "Shadow Blink", 140, 2.5);
        BLINK_COUNT = b.comment("闪袭次数，狂怒后额外 +1", "Number of blinks; +1 after rage").defineInRange("blinks", 2, 1, 6);
        b.pop();

        b.comment("狂怒：暮色坍缩，升空并把周围目标吸向中心，随后引爆并扩散可跳跃躲避的冲击环", "Rage: Twilight Collapse, rises and pulls targets in, then detonates and sends a jumpable shockwave ring");
        TWILIGHT_COLLAPSE = new Skill(b, "twilight_collapse", "暮色坍缩", "Twilight Collapse", 1, 4.0);
        COLLAPSE_WAVE_MULTIPLIER = b.comment("冲击环伤害倍率", "Shockwave ring damage multiplier").defineInRange("wave_multiplier", 1.2, 0.0, 100.0);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private LichConfig() {
    }
}
