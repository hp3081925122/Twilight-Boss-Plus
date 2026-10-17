package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.FxRenderTypes;
import com.twilightbossplus.client.fx.FxTextures;
import com.twilightbossplus.client.fx.NagaFxManager;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.entity.VenomPool;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class VenomPoolRenderer extends EntityRenderer<VenomPool> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/venom_pool.png");
    private static final ResourceLocation GLOB_TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/venom_glob.png");
    private static final int FRAMES = 4;
    private static final int SLICES = 24;

    public VenomPoolRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(VenomPool entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        float grow = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float alpha = Mth.clamp(age / 8.0F, 0.0F, 1.0F) * Mth.clamp((VenomPool.LIFETIME - age) / 20.0F, 0.0F, 1.0F);
        if (alpha <= 0.0F) {
            return;
        }
        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 7), LightTexture.sky(packedLight));
        int frame = (entity.tickCount / 4) % FRAMES;
        float frameV0 = frame / (float) FRAMES;
        float frameV1 = (frame + 1) / (float) FRAMES;
        float frameMid = (frameV0 + frameV1) * 0.5F;
        float frameHalf = (frameV1 - frameV0) * 0.5F;
        float radius = VenomPool.RADIUS * grow;
        float y = 0.03F;

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < SLICES; i++) {
            float a0 = Mth.TWO_PI * i / SLICES;
            float a1 = Mth.TWO_PI * (i + 1) / SLICES;
            float r0 = radius * (1.0F + 0.06F * Mth.sin(a0 * 3.0F + age * 0.2F));
            float r1 = radius * (1.0F + 0.06F * Mth.sin(a1 * 3.0F + age * 0.2F));
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            FxDraw.vertex(consumer, pose, 0.0F, y, 0.0F, 0.5F, frameMid, 1.0F, 1.0F, 1.0F, alpha, light, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, c0 * r0, y, s0 * r0, 0.5F + 0.5F * c0, frameMid + frameHalf * s0, 1.0F, 1.0F, 1.0F, alpha, light, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, c1 * r1, y, s1 * r1, 0.5F + 0.5F * c1, frameMid + frameHalf * s1, 1.0F, 1.0F, 1.0F, alpha, light, 0.0F, 1.0F, 0.0F);
            FxDraw.vertex(consumer, pose, 0.0F, y, 0.0F, 0.5F, frameMid, 1.0F, 1.0F, 1.0F, alpha, light, 0.0F, 1.0F, 0.0F);
        }

        VertexConsumer bubbles = buffers.getBuffer(RenderType.entityTranslucent(GLOB_TEXTURE));
        for (int i = 0; i < 5; i++) {
            float cycle = age * 0.04F + i * 0.2F;
            float phase = cycle - Mth.floor(cycle);
            int seed = Mth.floor(cycle) * 31 + i * 17 + entity.getId();
            float angle = (seed * 2.3999632F) % Mth.TWO_PI;
            float distance = radius * (0.2F + ((seed * 7) & 7) / 10.0F);
            float bx = Mth.cos(angle) * distance;
            float bz = Mth.sin(angle) * distance;
            float by = y + phase * 0.3F;
            float size = 0.03F + 0.06F * (1.0F - phase);
            FxDraw.box(bubbles, poseStack.last(), bx - size, by - size, bz - size, bx + size, by + size, bz + size,
                0.0F, 0.0F, 0.5F, 0.5F, 1.0F, 1.0F, 1.0F, alpha * (1.0F - phase), light);
        }

        FxDraw.camera(this.entityRenderDispatcher.camera);
        float pulse = 0.75F + 0.25F * Mth.sin(age * 0.3F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.flat(glow, pose, 0.0D, y + 0.01D, 0.0D, radius * 1.35F, 0.0F, 0.643F, 0.827F, 0.086F, 0.28F * alpha);
        VertexConsumer rim = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
        FxDraw.ring(rim, pose, 0.0D, y + 0.02D, 0.0D, radius - 0.14F, radius + 0.14F, SLICES, age * 0.02F, 4.0F, 0.784F, 0.878F, 0.29F,
            0.55F * alpha * pulse, 0.55F * alpha * pulse);
        if (ClientConfig.VFX_ENABLED.get()) {
            VertexConsumer fumes = buffers.getBuffer(FxRenderTypes.glow(NagaFxManager.DROPLET));
            for (int i = 0; i < 6; i++) {
                float cycle = age * 0.03F + i / 6.0F;
                float phase = cycle - Mth.floor(cycle);
                int seed = Mth.floor(cycle) * 53 + i * 29 + entity.getId();
                float angle = (seed * 2.3999632F) % Mth.TWO_PI;
                float distance = radius * (0.15F + ((seed * 5) & 7) / 9.0F);
                FxDraw.sprite(fumes, pose, Mth.cos(angle) * distance, y + 0.1F + phase * 1.4F, Mth.sin(angle) * distance, 0.22F * (1.0F - phase * 0.5F), 0.0F,
                    0.643F, 0.827F, 0.086F, 0.8F * alpha * Mth.sin(phase * Mth.PI));
            }
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VenomPool entity) {
        return TEXTURE;
    }
}
