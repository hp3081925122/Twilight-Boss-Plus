package com.twilightbossplus.client.vfx;

import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public enum VfxAtlas {
    NAGA("naga"),
    LICH("lich"),
    MINO("minoshroom"),
    HYDRA("hydra"),
    KNIGHT("knight_phantom"),
    URGHAST("ur_ghast");

    public static final float SIZE = 256.0F;

    public final RenderType glow;
    public final RenderType flat;

    VfxAtlas(String name) {
        ResourceLocation texture = new ResourceLocation(TwilightBossPlus.MODID, "textures/vfx/" + name + "_vfx.png");
        this.glow = VfxRenderTypes.glow(name, texture);
        this.flat = VfxRenderTypes.flat(name, texture);
    }
}
