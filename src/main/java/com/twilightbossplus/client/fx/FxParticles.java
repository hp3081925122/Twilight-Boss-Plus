package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FxParticles {
    public static final int SPRITE = 0;
    public static final int STREAK = 1;
    public static final int FLAT = 2;

    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static final Particle DISCARDED = new Particle();

    private FxParticles() {
    }

    public static Particle glow(ResourceLocation texture, double x, double y, double z, double vx, double vy, double vz, int life, float size) {
        return spawn(FxRenderTypes.glow(texture), x, y, z, vx, vy, vz, life, size);
    }

    public static Particle soft(ResourceLocation texture, double x, double y, double z, double vx, double vy, double vz, int life, float size) {
        return spawn(FxRenderTypes.soft(texture), x, y, z, vx, vy, vz, life, size);
    }

    public static Particle add(ResourceLocation texture, double x, double y, double z, double vx, double vy, double vz, int life, float size) {
        return spawn(FxRenderTypes.add(texture), x, y, z, vx, vy, vz, life, size);
    }

    public static Particle alpha(ResourceLocation texture, double x, double y, double z, double vx, double vy, double vz, int life, float size) {
        return spawn(FxRenderTypes.alpha(texture), x, y, z, vx, vy, vz, life, size);
    }

    private static Particle spawn(RenderType type, double x, double y, double z, double vx, double vy, double vz, int life, float size) {
        if (!ClientConfig.VFX_ENABLED.get() || PARTICLES.size() >= ClientConfig.PARTICLE_LIMIT.get()) {
            return DISCARDED;
        }
        Particle particle = new Particle();
        particle.type = type;
        particle.x = x;
        particle.y = y;
        particle.z = z;
        particle.px = x;
        particle.py = y;
        particle.pz = z;
        particle.vx = vx;
        particle.vy = vy;
        particle.vz = vz;
        particle.life = Math.max(1, life);
        particle.size0 = size;
        particle.size1 = size;
        PARTICLES.add(particle);
        return particle;
    }

    public static int count() {
        return PARTICLES.size();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            PARTICLES.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        Iterator<Particle> iterator = PARTICLES.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.px = p.x;
            p.py = p.y;
            p.pz = p.z;
            p.prevRoll = p.roll;
            p.x += p.vx;
            p.y += p.vy;
            p.z += p.vz;
            p.vx *= p.drag;
            p.vy = p.vy * p.drag - p.gravity;
            p.vz *= p.drag;
            p.roll += p.spin;
            if (!Double.isNaN(p.floor) && p.y < p.floor) {
                p.y = p.floor;
                p.vy = -p.vy * p.bounce;
                p.vx *= 0.6D;
                p.vz *= 0.6D;
            }
            if (++p.age >= p.life) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PARTICLES.clear();
        }
    }

    static void renderAll(PoseStack.Pose pose, float partialTick) {
        if (PARTICLES.isEmpty()) {
            return;
        }
        for (Particle p : PARTICLES) {
            if (p.flowing) {
                float t = Mth.clamp((p.age + partialTick) / p.life, 0.0F, 1.0F);
                float dissolve = 0.0F;
                if (p.dissolveFrom < 1.0F && t > p.dissolveFrom) {
                    dissolve = (t - p.dissolveFrom) / (1.0F - p.dissolveFrom) * 0.9F;
                }
                FxDraw.params(dissolve, 0.12F, p.scrollU, p.scrollV, p.hot, p.noise, p.flow);
            } else {
                FxDraw.resetParams();
            }
            renderParticle(FxBuffers.get(p.type), pose, p, partialTick);
        }
        FxDraw.resetParams();
    }

    private static void renderParticle(VertexConsumer consumer, PoseStack.Pose pose, Particle p, float partialTick) {
        float t = Mth.clamp((p.age + partialTick) / p.life, 0.0F, 1.0F);
        float fadeIn = 1.0F;
        if (p.fadeIn > 0.0F && t < p.fadeIn) {
            fadeIn = t / p.fadeIn;
        }
        float fadeOut = 1.0F - (float) Math.pow(t, p.fadePower);
        float alpha = p.alpha * fadeIn * fadeOut;
        if (p.flicker > 0.0F) {
            alpha *= 1.0F - p.flicker * (0.5F + 0.5F * Mth.sin((p.age + partialTick) * 1.7F + p.seed));
        }
        if (alpha <= 0.004F) {
            return;
        }
        float size = Mth.lerp(t, p.size0, p.size1);
        float r = Mth.lerp(t, p.r0, p.r1);
        float g = Mth.lerp(t, p.g0, p.g1);
        float b = Mth.lerp(t, p.b0, p.b1);
        double x = Mth.lerp(partialTick, p.px, p.x);
        double y = Mth.lerp(partialTick, p.py, p.y);
        double z = Mth.lerp(partialTick, p.pz, p.z);
        float roll = Mth.lerp(partialTick, p.prevRoll, p.roll);
        if (p.mode == STREAK) {
            double speed = Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz);
            if (speed < 1.0E-4D) {
                FxDraw.sprite(consumer, pose, x, y, z, size, roll, r, g, b, alpha);
                return;
            }
            double length = speed * p.streak + size * 0.5D;
            Vec3 head = new Vec3(x, y, z);
            Vec3 tail = head.subtract(p.vx / speed * length, p.vy / speed * length, p.vz / speed * length);
            FxDraw.beam(consumer, pose, tail, head, size * 0.5F, size * 0.5F, 0.0F, 1.0F, r, g, b, alpha, alpha);
        } else if (p.mode == FLAT) {
            FxDraw.flat(consumer, pose, x, y, z, size * 0.5F, roll, r, g, b, alpha);
        } else {
            FxDraw.sprite(consumer, pose, x, y, z, size, roll, r, g, b, alpha);
        }
    }

    public static final class Particle {
        private RenderType type;
        private double x;
        private double y;
        private double z;
        private double px;
        private double py;
        private double pz;
        private double vx;
        private double vy;
        private double vz;
        private double gravity;
        private double drag = 0.96D;
        private double floor = Double.NaN;
        private double bounce = 0.3D;
        private int life = 1;
        private int age;
        private int mode = SPRITE;
        private float size0;
        private float size1;
        private float r0 = 1.0F;
        private float g0 = 1.0F;
        private float b0 = 1.0F;
        private float r1 = 1.0F;
        private float g1 = 1.0F;
        private float b1 = 1.0F;
        private float alpha = 1.0F;
        private float fadeIn;
        private float fadePower = 2.0F;
        private float roll;
        private float prevRoll;
        private float spin;
        private float streak = 2.0F;
        private float flicker;
        private float seed;
        private boolean flowing;
        private float hot;
        private float flow;
        private float noise = 1.0F;
        private float scrollU;
        private float scrollV;
        private float dissolveFrom = 1.0F;

        private Particle() {
        }

        public Particle shade(float hot, float flow, float noise) {
            this.flowing = true;
            this.hot = hot;
            this.flow = flow;
            this.noise = noise;
            return this;
        }

        public Particle scroll(float u, float v) {
            this.flowing = true;
            this.scrollU = u;
            this.scrollV = v;
            return this;
        }

        public Particle dissolve(float from) {
            this.flowing = true;
            this.dissolveFrom = Mth.clamp(from, 0.0F, 0.99F);
            return this;
        }

        public Particle color(float r, float g, float b) {
            this.r0 = r;
            this.g0 = g;
            this.b0 = b;
            this.r1 = r;
            this.g1 = g;
            this.b1 = b;
            return this;
        }

        public Particle fadeTo(float r, float g, float b) {
            this.r1 = r;
            this.g1 = g;
            this.b1 = b;
            return this;
        }

        public Particle alpha(float alpha) {
            this.alpha = alpha;
            return this;
        }

        public Particle grow(float endSize) {
            this.size1 = endSize;
            return this;
        }

        public Particle gravity(double gravity) {
            this.gravity = gravity;
            return this;
        }

        public Particle drag(double drag) {
            this.drag = drag;
            return this;
        }

        public Particle floor(double floor, double bounce) {
            this.floor = floor;
            this.bounce = bounce;
            return this;
        }

        public Particle spin(float roll, float spin) {
            this.roll = roll;
            this.prevRoll = roll;
            this.spin = spin;
            return this;
        }

        public Particle streak(float stretch) {
            this.mode = STREAK;
            this.streak = stretch;
            return this;
        }

        public Particle flat() {
            this.mode = FLAT;
            return this;
        }

        public Particle fade(float fadeIn, float fadePower) {
            this.fadeIn = fadeIn;
            this.fadePower = fadePower;
            return this;
        }

        public Particle flicker(float amount, float seed) {
            this.flicker = amount;
            this.seed = seed;
            return this;
        }
    }
}
