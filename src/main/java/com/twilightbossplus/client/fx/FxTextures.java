package com.twilightbossplus.client.fx;

import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.resources.ResourceLocation;

public final class FxTextures {
    public static final ResourceLocation GLOW = common("glow");
    public static final ResourceLocation FLARE = common("flare");
    public static final ResourceLocation SPARK = common("px/spark");
    public static final ResourceLocation RING = common("ring");
    public static final ResourceLocation SHOCK = common("shock");
    public static final ResourceLocation BEAM = common("beam");
    public static final ResourceLocation ENERGY = common("energy");
    public static final ResourceLocation SMOKE = common("px/smoke");
    public static final ResourceLocation DUST = common("px/dust");
    public static final ResourceLocation STREAKS = common("streaks");
    public static final ResourceLocation MOTE = common("px/mote");
    public static final ResourceLocation SOLID = common("solid");

    private FxTextures() {
    }

    private static ResourceLocation common(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/common/" + name + ".png");
    }
}
