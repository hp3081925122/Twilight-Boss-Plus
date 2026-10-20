package com.twilightbossplus.client.vfx;

import com.twilightbossplus.naga.NagaFx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class NagaVfx {
    private static final VfxAtlas A = VfxAtlas.NAGA;
    private static final int SCALES = 0;
    private static final int CHEVRON = 1;
    private static final int WAVE = 2;
    private static final int DRIP = 3;
    private static final int SPLAT = 0;
    private static final int CRACK = 1;
    private static final int EYE = 2;
    private static final int SPIRAL = 3;
    private static final int BUBBLE = 0;
    private static final int DROP = 1;
    private static final int CLOD = 2;
    private static final int FANG = 3;
    private static final int HISS = 4;

    private static final int VENOM = 0x7CFF4F;
    private static final int VENOM_DARK = 0x2F9A34;
    private static final int SCALE = 0x5BD06A;
    private static final int IVORY = 0xFFF2C4;
    private static final int WARNING = 0xFFB03A;
    private static final int EARTH = 0x9A7448;
    private static final int EMBER = 0xFFB458;
    private static final int RAGE = 0xFF3355;

    private NagaVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        RandomSource random = level.getRandom();
        switch (type) {
            case NagaFx.WARN -> ClientVfx.add(new Warn(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color")));
            case NagaFx.LUNGE -> ClientVfx.add(new Lunge(ClientVfx.vec(d, "a"), ClientVfx.vec(d, "b"), d.getFloat("w"), d.getInt("d")));
            case NagaFx.GLOB -> {
                ClientVfx.add(new Glob(ClientVfx.vec(d, "o"), ClientVfx.vec(d, "v"), d.getDouble("g"), d.getInt("t")));
                ClientVfx.add(new Warn(level, ClientVfx.vec(d, "l"), d.getFloat("r"), d.getInt("t"), VENOM));
            }
            case NagaFx.POOL -> ClientVfx.add(new Pool(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case NagaFx.BREATH -> ClientVfx.add(new Breath(d.getInt("id"), d.getFloat("len"), d.getFloat("half"), d.getInt("windup"), d.getInt("d")));
            case NagaFx.QUAKE -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                ClientVfx.add(new Crater(level, c, radius));
                for (int delay : d.getIntArray("delays")) {
                    ClientVfx.add(new Mound(level, c, d.getFloat("speed"), d.getFloat("max"), delay));
                }
                clods(level, c, radius, ClientVfx.count(14), 0.55);
            }
            case NagaFx.LASH -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                ClientVfx.add(new Lash(c, radius, d.getFloat("yaw")));
                int amount = ClientVfx.count(10);
                for (int i = 0; i < amount; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double dist = radius * (0.5 + random.nextDouble() * 0.5);
                    double x = c.x + Math.cos(angle) * dist;
                    double z = c.z + Math.sin(angle) * dist;
                    clod(x, VfxCanvas.ground(level, x, z, c.y) + 0.15, z, Math.cos(angle) * 0.18, 0.25 + random.nextDouble() * 0.2, Math.sin(angle) * 0.18, random);
                }
            }
            case NagaFx.BURROW -> {
                int id = d.getInt("id");
                boolean found = false;
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Burrow burrow && burrow.id == id) {
                        burrow.active = d.getBoolean("on");
                        burrow.age = 0;
                        found = true;
                    }
                }
                if (!found && d.getBoolean("on")) {
                    ClientVfx.add(new Burrow(id));
                }
            }
            case NagaFx.ERUPT -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                boolean big = d.getBoolean("big");
                ClientVfx.add(new Eruption(level, c, radius, big));
                if (big) {
                    clods(level, c, radius, ClientVfx.count(18), 0.8);
                    int bubbles = ClientVfx.count(8);
                    for (int i = 0; i < bubbles; i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        ClientVfx.spawn(new Mote(A, BUBBLE, true).at(c.x + Math.cos(angle) * radius * 0.5, c.y + 0.3 + random.nextDouble(), c.z + Math.sin(angle) * radius * 0.5)
                                .vel(Math.cos(angle) * 0.08, 0.12 + random.nextDouble() * 0.12, Math.sin(angle) * 0.08).gravity(0.004).drag(0.94)
                                .size(0.22 + random.nextDouble() * 0.18).animate().life(18 + random.nextInt(8)));
                    }
                } else {
                    clods(level, c, radius, ClientVfx.count(8), 0.45);
                }
            }
            case NagaFx.HISS -> {
                Vec3 c = ClientVfx.vec(d, "c");
                ClientVfx.add(new Hiss(level, c, d.getFloat("r")));
                int amount = ClientVfx.count(10);
                for (int i = 0; i < amount; i++) {
                    double angle = i * Mth.TWO_PI / amount;
                    ClientVfx.spawn(new Mote(A, HISS, true).at(c.x + Math.cos(angle) * 1.2, c.y + 1.4 + random.nextDouble() * 0.6, c.z + Math.sin(angle) * 1.2)
                            .vel(Math.cos(angle) * 0.35, 0.02, Math.sin(angle) * 0.35).drag(0.93).size(0.55).grow(0.8)
                            .spin(-angle, 0.0).color(RAGE).animate().life(16));
                }
            }
            case NagaFx.AURA -> {
                int id = d.getInt("id");
                boolean found = false;
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Aura aura && aura.id == id) {
                        aura.left = Math.max(aura.left, d.getInt("d"));
                        found = true;
                    }
                }
                if (!found) {
                    ClientVfx.add(new Aura(id, d.getInt("d")));
                }
            }
            case NagaFx.BITE -> ClientVfx.add(new Bite(d.getInt("id"), d.getFloat("reach"), d.getFloat("half"), d.getInt("windup"), d.getInt("interval"), d.getInt("snaps")));
            default -> {
            }
        }
    }

    private static void clod(double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
        ClientVfx.spawn(new Mote(A, CLOD, false).at(x, y, z).vel(vx, vy, vz).gravity(0.055).drag(0.98)
                .size(0.13 + random.nextDouble() * 0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.5)
                .collide().life(32 + random.nextInt(18)));
    }

    private static void clods(ClientLevel level, Vec3 c, double radius, int amount, double up) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * radius;
            double x = c.x + Math.cos(angle) * dist;
            double z = c.z + Math.sin(angle) * dist;
            double out = 0.06 + random.nextDouble() * 0.16;
            clod(x, VfxCanvas.ground(level, x, z, c.y) + 0.15, z, Math.cos(angle) * out, up * (0.6 + random.nextDouble() * 0.6), Math.sin(angle) * out, random);
        }
    }

    private static void droplet(double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
        ClientVfx.spawn(new Mote(A, DROP, false).at(x, y, z).vel(vx, vy, vz).gravity(0.035).drag(0.95)
                .size(0.09 + random.nextDouble() * 0.08).frame(random.nextInt(4)).collide().life(16 + random.nextInt(8)));
    }

    private static final class Warn extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final int color;
        private final VfxCanvas.Patch patch;

        Warn(ClientLevel level, Vec3 c, float radius, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.duration = Math.max(1, duration);
            this.color = color;
            this.patch = VfxCanvas.patch(level, c, radius);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 8;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 4.0F, this.duration, 8.0F);
            c.decal(A, true, EYE, this.patch, t * 0.01, 0.35 + 0.65 * ease(p), 0.02, this.color, 0.3F * a);
            c.strip(A, true, SCALES, this.color, 0.9F * a).scroll(t * 0.8).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius - 0.6), this.radius, 0.0, Mth.TWO_PI, true);
            double inner = Math.max(0.35, this.radius * (1.0 - p * 0.8));
            c.strip(A, true, CHEVRON, this.color, 0.55F * a).lift(0.01).arc(this.c.x, this.c.y, this.c.z, inner - 0.35, inner, -t * 0.04, Mth.TWO_PI, true);
            if (t > this.duration) {
                float k = (t - this.duration) / 8.0F;
                double r = this.radius * (1.0 + k * 0.25);
                c.strip(A, true, SCALES, 0xFFFFFF, 0.8F * (1.0F - k)).lift(0.02).arc(this.c.x, this.c.y, this.c.z, r - 0.5, r, 0.0, Mth.TWO_PI, true);
            }
        }
    }

    private static final class Lunge extends VfxEffect {
        private final Vec3 a;
        private final Vec3 b;
        private final float width;
        private final int duration;
        private final Vec3[] full;

        Lunge(Vec3 a, Vec3 b, float width, int duration) {
            this.a = a;
            this.b = b;
            this.width = width;
            this.duration = Math.max(1, duration);
            this.full = VfxCanvas.line(b, a, 1.0);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float alpha = envelope(t, 3.0F, this.duration, 6.0F);
            c.strip(A, false, SCALES, VfxCanvas.lit(c.level, this.a, VENOM_DARK), 0.45F * alpha).path(this.full, this.width, true);
            c.strip(A, true, CHEVRON, WARNING, 0.35F * alpha).lift(0.01).scroll(t * 3.0).path(this.full, this.width * 0.8, true);
            if (p > 0.02F) {
                Vec3 reach = this.a.lerp(this.b, p);
                c.strip(A, true, CHEVRON, WARNING, 0.85F * alpha).lift(0.02).scroll(t * 3.0).fade(1.0F, 0.4F).path(VfxCanvas.line(reach, this.a, 1.0), this.width * 0.8, true);
            }
            double gy = VfxCanvas.ground(c.level, this.b.x, this.b.z, this.b.y) + 0.03;
            float pulse = 0.9F + 0.15F * Mth.sin(t * 0.6F);
            c.flatSprite(A, true, FANG, 0, this.b.x, gy, this.b.z, 0.9 * pulse, t * 0.05, IVORY, alpha);
        }
    }

    private static final class Glob extends VfxEffect {
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
        public boolean tick(ClientLevel level) {
            this.age++;
            RandomSource random = level.getRandom();
            Vec3 p = this.at(this.age);
            droplet(p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.04, 0.0, (random.nextDouble() - 0.5) * 0.04, random);
            if (this.age == this.flight) {
                ClientVfx.add(new Splash(level, this.at(this.flight)));
                int amount = ClientVfx.count(12);
                for (int i = 0; i < amount; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double speed = 0.12 + random.nextDouble() * 0.18;
                    droplet(p.x, p.y + 0.2, p.z, Math.cos(angle) * speed, 0.2 + random.nextDouble() * 0.25, Math.sin(angle) * speed, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age <= this.flight;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            Vec3 p = this.at(t);
            Vec3[] trail = new Vec3[6];
            for (int i = 0; i < trail.length; i++) {
                trail[i] = this.at(Math.max(0.0, t - (trail.length - 1 - i) * 0.9));
            }
            c.strip(A, true, WAVE, VENOM, 0.6F).fade(0.0F, 1.0F).scroll(t * 4.0).path(trail, 0.45, false);
            c.sprite(A, true, BUBBLE, 2, p.x, p.y, p.z, 0.85, -t * 0.1, VENOM, 0.45F);
            c.sprite(A, false, BUBBLE, 1 + (this.age / 3) % 2, p.x, p.y, p.z, 0.45, t * 0.2, 0xFFFFFF, 1.0F);
        }
    }

    private static final class Splash extends VfxEffect {
        private static final int LIFE = 10;
        private final VfxCanvas.Patch patch;
        private final double rotation;

        Splash(ClientLevel level, Vec3 c) {
            this.patch = VfxCanvas.patch(level, new Vec3(c.x, VfxCanvas.ground(level, c.x, c.z, c.y + 0.5) - 0.04, c.z), 3.2);
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c) / LIFE;
            c.decal(A, true, SPLAT, this.patch, this.rotation, 0.3 + ease(t * 2.0F) * 1.0, 0.04, VENOM, 0.9F * (1.0F - t));
        }
    }

    private static final class Pool extends VfxEffect {
        private final VfxCanvas.Patch patch;
        private final float radius;
        private final int duration;
        private final double rotation;
        private final int tint;

        Pool(ClientLevel level, Vec3 c, float radius, int duration) {
            this.patch = VfxCanvas.patch(level, c, radius * 1.15);
            this.radius = radius;
            this.duration = duration;
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, VENOM_DARK);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            RandomSource random = level.getRandom();
            if (this.age % 4 == 0 && this.age < this.duration - 10) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double dist = Math.sqrt(random.nextDouble()) * this.radius * 0.8;
                Vec3 c = this.patch.center;
                double x = c.x + Math.cos(angle) * dist;
                double z = c.z + Math.sin(angle) * dist;
                ClientVfx.spawn(new Mote(A, BUBBLE, true).at(x, VfxCanvas.ground(level, x, z, c.y) + 0.12, z).vel(0.0, 0.012, 0.0).drag(1.0)
                        .size(0.16 + random.nextDouble() * 0.14).animate().life(14 + random.nextInt(6)));
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float a = envelope(t, 5.0F, this.duration - 15.0F, 15.0F);
            double scale = Math.min(1.0, 0.4 + t / 6.0);
            double rot = this.rotation + t * 0.003;
            float pulse = 0.8F + 0.2F * Mth.sin(t * 0.2F);
            c.decal(A, false, SPLAT, this.patch, rot, scale, 0.015, this.tint, 0.85F * a);
            c.decal(A, true, SPLAT, this.patch, rot, scale, 0.025, VENOM, 0.3F * pulse * a);
        }
    }

    private static final class Breath extends VfxEffect {
        private final int id;
        private final float length;
        private final float half;
        private final int windup;
        private final int duration;

        Breath(int id, float length, float half, int windup, int duration) {
            this.id = id;
            this.length = length;
            this.half = half;
            this.windup = windup;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            if (this.age > this.windup && this.age < this.duration - 4) {
                RandomSource random = level.getRandom();
                Vec3 eye = entity.getEyePosition();
                int amount = ClientVfx.count(3);
                for (int i = 0; i < amount; i++) {
                    float yaw = entity.getYRot() + (random.nextFloat() * 2.0F - 1.0F) * this.half;
                    Vec3 dir = Vec3.directionFromRotation(10.0F + random.nextFloat() * 10.0F, yaw);
                    double speed = 0.5 + random.nextDouble() * 0.3;
                    if (i == 0) {
                        ClientVfx.spawn(new Mote(A, BUBBLE, true).at(eye.x + dir.x, eye.y - 0.3, eye.z + dir.z).vel(dir.x * speed, dir.y * speed, dir.z * speed)
                                .drag(0.9).size(0.25 + random.nextDouble() * 0.15).grow(0.6).animate().life(16));
                    } else {
                        droplet(eye.x + dir.x, eye.y - 0.3, eye.z + dir.z, dir.x * speed, dir.y * speed, dir.z * speed, random);
                    }
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            float yaw = Mth.rotLerp(c.partial, entity.yRotO, entity.getYRot());
            double start = Math.toRadians(yaw + 90.0F - this.half);
            double sweep = Math.toRadians(this.half * 2.0F);
            float a = envelope(t, 4.0F, this.duration - 6.0F, 6.0F);
            c.strip(A, true, SCALES, VENOM, 0.85F * a).arc(pos.x, pos.y, pos.z, this.length - 0.55, this.length, start, sweep, true);
            if (t < this.windup) {
                float p = t / this.windup;
                c.strip(A, true, WAVE, VENOM, 0.35F * a).density(6.0).lift(0.01).arc(pos.x, pos.y, pos.z, 0.6, 0.6 + (this.length - 0.6) * p, start, sweep, true);
                return;
            }
            c.strip(A, true, WAVE, VENOM, 0.3F * a).density(6.0).lift(0.01).scroll(-t * 2.0).arc(pos.x, pos.y, pos.z, 0.6, this.length, start, sweep, true);
            double mouth = entity.getEyeHeight() - 0.4;
            for (int k = 0; k < 4; k++) {
                float phase = ((t - this.windup) * 0.09F + k / 4.0F) % 1.0F;
                double r = 0.8 + phase * (this.length - 0.8);
                c.strip(A, true, WAVE, VENOM, 0.6F * a * (1.0F - phase)).scroll(t * 3.0).arc(pos.x, pos.y + mouth - phase * 0.6, pos.z, Math.max(0.0, r - 0.8), r, start, sweep, false);
            }
        }
    }

    private static final class Crater extends VfxEffect {
        private static final int LIFE = 44;
        private final VfxCanvas.Patch patch;
        private final float radius;
        private final double rotation;
        private final int tint;

        Crater(ClientLevel level, Vec3 c, float radius) {
            this.patch = VfxCanvas.patch(level, c, radius * 1.7);
            this.radius = radius;
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, 0x3A2817);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            double scale = 0.3 + 0.7 * ease(t / 5.0F);
            Vec3 o = this.patch.center;
            c.decal(A, false, CRACK, this.patch, this.rotation, scale, 0.02, this.tint, 0.9F * envelope(t, 1.0F, 26.0F, 18.0F));
            c.decal(A, true, CRACK, this.patch, this.rotation, scale, 0.03, EMBER, 0.7F * (1.0F - t / LIFE));
            if (t < 9.0F) {
                float k = t / 9.0F;
                c.strip(A, true, WAVE, EMBER, 0.8F * (1.0F - k)).wall(o.x, o.y, o.z, this.radius * (0.5 + k), 1.6 * (1.0F - k) + 0.2, 0.0, Mth.TWO_PI, true);
            }
        }
    }

    private static final class Mound extends VfxEffect {
        private final Vec3 c;
        private final float speed;
        private final float max;
        private final int delay;
        private final int tint;

        Mound(ClientLevel level, Vec3 c, float speed, float max, int delay) {
            this.c = c;
            this.speed = speed;
            this.max = max;
            this.delay = delay;
            this.tint = VfxCanvas.lit(level, c, EARTH);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            int active = this.age - this.delay;
            if (active > 0 && active % 2 == 0) {
                RandomSource random = level.getRandom();
                double r = active * this.speed;
                int amount = ClientVfx.count(2);
                for (int i = 0; i < amount; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double x = this.c.x + Math.cos(angle) * r;
                    double z = this.c.z + Math.sin(angle) * r;
                    clod(x, VfxCanvas.ground(level, x, z, this.c.y) + 0.4, z, Math.cos(angle) * 0.1, 0.22 + random.nextDouble() * 0.12, Math.sin(angle) * 0.1, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return (this.age - this.delay) * this.speed < this.max + 1.0F;
        }

        @Override
        public void render(VfxCanvas c) {
            float active = this.time(c) - this.delay;
            if (active <= 0.0F) {
                return;
            }
            double r = active * this.speed;
            float f = Mth.clamp(1.0F - (float) (r / (this.max + 1.0F)), 0.0F, 1.0F);
            float a = 0.35F + 0.65F * f;
            double h = 0.5 + 0.6 * f;
            c.strip(A, false, WAVE, this.tint, 0.95F * a).density(16.0 / h).scroll(r * 6.0).wall(this.c.x, this.c.y, this.c.z, r, h, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, WAVE, EMBER, 0.3F * a).density(16.0 / h).scroll(r * 6.0).wall(this.c.x, this.c.y, this.c.z, r + 0.03, h, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Lash extends VfxEffect {
        private static final int LIFE = 11;
        private final Vec3 c;
        private final float radius;
        private final double start;

        Lash(Vec3 c, float radius, float yaw) {
            this.c = c;
            this.radius = radius;
            this.start = Math.toRadians(yaw);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float reveal = ease(t / 4.0F);
            float a = 1.0F - Mth.clamp((t - 4.0F) / (LIFE - 4.0F), 0.0F, 1.0F);
            double sweep = Mth.TWO_PI * reveal;
            double tail = Math.min(sweep, 2.6);
            double head = this.start + sweep;
            c.strip(A, true, SCALES, SCALE, 0.9F * a).fade(0.0F, 1.0F).arc(this.c.x, this.c.y + 0.6, this.c.z, this.radius * 0.35, this.radius, head - tail, tail, false);
            c.strip(A, true, CHEVRON, 0xFFFFFF, 0.65F * a).fade(0.0F, 1.0F).arc(this.c.x, this.c.y + 0.62, this.c.z, this.radius - 0.4, this.radius, head - tail, tail, false);
            c.strip(A, true, SCALES, SCALE, 0.4F * a).fade(0.0F, 1.0F).arc(this.c.x, this.c.y + 0.25, this.c.z, this.radius * 0.5, this.radius * 0.95, head - tail * 0.8, tail * 0.8, false);
        }
    }

    private static final class Burrow extends VfxEffect {
        private final int id;
        private boolean active = true;

        Burrow(int id) {
            this.id = id;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null || !this.active) {
                return false;
            }
            double gy = VfxCanvas.ground(level, entity.getX(), entity.getZ(), entity.getY());
            Vec3 at = new Vec3(entity.getX(), gy - 0.04, entity.getZ());
            if (this.age % 3 == 0) {
                ClientVfx.add(new Scar(level, at));
            }
            if (this.age % 2 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                clod(at.x + Math.cos(angle) * 0.6, gy + 0.2, at.z + Math.sin(angle) * 0.6, Math.cos(angle) * 0.06, 0.3 + random.nextDouble() * 0.15, Math.sin(angle) * 0.06, random);
            }
            return this.age < 200;
        }

        @Override
        protected boolean alive() {
            return this.active;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            c.strip(A, false, WAVE, VfxCanvas.lit(c.level, pos, EARTH), 0.95F).density(28.0).scroll(t * 5.0).wall(pos.x, pos.y, pos.z, 1.0, 0.55, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, SCALES, SCALE, 0.5F).scroll(t * 2.0).arc(pos.x, pos.y, pos.z, 1.0, 1.5, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Scar extends VfxEffect {
        private static final int LIFE = 34;
        private final VfxCanvas.Patch patch;
        private final double rotation;
        private final int tint;

        Scar(ClientLevel level, Vec3 c) {
            this.patch = VfxCanvas.patch(level, c, 1.5);
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, 0x3A2817);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            c.decal(A, false, CRACK, this.patch, this.rotation, 0.6 + 0.4 * ease(t / 4.0F), 0.015, this.tint, 0.85F * envelope(t, 2.0F, 14.0F, 20.0F));
        }
    }

    private static final class Eruption extends VfxEffect {
        private final VfxCanvas.Patch patch;
        private final float radius;
        private final boolean big;
        private final double rotation;
        private final int tint;

        Eruption(ClientLevel level, Vec3 c, float radius, boolean big) {
            this.patch = VfxCanvas.patch(level, c, radius * 1.5);
            this.radius = radius;
            this.big = big;
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, 0x3A2817);
        }

        @Override
        protected boolean alive() {
            return this.age < 40;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            Vec3 o = this.patch.center;
            c.decal(A, false, CRACK, this.patch, this.rotation, 0.4 + 0.6 * ease(t / 4.0F), 0.02, this.tint, 0.9F * envelope(t, 1.0F, 24.0F, 16.0F));
            if (!this.big) {
                return;
            }
            float k = Mth.clamp(t / 12.0F, 0.0F, 1.0F);
            c.decal(A, true, SPIRAL, this.patch, -t * 0.15, 0.3 + 0.8 * ease(k), 0.04, VENOM, 0.85F * (1.0F - t / 40.0F));
            if (t < 10.0F) {
                float f = t / 10.0F;
                c.strip(A, false, WAVE, VfxCanvas.lit(c.level, o, EARTH), 0.95F * (1.0F - f)).density(10.0).wall(o.x, o.y, o.z, this.radius * (0.4 + f * 0.9), 2.0 * (1.0F - f) + 0.2, 0.0, Mth.TWO_PI, true);
                c.strip(A, true, SCALES, SCALE, 0.8F * (1.0F - f)).lift(0.03).arc(o.x, o.y, o.z, this.radius * (0.4 + f), this.radius * (0.4 + f) + 0.7, 0.0, Mth.TWO_PI, true);
            }
        }
    }

    private static final class Hiss extends VfxEffect {
        private static final int LIFE = 26;
        private final Vec3 c;
        private final float radius;
        private final VfxCanvas.Patch patch;

        Hiss(ClientLevel level, Vec3 c, float radius) {
            this.c = c;
            this.radius = radius;
            this.patch = VfxCanvas.patch(level, c, 4.0);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            c.decal(A, true, EYE, this.patch, 0.0, 0.6 + 0.4 * ease(t / 3.0F), 0.04, RAGE, 0.85F * (1.0F - t / LIFE));
            for (int k = 0; k < 3; k++) {
                double r = (t - k * 3.0F) * 1.25;
                if (r <= 0.2 || r > this.radius + 1.0) {
                    continue;
                }
                float f = 1.0F - (float) (r / (this.radius + 1.0));
                c.strip(A, true, WAVE, RAGE, 0.75F * f).density(14.0).scroll(t * 4.0).wall(this.c.x, this.c.y, this.c.z, r, 1.7 * f + 0.3, 0.0, Mth.TWO_PI, true);
                c.strip(A, true, SCALES, 0xFFFFFF, 0.55F * f).lift(0.02).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, r - 0.45), r, k * 0.7, Mth.TWO_PI, true);
            }
        }
    }

    private static final class Aura extends VfxEffect {
        private final int id;
        private int left;

        Aura(int id, int left) {
            this.id = id;
            this.left = left;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            this.left--;
            Entity entity = level.getEntity(this.id);
            if (entity == null || !entity.isAlive()) {
                return false;
            }
            if (this.age % 5 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                ClientVfx.spawn(new Mote(A, DROP, true).at(entity.getX() + Math.cos(angle) * 1.4, entity.getY() + 0.2, entity.getZ() + Math.sin(angle) * 1.4)
                        .vel(0.0, 0.06, 0.0).gravity(-0.004).drag(0.97).size(0.13).frame(random.nextInt(4)).color(RAGE).life(22));
            }
            return this.left > 0;
        }

        @Override
        protected boolean alive() {
            return this.left > 0;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            float a = Mth.clamp(this.left / 20.0F, 0.0F, 1.0F) * Mth.clamp(t / 8.0F, 0.0F, 1.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double gy = VfxCanvas.ground(c.level, pos.x, pos.z, pos.y) + 0.02;
            c.flatDecal(A, true, SPIRAL, pos.x, gy, pos.z, 2.8, t * 0.08, RAGE, 0.5F * a);
            c.flatDecal(A, true, SPIRAL, pos.x, gy + 0.01, pos.z, 1.9, -t * 0.12, EMBER, 0.3F * a);
        }
    }

    private static final class Bite extends VfxEffect {
        private static final double JAW = 1.5;
        private static final double TEETH = 0.55;
        private final int id;
        private final float reach;
        private final float half;
        private final int windup;
        private final int interval;
        private final int snaps;

        Bite(int id, float reach, float half, int windup, int interval, int snaps) {
            this.id = id;
            this.reach = reach;
            this.half = half;
            this.windup = windup;
            this.interval = interval;
            this.snaps = snaps;
        }

        private Vec3 mouth(Entity entity, float partial, double push) {
            Vec3 pos = VfxCanvas.pos(entity, partial);
            float yaw = Mth.rotLerp(partial, entity.yRotO, entity.getYRot());
            Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
            return pos.add(forward.scale(this.reach * 0.5 + push)).add(0.0, 1.0, 0.0);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            for (int s = 0; s < this.snaps; s++) {
                if (this.age == this.windup + s * this.interval) {
                    RandomSource random = level.getRandom();
                    Vec3 m = this.mouth(entity, 1.0F, 0.6);
                    for (int i = 0; i < 3; i++) {
                        ClientVfx.spawn(new Mote(A, FANG, true).at(m.x + (random.nextDouble() - 0.5) * 1.2, m.y + (random.nextDouble() - 0.5) * 0.8, m.z + (random.nextDouble() - 0.5) * 1.2)
                                .size(0.35 + random.nextDouble() * 0.2).color(IVORY).animate().life(8));
                    }
                    int drops = ClientVfx.count(8);
                    for (int i = 0; i < drops; i++) {
                        double angle = random.nextDouble() * Mth.TWO_PI;
                        droplet(m.x, m.y, m.z, Math.cos(angle) * 0.15, 0.1 + random.nextDouble() * 0.2, Math.sin(angle) * 0.15, random);
                    }
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + (this.snaps - 1) * this.interval + 8;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            float yaw = Mth.rotLerp(c.partial, entity.yRotO, entity.getYRot());
            float warn = envelope(t, 3.0F, this.windup + (this.snaps - 1) * this.interval, 4.0F);
            double start = Math.toRadians(yaw + 90.0F - this.half);
            double sweep = Math.toRadians(this.half * 2.0F);
            c.strip(A, true, CHEVRON, VENOM, 0.45F * warn).scroll(-t * 3.0).arc(pos.x, pos.y, pos.z, 0.6, this.reach, start, sweep, true);
            c.strip(A, true, SCALES, VENOM, 0.8F * warn).arc(pos.x, pos.y, pos.z, this.reach - 0.45, this.reach, start, sweep, true);
            Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
            Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
            for (int s = 0; s < this.snaps; s++) {
                float local = t - (this.windup + s * this.interval - 7.0F);
                if (local < 0.0F || local > 10.0F) {
                    continue;
                }
                float open;
                float alpha;
                if (local < 5.0F) {
                    open = local / 5.0F;
                } else if (local < 6.0F) {
                    open = 1.0F;
                } else if (local < 7.0F) {
                    open = 1.0F - (local - 6.0F);
                } else {
                    open = 0.0F;
                }
                if (local < 7.0F) {
                    alpha = Math.min(1.0F, local / 2.0F);
                } else {
                    alpha = 1.0F - (local - 7.0F) / 3.0F;
                }
                Vec3 m = this.mouth(entity, c.partial, (1.0 - open) * 0.6);
                this.jaw(c, m, side, open, 0.0, alpha);
                this.jaw(c, m, side, open, Math.PI, alpha);
            }
        }

        private void jaw(VfxCanvas c, Vec3 m, Vec3 side, float open, double from, float alpha) {
            int steps = 12;
            Vec3[] outer = new Vec3[steps + 1];
            Vec3[] inner = new Vec3[steps + 1];
            double lift = 0.05;
            if (from > 0.0) {
                lift = -0.05;
            }
            for (int i = 0; i <= steps; i++) {
                double theta = from + Math.PI * i / steps;
                double cos = Math.cos(theta);
                double sin = Math.sin(theta) * (0.15 + 0.85 * open);
                outer[i] = m.add(side.scale(JAW * cos)).add(0.0, JAW * sin + lift, 0.0);
                inner[i] = m.add(side.scale((JAW - TEETH) * cos)).add(0.0, (JAW - TEETH) * sin + lift, 0.0);
            }
            c.strip(A, true, DRIP, IVORY, 0.95F * alpha).band(outer, inner);
            c.strip(A, true, SCALES, SCALE, 0.5F * alpha).density(40.0).band(outer, inner);
        }
    }
}
