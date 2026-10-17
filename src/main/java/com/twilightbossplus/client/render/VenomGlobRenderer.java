package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.entity.VenomGlob;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VenomGlobRenderer extends EntityRenderer<VenomGlob> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/venom_glob.png");

    public VenomGlobRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
    }

    @Override
    public void render(VenomGlob entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Vec3 motion = entity.getDeltaMovement();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.35F, 0.0F);
        for (int i = 1; i <= 3; i++) {
            float trail = 0.13F - i * 0.03F;
            float back = i * 0.55F;
            poseStack.pushPose();
            poseStack.translate(-motion.x * back, -motion.y * back, -motion.z * back);
            FxDraw.box(consumer, poseStack.last(), -trail, -trail, -trail, trail, trail, trail, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.6F - i * 0.15F, FxDraw.FULL_BRIGHT);
            poseStack.popPose();
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(age * 12.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 7.0F));
        float wobble = 1.0F + 0.12F * Mth.sin(age * 0.9F);
        float core = 0.2F;
        FxDraw.box(consumer, poseStack.last(), -core * wobble, -core / wobble, -core * wobble, core * wobble, core / wobble, core * wobble,
            0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 0.95F, FxDraw.FULL_BRIGHT);
        float shell = 0.31F;
        FxDraw.box(consumer, poseStack.last(), -shell / wobble, -shell * wobble, -shell / wobble, shell / wobble, shell * wobble, shell / wobble,
            0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.5F, packedLight);
        for (int i = 0; i < 2; i++) {
            float orbit = age * 0.35F + i * Mth.PI;
            float dx = Mth.cos(orbit) * 0.4F;
            float dz = Mth.sin(orbit) * 0.4F;
            float drop = 0.06F;
            FxDraw.box(consumer, poseStack.last(), dx - drop, -drop, dz - drop, dx + drop, drop, dz + drop, 0.0F, 0.0F, 0.5F, 0.5F, 1.0F, 1.0F, 1.0F, 0.85F, FxDraw.FULL_BRIGHT);
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VenomGlob entity) {
        return TEXTURE;
    }
}
