package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MinoConfig {
    public static final class Skill {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue cooldown;
        public final ForgeConfigSpec.DoubleValue multiplier;

        Skill(ForgeConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 米诺菇攻击力 * 倍率", en + " damage multiplier; damage = Minoshroom attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SKILL_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue SKILL_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue PHASE_TWO_HEALTH;
    public static final ForgeConfigSpec.DoubleValue PHASE_THREE_HEALTH;
    public static final ForgeConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RAGE_SPEED_BONUS;
    public static final ForgeConfigSpec.IntValue STUN_TICKS;
    public static final ForgeConfigSpec.DoubleValue STUN_DAMAGE_BONUS;

    public static final Skill GORE_RUSH;
    public static final ForgeConfigSpec.IntValue RUSH_WINDUP;
    public static final Skill AXE_FISSURE;
    public static final Skill AXE_CLEAVE;
    public static final ForgeConfigSpec.DoubleValue CHOP_MULTIPLIER;
    public static final Skill SPORE_BURST;
    public static final ForgeConfigSpec.IntValue SPORE_TICKS;
    public static final Skill WHIRLWIND;
    public static final Skill LABYRINTH_QUAKE;
    public static final ForgeConfigSpec.DoubleValue RUBBLE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue RUBBLE_COUNT;
    public static final Skill MYCELIUM_SNARE;
    public static final ForgeConfigSpec.IntValue ROOT_TICKS;
    public static final Skill BULL_RAGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("minoshroom");
        ENABLED = b.comment("启用米诺菇增强，关闭后保持暮色原版；开启时原版冲锋与震地被新技能取代", "Enable the Minoshroom rework; disable to keep vanilla. When enabled the vanilla charge and ground slam are replaced").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.5, 0.1, 100.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率，所有技能伤害都以攻击力为基础（含钻石牛头人斧加成）", "Attack damage multiplier; every skill scales from attack damage (including the diamond minotaur axe)").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 50, 0, 1200);
        STUN_TICKS = b.comment("冲锋撞墙后的眩晕时间（tick）", "Stun duration after ramming a wall, in ticks").defineInRange("stun_ticks", 50, 0, 400);
        STUN_DAMAGE_BONUS = b.comment("眩晕期间额外承受的伤害比例", "Extra damage taken while stunned (ratio)").defineInRange("stun_damage_bonus", 0.3, 0.0, 5.0);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("进入二阶段的血量比例，进入时强制释放迷宫震地", "Health ratio to enter phase two; forces Labyrinth Quake").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("进入三阶段的血量比例，进入时释放狂牛之怒", "Health ratio to enter phase three; triggers Bull Rage").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段技能冷却倍率", "Skill cooldown multiplier in phase three").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        RAGE_SPEED_BONUS = b.comment("三阶段移动速度加成（比例）", "Movement speed bonus in phase three (ratio)").defineInRange("rage_speed_bonus", 0.15, 0.0, 2.0);
        b.pop();

        b.comment("全阶段：蛮牛冲锋，刨地蓄力后沿预警线冲锋；撞墙会眩晕，举盾可挡下伤害但盾牌会被击破；三阶段连续冲锋两次", "All phases: Gore Rush, paws the ground then charges along a telegraphed line; ramming a wall stuns it, a shield blocks the damage but gets disabled; charges twice in phase three");
        GORE_RUSH = new Skill(b, "gore_rush", "蛮牛冲锋", "Gore Rush", 140, 1.3);
        RUSH_WINDUP = b.comment("冲锋蓄力时间（tick）", "Gore Rush windup in ticks").defineInRange("windup_ticks", 18, 5, 100);
        b.pop();

        b.comment("全阶段：裂地战斧，战斧劈地，地裂沿直线向前推进并掀飞地面目标", "All phases: Axe Fissure, an axe slam that sends a fissure racing forward along the ground");
        AXE_FISSURE = new Skill(b, "axe_fissure", "裂地战斧", "Axe Fissure", 160, 1.0);
        b.pop();

        b.comment("全阶段：斧刃连斩，玩家贴身时先横扫再竖劈，竖劈落点会崩裂地面", "All phases: Axe Cleave, a close-range horizontal sweep followed by an overhead chop that cracks the ground");
        AXE_CLEAVE = new Skill(b, "axe_cleave", "斧刃连斩", "Axe Cleave", 90, 0.9);
        CHOP_MULTIPLIER = b.comment("竖劈伤害倍率，伤害 = 米诺菇攻击力 * 倍率", "Overhead chop damage multiplier; damage = Minoshroom attack damage * multiplier").defineInRange("chop_multiplier", 1.3, 0.0, 100.0);
        b.pop();

        b.comment("全阶段：孢子爆发，在目标周围撒下孢子云，云中持续受伤并减速虚弱", "All phases: Spore Burst, spore clouds around the target that hurt, slow and weaken");
        SPORE_BURST = new Skill(b, "spore_burst", "孢子爆发", "Spore Burst", 240, 0.25);
        SPORE_TICKS = b.comment("孢子云持续时间（tick）", "Spore cloud duration in ticks").defineInRange("cloud_ticks", 120, 20, 1200);
        b.pop();

        b.comment("二阶段：旋风斧，持斧旋转并缓慢逼近目标", "Phase 2: Whirlwind, spins with the axe while slowly closing in");
        WHIRLWIND = new Skill(b, "whirlwind", "旋风斧", "Whirlwind", 260, 0.6);
        b.pop();

        b.comment("二阶段：迷宫震地，重踏震伤地面目标，随后迷宫顶部落石", "Phase 2: Labyrinth Quake, a stomp that hurts grounded targets followed by falling rubble");
        LABYRINTH_QUAKE = new Skill(b, "labyrinth_quake", "迷宫震地", "Labyrinth Quake", 300, 1.2);
        RUBBLE_MULTIPLIER = b.comment("落石伤害倍率", "Falling rubble damage multiplier").defineInRange("rubble_multiplier", 0.9, 0.0, 100.0);
        RUBBLE_COUNT = b.comment("落石数量", "Number of falling rubble").defineInRange("rubble_count", 6, 0, 30);
        b.pop();

        b.comment("二阶段：菌丝缠绕，菌丝法阵定身目标后立即接一次蛮牛冲锋", "Phase 2: Mycelium Snare, roots the target with mycelium then immediately follows with a Gore Rush");
        MYCELIUM_SNARE = new Skill(b, "mycelium_snare", "菌丝缠绕", "Mycelium Snare", 280, 0.4);
        ROOT_TICKS = b.comment("定身时间（tick）", "Root duration in ticks").defineInRange("root_ticks", 30, 5, 200);
        b.pop();

        b.comment("三阶段：狂牛之怒，转阶段时怒吼重踏，释放两道可跳跃躲避的冲击环并撒下孢子云", "Phase 3: Bull Rage, a roaring stomp on entering phase three with two jumpable shockwave rings and spore clouds");
        BULL_RAGE = new Skill(b, "bull_rage", "狂牛之怒", "Bull Rage", 1, 0.8);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private MinoConfig() {
    }
}
