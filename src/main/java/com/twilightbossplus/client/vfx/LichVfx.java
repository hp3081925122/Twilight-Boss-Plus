package com.twilightbossplus.client.vfx;

import com.twilightbossplus.lich.LichFx;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class LichVfx {
    private static final VfxAtlas A = VfxAtlas.LICH;
    private static final int RUNES = 0;
    private static final int BOLT = 1;
    private static final int CHAIN = 2;
    private static final int COMET = 3;
    private static final int HEXAGRAM = 0;
    private static final int PENTAGRAM = 1;
    private static final int RIFT = 2;
    private static final int SKULL = 3;
    private static final int STAR = 0;
    private static final int SOUL = 1;
    private static final int CRYSTAL = 2;
    private static final int WISP = 3;
    private static final int MINI_SKULL = 4;

    private static final int TWILIGHT = 0x8A6BFF;
    private static final int SOUL_FIRE = 0x5CFFE0;
    private static final int DRAIN = 0xFF3F6E;
    private static final int VOID = 0xB04CFF;
    private static final int DANGER = 0xFF4A3A;
    private static final int STARLIGHT = 0xDCE6FF;

    private LichVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case LichFx.SIGIL -> ClientVfx.add(new Sigil(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color"), d.getInt("kind")));
            case LichFx.STAR -> {
                Vec3 c = ClientVfx.vec(d, "c");
                ClientVfx.add(new Sigil(level, c, d.getFloat("r"), d.getInt("d"), TWILIGHT, LichFx.SIGIL_PENTAGRAM));
                ClientVfx.add(new FallingStar(c, d.getFloat("r"), d.getFloat("h"), d.getInt("d")));
            }
            case LichFx.BEAM -> ClientVfx.add(new Beam(ClientVfx.vec(d, "a"), ClientVfx.vec(d, "b"), d.getFloat("w"), d.getInt("windup"), d.getInt("d"), d.getInt("color")));
            case LichFx.CHAIN -> {
                int from = d.getInt("from");
                int to = d.getInt("to");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Chain chain && chain.from == from && chain.to == to) {
                        chain.cut = true;
                    }
                }
                if (d.getInt("d") > 0) {
                    ClientVfx.add(new Chain(from, to, d.getInt("d"), d.getInt("color")));
                }
            }
            case LichFx.ORB -> ClientVfx.add(new Orb(d.getInt("id"), d.getFloat("h"), d.getFloat("s"), d.getInt("d"), d.getInt("color"), d.getInt("style")));
            case LichFx.MARK -> ClientVfx.add(new Mark(d.getInt("id"), d.getFloat("r"), d.getInt("d")));
            case LichFx.BLAST -> blast(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("color"));
            case LichFx.FLAME -> ClientVfx.add(new Flame(ClientVfx.vec(d, "c"), d.getFloat("r"), d.getFloat("h"), d.getInt("d"), d.getInt("color")));
            case LichFx.PRISON -> ClientVfx.add(new Prison(ClientVfx.vec(d, "c"), d.getFloat("r"), d.getFloat("h"), d.getInt("d")));
            case LichFx.VORTEX -> ClientVfx.add(new Vortex(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case LichFx.SLASH -> ClientVfx.add(new Slash(d.getInt("id"), d.getFloat("reach"), d.getFloat("half"), d.getInt("windup")));
            case LichFx.BLINK -> {
                Vec3 a = ClientVfx.vec(d, "a");
                Vec3 b = ClientVfx.vec(d, "b");
                ClientVfx.add(new Blink(a, b));
                wisps(level, a, 1.0, ClientVfx.count(8), VOID);
                stars(level, b, 0.6, ClientVfx.count(5), STARLIGHT);
            }
            case LichFx.RUNE_WAVE -> ClientVfx.add(new RuneWave(ClientVfx.vec(d, "c"), d.getFloat("speed"), d.getFloat("max"), d.getFloat("th"), d.getInt("delay"), d.getInt("color")));
            default -> {
            }
        }
    }

    private static void blast(ClientLevel level, Vec3 c, float radius, int color) {
        ClientVfx.add(new Blast(c, radius, color));
        RandomSource random = level.getRandom();
        int crystals = ClientVfx.count(6 + (int) (radius * 2.0F));
        for (int i = 0; i < crystals; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double speed = 0.12 + random.nextDouble() * 0.2;
            ClientVfx.spawn(new Mote(A, CRYSTAL, false).at(c.x + Math.cos(angle) * 0.4, c.y + 0.6, c.z + Math.sin(angle) * 0.4)
                    .vel(Math.cos(angle) * speed, 0.3 + random.nextDouble() * 0.3, Math.sin(angle) * speed).gravity(0.04).drag(0.97)
                    .size(0.18 + random.nextDouble() * 0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.6)
                    .collide().life(34 + random.nextInt(14)));
        }
        int souls = ClientVfx.count(4 + (int) radius);
        for (int i = 0; i < souls; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * radius * 0.7;
            ClientVfx.spawn(new Mote(A, SOUL, true).at(c.x + Math.cos(angle) * dist, c.y + 0.3, c.z + Math.sin(angle) * dist)
                    .vel(0.0, 0.08 + random.nextDouble() * 0.08, 0.0).gravity(-0.004).drag(0.95).size(0.3 + random.nextDouble() * 0.2)
                    .color(color).animate().life(20 + random.nextInt(10)));
        }
    }

    private static void wisps(ClientLevel level, Vec3 c, double spread, int amount, int color) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < amount; i++) {
            ClientVfx.spawn(new Mote(A, WISP, true).at(c.x + (random.nextDouble() - 0.5) * spread, c.y + (random.nextDouble() - 0.5) * spread * 1.6, c.z + (random.nextDouble() - 0.5) * spread)
                    .vel((random.nextDouble() - 0.5) * 0.08, 0.03, (random.nextDouble() - 0.5) * 0.08).drag(0.92).size(0.18 + random.nextDouble() * 0.12)
                    .color(color).animate().life(14 + random.nextInt(8)));
        }
    }

    private static void stars(ClientLevel level, Vec3 c, double spread, int amount, int color) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < amount; i++) {
            ClientVfx.spawn(new Mote(A, STAR, true).at(c.x + (random.nextDouble() - 0.5) * spread, c.y + (random.nextDouble() - 0.5) * spread, c.z + (random.nextDouble() - 0.5) * spread)
                    .vel((random.nextDouble() - 0.5) * 0.1, 0.04 + random.nextDouble() * 0.06, (random.nextDouble() - 0.5) * 0.1).drag(0.9)
                    .size(0.25 + random.nextDouble() * 0.2).color(color).animate().life(10 + random.nextInt(6)));
        }
    }

    private static final class Sigil extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final int color;
        private final int kind;
        private final VfxCanvas.Patch patch;

        Sigil(ClientLevel level, Vec3 c, float radius, int duration, int color, int kind) {
            this.c = c;
            this.radius = radius;
            this.duration = Math.max(1, duration);
            this.color = color;
            this.kind = kind;
            this.patch = VfxCanvas.patch(level, c, radius);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.kind != LichFx.SIGIL_RIFT && this.age % 5 == 0 && this.age < this.duration) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                ClientVfx.spawn(new Mote(A, STAR, true).at(this.c.x + Math.cos(angle) * this.radius, this.c.y + 0.2, this.c.z + Math.sin(angle) * this.radius)
                        .vel(0.0, 0.06, 0.0).drag(0.96).size(0.22).color(this.color).animate().life(16));
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 4.0F, this.duration, 6.0F);
            if (this.kind == LichFx.SIGIL_RIFT) {
                c.decal(A, true, RIFT, this.patch, -t * 0.25, 0.5 + 0.5 * ease(p), 0.02, this.color, 0.9F * a);
                c.strip(A, true, RUNES, this.color, 0.8F * a).scroll(t * 3.0).arc(this.c.x, this.c.y, this.c.z, this.radius - 0.35, this.radius, 0.0, Mth.TWO_PI, true);
                return;
            }
            int decal = HEXAGRAM;
            if (this.kind == LichFx.SIGIL_PENTAGRAM) {
                decal = PENTAGRAM;
            } else if (this.kind == LichFx.SIGIL_SKULL) {
                decal = SKULL;
            }
            c.decal(A, true, decal, this.patch, -t * 0.02, 1.0, 0.02, this.color, (0.35F + 0.55F * p) * a);
            c.strip(A, true, RUNES, this.color, 0.9F * a).scroll(t * 1.5).lift(0.01).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius - 0.45), this.radius, 0.0, Mth.TWO_PI, true);
            double inner = this.radius * 0.55;
            if (inner > 0.6) {
                c.strip(A, true, RUNES, STARLIGHT, 0.55F * a).scroll(-t * 2.0).lift(0.015).arc(this.c.x, this.c.y, this.c.z, inner - 0.3, inner, 0.0, Mth.TWO_PI, true);
            }
            if (p > 0.6F) {
                float charge = (p - 0.6F) / 0.4F;
                c.strip(A, true, RUNES, this.color, 0.7F * charge * a).scroll(t * 2.5).edges(0.0F, 1.0F).wall(this.c.x, this.c.y, this.c.z, this.radius, 0.3 + charge * 1.4, 0.0, Mth.TWO_PI, true);
            }
            if (t > this.duration) {
                float k = (t - this.duration) / 6.0F;
                c.decal(A, true, decal, this.patch, -t * 0.02, 1.0 + k * 0.15, 0.03, 0xFFFFFF, 0.8F * (1.0F - k));
            }
        }
    }

    private static final class FallingStar extends VfxEffect {
        private static final int FALL = 10;
        private static final int AFTER = 14;
        private final Vec3 c;
        private final float radius;
        private final float height;
        private final int fuse;

        FallingStar(Vec3 c, float radius, float height, int fuse) {
            this.c = c;
            this.radius = radius;
            this.height = height;
            this.fuse = fuse;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age == this.fuse) {
                blast(level, this.c, this.radius, TWILIGHT);
                stars(level, this.c.add(0.0, 1.0, 0.0), this.radius, ClientVfx.count(6), STARLIGHT);
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.fuse + AFTER;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float start = this.fuse - FALL;
            if (t >= start && t < this.fuse) {
                float k = (t - start) / FALL;
                double y = this.c.y + 16.0 * (1.0 - k * k);
                Vec3 head = new Vec3(this.c.x, y, this.c.z);
                c.strip(A, true, COMET, TWILIGHT, 0.95F).density(128.0 / 6.0).beam(head, head.add(0.0, 6.0, 0.0), 1.1);
                c.strip(A, true, COMET, 0xFFFFFF, 0.8F).density(128.0 / 4.0).beam(head, head.add(0.0, 4.0, 0.0), 0.45);
                c.sprite(A, true, STAR, 1, head.x, head.y, head.z, 1.1, t * 0.3, STARLIGHT, 1.0F);
                c.sprite(A, true, WISP, 2, head.x, head.y, head.z, 0.8, -t * 0.2, TWILIGHT, 0.8F);
                return;
            }
            if (t >= this.fuse) {
                float k = (t - this.fuse) / AFTER;
                float fade = 1.0F - k;
                Vec3 base = this.c;
                c.strip(A, true, BOLT, TWILIGHT, 0.9F * fade).scroll(-t * 10.0).beam(base, base.add(0.0, this.height, 0.0), this.radius * 1.3 * (1.0 - k * 0.6));
                c.strip(A, true, BOLT, 0xFFFFFF, 0.8F * fade).scroll(-t * 14.0).beam(base, base.add(0.0, this.height * 1.1, 0.0), this.radius * 0.45);
                c.sprite(A, true, STAR, 1, base.x, base.y + 0.8, base.z, this.radius * (1.6 - k), t * 0.2, STARLIGHT, fade);
            }
        }
    }

    private static final class Beam extends VfxEffect {
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
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age == this.windup) {
                RandomSource random = level.getRandom();
                int amount = ClientVfx.count(6);
                for (int i = 0; i < amount; i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    ClientVfx.spawn(new Mote(A, CRYSTAL, false).at(this.b.x, this.b.y, this.b.z)
                            .vel(Math.cos(angle) * 0.2, 0.15 + random.nextDouble() * 0.2, Math.sin(angle) * 0.2).gravity(0.04)
                            .size(0.14 + random.nextDouble() * 0.1).frame(random.nextInt(4)).spin(angle, 0.3).collide().life(26));
                }
                stars(level, this.b, 0.5, ClientVfx.count(3), STARLIGHT);
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
            if (t < this.windup) {
                float p = t / this.windup;
                float flicker = 0.7F + 0.3F * Mth.sin(t * 1.9F);
                c.strip(A, true, RUNES, this.color, 0.8F * flicker * Math.min(1.0F, t / 3.0F)).scroll(t * 6.0).density(24.0).beam(this.a, this.b, 0.35 + p * 0.15);
                c.sprite(A, true, STAR, 1, this.a.x, this.a.y, this.a.z, 0.3 + p * 0.6, t * 0.4, STARLIGHT, p);
                c.sprite(A, true, WISP, (int) (t / 2) % 4, this.b.x, this.b.y, this.b.z, 0.35, 0.0, this.color, 0.6F * p);
                return;
            }
            float fade = Mth.clamp(1.0F - (t - this.windup) / Math.max(1.0F, this.duration - this.windup), 0.0F, 1.0F);
            double w = this.width * (0.6 + fade * 0.6);
            c.strip(A, true, RUNES, this.color, 0.45F * fade).scroll(-t * 8.0).beam(this.a, this.b, w * 2.4);
            c.strip(A, true, BOLT, this.color, 0.95F * fade).scroll(-t * 16.0).beam(this.a, this.b, w * 1.4);
            c.strip(A, true, BOLT, 0xFFFFFF, 0.85F * fade).scroll(-t * 22.0).beam(this.a, this.b, w * 0.5);
            c.sprite(A, true, STAR, 1, this.a.x, this.a.y, this.a.z, w * 1.6, t * 0.3, STARLIGHT, fade);
            c.sprite(A, true, STAR, 1, this.b.x, this.b.y, this.b.z, w * 2.0, -t * 0.3, this.color, fade);
        }
    }

    private static final class Chain extends VfxEffect {
        private final int from;
        private final int to;
        private final int duration;
        private final int color;
        private boolean cut;

        Chain(int from, int to, int duration, int color) {
            this.from = from;
            this.to = to;
            this.duration = duration;
            this.color = color;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity a = level.getEntity(this.from);
            Entity b = level.getEntity(this.to);
            if (a == null || b == null) {
                return false;
            }
            if (this.cut) {
                Vec3 mid = a.position().add(b.position()).scale(0.5).add(0.0, 1.2, 0.0);
                wisps(level, mid, 1.5, ClientVfx.count(6), this.color);
                return false;
            }
            if (this.age % 4 == 0) {
                RandomSource random = level.getRandom();
                Vec3 at = b.position().add(0.0, b.getBbHeight() * 0.6, 0.0);
                Vec3 dir = a.position().add(0.0, a.getBbHeight() * 0.6, 0.0).subtract(at).normalize().scale(0.25);
                ClientVfx.spawn(new Mote(A, SOUL, true).at(at.x + (random.nextDouble() - 0.5) * 0.4, at.y, at.z + (random.nextDouble() - 0.5) * 0.4)
                        .vel(dir.x, dir.y + 0.02, dir.z).drag(1.0).size(0.22).color(this.color).animate().life(14));
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return !this.cut && this.age < this.duration;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity ea = c.level.getEntity(this.from);
            Entity eb = c.level.getEntity(this.to);
            if (ea == null || eb == null || this.cut) {
                return;
            }
            float t = this.time(c);
            float alpha = envelope(t, 4.0F, this.duration - 5.0F, 5.0F);
            Vec3 start = VfxCanvas.pos(ea, c.partial).add(0.0, ea.getBbHeight() * 0.6, 0.0);
            Vec3 end = VfxCanvas.pos(eb, c.partial).add(0.0, eb.getBbHeight() * 0.6, 0.0);
            Vec3 dir = end.subtract(start);
            Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
            if (side.lengthSqr() < 1.0E-6) {
                side = new Vec3(1.0, 0.0, 0.0);
            }
            side = side.normalize();
            int segments = 18;
            Vec3[] points = new Vec3[segments + 1];
            for (int i = 0; i <= segments; i++) {
                double s = i / (double) segments;
                double sag = Math.sin(s * Math.PI) * (0.6 + 0.2 * Math.sin(t * 0.3));
                double wave = Math.sin(s * Math.PI) * Math.sin(t * 0.45 + s * 8.0) * 0.25;
                points[i] = start.add(dir.scale(s)).add(side.scale(wave)).add(0.0, -sag, 0.0);
            }
            c.strip(A, true, CHAIN, this.color, 0.95F * alpha).density(28.0).scroll(t * 3.0).path(points, 0.55, false);
            c.strip(A, true, RUNES, this.color, 0.35F * alpha).density(20.0).scroll(-t * 5.0).path(points, 1.0, false);
        }
    }

    private static final class Orb extends VfxEffect {
        private final int id;
        private final float height;
        private final float size;
        private final int duration;
        private final int color;
        private final int style;

        Orb(int id, float height, float size, int duration, int color, int style) {
            this.id = id;
            this.height = height;
            this.size = size;
            this.duration = duration;
            this.color = color;
            this.style = style;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            if (this.style == LichFx.ORB_VOID && this.age % 2 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = 3.0 + random.nextDouble() * 2.0;
                Vec3 o = entity.position().add(0.0, this.height, 0.0);
                Vec3 from = o.add(Math.cos(angle) * r, (random.nextDouble() - 0.5) * 3.0, Math.sin(angle) * r);
                Vec3 v = o.subtract(from).scale(1.0 / 14.0);
                ClientVfx.spawn(new Mote(A, WISP, true).at(from.x, from.y, from.z).vel(v.x, v.y, v.z).drag(1.0).size(0.2).color(VOID).animate().life(14));
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
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 4.0F, this.duration - 3.0F, 3.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double x = pos.x;
            double y = pos.y + this.height;
            double z = pos.z;
            float pulse = 0.88F + 0.12F * Mth.sin(t * 0.6F);
            double s = this.size * (0.5 + p * 0.5) * pulse;
            if (this.style == LichFx.ORB_STAR) {
                c.faceDecal(A, true, PENTAGRAM, x, y, z, s * 2.4, t * 0.05, this.color, 0.55F * a);
                c.sprite(A, true, STAR, 1, x, y, z, s * 1.7, t * 0.2, STARLIGHT, a);
                for (int k = 0; k < 5; k++) {
                    double angle = t * 0.18 + k * Mth.TWO_PI / 5.0;
                    double orbit = s * 2.0;
                    c.sprite(A, true, STAR, 2, x + Math.cos(angle) * orbit, y + Math.sin(angle * 2.0) * s * 0.4, z + Math.sin(angle) * orbit, s * 0.5, angle, this.color, 0.9F * a);
                }
                return;
            }
            if (this.style == LichFx.ORB_SOUL) {
                c.faceDecal(A, true, HEXAGRAM, x, y, z, s * 2.6, -t * 0.06, this.color, 0.5F * a);
                c.sprite(A, true, SOUL, (int) (t / 2.0F) % 4, x, y, z, s * 1.7, 0.0, this.color, a);
                for (int k = 0; k < 3; k++) {
                    double angle = t * 0.15 + k * Mth.TWO_PI / 3.0;
                    double orbit = s * 2.1;
                    c.sprite(A, false, MINI_SKULL, (int) (t / 3.0F + k) % 4, x + Math.cos(angle) * orbit, y + Math.sin(angle * 1.5) * s * 0.4, z + Math.sin(angle) * orbit, s * 0.55, 0.0, 0xFFFFFF, a);
                }
                return;
            }
            if (this.style == LichFx.ORB_SHIELD) {
                c.faceDecal(A, true, HEXAGRAM, x, y, z, s * 1.5, t * 0.08, this.color, 0.7F * a);
                c.strip(A, true, RUNES, this.color, 0.8F * a).scroll(t * 4.0).edges(1.0F, 0.2F).wall(x, y - 0.4, z, s * 1.1, 0.6, 0.0, Mth.TWO_PI, false);
                c.strip(A, true, RUNES, STARLIGHT, 0.6F * a).scroll(-t * 5.0).edges(0.2F, 1.0F).wall(x, y + 0.5, z, s * 0.9, 0.5, 0.0, Mth.TWO_PI, false);
                for (int k = 0; k < 6; k++) {
                    double angle = -t * 0.12 + k * Mth.TWO_PI / 6.0;
                    double orbit = s * 1.3;
                    c.sprite(A, false, CRYSTAL, k % 4, x + Math.cos(angle) * orbit, y + Math.sin(t * 0.2 + k) * 0.3, z + Math.sin(angle) * orbit, s * 0.3, 0.0, 0xFFE8A0, a);
                }
                return;
            }
            c.faceDecal(A, true, RIFT, x, y, z, s * 2.2, -t * 0.2, VOID, 0.85F * a);
            c.faceDecal(A, true, RIFT, x, y, z, s * 1.4, t * 0.3, STARLIGHT, 0.4F * a);
            c.sprite(A, true, STAR, 1, x, y, z, s * 1.2 * pulse, t * 0.5, 0xFFFFFF, a);
        }
    }

    private static final class Mark extends VfxEffect {
        private final int id;
        private final float radius;
        private final int duration;

        Mark(int id, float radius, int duration) {
            this.id = id;
            this.radius = radius;
            this.duration = Math.max(1, duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null || !entity.isAlive()) {
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
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 4.0F, this.duration, 3.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            double gy = VfxCanvas.ground(c.level, pos.x, pos.z, pos.y);
            float beat = 0.75F + 0.25F * Mth.sin(t * (0.3F + p * 0.9F));
            c.flatDecal(A, true, SKULL, pos.x, gy + 0.02, pos.z, this.radius * 0.7, 0.0, DANGER, 0.7F * beat * a);
            c.strip(A, true, RUNES, DANGER, 0.85F * a).scroll(t * 2.0).arc(pos.x, gy, pos.z, this.radius - 0.4, this.radius, 0.0, Mth.TWO_PI, true);
            double left = Mth.TWO_PI * (1.0 - p);
            c.strip(A, true, RUNES, 0xFFFFFF, 0.7F * a).lift(0.01).arc(pos.x, gy, pos.z, this.radius - 0.75, this.radius - 0.45, -Math.PI * 0.5, left, true);
            double hy = pos.y + entity.getBbHeight() + 0.7 + Math.sin(t * 0.25) * 0.12;
            c.sprite(A, false, MINI_SKULL, (int) (t / 3.0F) % 4, pos.x, hy, pos.z, 0.45 + 0.1 * beat, 0.0, 0xFFFFFF, a);
            c.sprite(A, true, WISP, 2, pos.x, hy, pos.z, 0.8 * beat, 0.0, DRAIN, 0.5F * a);
        }
    }

    private static final class Blast extends VfxEffect {
        private static final int LIFE = 16;
        private final Vec3 c;
        private final float radius;
        private final int color;

        Blast(Vec3 c, float radius, int color) {
            this.c = c;
            this.radius = radius;
            this.color = color;
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = t / LIFE;
            float fade = 1.0F - k;
            double r = Math.max(0.4, this.radius * ease(t / 7.0F));
            c.strip(A, true, RUNES, this.color, 0.9F * fade).scroll(t * 3.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, r - 0.6), r, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, RUNES, this.color, 0.6F * fade).scroll(-t * 4.0).edges(0.0F, 1.0F).wall(this.c.x, this.c.y, this.c.z, r, 1.4 * fade + 0.2, 0.0, Mth.TWO_PI, true);
            c.sprite(A, true, STAR, 1, this.c.x, this.c.y + 0.8, this.c.z, this.radius * 0.8 * (1.2F - k), t * 0.2, STARLIGHT, fade);
            c.sprite(A, true, WISP, 2, this.c.x, this.c.y + 0.8, this.c.z, this.radius * 0.9 * (0.6F + k), 0.0, this.color, 0.7F * fade);
        }
    }

    private static final class Flame extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final float height;
        private final int duration;
        private final int color;

        Flame(Vec3 c, float radius, float height, int duration, int color) {
            this.c = c;
            this.radius = radius;
            this.height = height;
            this.duration = Math.max(2, duration);
            this.color = color;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age % 2 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                ClientVfx.spawn(new Mote(A, SOUL, true).at(this.c.x + Math.cos(angle) * this.radius * 0.6, this.c.y + random.nextDouble() * this.height * 0.4, this.c.z + Math.sin(angle) * this.radius * 0.6)
                        .vel(0.0, 0.25 + random.nextDouble() * 0.15, 0.0).drag(0.93).size(0.25 + random.nextDouble() * 0.2).color(this.color).animate().life(16));
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
            float grow = Mth.clamp(t / 3.0F, 0.0F, 1.0F);
            float fade = Mth.clamp(1.0F - t / this.duration, 0.0F, 1.0F);
            double h = this.height * grow;
            Vec3 top = this.c.add(0.0, h, 0.0);
            c.strip(A, true, COMET, this.color, 0.9F * fade).density(128.0 / Math.max(1.0, h)).beam(this.c, top, this.radius * 2.0);
            c.strip(A, true, BOLT, 0xFFFFFF, 0.6F * fade).scroll(-t * 12.0).beam(this.c, top, this.radius * 0.6);
            double step = this.radius * 0.9;
            int count = Math.max(2, (int) (h / step));
            for (int i = 0; i < count; i++) {
                double rise = (t * 0.25) % 1.0;
                double y = this.c.y + (i + rise) * step;
                double shrink = 1.0 - (i + rise) / (count + 1.0) * 0.6;
                c.sprite(A, true, SOUL, (int) (t / 2.0F + i) % 4, this.c.x, y + this.radius * 0.5, this.c.z, this.radius * 1.1 * shrink, 0.0, this.color, fade);
            }
            c.strip(A, true, RUNES, this.color, 0.8F * fade).scroll(t * 3.0).arc(this.c.x, this.c.y, this.c.z, this.radius, this.radius * 1.6, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Prison extends VfxEffect {
        private static final int BARS = 14;
        private final Vec3 c;
        private final float radius;
        private final float height;
        private final int duration;

        Prison(Vec3 c, float radius, float height, int duration) {
            this.c = c;
            this.radius = radius;
            this.height = height;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age % 3 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                ClientVfx.spawn(new Mote(A, SOUL, true).at(this.c.x + Math.cos(angle) * this.radius, this.c.y + 0.2, this.c.z + Math.sin(angle) * this.radius)
                        .vel(0.0, 0.12, 0.0).drag(0.95).size(0.3).color(SOUL_FIRE).animate().life(18));
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
            float rise = ease(t / 6.0F);
            float a = Math.min(rise, Mth.clamp((this.duration - t) / 10.0F, 0.0F, 1.0F));
            double h = this.height * rise;
            c.strip(A, true, RUNES, SOUL_FIRE, 0.9F * a).scroll(t * 2.0).arc(this.c.x, this.c.y, this.c.z, this.radius - 0.6, this.radius + 0.2, 0.0, Mth.TWO_PI, true);
            int rows = Math.max(1, (int) Math.round(h / 1.4));
            double rowHeight = h / rows;
            for (int k = 0; k < rows; k++) {
                double dir = 1.0;
                if (k % 2 == 1) {
                    dir = -1.0;
                }
                c.strip(A, true, CHAIN, SOUL_FIRE, 0.75F * a).density(26.0).scroll(t * 2.0 * dir).wall(this.c.x, this.c.y + k * rowHeight + rowHeight * 0.5 - 0.25, this.c.z, this.radius, 0.5, 0.0, Mth.TWO_PI, false);
            }
            c.strip(A, true, RUNES, STARLIGHT, 0.7F * a).scroll(-t * 2.0).arc(this.c.x, this.c.y + h, this.c.z, this.radius - 0.35, this.radius, 0.0, Mth.TWO_PI, false);
            double spin = t * 0.015;
            for (int i = 0; i < BARS; i++) {
                double angle = spin + i * Mth.TWO_PI / BARS;
                Vec3 base = new Vec3(this.c.x + Math.cos(angle) * this.radius, this.c.y, this.c.z + Math.sin(angle) * this.radius);
                float flicker = 0.7F + 0.3F * Mth.sin(t * 0.5F + i * 1.7F);
                c.strip(A, true, BOLT, SOUL_FIRE, 0.8F * a * flicker).scroll(-t * 9.0 + i * 13.0).beam(base, base.add(0.0, h, 0.0), 0.32);
            }
        }
    }

    private static final class Vortex extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final VfxCanvas.Patch patch;

        Vortex(ClientLevel level, Vec3 c, float radius, int duration) {
            this.c = c;
            this.radius = radius;
            this.duration = duration;
            this.patch = VfxCanvas.patch(level, c, radius);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            RandomSource random = level.getRandom();
            int amount = ClientVfx.count(2);
            for (int i = 0; i < amount; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = this.radius * (0.7 + random.nextDouble() * 0.3);
                double speed = r / 20.0;
                double tangent = speed * 0.8;
                double x = this.c.x + Math.cos(angle) * r;
                double z = this.c.z + Math.sin(angle) * r;
                ClientVfx.spawn(new Mote(A, SOUL, true).at(x, this.c.y + 0.3 + random.nextDouble() * 0.8, z)
                        .vel(-Math.cos(angle) * speed - Math.sin(angle) * tangent, 0.0, -Math.sin(angle) * speed + Math.cos(angle) * tangent)
                        .drag(1.0).size(0.25).color(VOID).animate().life(20));
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
            float a = envelope(t, 8.0F, this.duration - 6.0F, 6.0F);
            c.decal(A, true, RIFT, this.patch, -t * 0.09, 0.4 + 0.6 * ease(t / 15.0F), 0.02, VOID, 0.75F * a);
            c.decal(A, true, RIFT, this.patch, t * 0.05, 0.55, 0.03, STARLIGHT, 0.35F * a);
            for (int k = 0; k < 3; k++) {
                float phase = 1.0F - ((t * 0.03F + k / 3.0F) % 1.0F);
                double r = this.radius * phase;
                c.strip(A, true, RUNES, VOID, 0.6F * a * (1.0F - phase * 0.5F)).scroll(t * 4.0).lift(0.01).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, r - 0.4), r, t * 0.05, Mth.TWO_PI, true);
            }
        }
    }

    private static final class Slash extends VfxEffect {
        private static final int LIFE = 9;
        private final int id;
        private final float reach;
        private final float half;
        private final int windup;

        Slash(int id, float reach, float half, int windup) {
            this.id = id;
            this.reach = reach;
            this.half = half;
            this.windup = windup;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null) {
                return false;
            }
            if (this.age == this.windup) {
                float yaw = entity.getYRot();
                for (int i = -2; i <= 2; i++) {
                    double angle = Math.toRadians(yaw + 90.0F + i * this.half * 0.4F);
                    Vec3 at = entity.position().add(Math.cos(angle) * this.reach * 0.9, 1.1, Math.sin(angle) * this.reach * 0.9);
                    stars(level, at, 0.3, 1, STARLIGHT);
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
                c.strip(A, true, RUNES, DANGER, 0.85F).scroll(t * 3.0).arc(pos.x, pos.y, pos.z, this.reach - 0.4, this.reach, start, sweep, true);
                c.strip(A, true, COMET, DANGER, 0.3F).density(10.0).lift(0.01).arc(pos.x, pos.y, pos.z, 0.5, 0.5 + (this.reach - 0.5) * p, start, sweep, true);
                return;
            }
            float k = (t - this.windup) / LIFE;
            float reveal = ease(k * 3.0F);
            float fade = Mth.clamp(1.0F - (k - 0.25F) / 0.75F, 0.0F, 1.0F);
            double head = start + sweep * reveal;
            double mid = this.reach * 0.65;
            double dens = 128.0 / Math.max(0.5, mid * sweep * reveal);
            c.strip(A, true, COMET, VOID, 0.95F * fade).density(dens).arc(pos.x, pos.y + 1.1, pos.z, this.reach * 0.3, this.reach, head, -sweep * reveal, false);
            c.strip(A, true, COMET, 0xFFFFFF, 0.8F * fade).density(dens).arc(pos.x, pos.y + 1.12, pos.z, this.reach * 0.8, this.reach, head, -sweep * reveal, false);
            c.strip(A, true, COMET, TWILIGHT, 0.5F * fade).density(dens).arc(pos.x, pos.y + 0.7, pos.z, this.reach * 0.4, this.reach * 0.92, head, -sweep * reveal, false);
        }
    }

    private static final class Blink extends VfxEffect {
        private static final int LIFE = 10;
        private final Vec3 a;
        private final Vec3 b;

        Blink(Vec3 a, Vec3 b) {
            this.a = a;
            this.b = b;
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float fade = 1.0F - t / LIFE;
            double length = Math.max(1.0, this.a.distanceTo(this.b));
            c.strip(A, true, COMET, VOID, 0.9F * fade).density(128.0 / length).beam(this.b, this.a, 1.2 * fade + 0.2);
            c.strip(A, true, COMET, 0xFFFFFF, 0.7F * fade).density(128.0 / length).beam(this.b, this.a, 0.4);
            c.faceDecal(A, true, RIFT, this.a.x, this.a.y, this.a.z, 1.3 * fade, -t * 0.4, VOID, fade);
            c.faceDecal(A, true, RIFT, this.b.x, this.b.y, this.b.z, 1.3 * (1.0F - fade) + 0.2, t * 0.4, TWILIGHT, fade);
        }
    }

    private static final class RuneWave extends VfxEffect {
        private final Vec3 c;
        private final float speed;
        private final float max;
        private final float thickness;
        private final int delay;
        private final int color;

        RuneWave(Vec3 c, float speed, float max, float thickness, int delay, int color) {
            this.c = c;
            this.speed = speed;
            this.max = max;
            this.thickness = thickness;
            this.delay = delay;
            this.color = color;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            int active = this.age - this.delay;
            if (active > 0 && active % 3 == 0) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                double r = active * this.speed;
                ClientVfx.spawn(new Mote(A, STAR, true).at(this.c.x + Math.cos(angle) * r, this.c.y + 0.4, this.c.z + Math.sin(angle) * r)
                        .vel(Math.cos(angle) * 0.05, 0.08, Math.sin(angle) * 0.05).drag(0.94).size(0.25).color(this.color).animate().life(12));
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
            double inner = Math.max(0.0, r - this.thickness * 0.5);
            double outer = r + this.thickness * 0.5;
            c.strip(A, true, RUNES, this.color, 0.9F * f).scroll(active * 2.0).arc(this.c.x, this.c.y, this.c.z, inner, outer, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, BOLT, this.color, 0.7F * f).scroll(-active * 8.0).edges(0.0F, 1.0F).wall(this.c.x, this.c.y, this.c.z, outer, 0.9, 0.0, Mth.TWO_PI, true);
        }
    }
}
