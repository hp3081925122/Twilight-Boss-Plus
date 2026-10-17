package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.FxRenderTypes;
import com.twilightbossplus.client.fx.FxTextures;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.entity.SoulCandle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SoulCandleRenderer extends EntityRenderer<SoulCandle> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/soul_candle.png");

    public SoulCandleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.2F;
    }

    @Override
    public void render(SoulCandle entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));

        poseStack.pushPose();
        FxDraw.box(consumer, poseStack.last(), -0.15F, 0.0F, -0.15F, 0.15F, 0.6F, 0.15F, 0.0F, 0.0F, 0.5F, 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
        FxDraw.box(consumer, poseStack.last(), -0.02F, 0.6F, -0.02F, 0.02F, 0.68F, 0.02F, 0.0F, 0.45F, 0.05F, 0.5F, 0.3F, 0.3F, 0.3F, 1.0F, packedLight);

        int frame = ((int) (age / 3.0F)) % 4;
        float u0 = frame * 0.25F;
        float u1 = u0 + 0.25F;
        float flicker = 1.0F + 0.08F * Mth.sin(age * 1.7F);
        float half = 0.14F;
        float bottom = 0.58F;
        float top = bottom + 0.5F * flicker;
        for (int i = 0; i < 2; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + i * 90.0F));
            PoseStack.Pose pose = poseStack.last();
            FxDraw.vertex(consumer, pose, -half, bottom, 0.0F, u0, 1.0F, 1.0F, 1.0F, 1.0F, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            FxDraw.vertex(consumer, pose, half, bottom, 0.0F, u1, 1.0F, 1.0F, 1.0F, 1.0F, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            FxDraw.vertex(consumer, pose, half, top, 0.0F, u1, 0.5F, 1.0F, 1.0F, 1.0F, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            FxDraw.vertex(consumer, pose, -half, top, 0.0F, u0, 0.5F, 1.0F, 1.0F, 1.0F, 0.95F, FxDraw.FULL_BRIGHT, 0.0F, 0.0F, 1.0F);
            poseStack.popPose();
        }
        poseStack.popPose();

        FxDraw.camera(this.entityRenderDispatcher.camera);
        double ex = Mth.lerp(partialTick, entity.xo, entity.getX());
        double ey = Mth.lerp(partialTick, entity.yo, entity.getY());
        double ez = Mth.lerp(partialTick, entity.zo, entity.getZ());
        Vec3 flame = new Vec3(ex, ey + (bottom + top) * 0.5D, ez);
        poseStack.pushPose();
        poseStack.translate(-ex, -ey, -ez);
        PoseStack.Pose world = poseStack.last();
        VertexConsumer glow = buffers.getBuffer(FxRenderTypes.glow(FxTextures.GLOW));
        FxDraw.sprite(glow, world, flame.x, flame.y, flame.z, 1.0F * flicker, 0.0F, 0.37F, 0.99F, 0.89F, 0.5F);
        FxDraw.flat(glow, world, ex, ey + 0.02D, ez, 0.7F, 0.0F, 0.37F, 0.99F, 0.89F, 0.25F * flicker);
        Entity owner = entity.level().getEntity(entity.getOwnerEntityId());
        if (owner != null && owner.isAlive()) {
            Vec3 from = new Vec3(ex, ey + top, ez);
            Vec3 to = new Vec3(Mth.lerp(partialTick, owner.xo, owner.getX()), Mth.lerp(partialTick, owner.yo, owner.getY()) + owner.getBbHeight() * 0.55D,
                Mth.lerp(partialTick, owner.zo, owner.getZ()));
            float alpha = 0.3F + 0.15F * Mth.sin(age * 0.25F);
            float length = (float) from.distanceTo(to);
            float scroll = -age * 0.08F;
            VertexConsumer beam = buffers.getBuffer(FxRenderTypes.glow(FxTextures.BEAM));
            FxDraw.beam(beam, world, from, to, 0.14F, 0.1F, scroll, scroll + length * 0.5F, 0.37F, 0.99F, 0.89F, alpha, alpha * 0.4F);
            FxDraw.beam(beam, world, from, to, 0.04F, 0.03F, scroll, scroll + length * 0.5F, 0.78F, 1.0F, 0.96F, alpha * 2.0F, alpha * 0.8F);
            if (ClientConfig.VFX_ENABLED.get()) {
                VertexConsumer motes = buffers.getBuffer(FxRenderTypes.glow(FxTextures.MOTE));
                for (int i = 0; i < 3; i++) {
                    float cycle = age * 0.025F + i / 3.0F;
                    float t = cycle - Mth.floor(cycle);
                    Vec3 p = from.lerp(to, t);
                    FxDraw.sprite(motes, world, p.x, p.y + Mth.sin(t * Mth.PI) * 0.4D, p.z, 0.22F, 0.0F, 0.37F, 0.99F, 0.89F, 0.9F * Mth.sin(t * Mth.PI));
                }
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulCandle entity) {
        return TEXTURE;
    }
}
