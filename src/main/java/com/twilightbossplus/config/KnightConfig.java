package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class KnightConfig {
    public static final class Skill {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue cooldown;
        public final ModConfigSpec.DoubleValue multiplier;

        Skill(ModConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 骑士攻击力 * 倍率", en + " damage multiplier; damage = knight attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
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

    public static final Skill ECHO_SLASH;
    public static final ModConfigSpec.DoubleValue ECHO_MULTIPLIER;
    public static final Skill REVENANT_AXE;
    public static final Skill CHAIN_HOOK;
    public static final ModConfigSpec.DoubleValue HOOK_SLAM_MULTIPLIER;
    public static final Skill CHAIN_NOOSE;
    public static final ModConfigSpec.DoubleValue NOOSE_SNAP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue NOOSE_BREAK_RATIO;
    public static final Skill LANCE_PHALANX;
    public static final Skill OATHBLADE;
    public static final ModConfigSpec.DoubleValue OATH_BLADE_MULTIPLIER;
    public static final ModConfigSpec.IntValue OATH_BLADES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("knight_phantom");
        ENABLED = b.comment("启用幻影骑士增强，关闭后保持暮色原版；开启时原版的冲锋与投掷武器被骑士团技能取代", "Enable the Knight Phantom rework; disable to keep vanilla. When enabled the vanilla charge and weapon throws are replaced by squad skills").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("每名骑士的最大生命倍率", "Max health multiplier of each knight").defineInRange("health_multiplier", 1.5, 0.1, 100.0);
        BASE_ATTACK = b.comment("基础攻击力，骑士原版攻击力只有 1，所有技能伤害以此为基础", "Base attack damage; vanilla knights only have 1 attack so every skill scales from this value").defineInRange("base_attack", 7.0, 0.0, 1000.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率", "Attack damage multiplier").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性，锁链拉拽同样受此影响", "Skill knockback multiplier, also scaled by 1 - knockback resistance; chain pulls use it too").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 40, 0, 1200);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("骑士团总血量比例低于此值进入二阶段，进入时强制释放枪阵冲锋", "Squad health ratio to enter phase two; forces Lance Phalanx").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("骑士团总血量比例低于此值进入三阶段，进入时强制释放誓约圣剑", "Squad health ratio to enter phase three; forces Oathblade").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段或只剩最后一名骑士时的技能冷却倍率", "Skill cooldown multiplier in phase three or when only one knight remains").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        b.pop();

        b.comment("全阶段：回响连斩，持剑骑士现形贴身连斩三次，每一斩都会留下残影，在原地延迟重演一次", "All phases: Echo Slash, a sword knight manifests for three close slashes; each slash leaves an afterimage that replays it after a delay");
        ECHO_SLASH = new Skill(b, "echo_slash", "回响连斩", "Echo Slash", 100, 1.0);
        ECHO_MULTIPLIER = b.comment("残影重演斩击的伤害倍率", "Damage multiplier of the replayed afterimage slash").defineInRange("echo_multiplier", 0.8, 0.0, 100.0);
        b.pop();

        b.comment("全阶段：回旋幽斧，持斧骑士凝聚巨大幽灵斧沿弧线掷出并飞回，去程与回程各判定一次；二阶段起双斧交叉", "All phases: Revenant Axe, an axe knight hurls a giant spectral axe along a curved path that returns; out and back hit separately, two crossing axes from phase two");
        REVENANT_AXE = new Skill(b, "revenant_axe", "回旋幽斧", "Revenant Axe", 140, 1.2);
        b.pop();

        b.comment("全阶段：锁魂钩镐，持镐骑士掷出锁链镐钩，命中后把目标拽向自己并重击；落空则顺着锁链飞向钩点砸地", "All phases: Chain Hook, a pick knight throws a chained hook that drags the target in for a heavy blow; on a miss it reels itself to the hook and slams the ground");
        CHAIN_HOOK = new Skill(b, "chain_hook", "锁魂钩镐", "Chain Hook", 160, 1.5);
        HOOK_SLAM_MULTIPLIER = b.comment("落空后砸地的伤害倍率", "Damage multiplier of the ground slam after a miss").defineInRange("slam_multiplier", 1.0, 0.0, 100.0);
        b.pop();

        b.comment("全阶段：幽链绞阵，至少三名骑士围住目标，用锁链连成旋转收缩的多边形；锁链贴地，跳起可躲，打伤链上的骑士可以打断锁链", "All phases: Chain Noose, three or more knights surround the target with a rotating, shrinking polygon of chains; the chains are low and can be jumped, and hurting a knight breaks its chains");
        CHAIN_NOOSE = new Skill(b, "chain_noose", "幽链绞阵", "Chain Noose", 360, 0.45);
        NOOSE_SNAP_MULTIPLIER = b.comment("绞阵收紧到底时中心爆发的伤害倍率", "Damage multiplier of the snap when the noose fully closes").defineInRange("snap_multiplier", 1.2, 0.0, 100.0);
        NOOSE_BREAK_RATIO = b.comment("骑士在绞阵中受到的伤害达到其最大生命的该比例时，它两侧的锁链断裂", "A knight's chains break once it takes this fraction of its max health during the noose").defineInRange("break_ratio", 0.15, 0.01, 1.0);
        b.pop();

        b.comment("二阶段：枪阵冲锋，骑士团在一侧列成横排，地面标出冲锋路线并留出一条空档，随后整排持幽灵长枪冲过场地；三阶段会从垂直方向再冲一次", "Phase 2: Lance Phalanx, the squad lines up with one gap, telegraphs the lanes and charges across with spectral lances; a second perpendicular pass in phase three");
        LANCE_PHALANX = new Skill(b, "lance_phalanx", "枪阵冲锋", "Lance Phalanx", 320, 1.4);
        b.pop();

        b.comment("三阶段：誓约圣剑，骑士团升空环绕目标，以光束在其头顶铸成巨剑，锁定后插落地面，并向四周射出幽灵飞剑", "Phase 3: Oathblade, the squad rises around the target and forges a giant sword above it with beams; it locks, plunges into the ground and fires spectral blades outward");
        OATHBLADE = new Skill(b, "oathblade", "誓约圣剑", "Oathblade", 600, 2.2);
        OATH_BLADE_MULTIPLIER = b.comment("向外射出的幽灵飞剑伤害倍率", "Damage multiplier of the outward spectral blades").defineInRange("blade_multiplier", 0.8, 0.0, 100.0);
        OATH_BLADES = b.comment("幽灵飞剑数量", "Number of spectral blades").defineInRange("blades", 8, 0, 24);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private KnightConfig() {
    }
}
