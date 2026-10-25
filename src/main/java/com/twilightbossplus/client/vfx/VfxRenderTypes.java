package com.twilightbossplus.client.vfx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import java.util.Optional;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class VfxRenderTypes {
    private static final RenderPipeline GLOW = pipeline("glow", BlendFunction.LIGHTNING);
    private static final RenderPipeline FLAT = pipeline("flat", BlendFunction.TRANSLUCENT);

    private VfxRenderTypes() {
    }

    private static RenderPipeline pipeline(String name, BlendFunction blend) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withVertexShader(Identifier.fromNamespaceAndPath("twilightbossplus", "core/vfx"))
                .withFragmentShader(Identifier.fromNamespaceAndPath("twilightbossplus", "core/vfx"))
                .withSampler("Sampler0")
                .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
                .withCull(false)
                .withLocation(Identifier.fromNamespaceAndPath("twilightbossplus", "pipeline/" + name))
                .withColorTargetState(new ColorTargetState(Optional.of(blend), ColorTargetState.WRITE_COLOR))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
    }

    public static void register(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GLOW);
        event.registerPipeline(FLAT);
    }

    public static RenderType glow(String name, Identifier texture) {
        return RenderType.create("twilightbossplus_glow_" + name,
                RenderSetup.builder(GLOW).withTexture("Sampler0", texture).bufferSize(262144).createRenderSetup());
    }

    public static RenderType flat(String name, Identifier texture) {
        return RenderType.create("twilightbossplus_flat_" + name,
                RenderSetup.builder(FLAT).withTexture("Sampler0", texture).bufferSize(262144).createRenderSetup());
    }
}
