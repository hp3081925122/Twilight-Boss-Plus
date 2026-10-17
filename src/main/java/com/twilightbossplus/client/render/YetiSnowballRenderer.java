package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.FxRenderTypes;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.client.fx.YetiFxManager;
import com.twilightbossplus.entity.YetiSnowball;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

        Vec3 motion = entity.getDeltaMovement();
        double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (speed > 0.02D && ClientConfig.VFX_ENABLED.get()) {
            FxDraw.camera(this.entityRenderDispatcher.camera);
            double dx = motion.x / speed;
            double dz = motion.z / speed;
            float age = entity.tickCount + partialTick;
            PoseStack.Pose base = poseStack.last();
            VertexConsumer mist = buffers.getBuffer(FxRenderTypes.soft(YetiFxManager.MIST));
            for (int i = 0; i < 5; i++) {
                float cycle = age * 0.12F + i / 5.0F;
                float t = cycle - Mth.floor(cycle);
                double side = Mth.sin(Mth.floor(cycle) * 2.7F + i * 1.3F) * size * 0.35D;
                double back = size * 0.4D + t * size * 1.2D;
                FxDraw.sprite(mist, base, -dx * back - dz * side, size * 0.12D + t * size * 0.35D, -dz * back + dx * side, size * (0.35F + 0.5F * t), cycle,
                    0.95F, 0.97F, 1.0F, 0.65F * (1.0F - t));
            }
        }
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
