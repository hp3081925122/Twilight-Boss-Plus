package com.twilightbossplus.config;

import java.util.List;
import java.util.function.Predicate;
import net.minecraftforge.common.ForgeConfigSpec;

public final class GeneralConfig {
    private static final Predicate<Object> ITEM_ID_VALIDATOR = o -> o instanceof String s && !s.isBlank();

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_TROPHY_ACTIVATION;
    public static final ForgeConfigSpec.BooleanValue CONSUME_ACTIVATION_ITEM;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> NAGA_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LICH_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MINOSHROOM_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HYDRA_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> KNIGHT_PHANTOM_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UR_GHAST_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALPHA_YETI_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SNOW_QUEEN_ITEMS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("general");
        DEBUG_LOG = b.comment("输出 Boss 技能与阶段切换的调试日志", "Log boss skill and phase debug messages").define("debug_log", false);
        REQUIRE_TROPHY_ACTIVATION = b.comment(
                "是否需要手持 Boss 对应的战利品（奖杯）右键 Boss 才切换为强化形态；默认 false（默认即为强化形态）",
                "Require right-clicking a boss with its corresponding trophy drop to awaken its enhanced form; false by default (enhanced by default)"
        ).define("require_trophy_activation", false);
        CONSUME_ACTIVATION_ITEM = b.comment(
                "右键激活 Boss 强化形态时是否消耗 1 个手持物品（创造模式不消耗）",
                "Consume 1 held item when right-clicking a boss to awaken its enhanced form (not consumed in Creative mode)"
        ).define("consume_activation_item", true);

        b.push("activation_items");
        NAGA_ITEMS = b.comment("右键激活娜迦强化形态的物品 ID 列表", "Item IDs that can awaken Naga's enhanced form")
                .defineListAllowEmpty("naga", List.of("twilightforest:naga_trophy"), ITEM_ID_VALIDATOR);
        LICH_ITEMS = b.comment("右键激活巫妖强化形态的物品 ID 列表", "Item IDs that can awaken Lich's enhanced form")
                .defineListAllowEmpty("lich", List.of("twilightforest:lich_trophy"), ITEM_ID_VALIDATOR);
        MINOSHROOM_ITEMS = b.comment("右键激活米诺菇强化形态的物品 ID 列表", "Item IDs that can awaken Minoshroom's enhanced form")
                .defineListAllowEmpty("minoshroom", List.of("twilightforest:minoshroom_trophy"), ITEM_ID_VALIDATOR);
        HYDRA_ITEMS = b.comment("右键激活九头蛇强化形态的物品 ID 列表", "Item IDs that can awaken Hydra's enhanced form")
                .defineListAllowEmpty("hydra", List.of("twilightforest:hydra_trophy"), ITEM_ID_VALIDATOR);
        KNIGHT_PHANTOM_ITEMS = b.comment("右键激活幻影骑士强化形态的物品 ID 列表", "Item IDs that can awaken Knight Phantom's enhanced form")
                .defineListAllowEmpty("knight_phantom", List.of("twilightforest:knight_phantom_trophy"), ITEM_ID_VALIDATOR);
        UR_GHAST_ITEMS = b.comment("右键激活暮初恶魂强化形态的物品 ID 列表", "Item IDs that can awaken Ur-Ghast's enhanced form")
                .defineListAllowEmpty("ur_ghast", List.of("twilightforest:ur_ghast_trophy"), ITEM_ID_VALIDATOR);
        ALPHA_YETI_ITEMS = b.comment("右键激活雪怪首领强化形态的物品 ID 列表", "Item IDs that can awaken Alpha Yeti's enhanced form")
                .defineListAllowEmpty("alpha_yeti", List.of("twilightforest:alpha_yeti_trophy"), ITEM_ID_VALIDATOR);
        SNOW_QUEEN_ITEMS = b.comment("右键激活冰雪女王强化形态的物品 ID 列表", "Item IDs that can awaken Snow Queen's enhanced form")
                .defineListAllowEmpty("snow_queen", List.of("twilightforest:snow_queen_trophy"), ITEM_ID_VALIDATOR);
        b.pop();

        b.pop();
        SPEC = b.build();
    }

    private GeneralConfig() {
    }
}
