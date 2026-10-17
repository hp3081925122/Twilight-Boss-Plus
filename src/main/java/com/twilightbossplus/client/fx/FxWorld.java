package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FxWorld {
    public static final ResourceLocation GLOW = tex("glow");
    public static final ResourceLocation FLARE = tex("flare");
    public static final ResourceLocation SPARK = tex("spark");
    public static final ResourceLocation SMOKE = tex("smoke");
    public static final ResourceLocation RING = tex("ring");
    public static final ResourceLocation SHOCK = tex("shock");
    public static final ResourceLocation BAND = tex("band");
    public static final ResourceLocation SLASH = tex("slash");
    public static final ResourceLocation WALL = tex("wall");
    public static final ResourceLocation CELLS = tex("cells");
    public static final ResourceLocation DECAL = tex("decal");
    public static final ResourceLocation VIGNETTE = tex("vignette");
    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static final List<Effect> PENDING = new ArrayList<>();
    private static final int MAX_EFFECTS = 400;
    private static int clock;
    private static boolean rendering;

    private FxWorld() {
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TwilightBossPlus.MOD_ID, "textures/fx/common2/" + name + ".png");
    }

    public static float quality() {
        return ClientConfig.QUALITY.get().factor();
    }

    public static int scaled(int count) {
        if (count <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(count * quality()));
    }

    public static int segments(int count) {
        if (ClientConfig.QUALITY.get() == ClientConfig.Quality.LOW) {
            return Math.max(8, count / 2);
        }
        return count;
    }

    public static int clock() {
        return clock;
    }

    public static <T extends Effect> T add(T effect) {
        if (!ClientConfig.VFX_ENABLED.get() || EFFECTS.size() + PENDING.size() >= MAX_EFFECTS) {
            effect.age = effect.life;
            return effect;
        }
        if (rendering) {
            PENDING.add(effect);
        } else {
            EFFECTS.add(effect);
        }
        return effect;
    }

    public static Effect later(int delay, Runnable action) {
        Effect effect = new Effect(1) {
            @Override
            protected void tick() {
                action.run();
            }

            @Override
            protected void render(PoseStack.Pose pose, float partialTick, float t) {
            }
        };
        effect.delay(delay);
        return add(effect);
    }

    public static Shockwave shockwave(double x, double y, double z, float radius, float width, int life, float r, float g, float b) {
        return add(new Shockwave(x, y, z, radius, width, life, r, g, b));
    }

    public static Flash flash(double x, double y, double z, float size, int life, float r, float g, float b) {
        return add(new Flash(x, y, z, size, life, r, g, b));
    }

    public static Decal decal(ResourceLocation texture, double x, double y, double z, float radius, int life, float r, float g, float b) {
        return add(new Decal(texture, x, y, z, radius, life, r, g, b));
    }

    public static Pillar pillar(double x, double y, double z, float radius, float height, int life, float r, float g, float b) {
        return add(new Pillar(x, y, z, radius, height, life, r, g, b));
    }

    public static Slash slash(Vec3 center, Vec3 e1, Vec3 e2, float radius, float width, float from, float to, int life, float r, float g, float b) {
        return add(new Slash(center, e1, e2, radius, width, from, to, life, r, g, b));
    }

    public static Groove groove(ResourceLocation texture, Vec3 from, Vec3 to, float width, int life, float r, float g, float b) {
        return add(new Groove(texture, from, to, width, life, r, g, b));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            EFFECTS.clear();
            PENDING.clear();
            return;
        }
        if (clock % 100 == 0) {
            FxShaders.refreshShaderPack();
        }
        if (minecraft.isPaused()) {
            return;
        }
        clock++;
        EFFECTS.addAll(PENDING);
        PENDING.clear();
        Iterator<Effect> iterator = EFFECTS.iterator();
        while (iterator.hasNext()) {
            Effect effect = iterator.next();
            if (effect.delay > 0) {
                effect.delay--;
                continue;
            }
            effect.tick();
            effect.age++;
            if (effect.age >= effect.life) {
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            EFFECTS.clear();
            PENDING.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (EFFECTS.isEmpty() && FxParticles.count() == 0) {
            return;
        }
        FxDraw.camera(event.getCamera());
        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();
        rendering = true;
        try {
            for (Effect effect : EFFECTS) {
                if (effect.delay <= 0 && effect.age < effect.life) {
                    float t = Mth.clamp((effect.age + partialTick) / effect.life, 0.0F, 1.0F);
                    effect.render(pose, partialTick, t);
                    FxDraw.resetParams();
                }
            }
            FxParticles.renderAll(pose, partialTick);
        } finally {
            rendering = false;
            poseStack.popPose();
            FxBuffers.end();
        }
    }

    public abstract static class Effect {
        protected int age;
        protected final int life;
        protected int delay;

        protected Effect(int life) {
            this.life = Math.max(1, life);
        }

        public Effect delay(int ticks) {
            this.delay = ticks;
            return this;
        }

        public void kill() {
            this.age = this.life;
        }

        protected void tick() {
        }

        protected abstract void render(PoseStack.Pose pose, float partialTick, float t);
    }

    public static class Shockwave extends Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private final float width;
        private final float r;
        private final float g;
        private final float b;
        private float startRadius;
        private float wallHeight;
        private float alpha = 1.0F;
        private float hot = 0.6F;
        private boolean linear;
        private ResourceLocation texture = SHOCK;

        public Shockwave(double x, double y, double z, float radius, float width, int life, float r, float g, float b) {
            super(life);
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.width = width;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Shockwave wall(float height) {
            this.wallHeight = height;
            return this;
        }

        public Shockwave start(float startRadius) {
            this.startRadius = startRadius;
            return this;
        }

        public Shockwave alpha(float alpha) {
            this.alpha = alpha;
            return this;
        }

        public Shockwave linear() {
            this.linear = true;
            return this;
        }

        public Shockwave hot(float hot) {
            this.hot = hot;
            return this;
        }

        public Shockwave texture(ResourceLocation texture) {
            this.texture = texture;
            return this;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float e = FxEase.outCubic(t);
            if (this.linear) {
                e = t;
            }
            float outer = Mth.lerp(e, this.startRadius, this.radius);
            float inner = Math.max(0.0F, outer - this.width * (1.0F - t * 0.5F));
            float a = this.alpha * (1.0F - FxEase.inCubic(t));
            float dissolve = Math.max(0.0F, (t - 0.35F) / 0.65F) * 0.85F;
            int segments = FxWorld.segments(64);
            FxDraw.params(dissolve, 0.12F, 0.25F, 0.0F, this.hot, 2.0F, 0.6F);
            VertexConsumer ring = FxBuffers.get(FxRenderTypes.add(this.texture));
            FxDraw.ring(ring, pose, this.x, this.y + 0.06D, this.z, inner, outer, segments, clock * 0.01F, 6.0F, this.r, this.g, this.b, a, a);
            if (this.wallHeight > 0.0F) {
                float h = this.wallHeight * (1.0F - t * 0.6F);
                FxDraw.params(dissolve, 0.15F, 0.0F, -0.6F, this.hot * 0.5F, 3.0F, 0.8F);
                VertexConsumer wall = FxBuffers.get(FxRenderTypes.add(WALL));
                FxDraw.wallArc(wall, pose, this.x, this.y, this.z, outer - this.width * 0.3F, outer - this.width * 0.1F, h, 0.0F, Mth.TWO_PI, segments,
                    6.0F, 0.0F, this.r, this.g, this.b, a * 0.9F, 0.0F);
            }
        }
    }

    public static class Flash extends Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float size;
        private final float r;
        private final float g;
        private final float b;
        private float flareScale = 2.2F;

        public Flash(double x, double y, double z, float size, int life, float r, float g, float b) {
            super(life);
            this.x = x;
            this.y = y;
            this.z = z;
            this.size = size;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Flash flare(float scale) {
            this.flareScale = scale;
            return this;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float grow = FxEase.outBack(Math.min(1.0F, t * 3.0F));
            float a = 1.0F - FxEase.outCubic(t);
            FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
            VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(GLOW));
            FxDraw.sprite(glow, pose, this.x, this.y, this.z, this.size * grow, 0.0F, this.r, this.g, this.b, a);
            FxDraw.sprite(glow, pose, this.x, this.y, this.z, this.size * grow * 0.45F, 0.0F, 1.0F, 1.0F, 1.0F, a);
            if (this.flareScale > 0.0F) {
                VertexConsumer flare = FxBuffers.get(FxRenderTypes.add(FLARE));
                float fs = this.size * this.flareScale * (0.6F + 0.4F * grow);
                FxDraw.sprite(flare, pose, this.x, this.y, this.z, fs, t * 0.6F, this.r, this.g, this.b, a * 0.85F);
                FxDraw.sprite(flare, pose, this.x, this.y, this.z, fs * 0.7F, -t * 0.9F + 0.4F, 1.0F, 1.0F, 1.0F, a * 0.5F);
            }
        }
    }

    public static class Decal extends Effect {
        private final ResourceLocation texture;
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private final float r;
        private final float g;
        private final float b;
        private float spin = 0.02F;
        private float angle;
        private boolean charge = true;
        private float alpha = 0.9F;

        public Decal(ResourceLocation texture, double x, double y, double z, float radius, int life, float r, float g, float b) {
            super(life);
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Decal spin(float angle, float spin) {
            this.angle = angle;
            this.spin = spin;
            return this;
        }

        public Decal noCharge() {
            this.charge = false;
            return this;
        }

        public Decal alpha(float alpha) {
            this.alpha = alpha;
            return this;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float appear = FxEase.outBack(Math.min(1.0F, t * 5.0F));
            float a = this.alpha * FxEase.window(t, 0.08F, 0.12F);
            float rot = this.angle + (this.age + partialTick) * this.spin;
            float pulse = 0.85F + 0.15F * Mth.sin((this.age + partialTick) * 0.6F);
            float half = this.radius * Math.max(0.0F, appear);
            FxDraw.params(0.0F, 0.1F, 0.12F, 0.08F, 0.25F, 1.5F, 0.35F);
            VertexConsumer decal = FxBuffers.get(FxRenderTypes.add(this.texture));
            FxDraw.flat(decal, pose, this.x, this.y + 0.04D, this.z, half, rot, this.r, this.g, this.b, a * pulse);
            if (this.charge) {
                float fill = FxEase.inOutSine(t);
                FxDraw.params(0.0F, 0.1F, 0.0F, 0.0F, 0.8F, 1.0F, 0.0F);
                VertexConsumer glow = FxBuffers.get(FxRenderTypes.add(GLOW));
                FxDraw.flat(glow, pose, this.x, this.y + 0.05D, this.z, half * fill, rot, this.r, this.g, this.b, a * 0.7F * fill);
            }
        }
    }

    public static class Pillar extends Effect {
        private final double x;
        private final double y;
        private final double z;
        private final float radius;
        private final float height;
        private final float r;
        private final float g;
        private final float b;

        public Pillar(double x, double y, double z, float radius, float height, int life, float r, float g, float b) {
            super(life);
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.height = height;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float rise = FxEase.outQuint(Math.min(1.0F, t * 2.5F));
            float a = 1.0F - FxEase.inCubic(t);
            float dissolve = Math.max(0.0F, (t - 0.4F) / 0.6F) * 0.9F;
            float rad = this.radius * (1.0F - t * 0.4F);
            int segments = FxWorld.segments(24);
            FxDraw.params(dissolve, 0.15F, 0.0F, -1.2F, 0.7F, 2.5F, 0.85F);
            VertexConsumer wall = FxBuffers.get(FxRenderTypes.add(WALL));
            FxDraw.wallArc(wall, pose, this.x, this.y, this.z, rad, rad * 0.6F, this.height * rise, 0.0F, Mth.TWO_PI, segments, 3.0F, 0.0F,
                this.r, this.g, this.b, a, a * 0.2F);
            FxDraw.wallArc(wall, pose, this.x, this.y, this.z, rad * 0.45F, rad * 0.3F, this.height * rise * 1.15F, 0.7F, 0.7F + Mth.TWO_PI, segments, 2.0F,
                0.3F, 1.0F, 1.0F, 1.0F, a * 0.7F, 0.0F);
        }
    }

    public static class Slash extends Effect {
        private final Vec3 center;
        private final Vec3 e1;
        private final Vec3 e2;
        private final float radius;
        private final float width;
        private final float from;
        private final float to;
        private final float r;
        private final float g;
        private final float b;
        private float reveal = 0.35F;

        public Slash(Vec3 center, Vec3 e1, Vec3 e2, float radius, float width, float from, float to, int life, float r, float g, float b) {
            super(life);
            this.center = center;
            this.e1 = e1;
            this.e2 = e2;
            this.radius = radius;
            this.width = width;
            this.from = from;
            this.to = to;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Slash reveal(float fraction) {
            this.reveal = Math.max(0.05F, fraction);
            return this;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float head = FxEase.outCubic(t / this.reveal);
            float tail = FxEase.inOutSine(Math.max(0.0F, (t - this.reveal * 0.4F) / (1.0F - this.reveal * 0.4F)));
            if (head <= tail) {
                return;
            }
            float a0 = Mth.lerp(tail, this.from, this.to);
            float a1 = Mth.lerp(head, this.from, this.to);
            float a = 1.0F - FxEase.inCubic(Math.max(0.0F, (t - this.reveal) / (1.0F - this.reveal)));
            float dissolve = Math.max(0.0F, (t - this.reveal) / (1.0F - this.reveal)) * 0.7F;
            int segments = Math.max(6, (int) (FxWorld.segments(32) * Math.abs(a1 - a0) / Math.PI));
            FxDraw.params(dissolve, 0.1F, -1.5F, 0.0F, 0.9F, 2.0F, 0.5F);
            VertexConsumer slash = FxBuffers.get(FxRenderTypes.add(SLASH));
            FxDraw.arcRibbon(slash, pose, this.center, this.e1, this.e2, this.radius - this.width, this.radius, a0, a1, segments,
                this.r, this.g, this.b, a * 0.15F, a);
            FxDraw.arcRibbon(slash, pose, this.center, this.e1, this.e2, this.radius - this.width * 0.4F, this.radius + this.width * 0.08F, a0, a1, segments,
                1.0F, 1.0F, 1.0F, 0.0F, a * 0.8F);
        }
    }

    public static class Groove extends Effect {
        private final ResourceLocation texture;
        private final Vec3 from;
        private final Vec3 to;
        private final float width;
        private final float r;
        private final float g;
        private final float b;
        private int grow = 4;
        private float alpha = 1.0F;

        public Groove(ResourceLocation texture, Vec3 from, Vec3 to, float width, int life, float r, float g, float b) {
            super(life);
            this.texture = texture;
            this.from = from;
            this.to = to;
            this.width = width;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public Groove grow(int ticks) {
            this.grow = Math.max(1, ticks);
            return this;
        }

        public Groove alpha(float alpha) {
            this.alpha = alpha;
            return this;
        }

        @Override
        protected void render(PoseStack.Pose pose, float partialTick, float t) {
            float g0 = FxEase.outCubic((this.age + partialTick) / this.grow);
            Vec3 end = this.from.add(this.to.subtract(this.from).scale(g0));
            Vec3 dir = end.subtract(this.from);
            double len = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
            if (len < 1.0E-3D) {
                return;
            }
            Vec3 side = new Vec3(-dir.z / len * this.width * 0.5D, 0.0D, dir.x / len * this.width * 0.5D);
            float a = this.alpha * (1.0F - FxEase.inCubic(t));
            float dissolve = Math.max(0.0F, (t - 0.5F) / 0.5F) * 0.85F;
            FxDraw.params(dissolve, 0.12F, 0.0F, 0.0F, 0.4F, 1.5F, 0.4F);
            VertexConsumer consumer = FxBuffers.get(FxRenderTypes.add(this.texture));
            Vec3 p0 = this.from.add(side).add(0.0D, 0.05D, 0.0D);
            Vec3 p1 = this.from.subtract(side).add(0.0D, 0.05D, 0.0D);
            Vec3 p2 = end.subtract(side).add(0.0D, 0.05D, 0.0D);
            Vec3 p3 = end.add(side).add(0.0D, 0.05D, 0.0D);
            FxDraw.quad(consumer, pose, p0, p1, p2, p3, 0.0F, 0.0F, (float) (len / 4.0D), 1.0F, this.r, this.g, this.b, a);
        }
    }
}
