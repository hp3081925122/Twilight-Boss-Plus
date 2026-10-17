package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class FxDraw {
    public static final int FULL_BRIGHT = 0xF000F0;

    private FxDraw() {
    }

    public static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                              float r, float g, float b, float a, int light, float nx, float ny, float nz) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        consumer.vertex(matrix, x, y, z)
            .color(r, g, b, a)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(light)
            .normal(normal, nx, ny, nz)
            .endVertex();
    }

    public static void box(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                           float u0, float v0, float u1, float v1, float r, float g, float b, float a, int light) {
        vertex(consumer, pose, x0, y1, z0, u0, v0, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z1, u0, v1, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z1, u1, v1, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z0, u1, v0, r, g, b, a, light, 0.0F, 1.0F, 0.0F);

        vertex(consumer, pose, x0, y0, z0, u0, v0, r, g, b, a, light, 0.0F, -1.0F, 0.0F);
        vertex(consumer, pose, x1, y0, z0, u1, v0, r, g, b, a, light, 0.0F, -1.0F, 0.0F);
        vertex(consumer, pose, x1, y0, z1, u1, v1, r, g, b, a, light, 0.0F, -1.0F, 0.0F);
        vertex(consumer, pose, x0, y0, z1, u0, v1, r, g, b, a, light, 0.0F, -1.0F, 0.0F);

        vertex(consumer, pose, x0, y0, z0, u0, v1, r, g, b, a, light, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x0, y1, z0, u0, v0, r, g, b, a, light, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x1, y1, z0, u1, v0, r, g, b, a, light, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x1, y0, z0, u1, v1, r, g, b, a, light, 0.0F, 0.0F, -1.0F);

        vertex(consumer, pose, x1, y0, z1, u0, v1, r, g, b, a, light, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, x1, y1, z1, u0, v0, r, g, b, a, light, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, x0, y1, z1, u1, v0, r, g, b, a, light, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, x0, y0, z1, u1, v1, r, g, b, a, light, 0.0F, 0.0F, 1.0F);

        vertex(consumer, pose, x0, y0, z1, u0, v1, r, g, b, a, light, -1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z1, u0, v0, r, g, b, a, light, -1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z0, u1, v0, r, g, b, a, light, -1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x0, y0, z0, u1, v1, r, g, b, a, light, -1.0F, 0.0F, 0.0F);

        vertex(consumer, pose, x1, y0, z0, u0, v1, r, g, b, a, light, 1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z0, u0, v0, r, g, b, a, light, 1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z1, u1, v0, r, g, b, a, light, 1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y0, z1, u1, v1, r, g, b, a, light, 1.0F, 0.0F, 0.0F);
    }

    public static void prism(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                             float[] side, float[] top, float[] bottom, float r, float g, float b, float a, int light) {
        vertex(consumer, pose, x0, y1, z0, top[0], top[1], r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z1, top[0], top[3], r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z1, top[2], top[3], r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z0, top[2], top[1], r, g, b, a, light, 0.0F, 1.0F, 0.0F);

        vertex(consumer, pose, x0, y0, z0, bottom[0], bottom[1], r, g, b, a, light, 0.0F, -1.0F, 0.0F);
        vertex(consumer, pose, x1, y0, z0, bottom[2], bottom[1], r, g, b, a, light, 0.0F, -1.0F, 0.0F);
        vertex(consumer, pose, x1, y0, z1, bottom[2], bottom[3], r, g, b, a, light, 0.0F, -1.0F, 0.0F);
        vertex(consumer, pose, x0, y0, z1, bottom[0], bottom[3], r, g, b, a, light, 0.0F, -1.0F, 0.0F);

        vertex(consumer, pose, x0, y0, z0, side[0], side[3], r, g, b, a, light, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x0, y1, z0, side[0], side[1], r, g, b, a, light, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x1, y1, z0, side[2], side[1], r, g, b, a, light, 0.0F, 0.0F, -1.0F);
        vertex(consumer, pose, x1, y0, z0, side[2], side[3], r, g, b, a, light, 0.0F, 0.0F, -1.0F);

        vertex(consumer, pose, x1, y0, z1, side[0], side[3], r, g, b, a, light, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, x1, y1, z1, side[0], side[1], r, g, b, a, light, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, x0, y1, z1, side[2], side[1], r, g, b, a, light, 0.0F, 0.0F, 1.0F);
        vertex(consumer, pose, x0, y0, z1, side[2], side[3], r, g, b, a, light, 0.0F, 0.0F, 1.0F);

        vertex(consumer, pose, x0, y0, z1, side[0], side[3], r, g, b, a, light, -1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z1, side[0], side[1], r, g, b, a, light, -1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x0, y1, z0, side[2], side[1], r, g, b, a, light, -1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x0, y0, z0, side[2], side[3], r, g, b, a, light, -1.0F, 0.0F, 0.0F);

        vertex(consumer, pose, x1, y0, z0, side[0], side[3], r, g, b, a, light, 1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z0, side[0], side[1], r, g, b, a, light, 1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y1, z1, side[2], side[1], r, g, b, a, light, 1.0F, 0.0F, 0.0F);
        vertex(consumer, pose, x1, y0, z1, side[2], side[3], r, g, b, a, light, 1.0F, 0.0F, 0.0F);
    }

    public static void flatSquare(VertexConsumer consumer, PoseStack.Pose pose, float cx, float y, float cz, float half, float angle,
                                  float r, float g, float b, float a, int light) {
        float cos = (float) Math.cos(angle) * half;
        float sin = (float) Math.sin(angle) * half;
        vertex(consumer, pose, cx - cos + sin, y, cz - sin - cos, 0.0F, 0.0F, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, cx - cos - sin, y, cz - sin + cos, 0.0F, 1.0F, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, cx + cos - sin, y, cz + sin + cos, 1.0F, 1.0F, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
        vertex(consumer, pose, cx + cos + sin, y, cz + sin - cos, 1.0F, 0.0F, r, g, b, a, light, 0.0F, 1.0F, 0.0F);
    }
}
