package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
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
    private static final ResourceLocation BEAM = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/lich/beam.png");

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

        Entity owner = entity.level().getEntity(entity.getOwnerEntityId());
        if (owner != null && owner.isAlive()) {
            Vec3 self = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
            Vec3 ownerPos = new Vec3(Mth.lerp(partialTick, owner.xo, owner.getX()), Mth.lerp(partialTick, owner.yo, owner.getY()) + owner.getBbHeight() * 0.55D, Mth.lerp(partialTick, owner.zo, owner.getZ()));
            Vec3 from = new Vec3(0.0D, top, 0.0D);
            Vec3 to = ownerPos.subtract(self);
            Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(self);
            Vec3 dir = to.subtract(from);
            Vec3 view = camera.subtract(from.add(to).scale(0.5D));
            Vec3 side = dir.cross(view);
            if (side.lengthSqr() > 1.0E-8D) {
                side = side.normalize().scale(0.035D);
                float alpha = 0.25F + 0.15F * Mth.sin(age * 0.25F);
                float scroll = -age * 0.08F;
                float length = (float) dir.length();
                VertexConsumer beam = buffers.getBuffer(RenderType.entityTranslucent(BEAM));
                PoseStack.Pose pose = poseStack.last();
                Vec3 normal = view.normalize();
                float nx = (float) normal.x;
                float ny = (float) normal.y;
                float nz = (float) normal.z;
                FxDraw.vertex(beam, pose, (float) (from.x - side.x), (float) (from.y - side.y), (float) (from.z - side.z), scroll, 0.0F, 0.37F, 0.99F, 0.89F, alpha, FxDraw.FULL_BRIGHT, nx, ny, nz);
                FxDraw.vertex(beam, pose, (float) (from.x + side.x), (float) (from.y + side.y), (float) (from.z + side.z), scroll, 1.0F, 0.37F, 0.99F, 0.89F, alpha, FxDraw.FULL_BRIGHT, nx, ny, nz);
                FxDraw.vertex(beam, pose, (float) (to.x + side.x), (float) (to.y + side.y), (float) (to.z + side.z), scroll + length, 1.0F, 0.37F, 0.99F, 0.89F, alpha * 0.3F, FxDraw.FULL_BRIGHT, nx, ny, nz);
                FxDraw.vertex(beam, pose, (float) (to.x - side.x), (float) (to.y - side.y), (float) (to.z - side.z), scroll + length, 0.0F, 0.37F, 0.99F, 0.89F, alpha * 0.3F, FxDraw.FULL_BRIGHT, nx, ny, nz);
            }
        }
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoulCandle entity) {
        return TEXTURE;
    }
}
