package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public final class FxBuffers {
    private static final Map<RenderType, BufferBuilder> FIXED = new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource SOURCE = MultiBufferSource.immediateWithBuffers(FIXED, new BufferBuilder(2048));
    private static final List<RenderType> USED = new ArrayList<>();

    private FxBuffers() {
    }

    public static VertexConsumer get(RenderType type) {
        if (!FIXED.containsKey(type)) {
            FIXED.put(type, new BufferBuilder(8192));
        }
        if (!USED.contains(type)) {
            USED.add(type);
        }
        return SOURCE.getBuffer(type);
    }

    public static MultiBufferSource source() {
        return FxBuffers::get;
    }

    public static void end() {
        for (RenderType type : USED) {
            if (FxRenderTypes.isAlpha(type)) {
                SOURCE.endBatch(type);
            }
        }
        for (RenderType type : USED) {
            if (!FxRenderTypes.isAlpha(type)) {
                SOURCE.endBatch(type);
            }
        }
        SOURCE.endBatch();
        USED.clear();
        FxDraw.resetParams();
    }
}
