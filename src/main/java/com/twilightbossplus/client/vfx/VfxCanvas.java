package com.twilightbossplus.client.vfx;


import com.mojang.blaze3d.vertex.ByteBufferBuilder;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VfxCanvas {
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
    private static final float INSET = 0.04F;

    private final MultiBufferSource.BufferSource source;
    public ClientLevel level;
    public float partial;
    public Vec3 camera = Vec3.ZERO;
    public final Vector3f left = new Vector3f(1.0F, 0.0F, 0.0F);
    public final Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
    private Matrix4f matrix = new Matrix4f();
    private VertexConsumer vc;
    private float red = 1.0F;
    private float green = 1.0F;
    private float blue = 1.0F;

    private float regionU;
    private float regionV;
    private float regionW = 128.0F;
    private float regionH = 16.0F;
    private float alpha = 1.0F;
    private double scroll;
    private double density;
    private double lift;
    private float tail = 1.0F;
    private float head = 1.0F;
    private float outerEdge = 1.0F;
    private float innerEdge = 1.0F;

    public VfxCanvas() {
        SequencedMap<RenderType, ByteBufferBuilder> buffers = new LinkedHashMap<>();
        for (VfxAtlas atlas : VfxAtlas.values()) {
            buffers.put(atlas.flat, new ByteBufferBuilder(atlas.flat.bufferSize()));
            buffers.put(atlas.glow, new ByteBufferBuilder(atlas.glow.bufferSize()));
        }
        this.source = MultiBufferSource.immediateWithBuffers(buffers, new ByteBufferBuilder(256));
    }

    public void begin(ClientLevel level, Matrix4f matrix, Vec3 camera, Vector3f left, Vector3f up, float partial) {
        this.level = level;
        this.matrix = matrix;
        this.camera = camera;
        this.left.set(left);
        this.up.set(up);
        this.partial = partial;
        this.vc = null;
    }

    public void finish() {
        for (VfxAtlas atlas : VfxAtlas.values()) {
            this.source.endBatch(atlas.flat);
        }
        for (VfxAtlas atlas : VfxAtlas.values()) {
            this.source.endBatch(atlas.glow);
        }
        this.vc = null;
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

    public static int lit(ClientLevel level, Vec3 at, int color) {
        BlockPos pos = BlockPos.containing(at.x, at.y + 0.5, at.z);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        int sky = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        float light = 0.3F + 0.7F * Math.max(block, sky) / 15.0F;
        int r = (int) ((color >> 16 & 255) * light);
        int g = (int) ((color >> 8 & 255) * light);
        int b = (int) ((color & 255) * light);
        return r << 16 | g << 8 | b;
    }

    public static Vec3[] line(Vec3 a, Vec3 b, double step) {
        int count = Math.max(1, (int) Math.ceil(a.distanceTo(b) / step));
        Vec3[] points = new Vec3[count + 1];
        for (int i = 0; i <= count; i++) {
            points[i] = a.lerp(b, i / (double) count);
        }
        return points;
    }

    public static Vec3 pos(Entity entity, float partial) {
        return new Vec3(Mth.lerp(partial, entity.xo, entity.getX()), Mth.lerp(partial, entity.yo, entity.getY()), Mth.lerp(partial, entity.zo, entity.getZ()));
    }

    private void use(VfxAtlas atlas, boolean glow) {
        if (glow) {
            this.vc = this.source.getBuffer(atlas.glow);
        } else {
            this.vc = this.source.getBuffer(atlas.flat);
        }
    }

    private void color(int color) {
        this.red = (color >> 16 & 255) / 255.0F;
        this.green = (color >> 8 & 255) / 255.0F;
        this.blue = (color & 255) / 255.0F;
    }

    private void vertex(double x, double y, double z, float u, float v, float a) {
        this.vc.addVertex(this.matrix, (float) x, (float) y, (float) z).setColor(this.red, this.green, this.blue, Mth.clamp(a, 0.0F, 1.0F)).setUv(u / VfxAtlas.SIZE, v / VfxAtlas.SIZE).setLight(0xF000F0);
    }

    public VfxCanvas strip(VfxAtlas atlas, boolean glow, int index, int color, float alpha) {
        this.use(atlas, glow);
        this.color(color);
        this.regionU = 0.0F;
        this.regionV = index * 16.0F;
        this.regionW = 128.0F;
        this.regionH = 16.0F;
        this.alpha = alpha;
        this.scroll = 0.0;
        this.density = 0.0;
        this.lift = 0.0;
        this.tail = 1.0F;
        this.head = 1.0F;
        this.outerEdge = 1.0F;
        this.innerEdge = 1.0F;
        return this;
    }

    public VfxCanvas scroll(double pixels) {
        this.scroll = pixels;
        return this;
    }

    public VfxCanvas density(double pixelsPerBlock) {
        this.density = pixelsPerBlock;
        return this;
    }

    public VfxCanvas lift(double amount) {
        this.lift = amount;
        return this;
    }

    public VfxCanvas fade(float tail, float head) {
        this.tail = tail;
        this.head = head;
        return this;
    }

    public VfxCanvas edges(float outer, float inner) {
        this.outerEdge = outer;
        this.innerEdge = inner;
        return this;
    }

    private double y(double x, double z, double reference, boolean hug) {
        if (hug) {
            return ground(this.level, x, z, reference) + this.lift;
        }
        return reference + this.lift;
    }

    private void stripQuad(double ax0, double ay0, double az0, double bx0, double by0, double bz0,
                           double ax1, double ay1, double az1, double bx1, double by1, double bz1,
                           double uStart, double uLength, float alphaStart, float alphaEnd) {
        if (uLength <= 1.0E-5 || (alphaStart <= 0.003F && alphaEnd <= 0.003F)) {
            return;
        }
        double u = uStart % this.regionW;
        if (u < 0.0) {
            u += this.regionW;
        }
        float v0 = this.regionV + INSET;
        float v1 = this.regionV + this.regionH - INSET;
        double done = 0.0;
        int guard = 0;
        while (done < uLength - 1.0E-6 && guard < 64) {
            guard++;
            double take = Math.min(uLength - done, this.regionW - u);
            double f0 = done / uLength;
            double f1 = (done + take) / uLength;
            float a0 = Mth.lerp((float) f0, alphaStart, alphaEnd) * this.alpha;
            float a1 = Mth.lerp((float) f1, alphaStart, alphaEnd) * this.alpha;
            float u0 = (float) (this.regionU + u) + INSET;
            float u1 = (float) (this.regionU + u + take) - INSET;
            this.vertex(Mth.lerp(f0, ax0, ax1), Mth.lerp(f0, ay0, ay1), Mth.lerp(f0, az0, az1), u0, v0, a0 * this.outerEdge);
            this.vertex(Mth.lerp(f0, bx0, bx1), Mth.lerp(f0, by0, by1), Mth.lerp(f0, bz0, bz1), u0, v1, a0 * this.innerEdge);
            this.vertex(Mth.lerp(f1, bx0, bx1), Mth.lerp(f1, by0, by1), Mth.lerp(f1, bz0, bz1), u1, v1, a1 * this.innerEdge);
            this.vertex(Mth.lerp(f1, ax0, ax1), Mth.lerp(f1, ay0, ay1), Mth.lerp(f1, az0, az1), u1, v0, a1 * this.outerEdge);
            done += take;
            u = 0.0;
        }
    }

    public void arc(double cx, double cy, double cz, double inner, double outer, double start, double sweep, boolean hug) {
        if (outer <= inner || outer < 0.02 || Math.abs(sweep) < 1.0E-4) {
            return;
        }
        double mid = (inner + outer) * 0.5;
        double dens = this.density;
        if (dens <= 0.0) {
            dens = 16.0 / (outer - inner);
        }
        int steps = Mth.clamp((int) (Math.abs(sweep) * Math.max(outer, 1.0) * 1.2), 6, 128);
        double segmentU = mid * Math.abs(sweep) / steps * dens;
        double cos0 = Math.cos(start);
        double sin0 = Math.sin(start);
        double outerY0 = this.y(cx + cos0 * outer, cz + sin0 * outer, cy, hug);
        double innerY0 = this.y(cx + cos0 * inner, cz + sin0 * inner, cy, hug);
        for (int i = 1; i <= steps; i++) {
            double angle = start + sweep * i / steps;
            double cos1 = Math.cos(angle);
            double sin1 = Math.sin(angle);
            double outerY1 = this.y(cx + cos1 * outer, cz + sin1 * outer, cy, hug);
            double innerY1 = this.y(cx + cos1 * inner, cz + sin1 * inner, cy, hug);
            float aStart = Mth.lerp((float) (i - 1) / steps, this.tail, this.head);
            float aEnd = Mth.lerp((float) i / steps, this.tail, this.head);
            this.stripQuad(cx + cos0 * outer, outerY0, cz + sin0 * outer, cx + cos0 * inner, innerY0, cz + sin0 * inner,
                    cx + cos1 * outer, outerY1, cz + sin1 * outer, cx + cos1 * inner, innerY1, cz + sin1 * inner,
                    this.scroll + segmentU * (i - 1), segmentU, aStart, aEnd);
            cos0 = cos1;
            sin0 = sin1;
            outerY0 = outerY1;
            innerY0 = innerY1;
        }
    }

    public void wall(double cx, double cy, double cz, double radius, double height, double start, double sweep, boolean hug) {
        if (radius < 0.02 || height < 0.02 || Math.abs(sweep) < 1.0E-4) {
            return;
        }
        double dens = this.density;
        if (dens <= 0.0) {
            dens = 16.0 / height;
        }
        int steps = Mth.clamp((int) (Math.abs(sweep) * Math.max(radius, 1.0) * 1.2), 8, 128);
        double segmentU = radius * Math.abs(sweep) / steps * dens;
        double x0 = cx + Math.cos(start) * radius;
        double z0 = cz + Math.sin(start) * radius;
        double y0 = this.y(x0, z0, cy, hug);
        for (int i = 1; i <= steps; i++) {
            double angle = start + sweep * i / steps;
            double x1 = cx + Math.cos(angle) * radius;
            double z1 = cz + Math.sin(angle) * radius;
            double y1 = this.y(x1, z1, cy, hug);
            float aStart = Mth.lerp((float) (i - 1) / steps, this.tail, this.head);
            float aEnd = Mth.lerp((float) i / steps, this.tail, this.head);
            this.stripQuad(x0, y0 + height, z0, x0, y0, z0, x1, y1 + height, z1, x1, y1, z1,
                    this.scroll + segmentU * (i - 1), segmentU, aStart, aEnd);
            x0 = x1;
            z0 = z1;
            y0 = y1;
        }
    }

    public void path(Vec3[] points, double width, boolean hug) {
        if (points.length < 2) {
            return;
        }
        double total = 0.0;
        for (int i = 1; i < points.length; i++) {
            total += points[i].distanceTo(points[i - 1]);
        }
        if (total < 1.0E-4) {
            return;
        }
        double dens = this.density;
        if (dens <= 0.0) {
            dens = 16.0 / width;
        }
        double half = width * 0.5;
        double travelled = 0.0;
        for (int i = 1; i < points.length; i++) {
            Vec3 a = points[i - 1];
            Vec3 b = points[i];
            double length = a.distanceTo(b);
            if (length < 1.0E-5) {
                continue;
            }
            double ay = a.y;
            double by = b.y;
            double sx;
            double sy;
            double sz;
            if (hug) {
                double dx = b.x - a.x;
                double dz = b.z - a.z;
                double flat = Math.sqrt(dx * dx + dz * dz);
                if (flat < 1.0E-5) {
                    continue;
                }
                sx = -dz / flat * half;
                sy = 0.0;
                sz = dx / flat * half;
                ay = this.y(a.x, a.z, a.y, true);
                by = this.y(b.x, b.z, a.y, true);
            } else {
                Vec3 dir = b.subtract(a);
                Vec3 mid = a.add(b).scale(0.5);
                Vec3 side = dir.cross(this.camera.subtract(mid));
                if (side.lengthSqr() < 1.0E-10) {
                    continue;
                }
                side = side.normalize().scale(half);
                sx = side.x;
                sy = side.y;
                sz = side.z;
                ay += this.lift;
                by += this.lift;
            }
            float aStart = Mth.lerp((float) (travelled / total), this.tail, this.head);
            float aEnd = Mth.lerp((float) ((travelled + length) / total), this.tail, this.head);
            this.stripQuad(a.x + sx, ay + sy, a.z + sz, a.x - sx, ay - sy, a.z - sz,
                    b.x + sx, by + sy, b.z + sz, b.x - sx, by - sy, b.z - sz,
                    this.scroll + travelled * dens, length * dens, aStart, aEnd);
            travelled += length;
        }
    }

    public void band(Vec3[] outer, Vec3[] inner) {
        int count = Math.min(outer.length, inner.length);
        if (count < 2) {
            return;
        }
        double total = 0.0;
        for (int i = 1; i < count; i++) {
            total += outer[i].distanceTo(outer[i - 1]);
        }
        if (total < 1.0E-4) {
            return;
        }
        double dens = this.density;
        if (dens <= 0.0) {
            dens = 16.0 / Math.max(0.05, outer[0].distanceTo(inner[0]));
        }
        double travelled = 0.0;
        for (int i = 1; i < count; i++) {
            Vec3 a0 = outer[i - 1];
            Vec3 b0 = inner[i - 1];
            Vec3 a1 = outer[i];
            Vec3 b1 = inner[i];
            double length = a0.distanceTo(a1);
            float aStart = Mth.lerp((float) (travelled / total), this.tail, this.head);
            float aEnd = Mth.lerp((float) ((travelled + length) / total), this.tail, this.head);
            this.stripQuad(a0.x, a0.y + this.lift, a0.z, b0.x, b0.y + this.lift, b0.z, a1.x, a1.y + this.lift, a1.z, b1.x, b1.y + this.lift, b1.z,
                    this.scroll + travelled * dens, Math.max(1.0E-4, length) * dens, aStart, aEnd);
            travelled += length;
        }
    }

    public void beam(Vec3 a, Vec3 b, double width) {
        this.path(new Vec3[]{a, b}, width, false);
    }

    private void billboardRegion(float u0, float v0, float size, double x, double y, double z, double radius, double roll, float a) {
        double c = Math.cos(roll) * radius;
        double s = Math.sin(roll) * radius;
        double ex = this.left.x() * c + this.up.x() * s;
        double ey = this.left.y() * c + this.up.y() * s;
        double ez = this.left.z() * c + this.up.z() * s;
        double fx = -this.left.x() * s + this.up.x() * c;
        double fy = -this.left.y() * s + this.up.y() * c;
        double fz = -this.left.z() * s + this.up.z() * c;
        float uA = u0 + INSET;
        float uB = u0 + size - INSET;
        float vA = v0 + INSET;
        float vB = v0 + size - INSET;
        this.vertex(x + ex + fx, y + ey + fy, z + ez + fz, uA, vA, a);
        this.vertex(x + ex - fx, y + ey - fy, z + ez - fz, uA, vB, a);
        this.vertex(x - ex - fx, y - ey - fy, z - ez - fz, uB, vB, a);
        this.vertex(x - ex + fx, y - ey + fy, z - ez + fz, uB, vA, a);
    }

    private void flatRegion(float u0, float v0, float size, double x, double y, double z, double radius, double rotation, float a) {
        double c = Math.cos(rotation) * radius;
        double s = Math.sin(rotation) * radius;
        float uA = u0 + INSET;
        float uB = u0 + size - INSET;
        float vA = v0 + INSET;
        float vB = v0 + size - INSET;
        this.vertex(x - c + s, y, z - s - c, uA, vA, a);
        this.vertex(x - c - s, y, z - s + c, uA, vB, a);
        this.vertex(x + c - s, y, z + s + c, uB, vB, a);
        this.vertex(x + c + s, y, z + s - c, uB, vA, a);
    }

    public void sprite(VfxAtlas atlas, boolean glow, int row, int frame, double x, double y, double z, double size, double roll, int color, float alpha) {
        if (alpha <= 0.003F || size <= 0.001) {
            return;
        }
        this.use(atlas, glow);
        this.color(color);
        this.billboardRegion(frame * 16.0F, 128.0F + row * 16.0F, 16.0F, x, y, z, size, roll, alpha);
    }

    public void flatSprite(VfxAtlas atlas, boolean glow, int row, int frame, double x, double y, double z, double size, double rotation, int color, float alpha) {
        if (alpha <= 0.003F || size <= 0.001) {
            return;
        }
        this.use(atlas, glow);
        this.color(color);
        this.flatRegion(frame * 16.0F, 128.0F + row * 16.0F, 16.0F, x, y, z, size, rotation, alpha);
    }

    public void flatDecal(VfxAtlas atlas, boolean glow, int index, double x, double y, double z, double radius, double rotation, int color, float alpha) {
        if (alpha <= 0.003F || radius <= 0.001) {
            return;
        }
        this.use(atlas, glow);
        this.color(color);
        this.flatRegion(128.0F + 64.0F * (index % 2), 64.0F * (index / 2), 64.0F, x, y, z, radius, rotation, alpha);
    }

    public void faceDecal(VfxAtlas atlas, boolean glow, int index, double x, double y, double z, double radius, double roll, int color, float alpha) {
        if (alpha <= 0.003F || radius <= 0.001) {
            return;
        }
        this.use(atlas, glow);
        this.color(color);
        this.billboardRegion(128.0F + 64.0F * (index % 2), 64.0F * (index / 2), 64.0F, x, y, z, radius, roll, alpha);
    }

    public static Patch patch(ClientLevel level, Vec3 center, double radius) {
        int n = Mth.clamp((int) Math.ceil(radius * 1.2), 4, 24);
        double[] heights = new double[(n + 1) * (n + 1)];
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                double x = center.x - radius + 2.0 * radius * i / n;
                double z = center.z - radius + 2.0 * radius * j / n;
                heights[j * (n + 1) + i] = ground(level, x, z, center.y);
            }
        }
        return new Patch(center, radius, n, heights);
    }

    public void decal(VfxAtlas atlas, boolean glow, int index, Patch patch, double rotation, double scale, double lift, int color, float alpha) {
        if (alpha <= 0.003F || scale <= 0.01) {
            return;
        }
        this.use(atlas, glow);
        this.color(color);
        int n = patch.n;
        int stride = n + 1;
        float baseU = 128.0F + 64.0F * (index % 2);
        float baseV = 64.0F * (index / 2);
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        float[] us = new float[stride * stride];
        float[] vs = new float[stride * stride];
        int[] side = new int[stride * stride];
        for (int j = 0; j <= n; j++) {
            for (int i = 0; i <= n; i++) {
                double lx = -1.0 + 2.0 * i / n;
                double lz = -1.0 + 2.0 * j / n;
                double ru = (lx * cos - lz * sin) / scale;
                double rv = (lx * sin + lz * cos) / scale;
                int flags = 0;
                if (ru > 1.0) {
                    flags |= 1;
                }
                if (ru < -1.0) {
                    flags |= 2;
                }
                if (rv > 1.0) {
                    flags |= 4;
                }
                if (rv < -1.0) {
                    flags |= 8;
                }
                int k = j * stride + i;
                side[k] = flags;
                us[k] = baseU + (float) Mth.clamp((0.5 + ru * 0.5) * 64.0, 0.5, 63.5);
                vs[k] = baseV + (float) Mth.clamp((0.5 + rv * 0.5) * 64.0, 0.5, 63.5);
            }
        }
        double cell = 2.0 * patch.radius / n;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int k00 = j * stride + i;
                int k10 = k00 + 1;
                int k01 = k00 + stride;
                int k11 = k01 + 1;
                if ((side[k00] & side[k10] & side[k01] & side[k11]) != 0) {
                    continue;
                }
                double x0 = patch.center.x - patch.radius + cell * i;
                double z0 = patch.center.z - patch.radius + cell * j;
                this.vertex(x0, patch.heights[k00] + lift, z0, us[k00], vs[k00], alpha);
                this.vertex(x0, patch.heights[k01] + lift, z0 + cell, us[k01], vs[k01], alpha);
                this.vertex(x0 + cell, patch.heights[k11] + lift, z0 + cell, us[k11], vs[k11], alpha);
                this.vertex(x0 + cell, patch.heights[k10] + lift, z0, us[k10], vs[k10], alpha);
            }
        }
    }

    public static final class Patch {
        public final Vec3 center;
        public final double radius;
        private final int n;
        private final double[] heights;

        private Patch(Vec3 center, double radius, int n, double[] heights) {
            this.center = center;
            this.radius = radius;
            this.n = n;
            this.heights = heights;
        }
    }
}
