package com.twilightbossplus.client.vfx;

import com.twilightbossplus.minoshroom.MinoFx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MinoVfx {
    private static final VfxAtlas A = VfxAtlas.MINO;
    private static final int FRACTURE = 0;
    private static final int DUST_CREST = 1;
    private static final int HOOF = 2;
    private static final int TENDRIL = 3;
    private static final int CRESCENT = 4;
    private static final int STAR_CRACK = 0;
    private static final int CAP = 1;
    private static final int WEB = 2;
    private static final int MAZE = 3;
    private static final int DUST = 0;
    private static final int SPORE = 1;
    private static final int ROCK = 2;
    private static final int IMPACT = 3;
    private static final int DIZZY = 4;
    private static final int SPARK = 5;

    private static final int EARTH = 0xD9A55B;
    private static final int SAND = 0xE6D2AC;
    private static final int CRACK_DARK = 0x2A2018;
    private static final int DANGER = 0xFF4A3A;
    private static final int WARNING = 0xFFB03A;
    private static final int RAGE = 0xFF3355;
    private static final int SPORE_RED = 0xE0503C;
    private static final int MYCELIUM = 0xB58CE6;
    private static final int AXE = 0x9FF4FF;

    private MinoVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        RandomSource random = level.getRandom();
        switch (type) {
            case MinoFx.WARN -> ClientVfx.add(new Warn(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color")));
            case MinoFx.TRACK -> ClientVfx.add(new Track(ClientVfx.vec(d, "a"), ClientVfx.vec(d, "b"), d.getFloat("w"), d.getInt("d"), d.getInt("style")));
            case MinoFx.POP -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                ClientVfx.add(new Crack(level, c, radius * 1.3F, 30));
                rocks(level, c, radius, ClientVfx.count(4), 0.45);
                dusts(level, c, radius, ClientVfx.count(3));
            }
            case MinoFx.SLAM -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                ClientVfx.add(new Crack(level, c, radius * 1.6F, 50));
                ClientVfx.add(new Slam(level, c, radius));
                rocks(level, c, radius, ClientVfx.count(6 + (int) (radius * 2.0F)), 0.7);
                int amount = ClientVfx.count(10 + (int) (radius * 2.0F));
                for (int i = 0; i < amount; i++) {
                    double angle = i * Mth.TWO_PI / amount + random.nextDouble() * 0.3;
                    double speed = 0.2 + random.nextDouble() * 0.12;
                    dust(c.x + Math.cos(angle) * radius * 0.5, c.y + 0.3, c.z + Math.sin(angle) * radius * 0.5, Math.cos(angle) * speed, 0.02, Math.sin(angle) * speed, 0.55, random);
                }
            }
            case MinoFx.DUST -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                dusts(level, c, radius, ClientVfx.count(3));
                rocks(level, c, radius, 1, 0.3);
            }
            case MinoFx.RUBBLE -> ClientVfx.add(new Rubble(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case MinoFx.SPORE_CLOUD -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                ClientVfx.add(new SporeCloud(level, c, radius, d.getInt("d")));
                int amount = ClientVfx.count(10);
                for (int i = 0; i < amount; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double speed = 0.1 + random.nextDouble() * 0.12;
                    spore(c.x, c.y + 0.8, c.z, Math.cos(angle) * speed, 0.05 + random.nextDouble() * 0.08, Math.sin(angle) * speed, 0.45, random);
                }
            }
            case MinoFx.SPORE_CAP -> ClientVfx.add(new SporeCap(d.getInt("id"), d.getFloat("h"), d.getFloat("s"), d.getInt("d")));
            case MinoFx.WHIRL -> ClientVfx.add(new Whirl(d.getInt("id"), d.getFloat("r"), d.getInt("windup"), d.getInt("d"), d.getInt("pulse")));
            case MinoFx.SNARE -> ClientVfx.add(new Snare(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case MinoFx.ROOT -> ClientVfx.add(new Root(d.getInt("id"), d.getInt("d")));
            case MinoFx.SPROUT -> ClientVfx.add(new Sprout(level, ClientVfx.vec(d, "c"), d.getFloat("r")));
            case MinoFx.STUN -> ClientVfx.add(new Stun(d.getInt("id"), d.getFloat("h"), d.getInt("d")));
            case MinoFx.AXE_CHARGE -> ClientVfx.add(new AxeCharge(d.getInt("id"), d.getFloat("h"), d.getInt("d")));
            case MinoFx.STOMP_WAVE -> ClientVfx.add(new StompWave(level, ClientVfx.vec(d, "c"), d.getFloat("speed"), d.getFloat("max"), d.getFloat("th"), d.getInt("delay"), d.getInt("color")));
            case MinoFx.AURA -> {
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
            case MinoFx.CLEAVE -> ClientVfx.add(new Cleave(d.getInt("id"), d.getFloat("reach"), d.getFloat("half"), d.getInt("windup"), d.getInt("style")));
            default -> {
            }
        }
    }

    private static void rock(double x, double y, double z, double vx, double vy, double vz, double size, RandomSource random) {
        ClientVfx.spawn(new Mote(A, ROCK, false).at(x, y, z).vel(vx, vy, vz).gravity(0.06).drag(0.98).size(size)
                .frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.5).collide().life(34 + random.nextInt(16)));
    }

    private static void rocks(ClientLevel level, Vec3 c, double radius, int amount, double up) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * radius;
            double x = c.x + Math.cos(angle) * dist;
            double z = c.z + Math.sin(angle) * dist;
            double out = 0.05 + random.nextDouble() * 0.15;
            rock(x, VfxCanvas.ground(level, x, z, c.y) + 0.2, z, Math.cos(angle) * out, up * (0.6 + random.nextDouble() * 0.6), Math.sin(angle) * out, 0.14 + random.nextDouble() * 0.14, random);
        }
    }

    private static void dust(double x, double y, double z, double vx, double vy, double vz, double size, RandomSource random) {
        ClientVfx.spawn(new Mote(A, DUST, false).at(x, y, z).vel(vx, vy, vz).gravity(-0.002).drag(0.9).size(size * (0.8 + random.nextDouble() * 0.4))
                .grow(0.7).spin(random.nextDouble() * Mth.TWO_PI, 0.0).animate().life(18 + random.nextInt(8)));
    }

    private static void dusts(ClientLevel level, Vec3 c, double radius, int amount) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            dust(c.x + Math.cos(angle) * radius * 0.5, c.y + 0.25, c.z + Math.sin(angle) * radius * 0.5, Math.cos(angle) * 0.08, 0.04, Math.sin(angle) * 0.08, 0.45, random);
        }
    }

    private static void spore(double x, double y, double z, double vx, double vy, double vz, double size, RandomSource random) {
        ClientVfx.spawn(new Mote(A, SPORE, false).at(x, y, z).vel(vx, vy, vz).gravity(-0.002).drag(0.92).size(size * (0.8 + random.nextDouble() * 0.4))
                .grow(0.5).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.1).animate().life(22 + random.nextInt(10)));
    }

    private static void spark(double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
        ClientVfx.spawn(new Mote(A, SPARK, true).at(x, y, z).vel(vx, vy, vz).gravity(0.01).drag(0.9).size(0.22 + random.nextDouble() * 0.12)
                .spin(Math.atan2(vy, Math.sqrt(vx * vx + vz * vz)), 0.0).color(AXE).animate().life(9 + random.nextInt(4)));
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
            return this.age < this.duration + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 3.0F, this.duration, 6.0F);
            c.decal(A, true, MAZE, this.patch, 0.0, 0.15 + 0.85 * ease(p), 0.02, this.color, (0.35F + 0.45F * p) * a);
            double jitter = Math.floor(t / 3.0F) * 17.0;
            c.strip(A, true, FRACTURE, this.color, 0.95F * a).scroll(jitter).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius - 0.5), this.radius, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, FRACTURE, 0xFFFFFF, 0.4F * a).scroll(jitter + 40.0).lift(0.01).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius - 0.35), this.radius - 0.05, 0.0, Mth.TWO_PI, true);
            if (t > this.duration) {
                float k = (t - this.duration) / 6.0F;
                c.decal(A, true, MAZE, this.patch, 0.0, 1.0, 0.03, 0xFFFFFF, 0.6F * (1.0F - k));
            }
        }
    }

    private static final class Track extends VfxEffect {
        private final Vec3 a;
        private final Vec3 b;
        private final float width;
        private final int duration;
        private final int style;
        private final Vec3[] full;
        private final Vec3[] left;
        private final Vec3[] right;

        Track(Vec3 a, Vec3 b, float width, int duration, int style) {
            this.a = a;
            this.b = b;
            this.width = width;
            this.duration = Math.max(1, duration);
            this.style = style;
            this.full = VfxCanvas.line(a, b, 1.0);
            Vec3 dir = b.subtract(a);
            Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
            if (side.lengthSqr() < 1.0E-6) {
                side = new Vec3(1.0, 0.0, 0.0);
            }
            side = side.normalize().scale(width * 0.5);
            this.left = VfxCanvas.line(a.add(side), b.add(side), 1.0);
            this.right = VfxCanvas.line(a.subtract(side), b.subtract(side), 1.0);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 8;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float alpha = envelope(t, 3.0F, this.duration, 8.0F);
            if (this.style == MinoFx.TRACK_HOOF) {
                c.strip(A, true, HOOF, WARNING, 0.35F * alpha).density(20.0).path(this.full, this.width * 0.7, true);
                if (p > 0.02F) {
                    c.strip(A, true, HOOF, WARNING, 0.9F * alpha).density(20.0).lift(0.01).fade(1.0F, 0.5F).path(VfxCanvas.line(this.a, this.a.lerp(this.b, p), 1.0), this.width * 0.7, true);
                }
                c.strip(A, true, FRACTURE, WARNING, 0.7F * alpha).density(24.0).path(this.left, 0.35, true);
                c.strip(A, true, FRACTURE, WARNING, 0.7F * alpha).density(24.0).scroll(50.0).path(this.right, 0.35, true);
                return;
            }
            c.strip(A, true, FRACTURE, DANGER, 0.3F * alpha).density(12.0).path(this.full, this.width, true);
            if (p > 0.02F) {
                Vec3[] crack = VfxCanvas.line(this.a, this.a.lerp(this.b, p), 1.0);
                c.strip(A, false, FRACTURE, VfxCanvas.lit(c.level, this.a, CRACK_DARK), 0.95F * alpha).density(12.0).lift(0.01).path(crack, this.width * 0.6, true);
                c.strip(A, true, FRACTURE, DANGER, 0.9F * alpha).density(12.0).lift(0.02).fade(0.6F, 1.0F).path(crack, this.width * 0.6, true);
            }
        }
    }

    private static final class Crack extends VfxEffect {
        private final VfxCanvas.Patch patch;
        private final int life;
        private final double rotation;
        private final int tint;

        Crack(ClientLevel level, Vec3 c, float radius, int life) {
            this.patch = VfxCanvas.patch(level, c, radius);
            this.life = life;
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, CRACK_DARK);
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            double scale = 0.35 + 0.65 * ease(t / 3.0F);
            c.decal(A, false, STAR_CRACK, this.patch, this.rotation, scale, 0.02, this.tint, 0.9F * envelope(t, 1.0F, this.life * 0.55F, this.life * 0.45F));
            c.decal(A, true, STAR_CRACK, this.patch, this.rotation, scale, 0.03, EARTH, 0.7F * (1.0F - t / this.life));
        }
    }

    private static final class Slam extends VfxEffect {
        private static final int LIFE = 12;
        private final Vec3 c;
        private final float radius;
        private final int tint;

        Slam(ClientLevel level, Vec3 c, float radius) {
            this.c = c;
            this.radius = radius;
            this.tint = VfxCanvas.lit(level, c, SAND);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = t / LIFE;
            int frame = Mth.clamp((int) (k * 4.0F), 0, 3);
            c.sprite(A, true, IMPACT, frame, this.c.x, this.c.y + 0.7, this.c.z, Math.max(0.8, this.radius * 0.7) * (0.8 + k * 0.5), 0.0, 0xFFFFFF, 1.0F - k * 0.6F);
            c.strip(A, false, DUST_CREST, this.tint, 0.95F * (1.0F - k)).density(18.0).scroll(t * 4.0).wall(this.c.x, this.c.y, this.c.z, this.radius * (0.4 + ease(k) * 0.9), 1.4 * (1.0F - k) + 0.2, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, FRACTURE, EARTH, 0.85F * (1.0F - k)).arc(this.c.x, this.c.y, this.c.z, this.radius * (0.3 + k) - 0.5, this.radius * (0.3 + k), 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Rubble extends VfxEffect {
        private static final int FALL = 12;
        private final Vec3 c;
        private final float radius;
        private final int fuse;
        private final double spin;

        Rubble(ClientLevel level, Vec3 c, float radius, int fuse) {
            this.c = c;
            this.radius = radius;
            this.fuse = fuse;
            this.spin = (level.getRandom().nextDouble() - 0.5) * 0.6;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age == this.fuse) {
                ClientVfx.add(new Crack(level, this.c, this.radius * 1.4F, 34));
                rocks(level, this.c, this.radius, ClientVfx.count(6), 0.5);
                dusts(level, this.c, this.radius * 1.4, ClientVfx.count(5));
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age <= this.fuse;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.fuse, 0.0F, 1.0F);
            float a = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
            c.strip(A, true, FRACTURE, DANGER, 0.85F * a).scroll(Math.floor(t / 3.0F) * 23.0).arc(this.c.x, this.c.y, this.c.z, this.radius - 0.35, this.radius, 0.0, Mth.TWO_PI, true);
            double gy = VfxCanvas.ground(c.level, this.c.x, this.c.z, this.c.y + 0.5) + 0.02;
            c.flatSprite(A, false, ROCK, 1, this.c.x, gy, this.c.z, this.radius * (0.3 + 0.7 * p), t * 0.02, 0x000000, 0.45F * a);
            float start = this.fuse - FALL;
            if (t >= start) {
                float k = (t - start) / FALL;
                double y = this.c.y + 0.6 + 11.0 * (1.0 - k * k);
                c.sprite(A, false, ROCK, 1, this.c.x, y, this.c.z, 0.85, t * this.spin, VfxCanvas.lit(c.level, this.c, 0xFFFFFF), 1.0F);
                c.sprite(A, false, DUST, 3, this.c.x, y + 0.9, this.c.z, 0.6, 0.0, VfxCanvas.lit(c.level, this.c, 0xFFFFFF), 0.6F);
            }
        }
    }

    private static final class SporeCloud extends VfxEffect {
        private static final int PUFFS = 9;
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final VfxCanvas.Patch patch;
        private final double phase;
        private final int tint;

        SporeCloud(ClientLevel level, Vec3 c, float radius, int duration) {
            this.c = c;
            this.radius = radius;
            this.duration = duration;
            this.patch = VfxCanvas.patch(level, c, radius * 1.15);
            this.phase = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, 0xFFFFFF);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age % 4 == 0 && this.age < this.duration - 10) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                double dist = Math.sqrt(random.nextDouble()) * this.radius;
                ClientVfx.spawn(new Mote(A, SPORE, true).at(this.c.x + Math.cos(angle) * dist, this.c.y + 0.2, this.c.z + Math.sin(angle) * dist)
                        .vel(0.0, 0.035, 0.0).drag(0.98).size(0.1 + random.nextDouble() * 0.06).frame(0).color(0xFF9A80).life(30));
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
            float a = envelope(t, 6.0F, this.duration - 16.0F, 16.0F);
            c.decal(A, false, WEB, this.patch, this.phase, Math.min(1.0, 0.3 + t / 10.0), 0.015, VfxCanvas.lit(c.level, this.c, MYCELIUM), 0.7F * a);
            for (int i = 0; i < PUFFS; i++) {
                double angle = this.phase + i * Mth.TWO_PI / PUFFS + t * 0.01 * (1 + i % 2);
                double dist = this.radius * (0.35 + 0.45 * ((i * 37) % 10) / 10.0);
                double y = this.c.y + 0.5 + ((i * 53) % 10) / 10.0 * 1.3 + Math.sin(t * 0.08 + i) * 0.15;
                double size = 0.75 + ((i * 29) % 10) / 10.0 * 0.45;
                int frame = 1 + (int) ((t * 0.05 + i) % 2.0);
                c.sprite(A, false, SPORE, frame, this.c.x + Math.cos(angle) * dist, y, this.c.z + Math.sin(angle) * dist, size, t * 0.01 + i, this.tint, 0.85F * a);
            }
            c.sprite(A, false, SPORE, 2, this.c.x, this.c.y + 1.1, this.c.z, 1.3, -t * 0.008, this.tint, 0.8F * a);
        }
    }

    private static final class SporeCap extends VfxEffect {
        private final int id;
        private final float height;
        private final float size;
        private final int duration;

        SporeCap(int id, float height, float size, int duration) {
            this.id = id;
            this.height = height;
            this.size = size;
            this.duration = Math.max(1, duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            if (this.age == this.duration) {
                RandomSource random = level.getRandom();
                Vec3 o = entity.position().add(0.0, this.height, 0.0);
                int amount = ClientVfx.count(16);
                for (int i = 0; i < amount; i++) {
                    double angle = i * Mth.TWO_PI / amount;
                    double speed = 0.25 + random.nextDouble() * 0.15;
                    spore(o.x, o.y, o.z, Math.cos(angle) * speed, 0.05 + random.nextDouble() * 0.12, Math.sin(angle) * speed, 0.5, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 3;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double wobble = 1.0 + 0.08 * Math.sin(t * 1.3);
            double radius = this.size * (0.6 + 2.0 * ease(p)) * wobble;
            float a = 1.0F;
            if (t > this.duration) {
                a = 1.0F - (t - this.duration) / 3.0F;
                radius *= 1.0 + (t - this.duration) * 0.25;
            }
            int tint = VfxCanvas.lit(c.level, pos, 0xFFFFFF);
            double y = pos.y + this.height + p * 0.4;
            c.flatDecal(A, false, CAP, pos.x, y, pos.z, radius, t * 0.03, tint, a);
            c.flatDecal(A, true, CAP, pos.x, y + 0.01, pos.z, radius, t * 0.03, SPORE_RED, 0.25F * p * a);
        }
    }

    private static final class Whirl extends VfxEffect {
        private final int id;
        private final float radius;
        private final int windup;
        private final int duration;
        private final int pulse;

        Whirl(int id, float radius, int windup, int duration, int pulse) {
            this.id = id;
            this.radius = radius;
            this.windup = windup;
            this.duration = duration;
            this.pulse = Math.max(1, pulse);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            if (this.age > this.windup) {
                RandomSource random = level.getRandom();
                double angle = Math.toRadians(entity.getYRot() + 90.0F);
                Vec3 tip = entity.position().add(Math.cos(angle) * this.radius * 0.9, 1.2, Math.sin(angle) * this.radius * 0.9);
                spark(tip.x, tip.y, tip.z, -Math.sin(angle) * 0.2, 0.05, Math.cos(angle) * 0.2, random);
                if (this.age % 3 == 0) {
                    double a2 = random.nextDouble() * Mth.TWO_PI;
                    dust(entity.getX() + Math.cos(a2) * this.radius * 0.7, entity.getY() + 0.2, entity.getZ() + Math.sin(a2) * this.radius * 0.7,
                            -Math.sin(a2) * 0.18, 0.05, Math.cos(a2) * 0.18, 0.5, random);
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
            float a = envelope(t, 3.0F, this.duration - 5.0F, 5.0F);
            if (t < this.windup) {
                float p = t / this.windup;
                c.strip(A, true, FRACTURE, DANGER, 0.9F * a).scroll(Math.floor(t / 2.0F) * 19.0).arc(pos.x, pos.y, pos.z, this.radius - 0.45, this.radius, 0.0, Mth.TWO_PI, true);
                c.strip(A, true, FRACTURE, DANGER, 0.4F * a).lift(0.01).arc(pos.x, pos.y, pos.z, this.radius * (1.0 - p) - 0.3, this.radius * (1.0 - p) + 0.01, 0.0, Mth.TWO_PI, true);
                return;
            }
            int tint = VfxCanvas.lit(c.level, pos, SAND);
            for (int k = 0; k < 3; k++) {
                double r = this.radius * (0.55 + k * 0.15);
                c.strip(A, false, DUST_CREST, tint, 0.75F * a).density(14.0).scroll(t * (18.0 + k * 6.0)).edges(0.2F, 1.0F).wall(pos.x, pos.y + k * 0.65, pos.z, r, 0.8, 0.0, Mth.TWO_PI, k == 0);
            }
            float yaw = Mth.rotLerp(c.partial, entity.yRotO, entity.getYRot());
            double head = Math.toRadians(yaw + 90.0F);
            double tail = 2.6;
            double dens = 128.0 / (this.radius * 0.65 * tail);
            c.strip(A, true, CRESCENT, AXE, 0.95F * a).density(dens).arc(pos.x, pos.y + 1.2, pos.z, this.radius * 0.3, this.radius, head, -tail, false);
            c.strip(A, true, CRESCENT, 0xFFFFFF, 0.6F * a).density(dens).arc(pos.x, pos.y + 1.22, pos.z, this.radius * 0.75, this.radius, head, -tail * 0.7, false);
            float since = (t - this.windup) % this.pulse;
            if (since < 4.0F) {
                float k = since / 4.0F;
                double r = this.radius * (0.6 + k * 0.5);
                c.strip(A, true, FRACTURE, EARTH, 0.9F * (1.0F - k) * a).arc(pos.x, pos.y, pos.z, r - 0.4, r, 0.0, Mth.TWO_PI, true);
            }
        }
    }

    private static final class Snare extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final VfxCanvas.Patch patch;

        Snare(ClientLevel level, Vec3 c, float radius, int duration) {
            this.c = c;
            this.radius = radius;
            this.duration = Math.max(1, duration);
            this.patch = VfxCanvas.patch(level, c, radius * 1.4);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age % 3 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                ClientVfx.spawn(new Mote(A, SPORE, true).at(this.c.x + Math.cos(angle) * this.radius, this.c.y + 0.1, this.c.z + Math.sin(angle) * this.radius)
                        .vel(-Math.cos(angle) * 0.05, 0.04, -Math.sin(angle) * 0.05).drag(0.97).size(0.12).frame(0).color(MYCELIUM).life(20));
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 4;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 3.0F, this.duration, 4.0F);
            c.decal(A, true, WEB, this.patch, t * 0.01, 0.3 + 0.7 * ease(p), 0.02, MYCELIUM, 0.85F * a);
            c.strip(A, true, TENDRIL, MYCELIUM, 0.9F * a).density(22.0).scroll(t * 2.0).arc(this.c.x, this.c.y, this.c.z, this.radius - 0.4, this.radius + 0.1, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Root extends VfxEffect {
        private static final int VINES = 5;
        private final int id;
        private final int duration;

        Root(int id, int duration) {
            this.id = id;
            this.duration = Math.max(1, duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (level.getEntity(this.id) == null) {
                return false;
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
            float rise = ease(t / 5.0F);
            float a = Mth.clamp((this.duration - t) / 8.0F, 0.0F, 1.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double gy = VfxCanvas.ground(c.level, pos.x, pos.z, pos.y) - 0.04;
            double r = Math.max(0.45, entity.getBbWidth() * 0.75);
            double h = Math.min(1.6, entity.getBbHeight() * 0.8) * rise;
            for (int k = 0; k < 3; k++) {
                double y = gy + h * k / 3.0;
                c.strip(A, true, TENDRIL, MYCELIUM, 0.9F * a).density(30.0).scroll(k * 31.0 + t * 1.5).wall(pos.x, y, pos.z, r * (1.0 - k * 0.12), 0.45, k * 1.3, Mth.TWO_PI, false);
            }
            for (int i = 0; i < VINES; i++) {
                double angle = i * Mth.TWO_PI / VINES + 0.4;
                Vec3[] vine = new Vec3[7];
                for (int s = 0; s < vine.length; s++) {
                    double f = s / (double) (vine.length - 1);
                    double twist = angle + f * 2.2;
                    double rr = r * (1.25 - f * 0.45);
                    vine[s] = new Vec3(pos.x + Math.cos(twist) * rr, gy + h * 1.1 * f, pos.z + Math.sin(twist) * rr);
                }
                c.strip(A, true, TENDRIL, 0xD9C2FF, 0.85F * a).density(26.0).fade(1.0F, 0.3F).path(vine, 0.4, false);
            }
        }
    }

    private static final class Sprout extends VfxEffect {
        private static final int LIFE = 16;
        private static final int ARMS = 8;
        private final Vec3 c;
        private final float radius;
        private final VfxCanvas.Patch patch;

        Sprout(ClientLevel level, Vec3 c, float radius) {
            this.c = c;
            this.radius = radius;
            this.patch = VfxCanvas.patch(level, c, radius * 1.8);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = t / LIFE;
            float grow = ease(t / 5.0F);
            float a = 1.0F - Mth.clamp((t - 8.0F) / 8.0F, 0.0F, 1.0F);
            c.decal(A, true, WEB, this.patch, 0.3, 0.6 + 0.4 * grow, 0.03, 0xFFFFFF, 0.6F * (1.0F - k));
            for (int i = 0; i < ARMS; i++) {
                double angle = i * Mth.TWO_PI / ARMS + 0.2;
                Vec3[] arm = new Vec3[6];
                for (int s = 0; s < arm.length; s++) {
                    double f = s / (double) (arm.length - 1) * grow;
                    double r = this.radius * 1.6 * f;
                    double bend = angle + f * 0.8;
                    arm[s] = new Vec3(this.c.x + Math.cos(bend) * r, this.c.y + Math.sin(f * Math.PI) * 1.6, this.c.z + Math.sin(bend) * r);
                }
                c.strip(A, true, TENDRIL, MYCELIUM, 0.95F * a).density(24.0).fade(1.0F, 0.4F).path(arm, 0.45, false);
            }
        }
    }

    private static final class Stun extends VfxEffect {
        private final int id;
        private final float height;
        private final int duration;

        Stun(int id, float height, int duration) {
            this.id = id;
            this.height = height;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (level.getEntity(this.id) == null) {
                return false;
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
            float a = envelope(t, 3.0F, this.duration - 4.0F, 4.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double y = pos.y + this.height;
            for (int k = 0; k < 4; k++) {
                double angle = t * 0.22 + k * Mth.TWO_PI / 4.0;
                double bob = Math.sin(t * 0.3 + k) * 0.12;
                c.sprite(A, true, DIZZY, (int) (t / 2.0F + k) % 4, pos.x + Math.cos(angle) * 0.8, y + bob, pos.z + Math.sin(angle) * 0.8, 0.32, 0.0, 0xFFFFFF, a);
            }
        }
    }

    private static final class AxeCharge extends VfxEffect {
        private final int id;
        private final float height;
        private final int duration;

        AxeCharge(int id, float height, int duration) {
            this.id = id;
            this.height = height;
            this.duration = Math.max(1, duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            RandomSource random = level.getRandom();
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = 1.8 + random.nextDouble() * 0.6;
            Vec3 o = entity.position().add(0.0, this.height, 0.0);
            Vec3 from = o.add(Math.cos(angle) * r, (random.nextDouble() - 0.3) * 1.5, Math.sin(angle) * r);
            Vec3 v = o.subtract(from).scale(0.1);
            spark(from.x, from.y, from.z, v.x, v.y, v.z, random);
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
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double y = pos.y + this.height;
            float flicker = 0.85F + 0.15F * Mth.sin(t * 1.4F);
            c.sprite(A, true, IMPACT, 1, pos.x, y, pos.z, (0.2 + 0.6 * p) * flicker, t * 0.15, AXE, 0.9F);
            c.sprite(A, true, SPARK, 0, pos.x, y, pos.z, 0.5 + 0.5 * p, t * 0.4, 0xFFFFFF, 0.8F * p);
        }
    }

    private static final class StompWave extends VfxEffect {
        private final Vec3 c;
        private final float speed;
        private final float max;
        private final float thickness;
        private final int delay;
        private final int color;
        private final int tint;

        StompWave(ClientLevel level, Vec3 c, float speed, float max, float thickness, int delay, int color) {
            this.c = c;
            this.speed = speed;
            this.max = max;
            this.thickness = thickness;
            this.delay = delay;
            this.color = color;
            this.tint = VfxCanvas.lit(level, c, SAND);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            int active = this.age - this.delay;
            if (active > 0 && active % 2 == 0) {
                RandomSource random = level.getRandom();
                double r = active * this.speed;
                double angle = random.nextDouble() * Mth.TWO_PI;
                double x = this.c.x + Math.cos(angle) * r;
                double z = this.c.z + Math.sin(angle) * r;
                double gy = VfxCanvas.ground(level, x, z, this.c.y);
                if (random.nextBoolean()) {
                    rock(x, gy + 0.3, z, Math.cos(angle) * 0.08, 0.25 + random.nextDouble() * 0.15, Math.sin(angle) * 0.08, 0.14 + random.nextDouble() * 0.1, random);
                } else {
                    dust(x, gy + 0.3, z, Math.cos(angle) * 0.06, 0.03, Math.sin(angle) * 0.06, 0.5, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return (this.age - this.delay) * this.speed < this.max + this.thickness;
        }

        @Override
        public void render(VfxCanvas c) {
            float active = this.time(c) - this.delay;
            if (active <= 0.0F) {
                return;
            }
            double r = active * this.speed;
            float f = 0.35F + 0.65F * Mth.clamp(1.0F - (float) (r / (this.max + this.thickness)), 0.0F, 1.0F);
            double outer = r + this.thickness * 0.5;
            c.strip(A, false, DUST_CREST, this.tint, 0.95F * f).density(18.0).scroll(active * 5.0).wall(this.c.x, this.c.y, this.c.z, outer, 0.85, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, FRACTURE, this.color, 0.9F * f).scroll(Math.floor(active / 2.0F) * 29.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, outer - this.thickness), outer, 0.0, Mth.TWO_PI, true);
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
            if (this.age % 14 == 0) {
                RandomSource random = level.getRandom();
                Vec3 forward = Vec3.directionFromRotation(0.0F, entity.getYRot());
                Vec3 nose = entity.position().add(forward.scale(0.9)).add(0.0, entity.getBbHeight() * 0.78, 0.0);
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 right = new Vec3(-forward.z, 0.0, forward.x).scale(0.18 * side);
                    ClientVfx.spawn(new Mote(A, DUST, false).at(nose.x + right.x, nose.y, nose.z + right.z).vel(forward.x * 0.12 + right.x * 0.3, -0.03, forward.z * 0.12 + right.z * 0.3)
                            .drag(0.88).size(0.22).grow(1.2).color(0xFFD0C0).animate().life(14));
                }
                double angle = random.nextDouble() * Mth.TWO_PI;
                dust(entity.getX() + Math.cos(angle) * 0.8, entity.getY() + 0.1, entity.getZ() + Math.sin(angle) * 0.8, Math.cos(angle) * 0.1, 0.02, Math.sin(angle) * 0.1, 0.4, random);
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
            c.strip(A, true, FRACTURE, RAGE, 0.75F * a).scroll(Math.floor(t / 3.0F) * 21.0).arc(pos.x, pos.y, pos.z, 1.3, 1.8, t * 0.05, Mth.TWO_PI, true);
            c.strip(A, true, DUST_CREST, RAGE, 0.35F * a).density(16.0).scroll(t * 6.0).edges(0.0F, 1.0F).wall(pos.x, pos.y, pos.z, 1.6, 0.9, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Cleave extends VfxEffect {
        private static final int LIFE = 8;
        private final int id;
        private final float reach;
        private final float half;
        private final int windup;
        private final int style;

        Cleave(int id, float reach, float half, int windup, int style) {
            this.id = id;
            this.reach = reach;
            this.half = half;
            this.windup = Math.max(1, windup);
            this.style = style;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            if (this.age == this.windup) {
                RandomSource random = level.getRandom();
                float yaw = entity.getYRot();
                int amount = ClientVfx.count(6);
                for (int i = 0; i < amount; i++) {
                    double angle = Math.toRadians(yaw + 90.0F + (random.nextFloat() * 2.0F - 1.0F) * this.half);
                    double dist = this.reach * (0.5 + random.nextDouble() * 0.5);
                    spark(entity.getX() + Math.cos(angle) * dist, entity.getY() + 1.0, entity.getZ() + Math.sin(angle) * dist,
                            Math.cos(angle) * 0.2, 0.1, Math.sin(angle) * 0.2, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + LIFE;
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
            if (t < this.windup) {
                float p = t / this.windup;
                c.strip(A, true, FRACTURE, DANGER, 0.9F).scroll(Math.floor(t / 2.0F) * 19.0).arc(pos.x, pos.y, pos.z, this.reach - 0.4, this.reach, start, sweep, true);
                c.strip(A, true, CRESCENT, DANGER, 0.3F).density(128.0 / (this.reach * 0.6 * sweep)).lift(0.01).arc(pos.x, pos.y, pos.z, 0.6, 0.6 + (this.reach - 0.6) * p, start, sweep, true);
                return;
            }
            float k = (t - this.windup) / LIFE;
            float reveal = ease(k * 3.0F);
            float fade = Mth.clamp(1.0F - (k - 0.3F) / 0.7F, 0.0F, 1.0F);
            if (this.style == MinoFx.CLEAVE_CHOP) {
                Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
                Vec3 pivot = pos.add(0.0, 1.0, 0.0);
                int steps = 14;
                Vec3[] outer = new Vec3[steps + 1];
                Vec3[] inner = new Vec3[steps + 1];
                double from = Math.toRadians(110.0);
                double to = Math.toRadians(-25.0);
                for (int i = 0; i <= steps; i++) {
                    double theta = from + (to - from) * reveal * i / steps;
                    Vec3 dir = forward.scale(Math.cos(theta)).add(0.0, Math.sin(theta), 0.0);
                    outer[steps - i] = pivot.add(dir.scale(this.reach));
                    inner[steps - i] = pivot.add(dir.scale(this.reach * 0.35));
                }
                c.strip(A, true, CRESCENT, AXE, 0.95F * fade).density(128.0 / Math.max(0.5, this.reach * 2.4 * reveal)).band(outer, inner);
                c.strip(A, true, CRESCENT, 0xFFFFFF, 0.6F * fade).density(128.0 / Math.max(0.5, this.reach * 2.4 * reveal)).lift(0.02).band(outer, inner);
                return;
            }
            double head = start + sweep * reveal;
            double dens = 128.0 / Math.max(0.5, this.reach * 0.65 * sweep);
            c.strip(A, true, CRESCENT, AXE, 0.95F * fade).density(dens).arc(pos.x, pos.y + 1.2, pos.z, this.reach * 0.3, this.reach, head, -sweep * reveal, false);
            c.strip(A, true, CRESCENT, 0xFFFFFF, 0.6F * fade).density(dens).arc(pos.x, pos.y + 1.22, pos.z, this.reach * 0.7, this.reach, head, -sweep * reveal, false);
        }
    }
}
