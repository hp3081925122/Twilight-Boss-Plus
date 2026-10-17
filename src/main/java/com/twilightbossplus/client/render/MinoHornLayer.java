package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.client.fx.MinoFxManager;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import twilightforest.entity.boss.Minoshroom;

public class MinoHornLayer extends RenderLayer<Minoshroom, HumanoidModel<Minoshroom>> {
    private static final ResourceLocation GLOW = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/mino/horn_glow.png");
    private static final String[] CLASSIC_HORNS = {"right_horn_1", "left_horn_1"};
    private static final float[][] NEW_HORNS = {
        {-8.0F, -10.0F, -1.0F, -4.0F, -8.0F, 2.0F},
        {-8.0F, -13.0F, -1.0F, -6.0F, -10.0F, 2.0F},
        {4.0F, -10.0F, -1.0F, 8.0F, -8.0F, 2.0F},
        {6.0F, -13.0F, -1.0F, 8.0F, -10.0F, 2.0F}
    };
    private static final float INFLATE = 0.45F;

    public MinoHornLayer(RenderLayerParent<Minoshroom, HumanoidModel<Minoshroom>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Minoshroom entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        float glow = MinoFxManager.getHornGlow(entity.getId(), partialTick);
        if (glow <= 0.01F) {
            return;
        }
        float alpha = glow * (0.55F + 0.25F * Mth.sin(ageInTicks * 0.6F));
        ModelPart head = this.getParentModel().head;
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(GLOW));
        poseStack.pushPose();
        head.translateAndRotate(poseStack);
        boolean classic = false;
        for (String name : CLASSIC_HORNS) {
            if (head.hasChild(name)) {
                classic = true;
                ModelPart horn = head.getChild(name);
                horn.xScale = 1.15F;
                horn.yScale = 1.15F;
                horn.zScale = 1.15F;
                horn.render(poseStack, consumer, FxDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
                horn.xScale = 1.0F;
                horn.yScale = 1.0F;
                horn.zScale = 1.0F;
            }
        }
        if (!classic) {
            float pad = INFLATE / 16.0F;
            for (float[] box : NEW_HORNS) {
                FxDraw.box(consumer, poseStack.last(), box[0] / 16.0F - pad, box[1] / 16.0F - pad, box[2] / 16.0F - pad,
                    box[3] / 16.0F + pad, box[4] / 16.0F + pad, box[5] / 16.0F + pad, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, alpha, FxDraw.FULL_BRIGHT);
            }
        }
        poseStack.popPose();
    }
}
