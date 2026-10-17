package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class MinoshroomConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue PHASE2_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue PHASE3_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue CHARGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHARGE_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue CHARGE_SPEED;
    public static final ForgeConfigSpec.IntValue CHARGE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue CHARGE_BREAK_BLOCKS;
    public static final ForgeConfigSpec.IntValue STUN_TICKS;
    public static final ForgeConfigSpec.DoubleValue STUN_DAMAGE_BONUS;
    public static final ForgeConfigSpec.DoubleValue PHASE2_WINDUP_SCALE;
    public static final ForgeConfigSpec.DoubleValue SLAM_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SLAM_KNOCKBACK;
    public static final ForgeConfigSpec.DoubleValue SLAM_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SLAM_SPEED;
    public static final ForgeConfigSpec.IntValue SLAM_CHARGE_MIN;
    public static final ForgeConfigSpec.IntValue SLAM_CHARGE_MAX;
    public static final ForgeConfigSpec.IntValue SLAM_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue AXE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue AXE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue AXE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue AXE_MIN_RANGE;
    public static final ForgeConfigSpec.DoubleValue AXE_MAX_RANGE;
    public static final ForgeConfigSpec.DoubleValue AXE_MELEE_PENALTY;
    public static final ForgeConfigSpec.BooleanValue PILLAR_ENABLED;
    public static final ForgeConfigSpec.DoubleValue PILLAR_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue PILLAR_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue PILLAR_COUNT_PHASE1;
    public static final ForgeConfigSpec.IntValue PILLAR_COUNT_PHASE2;
    public static final ForgeConfigSpec.IntValue PILLAR_LIFETIME;
    public static final ForgeConfigSpec.IntValue PILLAR_HITS_TO_BREAK;
    public static final ForgeConfigSpec.BooleanValue PUFFBALL_ENABLED;
    public static final ForgeConfigSpec.DoubleValue PUFFBALL_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue PUFFBALL_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue PUFFBALL_COUNT;
    public static final ForgeConfigSpec.IntValue PUFFBALL_FUSE_TICKS;
    public static final ForgeConfigSpec.DoubleValue PUFFBALL_RADIUS;
    public static final ForgeConfigSpec.IntValue PUFFBALL_SLOWNESS_TICKS;
    public static final ForgeConfigSpec.BooleanValue STAMPEDE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue STAMPEDE_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue STAMPEDE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue STAMPEDE_CHARGES;
    public static final ForgeConfigSpec.IntValue STAMPEDE_COOLDOWN_TICKS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("minoshroom");
        ENABLED = builder
            .comment("启用米诺菇增强，关闭后保持暮色原版", "Enable the Minoshroom rework; disable to keep vanilla Twilight Forest behavior")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("最大生命倍率", "Max health multiplier")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率，技能伤害 = 攻击力 * 技能倍率（原版攻击力含斧头为 11）", "Attack damage multiplier; skill damage = attack damage * skill multiplier (vanilla attack including the axe is 11)")
            .defineInRange("attack_multiplier", 1.0D, 0.1D, 100.0D);
        PHASE2_THRESHOLD = builder
            .comment("血量低于该比例时进入二阶段", "Enters phase two below this health ratio")
            .defineInRange("phase2_threshold", 0.6D, 0.0D, 1.0D);
        PHASE3_THRESHOLD = builder
            .comment("血量低于该比例时进入三阶段", "Enters phase three below this health ratio")
            .defineInRange("phase3_threshold", 0.3D, 0.0D, 1.0D);

        builder.push("bull_charge");
        CHARGE_MULTIPLIER = builder
            .comment("斗牛冲锋命中伤害倍率", "Bull Charge hit damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        CHARGE_KNOCKBACK = builder
            .comment("冲锋命中击退强度，受击退抗性影响", "Charge hit knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 1.0D, 0.0D, 1.2D);
        CHARGE_SPEED = builder
            .comment("冲锋速度（格/tick）", "Charge speed in blocks per tick")
            .defineInRange("speed", 0.6D, 0.1D, 2.0D);
        CHARGE_COOLDOWN_TICKS = builder
            .comment("斗牛冲锋冷却（tick）", "Bull Charge cooldown in ticks")
            .defineInRange("cooldown_ticks", 120, 1, 12000);
        CHARGE_BREAK_BLOCKS = builder
            .comment("冲锋时是否破坏方块（还需要 mobGriefing 开启）", "Whether charging breaks blocks (also requires mobGriefing)")
            .define("charge_break_blocks", false);
        STUN_TICKS = builder
            .comment("撞墙或撞柱后的眩晕时长（tick）", "Stun duration after hitting a wall or pillar, in ticks")
            .defineInRange("stun_ticks", 50, 0, 600);
        STUN_DAMAGE_BONUS = builder
            .comment("眩晕期间额外承受的伤害比例", "Extra damage taken while stunned")
            .defineInRange("stun_damage_bonus", 0.25D, 0.0D, 10.0D);
        PHASE2_WINDUP_SCALE = builder
            .comment("二阶段起冲锋蓄力时间倍率", "Charge windup scale from phase two onward")
            .defineInRange("phase2_windup_scale", 0.7D, 0.1D, 1.0D);
        builder.pop();

        builder.push("ground_slam");
        SLAM_MULTIPLIER = builder
            .comment("菌环震地土浪伤害倍率", "Ground Slam shockwave damage multiplier")
            .defineInRange("multiplier", 0.5D, 0.0D, 100.0D);
        SLAM_KNOCKBACK = builder
            .comment("土浪击退强度，受击退抗性影响", "Shockwave knockback strength, reduced by knockback resistance")
            .defineInRange("knockback", 0.4D, 0.0D, 1.2D);
        SLAM_RADIUS = builder
            .comment("土浪最大半径", "Shockwave max radius")
            .defineInRange("radius", 7.5D, 1.0D, 32.0D);
        SLAM_SPEED = builder
            .comment("土浪扩散速度（格/tick）", "Shockwave expansion speed in blocks per tick")
            .defineInRange("speed", 0.6D, 0.1D, 4.0D);
        SLAM_CHARGE_MIN = builder
            .comment("砸地最短蓄力（tick）", "Minimum slam windup in ticks")
            .defineInRange("charge_min", 20, 1, 200);
        SLAM_CHARGE_MAX = builder
            .comment("砸地最长蓄力（tick）", "Maximum slam windup in ticks")
            .defineInRange("charge_max", 30, 1, 200);
        SLAM_COOLDOWN_TICKS = builder
            .comment("菌环震地冷却（tick）", "Ground Slam cooldown in ticks")
            .defineInRange("cooldown_ticks", 160, 1, 12000);
        builder.pop();

        builder.push("axe_boomerang");
        AXE_ENABLED = builder
            .comment("启用回旋巨斧", "Enable Axe Boomerang")
            .define("enabled", true);
        AXE_MULTIPLIER = builder
            .comment("回旋巨斧伤害倍率", "Axe Boomerang damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        AXE_COOLDOWN_TICKS = builder
            .comment("回旋巨斧冷却（tick）", "Axe Boomerang cooldown in ticks")
            .defineInRange("cooldown_ticks", 200, 1, 12000);
        AXE_MIN_RANGE = builder
            .comment("触发的最小目标距离", "Minimum target distance to trigger")
            .defineInRange("min_range", 5.0D, 0.0D, 64.0D);
        AXE_MAX_RANGE = builder
            .comment("触发的最大目标距离", "Maximum target distance to trigger")
            .defineInRange("max_range", 14.0D, 1.0D, 64.0D);
        AXE_MELEE_PENALTY = builder
            .comment("斧头飞行期间近战伤害倍率", "Melee damage multiplier while the axe is airborne")
            .defineInRange("melee_penalty", 0.5D, 0.0D, 1.0D);
        builder.pop();

        builder.push("mycelium_maze");
        PILLAR_ENABLED = builder
            .comment("启用菌柄迷城", "Enable Mycelium Maze")
            .define("enabled", true);
        PILLAR_MULTIPLIER = builder
            .comment("菌柄破土伤害倍率", "Pillar eruption damage multiplier")
            .defineInRange("multiplier", 0.6D, 0.0D, 100.0D);
        PILLAR_COOLDOWN_TICKS = builder
            .comment("菌柄迷城冷却（tick）", "Mycelium Maze cooldown in ticks")
            .defineInRange("cooldown_ticks", 400, 1, 12000);
        PILLAR_COUNT_PHASE1 = builder
            .comment("一阶段菌柄数量", "Pillar count in phase one")
            .defineInRange("count_phase1", 4, 1, 16);
        PILLAR_COUNT_PHASE2 = builder
            .comment("二阶段起菌柄数量", "Pillar count from phase two onward")
            .defineInRange("count_phase2", 6, 1, 16);
        PILLAR_LIFETIME = builder
            .comment("菌柄持续时间（tick）", "Pillar lifetime in ticks")
            .defineInRange("lifetime", 300, 20, 6000);
        PILLAR_HITS_TO_BREAK = builder
            .comment("玩家打碎菌柄所需的攻击次数", "Hits needed for a player to break a pillar")
            .defineInRange("hits_to_break", 3, 1, 64);
        builder.pop();

        builder.push("spore_burst");
        PUFFBALL_ENABLED = builder
            .comment("启用孢子爆囊", "Enable Spore Burst")
            .define("enabled", true);
        PUFFBALL_MULTIPLIER = builder
            .comment("孢子云伤害倍率", "Spore cloud damage multiplier")
            .defineInRange("multiplier", 0.6D, 0.0D, 100.0D);
        PUFFBALL_COOLDOWN_TICKS = builder
            .comment("孢子爆囊冷却（tick）", "Spore Burst cooldown in ticks")
            .defineInRange("cooldown_ticks", 280, 1, 12000);
        PUFFBALL_COUNT = builder
            .comment("每次抛出的马勃菇数量", "Puffballs thrown per cast")
            .defineInRange("count", 3, 1, 12);
        PUFFBALL_FUSE_TICKS = builder
            .comment("马勃菇落地后到爆开的时间（tick）", "Ticks from landing to bursting")
            .defineInRange("fuse_ticks", 30, 5, 400);
        PUFFBALL_RADIUS = builder
            .comment("孢子云半径", "Spore cloud radius")
            .defineInRange("radius", 2.5D, 0.5D, 16.0D);
        PUFFBALL_SLOWNESS_TICKS = builder
            .comment("孢子云施加的缓慢 I 时长（tick）", "Slowness I duration applied by the spore cloud, in ticks")
            .defineInRange("slowness_ticks", 60, 0, 1200);
        builder.pop();

        builder.push("crimson_stampede");
        STAMPEDE_ENABLED = builder
            .comment("启用赤角狂奔", "Enable Crimson Stampede")
            .define("enabled", true);
        STAMPEDE_HEALTH_THRESHOLD = builder
            .comment("血量低于该比例时解锁", "Unlocks below this health ratio")
            .defineInRange("health_threshold", 0.3D, 0.0D, 1.0D);
        STAMPEDE_MULTIPLIER = builder
            .comment("每次冲锋的伤害倍率", "Damage multiplier per charge")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        STAMPEDE_CHARGES = builder
            .comment("连续冲锋次数", "Number of consecutive charges")
            .defineInRange("charges", 3, 1, 8);
        STAMPEDE_COOLDOWN_TICKS = builder
            .comment("赤角狂奔冷却（tick）", "Crimson Stampede cooldown in ticks")
            .defineInRange("cooldown_ticks", 500, 1, 12000);
        builder.pop();

        builder.pop();
        SPEC = builder.build();
    }

    private MinoshroomConfig() {
    }
}
