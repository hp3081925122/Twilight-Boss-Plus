package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.FxRenderTypes;
import com.twilightbossplus.client.fx.FxTextures;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.entity.ThrownAxe;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ThrownAxeRenderer extends EntityRenderer<ThrownAxe> {
    private final ItemRenderer itemRenderer;

    public ThrownAxeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(ThrownAxe entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        Vec3 here = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
        if (ClientConfig.VFX_ENABLED.get()) {
            this.renderTrail(entity, here, age, poseStack, buffers);
        }
        ItemStack stack = entity.getItem();
        if (!stack.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-age * 48.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.scale(1.7F, 1.7F, 1.7F);
            this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
            poseStack.popPose();
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private void renderTrail(ThrownAxe entity, Vec3 here, float age, PoseStack poseStack, MultiBufferSource buffers) {
        List<Vec3> trail = entity.trail;
        Vec3 lift = new Vec3(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        Vec3 center = here.add(lift);
        FxDraw.camera(this.entityRenderDispatcher.camera);
        poseStack.pushPose();
        poseStack.translate(-here.x, -here.y, -here.z);
        PoseStack.Pose world = poseStack.last();
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, world, center.x, center.y, center.z, 1.6F, 0.0F, 1.0F, 0.29F, 0.16F, 0.3F + 0.08F * Mth.sin(age * 0.9F));
        int count = trail.size();
        if (count >= 2) {
            Vec3[] points = new Vec3[count];
            float[] outer = new float[count];
            float[] inner = new float[count];
            float[] outerAlpha = new float[count];
            float[] innerAlpha = new float[count];
            for (int k = 0; k < count; k++) {
                int index = count - 1 - k;
                if (index == 0) {
                    points[k] = center;
                } else {
                    points[k] = trail.get(index).add(lift);
                }
                float t = (float) k / (count - 1);
                outer[k] = 0.55F * t;
                inner[k] = 0.2F * t;
                outerAlpha[k] = 0.45F * t * t;
                innerAlpha[k] = 0.85F * t * t;
            }
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.trail(beam, world, points, count, outer, outerAlpha, 0.0F, 0.5F, 0.91F, 0.84F, 0.65F);
            FxDraw.trail(beam, world, points, count, inner, innerAlpha, 0.0F, 0.5F, 1.0F, 0.29F, 0.16F);
            VertexConsumer sparks = buffers.getBuffer(FxRenderTypes.glow(FxTextures.SPARK));
            for (int k = 1; k < count - 1; k += 2) {
                float t = (float) k / (count - 1);
                FxDraw.sprite(sparks, world, points[k].x, points[k].y + Mth.sin(age * 0.7F + k) * 0.15D, points[k].z, 0.18F * t + 0.06F, age * 0.3F + k,
                    1.0F, 0.6F, 0.3F, 0.8F * t);
            }
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownAxe entity) {
        return FxTextures.GLOW;
    }
}
