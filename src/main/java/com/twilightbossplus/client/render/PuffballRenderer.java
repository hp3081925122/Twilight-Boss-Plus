package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.entity.Puffball;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PuffballRenderer extends EntityRenderer<Puffball> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/puffball.png");
    private static final ResourceLocation WARNING = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/mino/cap_shadow.png");
    private static final float[] SIDE = {0.0F, 0.0F, 0.5F, 0.5F};
    private static final float[] TOP = {0.5F, 0.0F, 1.0F, 0.5F};
    private static final float[] BOTTOM = {0.0F, 0.5F, 1.0F, 1.0F};

    public PuffballRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.3F;
    }

    @Override
    public void render(Puffball entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        int landedAt = entity.getLandedAt();
        float progress = 0.0F;
        if (landedAt >= 0) {
            progress = Mth.clamp((age - landedAt) / Math.max(1, entity.getFuse()), 0.0F, 1.0F);
        }
        if (landedAt >= 0) {
            float radius = entity.getRadius();
            VertexConsumer warning = buffers.getBuffer(RenderType.entityTranslucent(WARNING));
            float pulse = 0.75F + 0.25F * Mth.sin(age * (0.3F + progress * 0.9F));
            FxDraw.flatSquare(warning, poseStack.last(), 0.0F, 0.04F, 0.0F, radius, 0.0F, 1.0F, 0.42F, 0.25F, 0.55F * pulse, FxDraw.FULL_BRIGHT);
            FxDraw.flatSquare(warning, poseStack.last(), 0.0F, 0.045F, 0.0F, radius * progress, age * 0.02F, 0.91F, 0.84F, 0.65F, 0.45F, FxDraw.FULL_BRIGHT);
        }
        float swell = 1.0F + 0.4F * progress * progress;
        float shakeX = 0.0F;
        float shakeZ = 0.0F;
        if (progress > 0.55F) {
            float amount = (progress - 0.55F) * 0.12F;
            shakeX = Mth.sin(age * 3.1F) * amount;
            shakeZ = Mth.cos(age * 2.7F) * amount;
        }
        float r = 1.0F;
        float g = 1.0F - 0.25F * progress;
        float b = 1.0F - 0.35F * progress;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        poseStack.pushPose();
        poseStack.translate(shakeX, 0.0F, shakeZ);
        if (landedAt < 0) {
            poseStack.translate(0.0F, 0.3F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 18.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(age * 11.0F));
            poseStack.translate(0.0F, -0.3F, 0.0F);
        }
        poseStack.scale(swell, swell, swell);
        PoseStack.Pose pose = poseStack.last();
        FxDraw.prism(consumer, pose, -0.28F, 0.0F, -0.28F, 0.28F, 0.5F, 0.28F, SIDE, TOP, BOTTOM, r, g, b, 1.0F, packedLight);
        FxDraw.prism(consumer, pose, -0.34F, 0.08F, -0.2F, 0.34F, 0.42F, 0.2F, SIDE, TOP, BOTTOM, r, g, b, 1.0F, packedLight);
        FxDraw.prism(consumer, pose, -0.2F, 0.08F, -0.34F, 0.2F, 0.42F, 0.34F, SIDE, TOP, BOTTOM, r, g, b, 1.0F, packedLight);
        FxDraw.prism(consumer, pose, -0.06F, 0.5F, -0.06F, 0.06F, 0.6F, 0.06F, BOTTOM, BOTTOM, BOTTOM, r, g, b, 1.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(Puffball entity) {
        return TEXTURE;
    }
}
