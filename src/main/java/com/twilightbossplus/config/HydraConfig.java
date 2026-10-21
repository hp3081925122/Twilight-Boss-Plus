package com.twilightbossplus.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HydraConfig {
    public static final class Skill {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue cooldown;
        public final ModConfigSpec.DoubleValue multiplier;

        Skill(ModConfigSpec.Builder b, String key, String cn, String en, int cooldown, double multiplier) {
            b.push(key);
            this.enabled = b.comment("启用" + cn, "Enable " + en).define("enabled", true);
            this.cooldown = b.comment(cn + "冷却（tick）", en + " cooldown in ticks").defineInRange("cooldown_ticks", cooldown, 1, 12000);
            this.multiplier = b.comment(cn + "伤害倍率，伤害 = 九头蛇攻击力 * 倍率", en + " damage multiplier; damage = Hydra attack damage * multiplier").defineInRange("multiplier", multiplier, 0.0, 100.0);
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

    public static final Skill MAGMA_VOLLEY;
    public static final ModConfigSpec.IntValue VOLLEY_SHELLS;
    public static final ModConfigSpec.DoubleValue POOL_MULTIPLIER;
    public static final ModConfigSpec.IntValue POOL_TICKS;
    public static final Skill FLAME_SWEEP;
    public static final Skill JAW_STRIKE;
    public static final Skill MAGMA_STOMP;
    public static final ModConfigSpec.DoubleValue STOMP_WAVE_MULTIPLIER;
    public static final Skill INFERNO_CROWN;
    public static final Skill EMBER_RAIN;
    public static final ModConfigSpec.IntValue RAIN_SHELLS;
    public static final Skill REGROWTH_ROAR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hydra");
        ENABLED = b.comment("启用九头蛇增强，关闭后保持暮色原版；开启时原版的咬击、喷火与迫击炮被新技能取代", "Enable the Hydra rework; disable to keep vanilla. When enabled the vanilla bite, flame and mortar attacks are replaced").define("enabled", true);
        HEALTH_MULTIPLIER = b.comment("最大生命倍率", "Max health multiplier").defineInRange("health_multiplier", 1.5, 0.1, 100.0);
        BASE_ATTACK = b.comment("基础攻击力，九头蛇本身没有攻击力属性，所有技能伤害以此为基础", "Base attack damage; the Hydra has no attack attribute so every skill scales from this value").defineInRange("base_attack", 10.0, 0.0, 1000.0);
        ATTACK_MULTIPLIER = b.comment("攻击力倍率", "Attack damage multiplier").defineInRange("attack_multiplier", 1.0, 0.1, 100.0);
        SKILL_KNOCKBACK = b.comment("技能击退倍率，再乘以 1 - 击退抗性", "Skill knockback multiplier, also scaled by 1 - knockback resistance").defineInRange("skill_knockback", 1.0, 0.0, 2.0);
        SKILL_INTERVAL = b.comment("两个技能之间的最短间隔（tick）", "Minimum ticks between two skills").defineInRange("skill_interval_ticks", 30, 0, 1200);

        b.push("phase");
        PHASE_TWO_HEALTH = b.comment("进入二阶段的血量比例，进入时强制释放九首炎冠", "Health ratio to enter phase two; forces Inferno Crown").defineInRange("phase_two_health", 0.6, 0.0, 1.0);
        PHASE_THREE_HEALTH = b.comment("进入三阶段的血量比例，进入时释放再生咆哮", "Health ratio to enter phase three; triggers Regrowth Roar").defineInRange("phase_three_health", 0.3, 0.0, 1.0);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("三阶段技能冷却倍率", "Skill cooldown multiplier in phase three").defineInRange("rage_cooldown_multiplier", 0.7, 0.1, 1.0);
        b.pop();

        b.comment("全阶段：熔核连射，数个头张口抛射熔岩弹，落点有预警，落地留下熔岩池；张口期间头部受到完整伤害", "All phases: Magma Volley, several heads lob telegraphed magma shells that leave lava pools; open mouths take full damage");
        MAGMA_VOLLEY = new Skill(b, "magma_volley", "熔核连射", "Magma Volley", 120, 1.1);
        VOLLEY_SHELLS = b.comment("每个头发射的熔岩弹数量", "Shells fired by each head").defineInRange("shells_per_head", 3, 1, 10);
        POOL_MULTIPLIER = b.comment("熔岩池每次灼烧的伤害倍率", "Lava pool damage multiplier per pulse").defineInRange("pool_multiplier", 0.2, 0.0, 100.0);
        POOL_TICKS = b.comment("熔岩池持续时间（tick）", "Lava pool duration in ticks").defineInRange("pool_ticks", 80, 0, 1200);
        b.pop();

        b.comment("全阶段：炎河横扫，前方的头喷出火焰柱扫过扇形区域，地面先出现扇形预警", "All phases: Flame Sweep, a front head sweeps a fire beam across a telegraphed fan");
        FLAME_SWEEP = new Skill(b, "flame_sweep", "炎河横扫", "Flame Sweep", 160, 0.45);
        b.pop();

        b.comment("全阶段：獠牙突咬，近身时前方的头锁定落点后猛咬，会击破举起的盾牌", "All phases: Jaw Strike, a close-range bite on a locked spot that breaks raised shields");
        JAW_STRIKE = new Skill(b, "jaw_strike", "獠牙突咬", "Jaw Strike", 80, 2.0);
        b.pop();

        b.comment("全阶段：熔岩践踏，群首咆哮后重踏地面，身边受到冲击并扩散出可跳跃躲避的火焰冲击环", "All phases: Magma Stomp, a roaring stomp that hurts nearby targets and sends out a jumpable ring of fire");
        MAGMA_STOMP = new Skill(b, "magma_stomp", "熔岩践踏", "Magma Stomp", 200, 1.0);
        STOMP_WAVE_MULTIPLIER = b.comment("火焰冲击环伤害倍率", "Fire ring damage multiplier").defineInRange("wave_multiplier", 0.8, 0.0, 100.0);
        b.pop();

        b.comment("二阶段：九首炎冠，所有头朝外喷火并绕身体旋转扫过地面，贴近本体可以躲开", "Phase 2: Inferno Crown, every head breathes fire outward while the beams rotate across the ground; hugging the body is safe");
        INFERNO_CROWN = new Skill(b, "inferno_crown", "九首炎冠", "Inferno Crown", 400, 0.35);
        b.pop();

        b.comment("二阶段：余烬火雨，后方的头仰天喷吐，熔岩弹在目标周围大范围落下", "Phase 2: Ember Rain, rear heads spew upward and magma shells rain down around the target");
        EMBER_RAIN = new Skill(b, "ember_rain", "余烬火雨", "Ember Rain", 300, 0.9);
        RAIN_SHELLS = b.comment("火雨熔岩弹数量", "Number of shells in Ember Rain").defineInRange("shells", 14, 1, 60);
        b.pop();

        b.comment("三阶段：再生咆哮，转阶段时断掉的头全部重生，群首咆哮并扩散两道火焰冲击环", "Phase 3: Regrowth Roar, severed heads regrow on entering phase three and two rings of fire spread out");
        REGROWTH_ROAR = new Skill(b, "regrowth_roar", "再生咆哮", "Regrowth Roar", 1, 0.8);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private HydraConfig() {
    }
}
