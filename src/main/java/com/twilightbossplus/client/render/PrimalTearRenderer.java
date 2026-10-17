package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.UrGhastFxManager;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.entity.PrimalTear;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class PrimalTearRenderer extends EntityRenderer<PrimalTear> {
    public PrimalTearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(PrimalTear entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(PrimalTear entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float progress = entity.progress(partialTick);
        float age = entity.tickCount + partialTick;
        Vec3 pos = entity.positionAt(progress);
        Quaternionf cameraRotation = this.entityRenderDispatcher.cameraOrientation();
        double baseX = Mth.lerp(partialTick, entity.xo, entity.getX());
        double baseY = Mth.lerp(partialTick, entity.yo, entity.getY());
        double baseZ = Mth.lerp(partialTick, entity.zo, entity.getZ());
        poseStack.pushPose();
        poseStack.translate(pos.x - baseX, pos.y - baseY, pos.z - baseZ);
        poseStack.translate(0.0D, 0.8D, 0.0D);
        UrGhastFxManager.drawTear(poseStack, buffers, cameraRotation, 1.6F, age, 0.95F);
        poseStack.popPose();

        Vec3 end = entity.getEnd();
        double ox = end.x - baseX;
        double oy = end.y - baseY;
        double oz = end.z - baseZ;
        float radius = UrGhastConfig.TEAR_RADIUS.get().floatValue();
        float size = radius * (1.5F - 0.5F * progress);
        float pulse = 0.55F + 0.3F * Mth.sin(age * (0.25F + 0.5F * progress));
        VertexConsumer marker = buffers.getBuffer(RenderType.entityTranslucent(UrGhastFxManager.MARKER));
        FxDraw.flatSquare(marker, poseStack.last(), (float) ox, (float) oy + 0.06F, (float) oz, size, age * 0.02F,
            UrGhastFxManager.CRIMSON_R, UrGhastFxManager.CRIMSON_G, UrGhastFxManager.CRIMSON_B, pulse, FxDraw.FULL_BRIGHT);

        if (!ClientConfig.VFX_ENABLED.get()) {
            return;
        }
        VertexConsumer trail = buffers.getBuffer(RenderType.entityTranslucent(UrGhastFxManager.TEAR));
        for (int i = 1; i <= 5; i++) {
            float back = progress - i * 0.03F;
            if (back <= 0.0F) {
                break;
            }
            Vec3 trailPos = entity.positionAt(back);
            float fade = 1.0F - i / 6.0F;
            UrGhastFxManager.billboardRect(poseStack, trail, cameraRotation, trailPos.x - baseX, trailPos.y - baseY + 0.8D,
                trailPos.z - baseZ, 0.3F * fade, 0.45F * fade,
                UrGhastFxManager.BLUE_R, UrGhastFxManager.BLUE_G, UrGhastFxManager.BLUE_B, 0.6F * fade);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PrimalTear entity) {
        return UrGhastFxManager.TEAR;
    }
}
