package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.twilightbossplus.TwilightBossPlus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class FxRenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> SOFT = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> ADD = new HashMap<>();
    private static final Map<ResourceLocation, RenderType> ALPHA = new HashMap<>();
    private static final Set<RenderType> ALPHA_TYPES = new HashSet<>();
    public static final ResourceLocation NOISE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/common2/noise.png");
    private static final ShaderStateShard FLOW_SHADER = new ShaderStateShard(FxShaders::flow);
    private static final ShaderStateShard FLOW_ADD_SHADER = new ShaderStateShard(FxShaders::flowAdd);

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

    public static RenderType add(ResourceLocation texture) {
        if (!FxShaders.fancy()) {
            return glow(texture);
        }
        return ADD.computeIfAbsent(texture, key -> create("twilightbossplus_fx_add", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144,
            false, false, CompositeState.builder()
                .setShaderState(FLOW_ADD_SHADER)
                .setTextureState(MultiTextureStateShard.builder().add(key, true, false).add(NOISE, true, false).build())
                .setTransparencyState(LIGHTNING_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false)));
    }

    public static RenderType alpha(ResourceLocation texture) {
        if (!FxShaders.fancy()) {
            return soft(texture);
        }
        return ALPHA.computeIfAbsent(texture, key -> {
            RenderType type = create("twilightbossplus_fx_alpha", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 262144,
                false, true, CompositeState.builder()
                    .setShaderState(FLOW_SHADER)
                    .setTextureState(MultiTextureStateShard.builder().add(key, true, false).add(NOISE, true, false).build())
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));
            ALPHA_TYPES.add(type);
            return type;
        });
    }

    public static boolean isAlpha(RenderType type) {
        return ALPHA_TYPES.contains(type) || SOFT.containsValue(type);
    }
}
