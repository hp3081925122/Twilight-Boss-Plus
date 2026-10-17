package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class FxRenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SOFT = new HashMap<>();

    private FxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort,
                          Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    public static boolean isPixel(ResourceLocation texture) {
        return texture.getPath().contains("/px/");
    }

    public static RenderType glow(ResourceLocation texture) {
        return GLOW.computeIfAbsent(texture, key -> create("twilightbossplus_fx_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144,
            false, false, CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new TextureStateShard(key, !isPixel(key), false))
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false)));
    }

    public static RenderType soft(ResourceLocation texture) {
        return SOFT.computeIfAbsent(texture, key -> create("twilightbossplus_fx_soft", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144,
            false, true, CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new TextureStateShard(key, !isPixel(key), false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false)));
    }
}
