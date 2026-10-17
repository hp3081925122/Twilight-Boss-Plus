package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class FxDraw {
    public static final int FULL_BRIGHT = 0xF000F0;

    private static double camX;
    private static double camY;
    private static double camZ;
    private static float rightX = 1.0F;
    private static float rightY;
    private static float rightZ;
    private static float upX;
    private static float upY = 1.0F;
    private static float upZ;
    private static boolean flowMode;
    private static int dissolveParam;
    private static int edgeParam = 80;
    private static int scrollUParam = 16000;
    private static int scrollVParam = 16000;
    private static float hotParam;
    private static float scaleParam = 0.125F;
    private static float flowParam;

    private FxDraw() {
    }

    public static void params(float dissolve, float edge, float scrollU, float scrollV, float hot, float noiseScale, float flow) {
        flowMode = true;
        dissolveParam = Mth.clamp((int) (dissolve * 1000.0F), 0, 1000);
        edgeParam = Mth.clamp((int) (edge * 1000.0F), 1, 1000);
        scrollUParam = 16000 + Mth.clamp((int) (scrollU * 1000.0F), -15000, 15000);
        scrollVParam = 16000 + Mth.clamp((int) (scrollV * 1000.0F), -15000, 15000);
        hotParam = Mth.clamp(hot, 0.0F, 1.0F);
        scaleParam = Mth.clamp(noiseScale / 8.0F, 0.0F, 1.0F);
        flowParam = Mth.clamp(flow, 0.0F, 1.0F);
    }

    public static void flow(float scrollU, float scrollV, float hot, float noiseScale, float flow) {
        params(0.0F, 0.08F, scrollU, scrollV, hot, noiseScale, flow);
    }

    public static void resetParams() {
        flowMode = false;
        dissolveParam = 0;
        edgeParam = 80;
        scrollUParam = 16000;
        scrollVParam = 16000;
        hotParam = 0.0F;
        scaleParam = 0.125F;
        flowParam = 0.0F;
    }

    public static void camera(Camera camera) {
        Vec3 pos = camera.getPosition();
        camX = pos.x;
        camY = pos.y;
        camZ = pos.z;
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        rightX = -left.x();
        rightY = -left.y();
        rightZ = -left.z();
        upX = up.x();
        upY = up.y();
        upZ = up.z();
    }

    public static Vec3 cameraPos() {
        return new Vec3(camX, camY, camZ);
    }

    public static double cameraDistanceSqr(double x, double y, double z) {
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        return dx * dx + dy * dy + dz * dz;
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

    public static void glowVertex(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float u, float v,
                                  float r, float g, float b, float a) {
        if (flowMode) {
            consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
                .color(r, g, b, Mth.clamp(a, 0.0F, 1.0F))
                .uv(u, v)
                .overlayCoords(dissolveParam, edgeParam)
                .uv2(scrollUParam, scrollVParam)
                .normal(hotParam, scaleParam, flowParam)
                .endVertex();
            return;
        }
        vertex(consumer, pose, (float) x, (float) y, (float) z, u, v, r, g, b, Mth.clamp(a, 0.0F, 1.0F), FULL_BRIGHT, 0.0F, 1.0F, 0.0F);
    }

    public static void sprite(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float size, float roll,
                              float r, float g, float b, float a) {
        stretchedSprite(consumer, pose, x, y, z, size, size, roll, r, g, b, a);
    }

    public static void stretchedSprite(VertexConsumer consumer, PoseStack.Pose pose, double x, double y, double z, float width, float height, float roll,
                                       float r, float g, float b, float a) {
        if (a <= 0.003F || width <= 0.0F || height <= 0.0F) {
            return;
        }
        float cos = Mth.cos(roll);
        float sin = Mth.sin(roll);
        float hw = width * 0.5F;
        float hh = height * 0.5F;
        float ax = (rightX * cos + upX * sin) * hw;
        float ay = (rightY * cos + upY * sin) * hw;
        float az = (rightZ * cos + upZ * sin) * hw;
        float bx = (upX * cos - rightX * sin) * hh;
        float by = (upY * cos - rightY * sin) * hh;
        float bz = (upZ * cos - rightZ * sin) * hh;
        glowVertex(consumer, pose, x - ax - bx, y - ay - by, z - az - bz, 0.0F, 1.0F, r, g, b, a);
        glowVertex(consumer, pose, x + ax - bx, y + ay - by, z + az - bz, 1.0F, 1.0F, r, g, b, a);
        glowVertex(consumer, pose, x + ax + bx, y + ay + by, z + az + bz, 1.0F, 0.0F, r, g, b, a);
        glowVertex(consumer, pose, x - ax + bx, y - ay + by, z - az + bz, 0.0F, 0.0F, r, g, b, a);
    }

    public static void beam(VertexConsumer consumer, PoseStack.Pose pose, Vec3 from, Vec3 to, float halfWidthFrom, float halfWidthTo,
                            float u0, float u1, float r, float g, float b, float alphaFrom, float alphaTo) {
        if (alphaFrom <= 0.003F && alphaTo <= 0.003F) {
            return;
        }
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double vx = camX - (from.x + to.x) * 0.5D;
        double vy = camY - (from.y + to.y) * 0.5D;
        double vz = camZ - (from.z + to.z) * 0.5D;
        double sx = dy * vz - dz * vy;
        double sy = dz * vx - dx * vz;
        double sz = dx * vy - dy * vx;
        double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1.0E-7D) {
            return;
        }
        sx /= len;
        sy /= len;
        sz /= len;
        glowVertex(consumer, pose, from.x - sx * halfWidthFrom, from.y - sy * halfWidthFrom, from.z - sz * halfWidthFrom, u0, 0.0F, r, g, b, alphaFrom);
        glowVertex(consumer, pose, from.x + sx * halfWidthFrom, from.y + sy * halfWidthFrom, from.z + sz * halfWidthFrom, u0, 1.0F, r, g, b, alphaFrom);
        glowVertex(consumer, pose, to.x + sx * halfWidthTo, to.y + sy * halfWidthTo, to.z + sz * halfWidthTo, u1, 1.0F, r, g, b, alphaTo);
        glowVertex(consumer, pose, to.x - sx * halfWidthTo, to.y - sy * halfWidthTo, to.z - sz * halfWidthTo, u1, 0.0F, r, g, b, alphaTo);
    }

    public static void trail(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, int count, float[] halfWidths, float[] alphas,
                             float u0, float uPerBlock, float r, float g, float b) {
        if (count < 2) {
            return;
        }
        double[] sxs = new double[count];
        double[] sys = new double[count];
        double[] szs = new double[count];
        for (int i = 0; i < count; i++) {
            Vec3 prev = points[Math.max(0, i - 1)];
            Vec3 next = points[Math.min(count - 1, i + 1)];
            double tx = next.x - prev.x;
            double ty = next.y - prev.y;
            double tz = next.z - prev.z;
            double vx = camX - points[i].x;
            double vy = camY - points[i].y;
            double vz = camZ - points[i].z;
            double sx = ty * vz - tz * vy;
            double sy = tz * vx - tx * vz;
            double sz = tx * vy - ty * vx;
            double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (len < 1.0E-7D) {
                len = 1.0D;
            }
            sxs[i] = sx / len;
            sys[i] = sy / len;
            szs[i] = sz / len;
        }
        float u = u0;
        for (int i = 0; i < count - 1; i++) {
            Vec3 p0 = points[i];
            Vec3 p1 = points[i + 1];
            float segment = (float) p0.distanceTo(p1);
            float nextU = u + segment * uPerBlock;
            float w0 = halfWidths[i];
            float w1 = halfWidths[i + 1];
            if (alphas[i] > 0.003F || alphas[i + 1] > 0.003F) {
                glowVertex(consumer, pose, p0.x - sxs[i] * w0, p0.y - sys[i] * w0, p0.z - szs[i] * w0, u, 0.0F, r, g, b, alphas[i]);
                glowVertex(consumer, pose, p0.x + sxs[i] * w0, p0.y + sys[i] * w0, p0.z + szs[i] * w0, u, 1.0F, r, g, b, alphas[i]);
                glowVertex(consumer, pose, p1.x + sxs[i + 1] * w1, p1.y + sys[i + 1] * w1, p1.z + szs[i + 1] * w1, nextU, 1.0F, r, g, b, alphas[i + 1]);
                glowVertex(consumer, pose, p1.x - sxs[i + 1] * w1, p1.y - sys[i + 1] * w1, p1.z - szs[i + 1] * w1, nextU, 0.0F, r, g, b, alphas[i + 1]);
            }
            u = nextU;
        }
    }

    public static Vec3[] jagged(RandomSource random, Vec3 from, Vec3 to, int segments, double amplitude) {
        Vec3[] points = new Vec3[segments + 1];
        Vec3 dir = to.subtract(from);
        Vec3 axis = dir.normalize();
        Vec3 helper = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(axis.y) > 0.9D) {
            helper = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 e1 = axis.cross(helper).normalize();
        Vec3 e2 = axis.cross(e1).normalize();
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double envelope = Math.sin(t * Math.PI);
            double o1 = (random.nextDouble() - 0.5D) * 2.0D * amplitude * envelope;
            double o2 = (random.nextDouble() - 0.5D) * 2.0D * amplitude * envelope;
            points[i] = from.add(dir.scale(t)).add(e1.scale(o1)).add(e2.scale(o2));
        }
        return points;
    }

    public static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float u0, float v0, float u1, float v1,
                            float r, float g, float b, float a) {
        if (a <= 0.003F) {
            return;
        }
        glowVertex(consumer, pose, p0.x, p0.y, p0.z, u0, v0, r, g, b, a);
        glowVertex(consumer, pose, p1.x, p1.y, p1.z, u0, v1, r, g, b, a);
        glowVertex(consumer, pose, p2.x, p2.y, p2.z, u1, v1, r, g, b, a);
        glowVertex(consumer, pose, p3.x, p3.y, p3.z, u1, v0, r, g, b, a);
    }

    public static void flat(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float half, float angle,
                            float r, float g, float b, float a) {
        if (a <= 0.003F || half <= 0.0F) {
            return;
        }
        float cos = Mth.cos(angle) * half;
        float sin = Mth.sin(angle) * half;
        glowVertex(consumer, pose, cx - cos + sin, y, cz - sin - cos, 0.0F, 0.0F, r, g, b, a);
        glowVertex(consumer, pose, cx - cos - sin, y, cz - sin + cos, 0.0F, 1.0F, r, g, b, a);
        glowVertex(consumer, pose, cx + cos - sin, y, cz + sin + cos, 1.0F, 1.0F, r, g, b, a);
        glowVertex(consumer, pose, cx + cos + sin, y, cz + sin - cos, 1.0F, 0.0F, r, g, b, a);
    }

    public static void ring(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float inner, float outer, int segments,
                            float angleOffset, float uRepeat, float r, float g, float b, float innerAlpha, float outerAlpha) {
        arc(consumer, pose, cx, y, cz, inner, outer, angleOffset, angleOffset + Mth.TWO_PI, segments, uRepeat, r, g, b, innerAlpha, outerAlpha);
    }

    public static void arc(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float inner, float outer,
                           float startAngle, float endAngle, int segments, float uRepeat, float r, float g, float b, float innerAlpha, float outerAlpha) {
        if ((innerAlpha <= 0.003F && outerAlpha <= 0.003F) || outer <= 0.0F) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float a0 = startAngle + (endAngle - startAngle) * t0;
            float a1 = startAngle + (endAngle - startAngle) * t1;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            glowVertex(consumer, pose, cx + c0 * inner, y, cz + s0 * inner, uRepeat * t0, 0.0F, r, g, b, innerAlpha);
            glowVertex(consumer, pose, cx + c0 * outer, y, cz + s0 * outer, uRepeat * t0, 1.0F, r, g, b, outerAlpha);
            glowVertex(consumer, pose, cx + c1 * outer, y, cz + s1 * outer, uRepeat * t1, 1.0F, r, g, b, outerAlpha);
            glowVertex(consumer, pose, cx + c1 * inner, y, cz + s1 * inner, uRepeat * t1, 0.0F, r, g, b, innerAlpha);
        }
    }

    public static void wall(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float radius, float height, int segments,
                            float uRepeat, float vOffset, float r, float g, float b, float bottomAlpha, float topAlpha) {
        wallArc(consumer, pose, cx, y, cz, radius, radius, height, 0.0F, Mth.TWO_PI, segments, uRepeat, vOffset, r, g, b, bottomAlpha, topAlpha);
    }

    public static void wallArc(VertexConsumer consumer, PoseStack.Pose pose, double cx, double y, double cz, float bottomRadius, float topRadius,
                               float height, float startAngle, float endAngle, int segments, float uRepeat, float vOffset,
                               float r, float g, float b, float bottomAlpha, float topAlpha) {
        if ((bottomAlpha <= 0.003F && topAlpha <= 0.003F) || height <= 0.0F) {
            return;
        }
        double top = y + height;
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float a0 = startAngle + (endAngle - startAngle) * t0;
            float a1 = startAngle + (endAngle - startAngle) * t1;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            glowVertex(consumer, pose, cx + c0 * topRadius, top, cz + s0 * topRadius, uRepeat * t0, vOffset, r, g, b, topAlpha);
            glowVertex(consumer, pose, cx + c0 * bottomRadius, y, cz + s0 * bottomRadius, uRepeat * t0, vOffset + 1.0F, r, g, b, bottomAlpha);
            glowVertex(consumer, pose, cx + c1 * bottomRadius, y, cz + s1 * bottomRadius, uRepeat * t1, vOffset + 1.0F, r, g, b, bottomAlpha);
            glowVertex(consumer, pose, cx + c1 * topRadius, top, cz + s1 * topRadius, uRepeat * t1, vOffset, r, g, b, topAlpha);
        }
    }

    public static void shard(VertexConsumer consumer, PoseStack.Pose pose, float radius, float height, float tip, int sides,
                             float r, float g, float b, float a) {
        if (a <= 0.003F || height <= 0.0F) {
            return;
        }
        float apex = height + tip;
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float u0 = (float) i / sides;
            float u1 = (float) (i + 1) / sides;
            float shade = 0.72F + 0.28F * Mth.cos(a0 * 0.5F + 0.6F) * Mth.cos(a0 * 0.5F + 0.6F);
            glowVertex(consumer, pose, x0, height, z0, u0, 0.25F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, x0, 0.0F, z0, u0, 1.0F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, x1, 0.0F, z1, u1, 1.0F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, x1, height, z1, u1, 0.25F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, 0.0F, apex, 0.0F, (u0 + u1) * 0.5F, 0.0F, r, g, b, a);
            glowVertex(consumer, pose, 0.0F, apex, 0.0F, (u0 + u1) * 0.5F, 0.0F, r, g, b, a);
            glowVertex(consumer, pose, x0, height, z0, u0, 0.25F, r, g, b, a);
            glowVertex(consumer, pose, x1, height, z1, u1, 0.25F, r, g, b, a);
        }
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

    public static void cube(VertexConsumer consumer, PoseStack.Pose pose, double cx, double cy, double cz, float half, float height, float yaw,
                            float r, float g, float b, float a) {
        if (a <= 0.003F || half <= 0.0F) {
            return;
        }
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        double[] xs = new double[4];
        double[] zs = new double[4];
        float[] lx = {-half, half, half, -half};
        float[] lz = {-half, -half, half, half};
        for (int i = 0; i < 4; i++) {
            xs[i] = cx + lx[i] * cos - lz[i] * sin;
            zs[i] = cz + lx[i] * sin + lz[i] * cos;
        }
        double top = cy + height;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            glowVertex(consumer, pose, xs[i], top, zs[i], 0.0F, 0.0F, r, g, b, a);
            glowVertex(consumer, pose, xs[i], cy, zs[i], 0.0F, 1.0F, r, g, b, a);
            glowVertex(consumer, pose, xs[j], cy, zs[j], 1.0F, 1.0F, r, g, b, a);
            glowVertex(consumer, pose, xs[j], top, zs[j], 1.0F, 0.0F, r, g, b, a);
        }
        glowVertex(consumer, pose, xs[0], top, zs[0], 0.0F, 0.0F, r, g, b, a);
        glowVertex(consumer, pose, xs[3], top, zs[3], 0.0F, 1.0F, r, g, b, a);
        glowVertex(consumer, pose, xs[2], top, zs[2], 1.0F, 1.0F, r, g, b, a);
        glowVertex(consumer, pose, xs[1], top, zs[1], 1.0F, 0.0F, r, g, b, a);
    }

    public static void spike(VertexConsumer consumer, PoseStack.Pose pose, double bx, double by, double bz, double tx, double ty, double tz,
                             float radius, int sides, float twist, float r, float g, float b, float a) {
        if (a <= 0.003F || radius <= 0.0F) {
            return;
        }
        Vec3 axis = new Vec3(tx - bx, ty - by, tz - bz);
        double length = axis.length();
        if (length < 1.0E-4D) {
            return;
        }
        axis = axis.scale(1.0D / length);
        Vec3 helper = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(axis.y) > 0.9D) {
            helper = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 e1 = axis.cross(helper).normalize();
        Vec3 e2 = axis.cross(e1).normalize();
        for (int i = 0; i < sides; i++) {
            float a0 = twist + Mth.TWO_PI * i / sides;
            float a1 = twist + Mth.TWO_PI * (i + 1) / sides;
            Vec3 p0 = new Vec3(bx, by, bz).add(e1.scale(Mth.cos(a0) * radius)).add(e2.scale(Mth.sin(a0) * radius));
            Vec3 p1 = new Vec3(bx, by, bz).add(e1.scale(Mth.cos(a1) * radius)).add(e2.scale(Mth.sin(a1) * radius));
            float u0 = (float) i / sides;
            float u1 = (float) (i + 1) / sides;
            float shade = 0.65F + 0.35F * Math.abs(Mth.cos(a0 * 0.5F + 0.4F));
            glowVertex(consumer, pose, tx, ty, tz, (u0 + u1) * 0.5F, 0.0F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, p0.x, p0.y, p0.z, u0, 1.0F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, p1.x, p1.y, p1.z, u1, 1.0F, r * shade, g * shade, b * shade, a);
            glowVertex(consumer, pose, tx, ty, tz, (u0 + u1) * 0.5F, 0.0F, r * shade, g * shade, b * shade, a);
        }
    }

    public static void arcRibbon(VertexConsumer consumer, PoseStack.Pose pose, Vec3 center, Vec3 e1, Vec3 e2, float inner, float outer,
                                 float fromAngle, float toAngle, int segments, float r, float g, float b, float tailAlpha, float headAlpha) {
        if ((tailAlpha <= 0.003F && headAlpha <= 0.003F) || outer <= inner) {
            return;
        }
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float a0 = fromAngle + (toAngle - fromAngle) * t0;
            float a1 = fromAngle + (toAngle - fromAngle) * t1;
            Vec3 d0 = e1.scale(Mth.cos(a0)).add(e2.scale(Mth.sin(a0)));
            Vec3 d1 = e1.scale(Mth.cos(a1)).add(e2.scale(Mth.sin(a1)));
            float al0 = Mth.lerp(t0, tailAlpha, headAlpha);
            float al1 = Mth.lerp(t1, tailAlpha, headAlpha);
            Vec3 i0 = center.add(d0.scale(inner));
            Vec3 o0 = center.add(d0.scale(outer));
            Vec3 o1 = center.add(d1.scale(outer));
            Vec3 i1 = center.add(d1.scale(inner));
            glowVertex(consumer, pose, i0.x, i0.y, i0.z, t0, 0.0F, r, g, b, al0);
            glowVertex(consumer, pose, o0.x, o0.y, o0.z, t0, 1.0F, r, g, b, al0);
            glowVertex(consumer, pose, o1.x, o1.y, o1.z, t1, 1.0F, r, g, b, al1);
            glowVertex(consumer, pose, i1.x, i1.y, i1.z, t1, 0.0F, r, g, b, al1);
        }
    }
}
