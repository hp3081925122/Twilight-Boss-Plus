package com.twilightbossplus.client.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import com.twilightbossplus.network.VfxPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientVfx {
    private static final int VENOM = 0x7CFF4F;
    private static final int EARTH = 0xD9A55B;
    private static final int RAGE = 0xFF3355;
    private static final int WHITE = 0xFFFFFF;
    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final List<Shard> SHARDS = new ArrayList<>();
    private static final List<float[]> SHAKES = new ArrayList<>();
    private static final Map<Integer, Integer> BURROWED = new HashMap<>();
    private static final Map<Integer, Integer> AURAS = new HashMap<>();
    private static final Vector3f LEFT = new Vector3f(1.0F, 0.0F, 0.0F);
    private static final Vector3f UP = new Vector3f(0.0F, 1.0F, 0.0F);
    private static Vec3 cameraPos = Vec3.ZERO;

    private ClientVfx() {
    }

    private static Vec3 vec(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "x"), tag.getDouble(key + "y"), tag.getDouble(key + "z"));
    }

    public static void handle(int type, CompoundTag d) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        switch (type) {
            case VfxPacket.LINE -> EFFECTS.add(new LineWarning(vec(d, "a"), vec(d, "b"), d.getFloat("w"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.CIRCLE -> EFFECTS.add(new CircleWarning(vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.GLOB -> {
                EFFECTS.add(new Glob(vec(d, "o"), vec(d, "v"), d.getDouble("g"), d.getInt("t")));
                EFFECTS.add(new CircleWarning(vec(d, "l"), d.getFloat("r"), d.getInt("t"), VENOM));
            }
            case VfxPacket.POOL -> EFFECTS.add(new Pool(vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case VfxPacket.RING -> EFFECTS.add(new Ring(vec(d, "c"), d.getFloat("speed"), d.getFloat("max"), d.getFloat("th"), d.getInt("delay"), d.getInt("color")));
            case VfxPacket.CONE -> EFFECTS.add(new Cone(d.getInt("id"), d.getFloat("len"), d.getFloat("half"), d.getInt("windup"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.BURROW -> {
                if (d.getBoolean("on")) {
                    BURROWED.put(d.getInt("id"), 200);
                } else {
                    BURROWED.remove(d.getInt("id"));
                }
            }
            case VfxPacket.BURST -> burst(level, vec(d, "c"), d.getFloat("r"), d.getInt("kind"), d.getInt("color"));
            case VfxPacket.SHAKE -> {
                Vec3 c = vec(d, "c");
                SHAKES.add(new float[]{(float) c.x, (float) c.y, (float) c.z, d.getFloat("s"), d.getFloat("r"), d.getInt("d"), 0.0F});
            }
            case VfxPacket.AURA -> AURAS.put(d.getInt("id"), d.getInt("duration"));
            case VfxPacket.BEAM -> EFFECTS.add(new Beam(vec(d, "a"), vec(d, "b"), d.getFloat("w"), d.getInt("windup"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.PILLAR -> {
                Vec3 c = vec(d, "c");
                EFFECTS.add(new Pillar(c, d.getFloat("r"), d.getFloat("h"), d.getInt("d"), d.getInt("color")));
                sparks(level, c, d.getFloat("r"), d.getInt("color"), 8, 0.35, 0.55);
            }
            case VfxPacket.TETHER -> {
                int from = d.getInt("from");
                int to = d.getInt("to");
                for (Effect effect : EFFECTS) {
                    if (effect instanceof Tether tether && tether.from == from && tether.to == to) {
                        tether.cut();
                    }
                }
                if (d.getInt("d") > 0) {
                    EFFECTS.add(new Tether(from, to, d.getInt("d"), d.getInt("color")));
                }
            }
            case VfxPacket.SIGIL -> EFFECTS.add(new Sigil(vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.VORTEX -> EFFECTS.add(new Vortex(vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.SLASH -> EFFECTS.add(new Slash(vec(d, "c"), d.getFloat("yaw"), d.getFloat("r"), d.getFloat("sweep"), d.getInt("color")));
            case VfxPacket.ORB -> EFFECTS.add(new Orb(d.getInt("id"), d.getFloat("h"), d.getFloat("s"), d.getFloat("ring"), d.getInt("d"), d.getInt("color")));
            case VfxPacket.WALL -> EFFECTS.add(new Wall(vec(d, "c"), d.getFloat("r"), d.getFloat("h"), d.getInt("d"), d.getInt("color")));
            default -> {
            }
        }
    }

    private static int count(int base) {
        return Math.max(1, Math.round(base * ClientConfig.QUALITY.get().scale));
    }

    private static void shard(double x, double y, double z, double vx, double vy, double vz, double gravity, double size, int color, int life, boolean solid) {
        if (!ClientConfig.ENABLED.get() || SHARDS.size() >= ClientConfig.MAX_SHARDS.get()) {
            return;
        }
        SHARDS.add(new Shard(x, y, z, vx, vy, vz, gravity, size, color, life, solid));
    }

    private static void sparks(ClientLevel level, Vec3 c, double radius, int color, int base, double up, double spread) {
        RandomSource random = level.getRandom();
        int amount = count(base);
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * radius;
            double out = spread * (0.3 + random.nextDouble() * 0.7);
            shard(c.x + Math.cos(angle) * dist, c.y + 0.2, c.z + Math.sin(angle) * dist,
                    Math.cos(angle) * out * 0.4, up * (0.5 + random.nextDouble()), Math.sin(angle) * out * 0.4,
                    0.015, 0.09 + random.nextDouble() * 0.08, color, 14 + random.nextInt(10), false);
        }
    }

    private static void debris(ClientLevel level, Vec3 c, double radius, int base, double up) {
        RandomSource random = level.getRandom();
        int amount = count(base);
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * radius;
            double x = c.x + Math.cos(angle) * dist;
            double z = c.z + Math.sin(angle) * dist;
            double y = GroundDraw.ground(level, x, z, c.y);
            double out = 0.06 + random.nextDouble() * 0.16;
            int color = GroundDraw.shade(GroundDraw.groundColor(level, x, y, z), 0.85F + random.nextFloat() * 0.3F);
            shard(x, y + 0.1, z, Math.cos(angle) * out, up * (0.6 + random.nextDouble() * 0.6), Math.sin(angle) * out,
                    0.06, 0.08 + random.nextDouble() * 0.12, color, 30 + random.nextInt(20), true);
        }
    }

    private static void burst(ClientLevel level, Vec3 c, float radius, int kind, int color) {
        if (!ClientConfig.ENABLED.get()) {
            return;
        }
        switch (kind) {
            case VfxPacket.BURST_VENOM -> {
                EFFECTS.add(new Flash(c, radius, VENOM, 0.8F, 12));
                sparks(level, c, radius * 0.7, VENOM, 10 + (int) (radius * 3), 0.25, 0.5);
            }
            case VfxPacket.BURST_SWEEP -> EFFECTS.add(new Sweep(c, radius, WHITE));
            case VfxPacket.BURST_SOUL -> {
                int tint = color;
                if (tint == 0) {
                    tint = 0x5CFFE0;
                }
                EFFECTS.add(new Flash(c, radius, tint, 1.4F, 14));
                sparks(level, c, radius * 0.6, tint, 10 + (int) (radius * 3), 0.4, 0.8);
            }
            default -> {
                float wall = 1.0F;
                if (kind == VfxPacket.BURST_SLAM) {
                    wall = 1.8F;
                }
                EFFECTS.add(new Flash(c, radius, EARTH, wall, 12));
                debris(level, c, radius, 8 + (int) (radius * 3), 0.45);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            if (level == null) {
                EFFECTS.clear();
                SHARDS.clear();
                SHAKES.clear();
                BURROWED.clear();
                AURAS.clear();
            }
            return;
        }
        boolean visuals = ClientConfig.ENABLED.get();
        EFFECTS.removeIf(effect -> !effect.tick(level, visuals));
        SHARDS.removeIf(shard -> !shard.tick(level));
        SHAKES.removeIf(shake -> ++shake[6] >= shake[5]);
        tickBurrows(level, visuals);
        AURAS.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getKey());
            int left = entry.getValue() - 1;
            entry.setValue(left);
            return entity == null || !entity.isAlive() || left <= 0;
        });
    }

    private static void tickBurrows(ClientLevel level, boolean visuals) {
        Iterator<Map.Entry<Integer, Integer>> it = BURROWED.entrySet().iterator();
        RandomSource random = level.getRandom();
        while (it.hasNext()) {
            Map.Entry<Integer, Integer> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            int left = entry.getValue() - 1;
            if (entity == null || !entity.isAlive() || left <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(left);
            if (!visuals) {
                continue;
            }
            double ground = GroundDraw.ground(level, entity.getX(), entity.getZ(), entity.getY());
            if (left % 4 == 0) {
                EFFECTS.add(new Ripple(new Vec3(entity.getX(), ground, entity.getZ()), EARTH));
            }
            if (left % 2 == 0) {
                double ox = (random.nextDouble() - 0.5) * 2.0;
                double oz = (random.nextDouble() - 0.5) * 2.0;
                int color = GroundDraw.groundColor(level, entity.getX() + ox, ground, entity.getZ() + oz);
                shard(entity.getX() + ox, ground + 0.1, entity.getZ() + oz, ox * 0.05, 0.3 + random.nextDouble() * 0.2, oz * 0.05,
                        0.06, 0.1 + random.nextDouble() * 0.08, color, 24, true);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !ClientConfig.ENABLED.get()) {
            return;
        }
        if (EFFECTS.isEmpty() && SHARDS.isEmpty() && AURAS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Camera camera = event.getCamera();
        cameraPos = camera.getPosition();
        LEFT.set(camera.getLeftVector());
        UP.set(camera.getUpVector());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        float partial = event.getPartialTick();
        VertexConsumer solid = buffers.getBuffer(RenderType.debugQuads());
        for (Shard shard : SHARDS) {
            if (shard.solid) {
                shard.render(solid, matrix, partial);
            }
        }
        buffers.endBatch(RenderType.debugQuads());
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        for (Effect effect : EFFECTS) {
            effect.render(level, glow, matrix, partial);
        }
        for (Shard shard : SHARDS) {
            if (!shard.solid) {
                shard.render(glow, matrix, partial);
            }
        }
        for (Map.Entry<Integer, Integer> entry : AURAS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null) {
                renderAura(level, glow, matrix, entity, partial, entry.getValue());
            }
        }
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }

    private static void renderAura(ClientLevel level, VertexConsumer vc, Matrix4f m, Entity entity, float partial, int left) {
        double x = Mth.lerp(partial, entity.xo, entity.getX());
        double z = Mth.lerp(partial, entity.zo, entity.getZ());
        double y = GroundDraw.ground(level, x, z, Mth.lerp(partial, entity.yo, entity.getY()));
        float t = entity.tickCount + partial;
        float alpha = Mth.clamp(left / 20.0F, 0.0F, 1.0F);
        double spin = t * 0.06;
        for (int i = 0; i < 6; i++) {
            double start = spin + i * Mth.TWO_PI / 6.0;
            GroundDraw.band(level, vc, m, x, y, z, 2.0, 2.5, start, 0.55, RAGE, 0.55F * alpha);
        }
        GroundDraw.ring(level, vc, m, x, y + 0.01, z, 2.55, 2.7, RAGE, 0.4F * alpha);
        float pulse = 0.6F + 0.4F * Mth.sin(t * 0.2F);
        GroundDraw.cylinder(vc, m, x, y, z, 1.7, 2.6, 20, -spin, RAGE, 0.22F * alpha * pulse, 0.0F);
    }

    private static void groundLine(ClientLevel level, VertexConsumer vc, Matrix4f m, double x0, double z0, double x1, double z1, double ref,
                                   double width, int color, float alpha) {
        double dx = x1 - x0;
        double dz = z1 - z0;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.05) {
            return;
        }
        double px = -dz / length * width * 0.5;
        double pz = dx / length * width * 0.5;
        int steps = Math.max(1, (int) Math.ceil(length / 0.8));
        double pxp = x0;
        double pzp = z0;
        double pyp = GroundDraw.ground(level, x0, z0, ref);
        for (int i = 1; i <= steps; i++) {
            double x = x0 + dx * i / steps;
            double z = z0 + dz * i / steps;
            double y = GroundDraw.ground(level, x, z, ref);
            GroundDraw.quad(vc, m, pxp - px, pyp, pzp - pz, pxp + px, pyp, pzp + pz, x + px, y, z + pz, x - px, y, z - pz, color, alpha);
            pxp = x;
            pzp = z;
            pyp = y;
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (SHAKES.isEmpty() || !ClientConfig.SCREEN_SHAKE.get()) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        double strength = 0.0;
        for (float[] shake : SHAKES) {
            double dx = cam.x - shake[0];
            double dy = cam.y - shake[1];
            double dz = cam.z - shake[2];
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double falloff = Math.max(0.0, 1.0 - distance / shake[4]);
            double fade = 1.0 - shake[6] / shake[5];
            strength = Math.max(strength, shake[3] * falloff * fade);
        }
        if (strength <= 0.0) {
            return;
        }
        strength *= ClientConfig.SHAKE_STRENGTH.get() * 2.2;
        double time = (Minecraft.getInstance().level.getGameTime() + event.getPartialTick()) * 1.7;
        event.setYaw((float) (event.getYaw() + Math.sin(time * 3.1) * strength));
        event.setPitch((float) (event.getPitch() + Math.cos(time * 2.7) * strength));
        event.setRoll((float) (event.getRoll() + Math.sin(time * 4.3) * strength * 0.5));
    }

    private static final class Shard {
        private double x;
        private double y;
        private double z;
        private double px;
        private double py;
        private double pz;
        private double vx;
        private double vy;
        private double vz;
        private final double gravity;
        private final double size;
        private final int color;
        private final int life;
        private final boolean solid;
        private final double spinSpeed;
        private double spin;
        private int age;
        private boolean resting;

        Shard(double x, double y, double z, double vx, double vy, double vz, double gravity, double size, int color, int life, boolean solid) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.px = x;
            this.py = y;
            this.pz = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.gravity = gravity;
            this.size = size;
            this.color = color;
            this.life = life;
            this.solid = solid;
            this.spinSpeed = (vx + vz) * 3.0 + 0.2;
        }

        boolean tick(ClientLevel level) {
            this.px = this.x;
            this.py = this.y;
            this.pz = this.z;
            this.age++;
            if (this.resting) {
                return this.age < this.life;
            }
            this.vy -= this.gravity;
            double drag = 0.98;
            if (!this.solid) {
                drag = 0.9;
            }
            this.vx *= drag;
            this.vy *= drag;
            this.vz *= drag;
            double nx = this.x + this.vx;
            double ny = this.y + this.vy;
            double nz = this.z + this.vz;
            if (this.solid && this.vy < 0.0) {
                BlockPos pos = BlockPos.containing(nx, ny - this.size, nz);
                if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                    this.resting = true;
                    return this.age < this.life;
                }
            }
            this.x = nx;
            this.y = ny;
            this.z = nz;
            this.spin += this.spinSpeed;
            return this.age < this.life;
        }

        void render(VertexConsumer vc, Matrix4f m, float partial) {
            double rx = Mth.lerp(partial, this.px, this.x);
            double ry = Mth.lerp(partial, this.py, this.y);
            double rz = Mth.lerp(partial, this.pz, this.z);
            float t = (this.age + partial) / this.life;
            float alpha = Mth.clamp((1.0F - t) * 3.0F, 0.0F, 1.0F);
            if (this.solid) {
                double shrink = this.size * Mth.clamp((1.0F - t) * 2.5F, 0.0F, 1.0F);
                GroundDraw.cube(vc, m, rx, ry, rz, shrink, this.spin, this.color, 0.95F);
                return;
            }
            GroundDraw.billboard(vc, m, LEFT, UP, rx, ry, rz, this.size * 2.2, this.color, 0.25F * alpha);
            GroundDraw.billboard(vc, m, LEFT, UP, rx, ry, rz, this.size, WHITE, 0.7F * alpha);
        }
    }

    private abstract static class Effect {
        protected int age;

        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            return this.alive();
        }

        abstract boolean alive();

        abstract void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial);

        static float fadeIn(float t, float length) {
            return Mth.clamp(t / length, 0.0F, 1.0F);
        }
    }

    private static final class LineWarning extends Effect {
        private final Vec3 a;
        private final Vec3 b;
        private final float width;
        private final int duration;
        private final int color;

        LineWarning(Vec3 a, Vec3 b, float width, int duration, int color) {
            this.a = a;
            this.b = b;
            this.width = width;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < this.duration + 6;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float progress = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float alpha = fadeIn(t, 4.0F);
            if (t > this.duration) {
                alpha = Math.max(0.0F, 1.0F - (t - this.duration) / 6.0F);
            }
            Vec3 dir = this.b.subtract(this.a);
            double length = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            if (length < 0.1) {
                return;
            }
            double nx = dir.x / length;
            double nz = dir.z / length;
            double px = -nz * this.width * 0.5;
            double pz = nx * this.width * 0.5;
            double edge = 0.22;
            int steps = Math.max(1, (int) Math.ceil(length));
            for (int i = 0; i < steps; i++) {
                double s0 = length * i / steps;
                double s1 = length * (i + 1) / steps;
                double x0 = this.a.x + nx * s0;
                double z0 = this.a.z + nz * s0;
                double x1 = this.a.x + nx * s1;
                double z1 = this.a.z + nz * s1;
                double y0 = GroundDraw.ground(level, x0, z0, this.a.y);
                double y1 = GroundDraw.ground(level, x1, z1, this.a.y);
                GroundDraw.quad(vc, m, x0 - px, y0, z0 - pz, x0 + px, y0, z0 + pz, x1 + px, y1, z1 + pz, x1 - px, y1, z1 - pz, this.color, 0.16F * alpha);
                double ex = px / (this.width * 0.5) * edge;
                double ez = pz / (this.width * 0.5) * edge;
                GroundDraw.quad(vc, m, x0 + px - ex, y0, z0 + pz - ez, x0 + px, y0, z0 + pz, x1 + px, y1, z1 + pz, x1 + px - ex, y1, z1 + pz - ez, this.color, 0.75F * alpha);
                GroundDraw.quad(vc, m, x0 - px, y0, z0 - pz, x0 - px + ex, y0, z0 - pz + ez, x1 - px + ex, y1, z1 - pz + ez, x1 - px, y1, z1 - pz, this.color, 0.75F * alpha);
                if (s1 <= length * progress) {
                    GroundDraw.quad(vc, m, x0 - px * 0.7, y0 + 0.01, z0 - pz * 0.7, x0 + px * 0.7, y0 + 0.01, z0 + pz * 0.7, x1 + px * 0.7, y1 + 0.01, z1 + pz * 0.7, x1 - px * 0.7, y1 + 0.01, z1 - pz * 0.7, this.color, 0.28F * alpha);
                }
            }
        }
    }

    private static final class CircleWarning extends Effect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final int color;

        CircleWarning(Vec3 c, float radius, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < this.duration + 6;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float progress = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float alpha = fadeIn(t, 4.0F);
            if (t > this.duration) {
                alpha = Math.max(0.0F, 1.0F - (t - this.duration) / 6.0F);
            }
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, 0.0, this.radius, this.color, 0.12F * alpha);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.01, this.c.z, 0.0, this.radius * progress, this.color, 0.22F * alpha);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.02, this.c.z, this.radius - 0.22, this.radius, this.color, 0.8F * alpha);
        }
    }

    private static final class Ring extends Effect {
        private final Vec3 c;
        private final float speed;
        private final float max;
        private final float thickness;
        private final int delay;
        private final int color;

        Ring(Vec3 c, float speed, float max, float thickness, int delay, int color) {
            this.c = c;
            this.speed = speed;
            this.max = max;
            this.thickness = thickness;
            this.delay = delay;
            this.color = color;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            int active = this.age - this.delay;
            if (visuals && active > 0 && active % 2 == 0) {
                RandomSource random = level.getRandom();
                double radius = active * this.speed;
                double angle = random.nextDouble() * Mth.TWO_PI;
                double x = this.c.x + Math.cos(angle) * radius;
                double z = this.c.z + Math.sin(angle) * radius;
                double y = GroundDraw.ground(level, x, z, this.c.y);
                if (this.color == EARTH) {
                    shard(x, y + 0.1, z, Math.cos(angle) * 0.08, 0.3, Math.sin(angle) * 0.08, 0.06, 0.12, GroundDraw.groundColor(level, x, y, z), 26, true);
                } else {
                    shard(x, y + 0.2, z, Math.cos(angle) * 0.1, 0.18, Math.sin(angle) * 0.1, 0.01, 0.12, this.color, 14, false);
                }
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return (this.age - this.delay) * this.speed < this.max + this.thickness;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float active = this.age - this.delay + partial;
            if (active <= 0.0F) {
                return;
            }
            double radius = active * this.speed;
            float alpha = Math.max(0.0F, 1.0F - (float) (radius / (this.max + this.thickness)));
            alpha = 0.35F + alpha * 0.65F;
            double inner = Math.max(0.0, radius - this.thickness * 0.5);
            double outer = radius + this.thickness * 0.5;
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.03, this.c.z, inner, outer, this.color, 0.55F * alpha);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.04, this.c.z, outer - 0.15, outer, WHITE, 0.5F * alpha);
            GroundDraw.cylinder(vc, m, this.c.x, this.c.y, this.c.z, outer, 0.75, 48, 0.0, this.color, 0.45F * alpha, 0.0F);
        }
    }

    private static final class Ripple extends Effect {
        private static final int LIFE = 12;
        private final Vec3 c;
        private final int color;

        Ripple(Vec3 c, int color) {
            this.c = c;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < LIFE;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = (this.age + partial) / LIFE;
            double radius = 0.6 + t * 1.8;
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, radius - 0.25, radius, this.color, 0.45F * (1.0F - t));
        }
    }

    private static final class Flash extends Effect {
        private final Vec3 c;
        private final float radius;
        private final int color;
        private final float wallHeight;
        private final int life;

        Flash(Vec3 c, float radius, int color, float wallHeight, int life) {
            this.c = c;
            this.radius = radius;
            this.color = color;
            this.wallHeight = wallHeight;
            this.life = life;
        }

        @Override
        boolean alive() {
            return this.age < this.life;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = (this.age + partial) / this.life;
            float ease = 1.0F - (1.0F - t) * (1.0F - t);
            float fade = 1.0F - t;
            double r = Math.max(0.3, this.radius * (0.35 + ease * 0.85));
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, 0.0, r * 0.8, this.color, 0.3F * fade * fade);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.01, this.c.z, r - 0.35, r, WHITE, 0.6F * fade);
            GroundDraw.cylinder(vc, m, this.c.x, this.c.y, this.c.z, r, this.wallHeight * (0.5 + fade * 0.5), 40, 0.0, this.color, 0.55F * fade, 0.0F);
        }
    }

    private static final class Sweep extends Effect {
        private static final int LIFE = 9;
        private final Vec3 c;
        private final float radius;
        private final int color;

        Sweep(Vec3 c, float radius, int color) {
            this.c = c;
            this.radius = radius;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < LIFE;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = (this.age + partial) / LIFE;
            float fade = 1.0F - t;
            double start = t * Mth.TWO_PI * 0.8;
            double y = this.c.y + 0.8;
            GroundDraw.flatBand(vc, m, this.c.x, y, this.c.z, this.radius * 0.45, this.radius, start, Math.PI * 1.4, this.color, 0.0F, 0.65F * fade);
            GroundDraw.flatBand(vc, m, this.c.x, y + 0.01, this.c.z, this.radius - 0.2, this.radius, start, Math.PI * 1.4, WHITE, 0.8F * fade, 0.8F * fade);
        }
    }

    private static final class Glob extends Effect {
        private final Vec3 origin;
        private final Vec3 velocity;
        private final double gravity;
        private final int flight;

        Glob(Vec3 origin, Vec3 velocity, double gravity, int flight) {
            this.origin = origin;
            this.velocity = velocity;
            this.gravity = gravity;
            this.flight = flight;
        }

        private Vec3 at(double t) {
            return this.origin.add(this.velocity.x * t, this.velocity.y * t - 0.5 * this.gravity * t * t, this.velocity.z * t);
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            if (visuals && this.age == this.flight) {
                burst(level, this.at(this.flight), 2.6F, VfxPacket.BURST_VENOM, 0);
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return this.age <= this.flight;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            Vec3 p = this.at(t);
            GroundDraw.billboard(vc, m, LEFT, UP, p.x, p.y, p.z, 0.8, VENOM, 0.3F);
            GroundDraw.billboard(vc, m, LEFT, UP, p.x, p.y, p.z, 0.42, VENOM, 0.85F);
            GroundDraw.billboard(vc, m, LEFT, UP, p.x, p.y, p.z, 0.18, WHITE, 0.9F);
            for (int i = 1; i <= 5; i++) {
                double back = t - i * 0.7;
                if (back < 0.0) {
                    break;
                }
                Vec3 q = this.at(back);
                float k = 1.0F - i / 6.0F;
                GroundDraw.billboard(vc, m, LEFT, UP, q.x, q.y, q.z, 0.34 * k, VENOM, 0.5F * k);
            }
        }
    }

    private static final class Pool extends Effect {
        private final Vec3 c;
        private final float radius;
        private final int duration;

        Pool(Vec3 c, float radius, int duration) {
            this.c = c;
            this.radius = radius;
            this.duration = duration;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            if (visuals && this.age % 6 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                double dist = Math.sqrt(random.nextDouble()) * this.radius;
                double x = this.c.x + Math.cos(angle) * dist;
                double z = this.c.z + Math.sin(angle) * dist;
                double y = GroundDraw.ground(level, x, z, this.c.y);
                shard(x, y + 0.05, z, 0.0, 0.05, 0.0, -0.002, 0.1, VENOM, 20, false);
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float alpha = Math.min(fadeIn(t, 5.0F), Mth.clamp((this.duration - t) / 15.0F, 0.0F, 1.0F));
            float pulse = 0.8F + 0.2F * Mth.sin(t * 0.25F);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, 0.0, this.radius, 0x4FCF2A, 0.3F * alpha * pulse);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.01, this.c.z, this.radius - 0.2, this.radius, 0x9CFF6A, 0.6F * alpha);
            float wave = (t % 20.0F) / 20.0F;
            double r = 0.3 + wave * (this.radius - 0.4);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.02, this.c.z, r - 0.12, r, 0xC8FF9A, 0.45F * alpha * (1.0F - wave));
        }
    }

    private static final class Cone extends Effect {
        private final int entityId;
        private final float length;
        private final float half;
        private final int windup;
        private final int duration;
        private final int color;

        Cone(int entityId, float length, float half, int windup, int duration, int color) {
            this.entityId = entityId;
            this.length = length;
            this.half = half;
            this.windup = windup;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            Entity entity = level.getEntity(this.entityId);
            if (entity == null) {
                return false;
            }
            if (visuals && this.age > this.windup && this.duration - this.windup > 10) {
                RandomSource random = level.getRandom();
                Vec3 eye = entity.getEyePosition();
                float angle = entity.getYRot() + (random.nextFloat() * 2.0F - 1.0F) * this.half;
                Vec3 dir = Vec3.directionFromRotation(12.0F + random.nextFloat() * 8.0F, angle);
                double speed = 0.45 + random.nextDouble() * 0.3;
                shard(eye.x + dir.x, eye.y - 0.4, eye.z + dir.z, dir.x * speed, dir.y * speed, dir.z * speed, 0.0, 0.16, this.color, 16, false);
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            Entity entity = level.getEntity(this.entityId);
            if (entity == null) {
                return;
            }
            float t = this.age + partial;
            float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
            double start = Math.toRadians(yaw + 90.0F - this.half);
            double sweep = Math.toRadians(this.half * 2.0F);
            double x = Mth.lerp(partial, entity.xo, entity.getX());
            double z = Mth.lerp(partial, entity.zo, entity.getZ());
            double y = GroundDraw.ground(level, x, z, entity.getY());
            float alpha = Math.min(fadeIn(t, 4.0F), Mth.clamp((this.duration - t) / 6.0F, 0.0F, 1.0F));
            if (t < this.windup) {
                float progress = t / this.windup;
                GroundDraw.band(level, vc, m, x, y, z, 0.0, this.length, start, sweep, this.color, 0.12F * alpha);
                GroundDraw.band(level, vc, m, x, y + 0.01, z, 0.0, this.length * progress, start, sweep, this.color, 0.18F * alpha);
                GroundDraw.band(level, vc, m, x, y + 0.02, z, this.length - 0.25, this.length, start, sweep, this.color, 0.8F * alpha);
                return;
            }
            GroundDraw.band(level, vc, m, x, y, z, 0.0, this.length, start, sweep, this.color, 0.3F * alpha);
            GroundDraw.band(level, vc, m, x, y + 0.01, z, this.length - 0.25, this.length, start, sweep, this.color, 0.6F * alpha);
            for (int k = 0; k < 3; k++) {
                float phase = ((t - this.windup) * 0.06F + k / 3.0F) % 1.0F;
                double r = 0.8 + phase * (this.length - 0.8);
                double height = y + 0.5 + phase * 1.2;
                GroundDraw.flatBand(vc, m, x, height, z, Math.max(0.0, r - 0.6), r, start, sweep, this.color, 0.0F, 0.45F * alpha * (1.0F - phase));
            }
        }
    }

    private static final class Beam extends Effect {
        private final Vec3 a;
        private final Vec3 b;
        private final float width;
        private final int windup;
        private final int duration;
        private final int color;

        Beam(Vec3 a, Vec3 b, float width, int windup, int duration, int color) {
            this.a = a;
            this.b = b;
            this.width = width;
            this.windup = windup;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            if (visuals && this.age == this.windup && this.windup > 0) {
                sparks(level, this.b, 0.4, this.color, 6, 0.2, 0.9);
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            if (t < this.windup) {
                float progress = t / this.windup;
                float flicker = 0.7F + 0.3F * Mth.sin(t * 1.7F);
                GroundDraw.ribbon(vc, m, cameraPos, this.a, this.b, 0.06 + progress * 0.1, this.color, 0.75F * flicker, 0.25F * flicker);
                GroundDraw.ribbon(vc, m, cameraPos, this.a, this.b, this.width * (1.0 - progress * 0.6), this.color, 0.08F * progress, 0.0F);
                GroundDraw.billboard(vc, m, LEFT, UP, this.a.x, this.a.y, this.a.z, 0.25 + progress * 0.35, this.color, 0.7F * progress);
                return;
            }
            float fade = 1.0F;
            int active = this.duration - this.windup;
            if (active > 0) {
                fade = Mth.clamp(1.0F - (t - this.windup) / active, 0.0F, 1.0F);
            }
            double w = this.width * (0.5 + fade * 0.5);
            GroundDraw.ribbon(vc, m, cameraPos, this.a, this.b, w * 1.8, this.color, 0.25F * fade, 0.2F * fade);
            GroundDraw.ribbon(vc, m, cameraPos, this.a, this.b, w, this.color, 0.7F * fade, 0.6F * fade);
            GroundDraw.ribbon(vc, m, cameraPos, this.a, this.b, w * 0.3, WHITE, 0.9F * fade, 0.8F * fade);
            GroundDraw.billboard(vc, m, LEFT, UP, this.a.x, this.a.y, this.a.z, w * 1.3, WHITE, 0.6F * fade);
            GroundDraw.billboard(vc, m, LEFT, UP, this.b.x, this.b.y, this.b.z, w * 1.6, this.color, 0.6F * fade);
        }
    }

    private static final class Pillar extends Effect {
        private final Vec3 c;
        private final float radius;
        private final float height;
        private final int duration;
        private final int color;

        Pillar(Vec3 c, float radius, float height, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.height = height;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float grow = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
            float fade = Mth.clamp(1.0F - t / this.duration, 0.0F, 1.0F);
            double r = this.radius * (0.4 + grow * 0.6) * (0.6 + fade * 0.4);
            double h = this.height * grow;
            double twist = t * 0.12;
            GroundDraw.cylinder(vc, m, this.c.x, this.c.y, this.c.z, r, h, 20, twist, this.color, 0.55F * fade, 0.0F);
            GroundDraw.cylinder(vc, m, this.c.x, this.c.y, this.c.z, r * 0.45, h * 1.1, 12, -twist, WHITE, 0.8F * fade, 0.05F * fade);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, 0.0, r * 1.4, this.color, 0.35F * fade);
        }
    }

    private static final class Tether extends Effect {
        private final int from;
        private final int to;
        private final int duration;
        private final int color;
        private boolean cut;

        Tether(int from, int to, int duration, int color) {
            this.from = from;
            this.to = to;
            this.duration = duration;
            this.color = color;
        }

        void cut() {
            this.cut = true;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            if (level.getEntity(this.from) == null || level.getEntity(this.to) == null) {
                return false;
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return !this.cut && this.age < this.duration;
        }

        private static Vec3 point(Entity entity, float partial) {
            double x = Mth.lerp(partial, entity.xo, entity.getX());
            double y = Mth.lerp(partial, entity.yo, entity.getY()) + entity.getBbHeight() * 0.6;
            double z = Mth.lerp(partial, entity.zo, entity.getZ());
            return new Vec3(x, y, z);
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            Entity a = level.getEntity(this.from);
            Entity b = level.getEntity(this.to);
            if (a == null || b == null || this.cut) {
                return;
            }
            float t = this.age + partial;
            float alpha = Math.min(fadeIn(t, 4.0F), Mth.clamp((this.duration - t) / 5.0F, 0.0F, 1.0F));
            Vec3 start = point(a, partial);
            Vec3 end = point(b, partial);
            Vec3 dir = end.subtract(start);
            Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
            if (side.lengthSqr() < 1.0E-6) {
                side = new Vec3(1.0, 0.0, 0.0);
            }
            side = side.normalize();
            int segments = 14;
            Vec3 previous = start;
            for (int i = 1; i <= segments; i++) {
                double s = i / (double) segments;
                double wave = Math.sin(s * Math.PI) * Math.sin(t * 0.5 + s * 9.0) * 0.35;
                Vec3 next = start.add(dir.scale(s)).add(side.scale(wave)).add(0.0, Math.cos(t * 0.4 + s * 7.0) * wave * 0.6, 0.0);
                GroundDraw.ribbon(vc, m, cameraPos, previous, next, 0.28, this.color, 0.45F * alpha, 0.45F * alpha);
                GroundDraw.ribbon(vc, m, cameraPos, previous, next, 0.08, WHITE, 0.8F * alpha, 0.8F * alpha);
                previous = next;
            }
            for (int k = 0; k < 3; k++) {
                double s = 1.0 - ((t * 0.05 + k / 3.0) % 1.0);
                Vec3 bead = start.add(dir.scale(s));
                GroundDraw.billboard(vc, m, LEFT, UP, bead.x, bead.y, bead.z, 0.22, this.color, 0.8F * alpha);
            }
        }
    }

    private static final class Sigil extends Effect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final int color;

        Sigil(Vec3 c, float radius, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < this.duration + 5;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float progress = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float alpha = fadeIn(t, 4.0F);
            if (t > this.duration) {
                alpha = Math.max(0.0F, 1.0F - (t - this.duration) / 5.0F);
            }
            double r = this.radius;
            double inner = r * 0.62;
            double spin = t * 0.05;
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, r - 0.16, r, this.color, 0.85F * alpha);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, inner - 0.1, inner, this.color, 0.7F * alpha);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.01, this.c.z, 0.0, r * progress, this.color, 0.2F * alpha);
            for (int i = 0; i < 12; i++) {
                double angle = spin + i * Mth.TWO_PI / 12.0;
                GroundDraw.band(level, vc, m, this.c.x, this.c.y + 0.01, this.c.z, inner + 0.12, r - 0.24, angle, 0.06, this.color, 0.6F * alpha);
            }
            for (int tri = 0; tri < 2; tri++) {
                double base = -spin * 1.5 + tri * Math.PI;
                for (int i = 0; i < 3; i++) {
                    double a0 = base + i * Mth.TWO_PI / 3.0;
                    double a1 = base + (i + 1) * Mth.TWO_PI / 3.0;
                    groundLine(level, vc, m, this.c.x + Math.cos(a0) * inner, this.c.z + Math.sin(a0) * inner,
                            this.c.x + Math.cos(a1) * inner, this.c.z + Math.sin(a1) * inner, this.c.y, 0.12, this.color, 0.75F * alpha);
                }
            }
            if (progress > 0.75F) {
                float charge = (progress - 0.75F) * 4.0F;
                GroundDraw.cylinder(vc, m, this.c.x, this.c.y, this.c.z, r, 0.4 + charge * 1.2, 32, spin, this.color, 0.35F * charge * alpha, 0.0F);
            }
        }
    }

    private static final class Vortex extends Effect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final int color;

        Vortex(Vec3 c, float radius, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            if (visuals) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = this.radius * (0.7 + random.nextDouble() * 0.3);
                double x = this.c.x + Math.cos(angle) * r;
                double z = this.c.z + Math.sin(angle) * r;
                double speed = r / 18.0;
                double tangent = speed * 0.6;
                shard(x, this.c.y + 0.3 + random.nextDouble(), z,
                        -Math.cos(angle) * speed - Math.sin(angle) * tangent, 0.02, -Math.sin(angle) * speed + Math.cos(angle) * tangent,
                        0.0, 0.12, this.color, 18, false);
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float alpha = Math.min(fadeIn(t, 6.0F), Mth.clamp((this.duration - t) / 6.0F, 0.0F, 1.0F));
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, this.radius - 0.2, this.radius, this.color, 0.6F * alpha);
            for (int k = 0; k < 3; k++) {
                float phase = 1.0F - ((t * 0.025F + k / 3.0F) % 1.0F);
                double r = this.radius * phase;
                GroundDraw.ring(level, vc, m, this.c.x, this.c.y + 0.01, this.c.z, Math.max(0.0, r - 0.35), r, this.color, 0.4F * alpha * (1.0F - phase * 0.5F));
            }
            double spin = t * 0.12;
            for (int arm = 0; arm < 4; arm++) {
                for (int s = 1; s <= 8; s++) {
                    double r = this.radius * s / 8.0;
                    double angle = spin + arm * Math.PI * 0.5 + s * 0.32;
                    GroundDraw.band(level, vc, m, this.c.x, this.c.y + 0.02, this.c.z, r - this.radius / 8.0, r, angle, 0.35, this.color, 0.3F * alpha);
                }
            }
        }
    }

    private static final class Slash extends Effect {
        private static final int LIFE = 9;
        private final Vec3 c;
        private final float yaw;
        private final float radius;
        private final float sweep;
        private final int color;

        Slash(Vec3 c, float yaw, float radius, float sweep, int color) {
            this.c = c;
            this.yaw = yaw;
            this.radius = radius;
            this.sweep = sweep;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < LIFE;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float reveal = Mth.clamp(t / 2.5F, 0.0F, 1.0F);
            float fade = Mth.clamp(1.0F - (t - 2.0F) / (LIFE - 2.0F), 0.0F, 1.0F);
            double start = Math.toRadians(this.yaw + 90.0F - this.sweep * 0.5F);
            double sweepRad = Math.toRadians(this.sweep) * reveal;
            GroundDraw.flatBand(vc, m, this.c.x, this.c.y, this.c.z, this.radius * 0.3, this.radius, start, sweepRad, this.color, 0.0F, 0.7F * fade);
            GroundDraw.flatBand(vc, m, this.c.x, this.c.y + 0.01, this.c.z, this.radius - 0.18, this.radius, start, sweepRad, WHITE, 0.9F * fade, 0.9F * fade);
            GroundDraw.flatBand(vc, m, this.c.x, this.c.y - 0.25, this.c.z, this.radius * 0.5, this.radius * 0.95, start, sweepRad, this.color, 0.0F, 0.35F * fade);
        }
    }

    private static final class Orb extends Effect {
        private final int entityId;
        private final float height;
        private final float size;
        private final float ring;
        private final int duration;
        private final int color;

        Orb(int entityId, float height, float size, float ring, int duration, int color) {
            this.entityId = entityId;
            this.height = height;
            this.size = size;
            this.ring = ring;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean tick(ClientLevel level, boolean visuals) {
            this.age++;
            if (level.getEntity(this.entityId) == null) {
                return false;
            }
            return this.alive();
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            Entity entity = level.getEntity(this.entityId);
            if (entity == null) {
                return;
            }
            float t = this.age + partial;
            float progress = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float alpha = Math.min(fadeIn(t, 4.0F), Mth.clamp((this.duration - t) / 3.0F, 0.0F, 1.0F));
            double x = Mth.lerp(partial, entity.xo, entity.getX());
            double y = Mth.lerp(partial, entity.yo, entity.getY());
            double z = Mth.lerp(partial, entity.zo, entity.getZ());
            double oy = y + this.height;
            float pulse = 0.85F + 0.15F * Mth.sin(t * 0.6F);
            double s = this.size * (0.4 + progress * 0.6) * pulse;
            GroundDraw.billboard(vc, m, LEFT, UP, x, oy, z, s * 2.4, this.color, 0.2F * alpha);
            GroundDraw.billboard(vc, m, LEFT, UP, x, oy, z, s, this.color, 0.7F * alpha);
            GroundDraw.billboard(vc, m, LEFT, UP, x, oy, z, s * 0.45, WHITE, 0.9F * alpha);
            for (int k = 0; k < 3; k++) {
                double angle = t * 0.25 + k * Mth.TWO_PI / 3.0;
                double orbit = s * 1.6;
                GroundDraw.billboard(vc, m, LEFT, UP, x + Math.cos(angle) * orbit, oy + Math.sin(angle * 1.3) * s * 0.5, z + Math.sin(angle) * orbit,
                        s * 0.22, this.color, 0.8F * alpha);
            }
            if (this.ring > 0.0F) {
                double gy = GroundDraw.ground(level, x, z, y);
                GroundDraw.ring(level, vc, m, x, gy, z, this.ring - 0.2, this.ring, this.color, 0.8F * alpha);
                GroundDraw.ring(level, vc, m, x, gy + 0.01, z, 0.0, this.ring * progress, this.color, 0.22F * alpha);
            }
        }
    }

    private static final class Wall extends Effect {
        private final Vec3 c;
        private final float radius;
        private final float height;
        private final int duration;
        private final int color;

        Wall(Vec3 c, float radius, float height, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.height = height;
            this.duration = duration;
            this.color = color;
        }

        @Override
        boolean alive() {
            return this.age < this.duration;
        }

        @Override
        void render(ClientLevel level, VertexConsumer vc, Matrix4f m, float partial) {
            float t = this.age + partial;
            float rise = Mth.clamp(t / 6.0F, 0.0F, 1.0F);
            float alpha = Math.min(rise, Mth.clamp((this.duration - t) / 10.0F, 0.0F, 1.0F));
            double h = this.height * rise;
            double spin = t * 0.02;
            GroundDraw.cylinder(vc, m, this.c.x, this.c.y, this.c.z, this.radius, h, 48, spin, this.color, 0.3F * alpha, 0.02F * alpha);
            GroundDraw.ring(level, vc, m, this.c.x, this.c.y, this.c.z, this.radius - 0.3, this.radius + 0.1, this.color, 0.7F * alpha);
            GroundDraw.flatBand(vc, m, this.c.x, this.c.y + h, this.c.z, this.radius - 0.12, this.radius, 0.0, Mth.TWO_PI, this.color, 0.5F * alpha, 0.5F * alpha);
            int bars = 18;
            for (int i = 0; i < bars; i++) {
                double angle = spin + i * Mth.TWO_PI / bars;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                double bx = this.c.x + cos * this.radius;
                double bz = this.c.z + sin * this.radius;
                double tx = -sin * 0.09;
                double tz = cos * 0.09;
                float flicker = 0.75F + 0.25F * Mth.sin(t * 0.4F + i);
                GroundDraw.gradient(vc, m, bx - tx, this.c.y, bz - tz, bx + tx, this.c.y, bz + tz, bx + tx, this.c.y + h, bz + tz, bx - tx, this.c.y + h, bz - tz,
                        WHITE, 0.75F * alpha * flicker, 0.1F * alpha);
            }
        }
    }
}
