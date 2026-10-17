package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.YetiFxManager;
import com.twilightbossplus.entity.YetiSnowball;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class YetiSnowballRenderer extends EntityRenderer<YetiSnowball> {
    private static final int SLICES = 14;
    private static final int STACKS = 10;

    public YetiSnowballRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.0F;
    }

    @Override
    public void render(YetiSnowball entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float size = entity.visualSize(partialTick);
        float roll = Mth.lerp(partialTick, entity.prevRollAngle, entity.rollAngle);
        poseStack.pushPose();
        poseStack.translate(0.0F, size * 0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(roll));
        drawSphere(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(YetiFxManager.SNOW)), size * 0.5F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    public static void drawSphere(PoseStack poseStack, VertexConsumer consumer, float radius, int light) {
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < STACKS; i++) {
            float lat0 = (float) Math.PI * ((float) i / STACKS - 0.5F);
            float lat1 = (float) Math.PI * ((float) (i + 1) / STACKS - 0.5F);
            float v0 = (float) i / STACKS * 2.0F;
            float v1 = (float) (i + 1) / STACKS * 2.0F;
            for (int j = 0; j < SLICES; j++) {
                float lon0 = (float) Math.PI * 2.0F * j / SLICES;
                float lon1 = (float) Math.PI * 2.0F * (j + 1) / SLICES;
                float u0 = (float) j / SLICES * 3.0F;
                float u1 = (float) (j + 1) / SLICES * 3.0F;
                spherePoint(consumer, pose, radius, lat0, lon0, u0, v0, light);
                spherePoint(consumer, pose, radius, lat0, lon1, u1, v0, light);
                spherePoint(consumer, pose, radius, lat1, lon1, u1, v1, light);
                spherePoint(consumer, pose, radius, lat1, lon0, u0, v1, light);
            }
        }
    }

    private static void spherePoint(VertexConsumer consumer, PoseStack.Pose pose, float radius, float lat, float lon, float u, float v, int light) {
        float nx = Mth.cos(lat) * Mth.cos(lon);
        float ny = Mth.sin(lat);
        float nz = Mth.cos(lat) * Mth.sin(lon);
        float shade = 0.82F + 0.18F * (ny * 0.5F + 0.5F);
        FxDraw.vertex(consumer, pose, nx * radius, ny * radius, nz * radius, u, v, shade, shade, shade, 1.0F, light, nx, ny, nz);
    }

    @Override
    public boolean shouldRender(YetiSnowball entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(YetiSnowball entity) {
        return YetiFxManager.SNOW;
    }
}
