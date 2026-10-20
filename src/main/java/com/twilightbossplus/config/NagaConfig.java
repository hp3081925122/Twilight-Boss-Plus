package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class NagaConfig {
    public static final class Skill {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue cooldown;
        public final ForgeConfigSpec.DoubleValue multiplier;

        Skill(ForgeConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 娜迦攻击力 * 倍率", en + " damage multiplier; damage = Naga attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MELEE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue SKILL_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue SKILL_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue PHASE_TWO_HEALTH;
    public static final ForgeConfigSpec.DoubleValue PHASE_THREE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RAGE_SPEED_BONUS;

    public static final Skill SERPENT_LUNGE;
    public static final ForgeConfigSpec.IntValue LUNGE_WINDUP;
    public static final Skill VENOM_SPIT;
    public static final ForgeConfigSpec.DoubleValue SPIT_POOL_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SPIT_POOL_TICKS;
    public static final Skill TAIL_LASH;
    public static final Skill FANG_SNAP;
    public static final ForgeConfigSpec.IntValue FANG_SNAPS;
    public static final Skill QUAKE_SLAM;
    public static final ForgeConfigSpec.DoubleValue QUAKE_WAVE_MULTIPLIER;
    public static final Skill BURROW_AMBUSH;
    public static final Skill FRENZIED_HISS;
    public static final Skill VENOM_BREATH;
    public static final Skill SERPENT_CHAIN;
    public static final ForgeConfigSpec.IntValue CHAIN_COUNT;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("naga");
        ENABLED = b.comment("启用娜迦增强，关闭后保持暮色原版", "Enable the Naga rework; disable to keep vanilla Twilight Forest behavior").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.0, 0.1, 100.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率，所有技能伤害都以攻击力为基础", "Attack damage multiplier; every skill scales from attack damage").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        MELEE_KNOCKBACK = b.comment("近战击退倍率（原版为 1.0），再乘以 1 - 击退抗性", "Melee knockback multiplier (vanilla 1.0), also scaled by 1 - knockback resistance").defineInRange("melee_knockback", 0.45, 0.0, 1.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 60, 0, 1200);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("进入二阶段的血量比例，进入时强制释放潜地伏击", "Health ratio to enter phase two; forces a Burrow Ambush").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("进入三阶段的血量比例，进入时释放狂怒嘶啸", "Health ratio to enter phase three; triggers Frenzied Hiss").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段技能冷却倍率", "Skill cooldown multiplier in phase three").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        RAGE_SPEED_BONUS = b.comment("三阶段移动速度加成（比例）", "Movement speed bonus in phase three (ratio)").defineInRange("rage_speed_bonus", 0.2, 0.0, 2.0);
        b.pop();

        b.comment("一阶段：蛇袭突进，盘身蓄力后沿预警线直线突进，被举盾挡下会陷入眩晕", "Phase 1: Serpent Lunge, a telegraphed straight dash; blocking it with a shield dazes the Naga");
        SERPENT_LUNGE = new Skill(b, "serpent_lunge", "蛇袭突进", "Serpent Lunge", 140, 1.2);
        LUNGE_WINDUP = b.comment("蛇袭突进蓄力时间（tick）", "Serpent Lunge windup in ticks").defineInRange("windup_ticks", 20, 5, 100);
        b.pop();

        b.comment("一阶段：毒涎喷吐，抛射毒液团，落点留下毒池", "Phase 1: Venom Spit, lobbed venom globs that leave poison pools");
        VENOM_SPIT = new Skill(b, "venom_spit", "毒涎喷吐", "Venom Spit", 200, 0.6);
        SPIT_POOL_MULTIPLIER = b.comment("毒池每 0.5 秒伤害倍率", "Poison pool damage multiplier per half second").defineInRange("pool_multiplier", 0.15, 0.0, 100.0);
        SPIT_POOL_TICKS = b.comment("毒池持续时间（tick）", "Poison pool duration in ticks").defineInRange("pool_ticks", 120, 20, 1200);
        b.pop();

        b.comment("一阶段：尾鞭横扫，玩家贴近尾部时甩尾", "Phase 1: Tail Lash, swings the tail at players hugging the body");
        TAIL_LASH = new Skill(b, "tail_lash", "尾鞭横扫", "Tail Lash", 100, 0.9);
        b.pop();

        b.comment("一阶段：毒牙连咬，玩家贴近蛇头时张口连续撕咬前方并施加中毒", "Phase 1: Fang Snap, snaps the jaws at players close to the head and poisons them");
        FANG_SNAP = new Skill(b, "fang_snap", "毒牙连咬", "Fang Snap", 90, 0.8);
        FANG_SNAPS = b.comment("毒牙连咬撕咬次数", "Number of bites in Fang Snap").defineInRange("snaps", 2, 1, 5);
        b.pop();

        b.comment("二阶段：震地猛击，跃起砸地并扩散三道可跳跃躲避的冲击环", "Phase 2: Quake Slam, leaps and slams, sending three jumpable shockwave rings");
        QUAKE_SLAM = new Skill(b, "quake_slam", "震地猛击", "Quake Slam", 260, 1.3);
        QUAKE_WAVE_MULTIPLIER = b.comment("冲击环伤害倍率", "Shockwave ring damage multiplier").defineInRange("wave_multiplier", 0.7, 0.0, 100.0);
        b.pop();

        b.comment("二阶段：潜地伏击，钻入地下追踪目标后破土而出", "Phase 2: Burrow Ambush, burrows, tracks the target underground and erupts");
        BURROW_AMBUSH = new Skill(b, "burrow_ambush", "潜地伏击", "Burrow Ambush", 320, 1.5);
        b.pop();

        b.comment("三阶段：狂怒嘶啸，转阶段时释放的冲击波", "Phase 3: Frenzied Hiss, the shockwave released when entering phase three");
        FRENZIED_HISS = new Skill(b, "frenzied_hiss", "狂怒嘶啸", "Frenzied Hiss", 1, 0.5);
        b.pop();

        b.comment("三阶段：毒雾吐息，向前方扇形持续喷吐毒雾并缓慢转向", "Phase 3: Venom Breath, a sweeping cone of poison mist");
        VENOM_BREATH = new Skill(b, "venom_breath", "毒雾吐息", "Venom Breath", 240, 0.3);
        b.pop();

        b.comment("三阶段：连环蛇袭，连续多次蛇袭突进", "Phase 3: Serpent Chain, several Serpent Lunges in a row");
        SERPENT_CHAIN = new Skill(b, "serpent_chain", "连环蛇袭", "Serpent Chain", 280, 1.0);
        CHAIN_COUNT = b.comment("连环蛇袭突进次数", "Number of lunges in Serpent Chain").defineInRange("lunges", 3, 2, 6);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private NagaConfig() {
    }
}
