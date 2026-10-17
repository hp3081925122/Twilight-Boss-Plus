package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.FxRenderTypes;
import com.twilightbossplus.client.fx.FxTextures;
import com.twilightbossplus.client.fx.LichFxManager;
import com.twilightbossplus.entity.VoidOrb;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class VoidOrbRenderer extends EntityRenderer<VoidOrb> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/void_orb.png");

    public VoidOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(VoidOrb entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        float grow = Mth.clamp(age / VoidOrb.WINDUP, 0.15F, 1.0F);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Vec3 motion = entity.getDeltaMovement();

        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() * 0.5F, 0.0F);
        if (age >= VoidOrb.WINDUP) {
            for (int i = 1; i <= 3; i++) {
                float trail = 0.16F - i * 0.035F;
                float back = i * 0.7F;
                poseStack.pushPose();
                poseStack.translate(-motion.x * back, -motion.y * back, -motion.z * back);
                poseStack.mulPose(Axis.YP.rotationDegrees(age * 10.0F + i * 30.0F));
                FxDraw.box(consumer, poseStack.last(), -trail, -trail, -trail, trail, trail, trail, 0.5F, 0.0F, 1.0F, 1.0F, 0.75F, 0.6F, 1.0F, 0.55F - i * 0.13F, FxDraw.FULL_BRIGHT);
                poseStack.popPose();
            }
        }

        poseStack.scale(grow, grow, grow);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 6.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(age * 4.0F));
        float core = 0.2F + 0.02F * Mth.sin(age * 0.7F);
        FxDraw.box(consumer, poseStack.last(), -core, -core, -core, core, core, core, 0.0F, 0.0F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, FxDraw.FULL_BRIGHT);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 9.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(age * 5.0F));
        float shell = 0.34F - 0.03F * Mth.sin(age * 0.7F);
        FxDraw.box(consumer, poseStack.last(), shell, shell, shell, -shell, -shell, -shell, 0.5F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.55F, FxDraw.FULL_BRIGHT);
        poseStack.popPose();

        for (int i = 0; i < 3; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(i * 60.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(35.0F + i * 40.0F));
            float orbit = age * (0.25F + i * 0.06F) + i * 2.1F;
            float dx = Mth.cos(orbit) * 0.5F;
            float dz = Mth.sin(orbit) * 0.5F;
            float bit = 0.055F;
            float r = 0.37F;
            float g = 0.99F;
            float b = 0.89F;
            if (i == 1) {
                r = 0.72F;
                g = 0.61F;
                b = 1.0F;
            }
            poseStack.translate(dx, 0.0F, dz);
            poseStack.mulPose(Axis.YP.rotationDegrees(age * 20.0F));
            FxDraw.box(consumer, poseStack.last(), -bit, -bit, -bit, bit, bit, bit, 0.5F, 0.0F, 0.75F, 0.5F, r, g, b, 0.9F, FxDraw.FULL_BRIGHT);
            poseStack.popPose();
        }
        poseStack.popPose();

        FxDraw.camera(this.entityRenderDispatcher.camera);
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        Vec3 center = new Vec3(ex, ey + entity.getBbHeight() * 0.5D, ez);
        poseStack.pushPose();
        poseStack.translate(-ex, -ey, -ez);
        PoseStack.Pose world = poseStack.last();
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.7F);
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, world, center.x, center.y, center.z, 1.8F * grow * pulse, 0.0F, 0.48F, 0.31F, 0.82F, 0.6F);
        FxDraw.sprite(glow, world, center.x, center.y, center.z, 0.8F * grow, 0.0F, 0.78F, 0.66F, 1.0F, 0.5F);
        VertexConsumer ring = buffers.getBuffer(FxRenderTypes.glow(FxTextures.RING));
        FxDraw.sprite(ring, world, center.x, center.y, center.z, 1.3F * grow * (1.1F - 0.1F * pulse), age * 0.05F, 0.78F, 0.66F, 1.0F, 0.55F);
        if (age >= VoidOrb.WINDUP && motion.lengthSqr() > 1.0E-4D) {
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            Vec3 tail = center.subtract(motion.scale(4.0D));
            FxDraw.beam(beam, world, center, tail, 0.3F, 0.02F, 0.0F, 1.0F, 0.48F, 0.31F, 0.82F, 0.7F, 0.0F);
            FxDraw.beam(beam, world, center, tail, 0.1F, 0.0F, 0.0F, 1.0F, 0.37F, 0.99F, 0.89F, 0.8F, 0.0F);
        }
        if (age < VoidOrb.WINDUP + 4.0F) {
            float t = Mth.clamp(age / VoidOrb.WINDUP, 0.0F, 1.0F);
            float out = Mth.clamp((VoidOrb.WINDUP + 4.0F - age) / 4.0F, 0.0F, 1.0F);
            for (int i = 0; i < 4; i++) {
                float angle = age * 0.18F + i * Mth.HALF_PI;
                float dist = 0.35F + 1.3F * (1.0F - t);
                VertexConsumer rune = buffers.getBuffer(FxRenderTypes.glow(LichFxManager.RUNES[i]));
                FxDraw.sprite(rune, world, center.x + Mth.cos(angle) * dist, center.y + Mth.sin(age * 0.2F + i) * 0.2F, center.z + Mth.sin(angle) * dist,
                    0.5F, 0.0F, 0.78F, 0.66F, 1.0F, (0.3F + 0.7F * t) * out);
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(VoidOrb entity) {
        return TEXTURE;
    }
}
