package com.twilightbossplus.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.fx.FxDraw;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.entity.ThrownAxe;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
    private static final ResourceLocation TRAIL = new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/mino/axe_trail.png");
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
            this.renderTrail(entity, here, poseStack, buffers);
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

    private void renderTrail(ThrownAxe entity, Vec3 here, PoseStack poseStack, MultiBufferSource buffers) {
        List<Vec3> trail = entity.trail;
        if (trail.size() < 2) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 lift = new Vec3(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TRAIL));
        PoseStack.Pose pose = poseStack.last();
        int count = trail.size();
        for (int i = 0; i < count - 1; i++) {
            Vec3 from = here;
            if (i > 0) {
                from = trail.get(i);
            }
            Vec3 to = trail.get(i + 1);
            Vec3 dir = to.subtract(from);
            Vec3 view = camera.subtract(from.add(to).scale(0.5D).add(lift));
            Vec3 side = dir.cross(view);
            if (side.lengthSqr() < 1.0E-8D) {
                continue;
            }
            side = side.normalize();
            float w0 = 0.55F * (1.0F - (float) i / count);
            float w1 = 0.55F * (1.0F - (float) (i + 1) / count);
            float a0 = 0.7F * (1.0F - (float) i / count);
            float a1 = 0.7F * (1.0F - (float) (i + 1) / count);
            float u0 = (float) i / count;
            float u1 = (float) (i + 1) / count;
            Vec3 f = from.subtract(here).add(lift);
            Vec3 t = to.subtract(here).add(lift);
            Vec3 normal = view.normalize();
            float nx = (float) normal.x;
            float ny = (float) normal.y;
            float nz = (float) normal.z;
            FxDraw.vertex(consumer, pose, (float) (f.x - side.x * w0), (float) (f.y - side.y * w0), (float) (f.z - side.z * w0), u0, 0.0F, 1.0F, 1.0F, 1.0F, a0, FxDraw.FULL_BRIGHT, nx, ny, nz);
            FxDraw.vertex(consumer, pose, (float) (f.x + side.x * w0), (float) (f.y + side.y * w0), (float) (f.z + side.z * w0), u0, 1.0F, 1.0F, 1.0F, 1.0F, a0, FxDraw.FULL_BRIGHT, nx, ny, nz);
            FxDraw.vertex(consumer, pose, (float) (t.x + side.x * w1), (float) (t.y + side.y * w1), (float) (t.z + side.z * w1), u1, 1.0F, 1.0F, 1.0F, 1.0F, a1, FxDraw.FULL_BRIGHT, nx, ny, nz);
            FxDraw.vertex(consumer, pose, (float) (t.x - side.x * w1), (float) (t.y - side.y * w1), (float) (t.z - side.z * w1), u1, 0.0F, 1.0F, 1.0F, 1.0F, a1, FxDraw.FULL_BRIGHT, nx, ny, nz);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownAxe entity) {
        return TRAIL;
    }
}
