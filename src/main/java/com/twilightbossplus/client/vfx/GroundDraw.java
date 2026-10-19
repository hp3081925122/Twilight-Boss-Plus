package com.twilightbossplus.client.vfx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class GroundDraw {
    private static final int SEGMENTS = 48;
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    private GroundDraw() {
    }

    public static double ground(ClientLevel level, double x, double z, double reference) {
        int top = Mth.floor(reference) + 2;
        for (int y = top; y > top - 10; y--) {
            POS.set(Mth.floor(x), y, Mth.floor(z));
            BlockState state = level.getBlockState(POS);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(level, POS);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y) + 0.04;
            }
        }
        return reference + 0.04;
    }

    public static int groundColor(ClientLevel level, double x, double y, double z) {
        POS.set(Mth.floor(x), Mth.floor(y - 0.5), Mth.floor(z));
        BlockState state = level.getBlockState(POS);
        if (state.isAir()) {
            return 0x7A5A3A;
        }
        int color = state.getMapColor(level, POS).col;
        if (color == 0) {
            return 0x7A5A3A;
        }
        return color;
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, double x, double y, double z, int color, float alpha) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        vc.vertex(m, (float) x, (float) y, (float) z).color(r, g, b, Mth.clamp(alpha, 0.0F, 1.0F)).endVertex();
    }

    public static void quad(VertexConsumer vc, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2,
                            double x3, double y3, double z3, double x4, double y4, double z4, int color, float alpha) {
        gradient(vc, m, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, color, alpha, alpha);
    }

    public static void gradient(VertexConsumer vc, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2,
                                double x3, double y3, double z3, double x4, double y4, double z4, int color, float alpha12, float alpha34) {
        if (alpha12 <= 0.004F && alpha34 <= 0.004F) {
            return;
        }
        vertex(vc, m, x1, y1, z1, color, alpha12);
        vertex(vc, m, x2, y2, z2, color, alpha12);
        vertex(vc, m, x3, y3, z3, color, alpha34);
        vertex(vc, m, x4, y4, z4, color, alpha34);
        vertex(vc, m, x4, y4, z4, color, alpha34);
        vertex(vc, m, x3, y3, z3, color, alpha34);
        vertex(vc, m, x2, y2, z2, color, alpha12);
        vertex(vc, m, x1, y1, z1, color, alpha12);
    }

    public static void band(ClientLevel level, VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double inner, double outer,
                            double startAngle, double sweep, int color, float alpha) {
        if (outer <= 0.01 || outer <= inner) {
            return;
        }
        int steps = Math.max(6, (int) (SEGMENTS * Math.abs(sweep) / Mth.TWO_PI));
        double previousCos = Math.cos(startAngle);
        double previousSin = Math.sin(startAngle);
        double pInnerY = ground(level, cx + previousCos * inner, cz + previousSin * inner, cy);
        double pOuterY = ground(level, cx + previousCos * outer, cz + previousSin * outer, cy);
        for (int i = 1; i <= steps; i++) {
            double angle = startAngle + sweep * i / steps;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double innerY = ground(level, cx + cos * inner, cz + sin * inner, cy);
            double outerY = ground(level, cx + cos * outer, cz + sin * outer, cy);
            quad(vc, m,
                    cx + previousCos * inner, pInnerY, cz + previousSin * inner,
                    cx + previousCos * outer, pOuterY, cz + previousSin * outer,
                    cx + cos * outer, outerY, cz + sin * outer,
                    cx + cos * inner, innerY, cz + sin * inner,
                    color, alpha);
            previousCos = cos;
            previousSin = sin;
            pInnerY = innerY;
            pOuterY = outerY;
        }
    }

    public static void ring(ClientLevel level, VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double inner, double outer, int color, float alpha) {
        band(level, vc, m, cx, cy, cz, inner, outer, 0.0, Mth.TWO_PI, color, alpha);
    }

    public static void flatBand(VertexConsumer vc, Matrix4f m, double cx, double y, double cz, double inner, double outer,
                                double startAngle, double sweep, int color, float alphaInner, float alphaOuter) {
        if (outer <= 0.01 || outer <= inner) {
            return;
        }
        int steps = Math.max(6, (int) (SEGMENTS * Math.abs(sweep) / Mth.TWO_PI));
        double previousCos = Math.cos(startAngle);
        double previousSin = Math.sin(startAngle);
        for (int i = 1; i <= steps; i++) {
            double angle = startAngle + sweep * i / steps;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            gradient(vc, m,
                    cx + cos * inner, y, cz + sin * inner,
                    cx + previousCos * inner, y, cz + previousSin * inner,
                    cx + previousCos * outer, y, cz + previousSin * outer,
                    cx + cos * outer, y, cz + sin * outer,
                    color, alphaInner, alphaOuter);
            previousCos = cos;
            previousSin = sin;
        }
    }

    public static void cylinder(VertexConsumer vc, Matrix4f m, double cx, double y, double cz, double radius, double height,
                                int sides, double twist, int color, float alphaBottom, float alphaTop) {
        if (radius <= 0.01 || height <= 0.01) {
            return;
        }
        double previous = twist;
        for (int i = 1; i <= sides; i++) {
            double angle = twist + Mth.TWO_PI * i / sides;
            double x0 = cx + Math.cos(previous) * radius;
            double z0 = cz + Math.sin(previous) * radius;
            double x1 = cx + Math.cos(angle) * radius;
            double z1 = cz + Math.sin(angle) * radius;
            gradient(vc, m, x0, y, z0, x1, y, z1, x1, y + height, z1, x0, y + height, z0, color, alphaBottom, alphaTop);
            previous = angle;
        }
    }

    public static void billboard(VertexConsumer vc, Matrix4f m, Vector3f left, Vector3f up, double x, double y, double z, double size, int color, float alpha) {
        double ux = up.x() * size;
        double uy = up.y() * size;
        double uz = up.z() * size;
        double lx = left.x() * size;
        double ly = left.y() * size;
        double lz = left.z() * size;
        quad(vc, m, x + ux, y + uy, z + uz, x + lx, y + ly, z + lz, x - ux, y - uy, z - uz, x - lx, y - ly, z - lz, color, alpha);
    }

    public static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 camera, Vec3 a, Vec3 b, double width, int color, float alphaA, float alphaB) {
        Vec3 dir = b.subtract(a);
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = dir.cross(camera.subtract(mid));
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width * 0.5);
        gradient(vc, m, a.x - side.x, a.y - side.y, a.z - side.z, a.x + side.x, a.y + side.y, a.z + side.z,
                b.x + side.x, b.y + side.y, b.z + side.z, b.x - side.x, b.y - side.y, b.z - side.z, color, alphaA, alphaB);
    }

    public static void cube(VertexConsumer vc, Matrix4f m, double x, double y, double z, double half, double spin, int color, float alpha) {
        double c = Math.cos(spin) * half;
        double s = Math.sin(spin) * half;
        double ax = x - c + s;
        double az = z - s - c;
        double bx = x + c + s;
        double bz = z + s - c;
        double cxp = x + c - s;
        double czp = z + s + c;
        double dx = x - c - s;
        double dz = z - s + c;
        double y0 = y - half;
        double y1 = y + half;
        int top = shade(color, 1.0F);
        int side = shade(color, 0.78F);
        int side2 = shade(color, 0.62F);
        quadOnce(vc, m, ax, y1, az, dx, y1, dz, cxp, y1, czp, bx, y1, bz, top, alpha);
        quadOnce(vc, m, ax, y0, az, bx, y0, bz, cxp, y0, czp, dx, y0, dz, side2, alpha);
        quadOnce(vc, m, ax, y0, az, ax, y1, az, bx, y1, bz, bx, y0, bz, side, alpha);
        quadOnce(vc, m, bx, y0, bz, bx, y1, bz, cxp, y1, czp, cxp, y0, czp, side2, alpha);
        quadOnce(vc, m, cxp, y0, czp, cxp, y1, czp, dx, y1, dz, dx, y0, dz, side, alpha);
        quadOnce(vc, m, dx, y0, dz, dx, y1, dz, ax, y1, az, ax, y0, az, side2, alpha);
    }

    private static void quadOnce(VertexConsumer vc, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2,
                                 double x3, double y3, double z3, double x4, double y4, double z4, int color, float alpha) {
        vertex(vc, m, x1, y1, z1, color, alpha);
        vertex(vc, m, x2, y2, z2, color, alpha);
        vertex(vc, m, x3, y3, z3, color, alpha);
        vertex(vc, m, x4, y4, z4, color, alpha);
    }

    public static int shade(int color, float factor) {
        int r = Mth.clamp((int) ((color >> 16 & 255) * factor), 0, 255);
        int g = Mth.clamp((int) ((color >> 8 & 255) * factor), 0, 255);
        int b = Mth.clamp((int) ((color & 255) * factor), 0, 255);
        return r << 16 | g << 8 | b;
    }
}
