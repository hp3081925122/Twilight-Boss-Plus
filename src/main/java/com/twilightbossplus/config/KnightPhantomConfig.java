package com.twilightbossplus.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class KnightPhantomConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BASE_ATTACK;
    public static final ForgeConfigSpec.DoubleValue PHASE2_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue PHASE3_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue NON_CHARGING_ARMOR_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue REGROUP_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.IntValue REGROUP_TICKS;
    public static final ForgeConfigSpec.DoubleValue CONTACT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHARGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHARGE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue CHARGE_TRACK_TICKS;
    public static final ForgeConfigSpec.IntValue CHARGE_LOCK_TICKS;
    public static final ForgeConfigSpec.DoubleValue CHARGE_OVERSHOOT;
    public static final ForgeConfigSpec.DoubleValue CHARGE_SPEED;
    public static final ForgeConfigSpec.DoubleValue AXE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue AXE_VOLLEYS;
    public static final ForgeConfigSpec.IntValue AXES_PER_VOLLEY;
    public static final ForgeConfigSpec.DoubleValue AXE_SPREAD_DEGREES;
    public static final ForgeConfigSpec.IntValue AXE_VOLLEY_INTERVAL;
    public static final ForgeConfigSpec.IntValue AXE_GLOW_TICKS;
    public static final ForgeConfigSpec.DoubleValue PICK_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue PICK_RINGS;
    public static final ForgeConfigSpec.IntValue PICK_RING_INTERVAL;
    public static final ForgeConfigSpec.IntValue PICKS_PER_RING;
    public static final ForgeConfigSpec.BooleanValue ECHO_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ECHO_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ECHO_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue ECHO_DELAY_TICKS;
    public static final ForgeConfigSpec.DoubleValue ECHO_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue ENCIRCLE_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ENCIRCLE_WALL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ENCIRCLE_THRUST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ENCIRCLE_KNOCKBACK;
    public static final ForgeConfigSpec.IntValue ENCIRCLE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue ENCIRCLE_START_RADIUS;
    public static final ForgeConfigSpec.DoubleValue ENCIRCLE_END_RADIUS;
    public static final ForgeConfigSpec.IntValue ENCIRCLE_SHRINK_TICKS;
    public static final ForgeConfigSpec.IntValue ENCIRCLE_WARNING_TICKS;
    public static final ForgeConfigSpec.IntValue ENCIRCLE_WALL_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue ENCIRCLE_THRUST_RADIUS;
    public static final ForgeConfigSpec.BooleanValue CROSS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CROSS_CHANCE;
    public static final ForgeConfigSpec.IntValue CROSS_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue CROSS_SECOND_DELAY;
    public static final ForgeConfigSpec.BooleanValue OATH_ENABLED;
    public static final ForgeConfigSpec.DoubleValue OATH_ATTACK_PER_STACK;
    public static final ForgeConfigSpec.DoubleValue OATH_SPEED_PER_STACK;
    public static final ForgeConfigSpec.IntValue OATH_MAX_STACKS;
    public static final ForgeConfigSpec.BooleanValue LAST_ENABLED;
    public static final ForgeConfigSpec.IntValue LAST_ECHO_COUNT;
    public static final ForgeConfigSpec.DoubleValue LAST_ECHO_ANGLE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("knight_phantom");
        ENABLED = builder
            .comment("启用幻影骑士增强，关闭后保持暮色原版", "Enable the Knight Phantom rework; disable to keep vanilla Twilight Forest behavior")
            .define("enabled", true);
        HEALTH_MULTIPLIER = builder
            .comment("每只骑士的最大生命倍率", "Max health multiplier of each knight")
            .defineInRange("health_multiplier", 1.0D, 0.1D, 100.0D);
        ATTACK_MULTIPLIER = builder
            .comment("攻击力倍率，技能伤害 = 攻击力 * 技能倍率", "Attack damage multiplier; skill damage = attack damage * skill multiplier")
            .defineInRange("attack_multiplier", 1.0D, 0.0D, 100.0D);
        BASE_ATTACK = builder
            .comment("骑士的基础攻击力（原版为 1，冲锋时 +7）", "Base attack damage of each knight (vanilla is 1, +7 while charging)")
            .defineInRange("base_attack", 6.0D, 0.0D, 1000.0D);
        PHASE2_THRESHOLD = builder
            .comment("全队血量比例低于此值进入二阶段「合围」", "Enter phase 2 when the group health ratio drops below this value")
            .defineInRange("phase2_threshold", 0.6D, 0.0D, 1.0D);
        PHASE3_THRESHOLD = builder
            .comment("全队血量比例低于此值进入三阶段「誓约」，只剩 1 只时也会进入", "Enter phase 3 below this group health ratio, or when only one knight is left")
            .defineInRange("phase3_threshold", 0.3D, 0.0D, 1.0D);
        NON_CHARGING_ARMOR_MULTIPLIER = builder
            .comment("未出手骑士的护甲倍率（原版为 5）", "Armor multiplier of knights that are not attacking (vanilla is 5)")
            .defineInRange("non_charging_armor_multiplier", 5.0D, 1.0D, 100.0D);
        REGROUP_DAMAGE_REDUCTION = builder
            .comment("转阶段聚拢期间受到伤害的减免比例", "Damage reduction while regrouping on phase change")
            .defineInRange("regroup_damage_reduction", 0.5D, 0.0D, 1.0D);
        REGROUP_TICKS = builder
            .comment("转阶段聚拢的持续时间", "Duration of the phase change regroup")
            .defineInRange("regroup_ticks", 30, 0, 200);
        CONTACT_MULTIPLIER = builder
            .comment("未冲锋时贴身接触的伤害倍率（原版为攻击力 1）", "Damage multiplier of contact hits while not charging (vanilla deals 1)")
            .defineInRange("contact_multiplier", 0.2D, 0.0D, 100.0D);
        builder.pop();

        builder.push("charge");
        CHARGE_MULTIPLIER = builder
            .comment("剑骑士冲锋伤害倍率", "Sword charge damage multiplier")
            .defineInRange("multiplier", 1.4D, 0.0D, 100.0D);
        CHARGE_KNOCKBACK = builder
            .comment("冲锋击退强度，受击退抗性影响，上限 1.0", "Charge knockback strength, reduced by knockback resistance, capped at 1.0")
            .defineInRange("knockback", 0.8D, 0.0D, 1.0D);
        CHARGE_TRACK_TICKS = builder
            .comment("冲锋起手中冲锋点跟随玩家的时长", "Ticks the charge point follows the player during the windup")
            .defineInRange("track_ticks", 35, 1, 200);
        CHARGE_LOCK_TICKS = builder
            .comment("冲锋点锁定后到出手前的时长", "Ticks between locking the charge point and charging")
            .defineInRange("lock_ticks", 15, 1, 200);
        CHARGE_OVERSHOOT = builder
            .comment("冲锋穿过冲锋点后继续冲出的距离", "Distance the charge continues past the charge point")
            .defineInRange("overshoot", 4.0D, 0.0D, 16.0D);
        CHARGE_SPEED = builder
            .comment("冲锋速度（格 / tick）", "Charge speed in blocks per tick")
            .defineInRange("speed", 0.9D, 0.1D, 4.0D);
        builder.pop();

        builder.push("axe");
        AXE_MULTIPLIER = builder
            .comment("飞斧伤害倍率", "Thrown axe damage multiplier")
            .defineInRange("multiplier", 1.0D, 0.0D, 100.0D);
        AXE_VOLLEYS = builder
            .comment("每次出手的投掷轮数", "Volleys per attack")
            .defineInRange("volleys", 3, 1, 10);
        AXES_PER_VOLLEY = builder
            .comment("每轮扇形投出的飞斧数量", "Axes per fan volley")
            .defineInRange("axes_per_volley", 3, 1, 9);
        AXE_SPREAD_DEGREES = builder
            .comment("扇形中相邻飞斧的夹角", "Angle between neighbouring axes in a volley")
            .defineInRange("spread_degrees", 12.0D, 0.0D, 60.0D);
        AXE_VOLLEY_INTERVAL = builder
            .comment("相邻两轮投掷的间隔", "Ticks between volleys")
            .defineInRange("volley_interval_ticks", 15, 1, 100);
        AXE_GLOW_TICKS = builder
            .comment("每轮出手前斧刃发光预警的时长", "Warning glow ticks before each volley")
            .defineInRange("glow_ticks", 6, 0, 40);
        builder.pop();

        builder.push("pick");
        PICK_MULTIPLIER = builder
            .comment("飞镐伤害倍率", "Thrown pickaxe damage multiplier")
            .defineInRange("multiplier", 0.5D, 0.0D, 100.0D);
        PICK_RINGS = builder
            .comment("每次出手撒出的镐圈数", "Pickaxe rings per attack")
            .defineInRange("rings", 5, 1, 20);
        PICK_RING_INTERVAL = builder
            .comment("相邻两圈的间隔", "Ticks between rings")
            .defineInRange("ring_interval_ticks", 10, 1, 100);
        PICKS_PER_RING = builder
            .comment("每圈飞镐数量，相邻两圈错开半个间隔", "Pickaxes per ring; every other ring is offset by half a step")
            .defineInRange("picks_per_ring", 8, 1, 32);
        builder.pop();

        builder.push("death_echo");
        ECHO_ENABLED = builder
            .comment("启用亡骑残影：骑士死亡处留下残影并直线冲锋", "Enable death echo: a dying knight leaves an echo that charges in a line")
            .define("enabled", true);
        ECHO_MULTIPLIER = builder
            .comment("残影冲锋伤害倍率", "Echo charge damage multiplier")
            .defineInRange("multiplier", 1.2D, 0.0D, 100.0D);
        ECHO_KNOCKBACK = builder
            .comment("残影冲锋击退强度，上限 1.0", "Echo charge knockback strength, capped at 1.0")
            .defineInRange("knockback", 0.6D, 0.0D, 1.0D);
        ECHO_DELAY_TICKS = builder
            .comment("残影出现到冲锋的延迟", "Ticks between the echo appearing and charging")
            .defineInRange("delay_ticks", 30, 1, 200);
        ECHO_DISTANCE = builder
            .comment("残影冲锋距离", "Echo charge distance")
            .defineInRange("distance", 16.0D, 1.0D, 48.0D);
        builder.pop();

        builder.push("encircle");
        ENCIRCLE_ENABLED = builder
            .comment("启用合围剑阵（二阶段起替代一次小圈盘旋）", "Enable the encircling sword ring (phase 2+, replaces a small circle formation)")
            .define("enabled", true);
        ENCIRCLE_WALL_MULTIPLIER = builder
            .comment("剑墙伤害倍率", "Sword wall damage multiplier")
            .defineInRange("wall_multiplier", 0.5D, 0.0D, 100.0D);
        ENCIRCLE_THRUST_MULTIPLIER = builder
            .comment("收束刺击伤害倍率", "Final thrust damage multiplier")
            .defineInRange("thrust_multiplier", 1.5D, 0.0D, 100.0D);
        ENCIRCLE_KNOCKBACK = builder
            .comment("剑墙把目标推回圆心的击退强度，上限 1.0", "Knockback that pushes targets back toward the centre, capped at 1.0")
            .defineInRange("knockback", 0.6D, 0.0D, 1.0D);
        ENCIRCLE_COOLDOWN_TICKS = builder
            .comment("合围剑阵冷却", "Encircle cooldown in ticks")
            .defineInRange("cooldown_ticks", 400, 0, 12000);
        ENCIRCLE_START_RADIUS = builder
            .comment("列阵初始半径", "Starting ring radius")
            .defineInRange("start_radius", 9.0D, 2.0D, 20.0D);
        ENCIRCLE_END_RADIUS = builder
            .comment("收缩结束时的半径", "Ring radius at the end of shrinking")
            .defineInRange("end_radius", 3.0D, 1.0D, 20.0D);
        ENCIRCLE_SHRINK_TICKS = builder
            .comment("收缩时长", "Shrinking duration")
            .defineInRange("shrink_ticks", 40, 1, 400);
        ENCIRCLE_WARNING_TICKS = builder
            .comment("列阵完成前剑墙为虚线、不造成伤害的时长", "Ticks the walls stay dashed and harmless before the ring closes")
            .defineInRange("warning_ticks", 20, 0, 200);
        ENCIRCLE_WALL_INTERVAL = builder
            .comment("剑墙伤害结算间隔", "Ticks between sword wall hits")
            .defineInRange("wall_interval_ticks", 10, 1, 100);
        ENCIRCLE_THRUST_RADIUS = builder
            .comment("收束刺击的圆心判定半径", "Radius of the final thrust around the centre")
            .defineInRange("thrust_radius", 2.5D, 0.5D, 10.0D);
        builder.pop();

        builder.push("cross_charge");
        CROSS_ENABLED = builder
            .comment("启用交叉冲锋（二阶段起）", "Enable the cross charge (phase 2+)")
            .define("enabled", true);
        CROSS_CHANCE = builder
            .comment("领队选择冲锋时改用交叉冲锋的几率", "Chance to use a cross charge when the leader picks an attack")
            .defineInRange("chance", 0.33D, 0.0D, 1.0D);
        CROSS_COOLDOWN_TICKS = builder
            .comment("交叉冲锋冷却", "Cross charge cooldown in ticks")
            .defineInRange("cooldown_ticks", 360, 0, 12000);
        CROSS_SECOND_DELAY = builder
            .comment("第二只骑士相对第一只的冲锋延迟", "Delay of the second knight's charge")
            .defineInRange("second_delay_ticks", 12, 0, 100);
        builder.pop();

        builder.push("oath");
        OATH_ENABLED = builder
            .comment("启用誓约印记（三阶段）", "Enable oath marks (phase 3)")
            .define("enabled", true);
        OATH_ATTACK_PER_STACK = builder
            .comment("每层印记增加的攻击比例", "Attack bonus per stack")
            .defineInRange("attack_per_stack", 0.08D, 0.0D, 10.0D);
        OATH_SPEED_PER_STACK = builder
            .comment("每层印记增加的移动速度比例", "Movement speed bonus per stack")
            .defineInRange("speed_per_stack", 0.05D, 0.0D, 10.0D);
        OATH_MAX_STACKS = builder
            .comment("印记层数上限，每有 1 只骑士死亡计 1 层", "Max stacks; one stack per dead knight")
            .defineInRange("max_stacks", 5, 0, 20);
        builder.pop();

        builder.push("last_knight");
        LAST_ENABLED = builder
            .comment("启用残誓骑士：最后一只骑士循环切换武器并召唤残影", "Enable the last knight: it cycles weapons and summons echoes")
            .define("enabled", true);
        LAST_ECHO_COUNT = builder
            .comment("每次切换武器召唤的残影数量", "Echoes summoned on each weapon switch")
            .defineInRange("echo_count", 2, 0, 6);
        LAST_ECHO_ANGLE = builder
            .comment("剑形态残影冲锋的偏角", "Angle offset of the echo charges in sword form")
            .defineInRange("echo_angle", 35.0D, 0.0D, 90.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private KnightPhantomConfig() {
    }
}
