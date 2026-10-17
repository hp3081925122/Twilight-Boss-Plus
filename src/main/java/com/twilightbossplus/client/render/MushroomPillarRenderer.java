package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.entity.MushroomPillar;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class MushroomPillarRenderer extends EntityRenderer<MushroomPillar> {
    public static final ResourceLocation TEXTURE = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/entity/mushroom_pillar.png");
    public static final float[] STEM = {0.0F, 0.0F, 0.5F, 0.5F};
    private static final float[] GILLS = {0.0F, 0.5F, 0.5F, 1.0F};
    public static final float[] RED_SIDE = {0.5F, 0.0F, 1.0F, 0.25F};
    private static final float[] RED_TOP = {0.5F, 0.25F, 1.0F, 0.5F};
    public static final float[] BROWN_SIDE = {0.5F, 0.5F, 1.0F, 0.75F};
    private static final float[] BROWN_TOP = {0.5F, 0.75F, 1.0F, 1.0F};

    public MushroomPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(MushroomPillar entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float age = entity.tickCount + partialTick;
        float t = Mth.clamp(age / MushroomPillar.RISE_TICKS, 0.0F, 1.0F);
        float rise = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        float settle = age - MushroomPillar.RISE_TICKS;
        float wobble = 0.0F;
        if (settle > 0.0F && settle < 12.0F) {
            wobble = Mth.sin(settle * 1.3F) * 0.08F * (1.0F - settle / 12.0F);
        }
        float[] capSide = RED_SIDE;
        float[] capTop = RED_TOP;
        if (entity.getKind() == 1) {
            capSide = BROWN_SIDE;
            capTop = BROWN_TOP;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        poseStack.pushPose();
        poseStack.translate(0.0F, -MushroomPillar.HEIGHT * (1.0F - rise), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.getId() * 37) % 360));
        poseStack.scale(1.0F - wobble * 0.5F, 1.0F + wobble, 1.0F - wobble * 0.5F);
        PoseStack.Pose pose = poseStack.last();
        FxDraw.prism(consumer, pose, -0.38F, 0.0F, -0.38F, 0.38F, 1.05F, 0.38F, STEM, STEM, STEM, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
        FxDraw.prism(consumer, pose, -0.31F, 1.05F, -0.31F, 0.31F, 2.25F, 0.31F, STEM, STEM, STEM, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
        FxDraw.prism(consumer, pose, -0.85F, 2.2F, -0.85F, 0.85F, 2.78F, 0.85F, capSide, capTop, GILLS, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
        FxDraw.prism(consumer, pose, -0.58F, 2.78F, -0.58F, 0.58F, 3.0F, 0.58F, capSide, capTop, capTop, 1.0F, 1.0F, 1.0F, 1.0F, packedLight);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MushroomPillar entity) {
        return TEXTURE;
    }
}
