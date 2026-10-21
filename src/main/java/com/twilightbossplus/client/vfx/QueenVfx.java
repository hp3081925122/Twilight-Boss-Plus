package com.twilightbossplus.client.vfx;

import com.twilightbossplus.queen.QueenColors;
import com.twilightbossplus.queen.QueenFx;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class QueenVfx {
    private static final VfxAtlas A = VfxAtlas.QUEEN;
    private static final int AURORA = 0;
    private static final int LINE = 1;
    private static final int RIME = 2;
    private static final int GLASS_STRIP = 3;
    private static final int PANEL = 4;
    private static final int SWIRL = 5;
    private static final int BEAM = 6;
    private static final int SIGIL = 0;
    private static final int HEX = 1;
    private static final int FLOWER = 2;
    private static final int STAR = 3;
    private static final int SPARK = 0;
    private static final int FLAKE = 1;
    private static final int PETAL = 2;
    private static final int MOTE = 3;
    private static final int SHARD = 4;
    private static final int RIFT = 5;

    private static final int GREEN = QueenColors.AURORA_GREEN;
    private static final int CYAN = QueenColors.AURORA_CYAN;
    private static final int VIOLET = QueenColors.AURORA_VIOLET;
    private static final int ICE = QueenColors.ICE;
    private static final int PALE = QueenColors.PALE;
    private static final int DEEP = QueenColors.DEEP;
    private static final int WARNING = QueenColors.WARNING;
    private static final int GLASS = QueenColors.GLASS;

    private QueenVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case QueenFx.WALTZ -> ClientVfx.add(new Waltz(level, d.getInt("id"), ClientVfx.vec(d, "c"), d.getInt("spin"), d.getFloat("from"), d.getFloat("to"), d.getInt("n"), d.getFloat("a"), d.getFloat("speed"), d.getFloat("len")));
            case QueenFx.SHELL -> ClientVfx.add(new Shell(d.getInt("id"), d.getInt("d")));
            case QueenFx.SHELL_BURST -> {
                int id = d.getInt("id");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Shell shell && shell.id == id) {
                        shell.dead = true;
                    }
                }
                ClientVfx.add(new ShellBurst(level, ClientVfx.vec(d, "c"), d.getFloat("r")));
            }
            case QueenFx.TRACE -> ClientVfx.add(new Trace(d.getInt("id"), d.getInt("d")));
            case QueenFx.TRACE_NODE -> {
                int id = d.getInt("id");
                Trace found = null;
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Trace trace && trace.id == id && !trace.closed()) {
                        found = trace;
                    }
                }
                if (found == null) {
                    found = new Trace(id, 2);
                    ClientVfx.add(found);
                }
                found.node(level, ClientVfx.vec(d, "p"), d.getInt("delay"), d.getFloat("r"));
            }
            case QueenFx.COFFIN -> ClientVfx.add(new Coffin(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("close"), d.getFloat("a")));
            case QueenFx.COFFIN_SHATTER -> ClientVfx.add(new CoffinShatter(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getFloat("a")));
            case QueenFx.COURT -> ClientVfx.add(new Court(d.getInt("id"), d.getInt("d")));
            case QueenFx.COURT_RIFT -> ClientVfx.add(new Rift(ClientVfx.vec(d, "p")));
            case QueenFx.PULSE -> ClientVfx.add(new Pulse(level, ClientVfx.vec(d, "c"), d.getFloat("r")));
            case QueenFx.MIRRORS -> ClientVfx.add(new Mirrors(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getFloat("a"), d.getFloat("h"), d.getInt("life")));
            case QueenFx.MIRROR_BEAM -> ClientVfx.add(new MirrorBeam(ClientVfx.vec(d, "a"), ClientVfx.vec(d, "b"), d.getInt("warn"), d.getInt("fire"), d.getInt("tint")));
            default -> {
            }
        }
    }

    @Nullable
    private static Entity entity(int id) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || id < 0) {
            return null;
        }
        Entity entity = level.getEntity(id);
        if (entity == null || !entity.isAlive()) {
            return null;
        }
        return entity;
    }

    private static int tint(int index) {
        if (index == 1) {
            return VIOLET;
        }
        return GREEN;
    }

    private static void leaf(VfxCanvas c, Vec3 base, Vec3 tip, Vec3 side, int color, float alpha) {
        if (alpha <= 0.01F) {
            return;
        }
        Vec3 mid = base.lerp(tip, 0.45);
        Vec3[] outer = new Vec3[]{base, mid.add(side), tip};
        Vec3[] inner = new Vec3[]{base, mid.subtract(side), tip};
        double dens = 128.0 / Math.max(0.3, base.distanceTo(tip) * 2.0);
        c.strip(A, false, GLASS_STRIP, color, 0.82F * alpha).density(dens);
        c.band(outer, inner);
        c.strip(A, true, GLASS_STRIP, CYAN, 0.35F * alpha).density(dens);
        c.band(outer, inner);
    }

    private static void pane(VfxCanvas c, Vec3 a, Vec3 b, double bottom, double top, int body, int glow, float alpha, double scroll) {
        if (top - bottom < 0.02 || alpha <= 0.01F) {
            return;
        }
        Vec3[] outer = new Vec3[]{new Vec3(a.x, top, a.z), new Vec3(b.x, top, b.z)};
        Vec3[] inner = new Vec3[]{new Vec3(a.x, bottom, a.z), new Vec3(b.x, bottom, b.z)};
        double dens = 16.0 / Math.max(0.2, top - bottom);
        c.strip(A, false, GLASS_STRIP, body, 0.5F * alpha).density(dens);
        c.band(outer, inner);
        c.strip(A, true, GLASS_STRIP, glow, 0.35F * alpha).density(dens).scroll(scroll);
        c.band(outer, inner);
    }

    private static void spark(double x, double y, double z, double vx, double vy, double vz, double size, int color, int life) {
        ClientVfx.spawn(new Mote(A, SPARK, true).at(x, y, z).vel(vx, vy, vz).drag(0.92).size(size).animate().color(color).life(life));
    }

    private static void flakes(Vec3 at, int amount, double speed, int color, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.4 + random.nextDouble());
            ClientVfx.spawn(new Mote(A, FLAKE, true).at(at.x, at.y, at.z).vel(Math.cos(a) * s, 0.04 + random.nextDouble() * 0.12, Math.sin(a) * s)
                    .gravity(0.003).drag(0.93).size(0.14 + random.nextDouble() * 0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.3)
                    .color(color).life(18 + random.nextInt(14)));
        }
    }

    private static void shards(Vec3 at, int amount, double speed, double lift, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.5 + random.nextDouble() * 0.8);
            ClientVfx.spawn(new Mote(A, SHARD, false).at(at.x, at.y, at.z).vel(Math.cos(a) * s, lift + random.nextDouble() * 0.25, Math.sin(a) * s)
                    .gravity(0.045).drag(0.95).size(0.18 + random.nextDouble() * 0.16).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.5)
                    .collide().color(ICE).life(24 + random.nextInt(14)));
        }
    }

    private static void motes(Vec3 at, double spread, int amount, int color, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            ClientVfx.spawn(new Mote(A, MOTE, true).at(at.x + (random.nextDouble() - 0.5) * spread, at.y + (random.nextDouble() - 0.5) * spread, at.z + (random.nextDouble() - 0.5) * spread)
                    .vel(0.0, 0.03 + random.nextDouble() * 0.03, 0.0).drag(0.95).size(0.16 + random.nextDouble() * 0.1).frame(random.nextInt(4)).color(color).life(16 + random.nextInt(10)));
        }
    }

    private static final class Waltz extends VfxEffect {
        private final int id;
        private final Vec3 c;
        private final int spin;
        private final double from;
        private final double to;
        private final int blades;
        private final double angle;
        private final double speed;
        private final double length;
        private final VfxCanvas.Patch patch;

        Waltz(ClientLevel level, int id, Vec3 c, int spin, double from, double to, int blades, double angle, double speed, double length) {
            this.id = id;
            this.c = c;
            this.spin = Math.max(1, spin);
            this.from = from;
            this.to = to;
            this.blades = Math.max(1, blades);
            this.angle = angle;
            this.speed = speed;
            this.length = length;
            this.patch = VfxCanvas.patch(level, new Vec3(c.x, c.y - 0.5, c.z), to + 1.0);
            flakes(c, 14, 0.25, PALE, level.getRandom());
        }

        private double radius(float t) {
            float k = Mth.clamp(t / this.spin, 0.0F, 1.0F);
            float tri = 1.0F - Math.abs(2.0F * k - 1.0F);
            float smooth = tri * tri * (3.0F - 2.0F * tri);
            return this.from + (this.to - this.from) * smooth;
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age < this.spin) {
                RandomSource random = level.getRandom();
                double r = this.radius(this.age);
                double base = this.angle + this.speed * this.age;
                for (int i = 0; i < this.blades; i++) {
                    double b = base + Mth.TWO_PI * i / this.blades;
                    double x = this.c.x + Math.cos(b) * r;
                    double z = this.c.z + Math.sin(b) * r;
                    ClientVfx.spawn(new Mote(A, FLAKE, true).at(x, this.c.y, z).vel(-Math.sin(b) * 0.08, 0.02, Math.cos(b) * 0.08).drag(0.9)
                            .size(0.14 + random.nextDouble() * 0.08).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, 0.3).color(PALE).life(12));
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.spin + 10;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float fade = envelope(t, 3.0F, this.spin, 8.0F);
            double r = this.radius(Math.min(t, this.spin));
            double base = this.angle + this.speed * t;
            canvas.decal(A, true, SIGIL, this.patch, base, Mth.clamp(r / (this.to + 1.0), 0.2, 1.0), 0.04, CYAN, 0.55F * fade);
            canvas.decal(A, false, SIGIL, this.patch, -base * 0.5, Mth.clamp(r / (this.to + 1.0), 0.2, 1.0) * 1.05, 0.03, DEEP, 0.3F * fade);
            double inner = Math.max(0.3, r - this.length);
            for (int i = 0; i < this.blades; i++) {
                double b = base + Mth.TWO_PI * i / this.blades;
                Vec3 dir = new Vec3(Math.cos(b), 0.0, Math.sin(b));
                Vec3 perp = new Vec3(-dir.z, 0.0, dir.x);
                Vec3 p0 = this.c.add(dir.scale(inner));
                Vec3 p1 = this.c.add(dir.scale(r));
                leaf(canvas, p0, p1, perp.scale(0.38), ICE, fade);
                leaf(canvas, p0, p1, new Vec3(0.0, 0.26, 0.0), ICE, fade);
                canvas.strip(A, true, SWIRL, GREEN, 0.55F * fade).fade(0.0F, 1.0F).density(4.0).scroll(-t * 6.0).arc(this.c.x, this.c.y, this.c.z, inner, r, b - 0.7, 0.7, false);
            }
            Entity queen = entity(this.id);
            if (queen != null && t < this.spin) {
                Vec3 p = VfxCanvas.pos(queen, canvas.partial);
                canvas.strip(A, true, AURORA, VIOLET, 0.45F * fade).density(5.0).scroll(-t * 10.0).wall(p.x, p.y - 0.2, p.z, 1.1, 2.6, base * 2.0, Mth.TWO_PI, false);
            }
        }
    }

    private static final class Shell extends VfxEffect {
        private static final double RADIUS = 1.5;
        private final int id;
        private final int duration;
        private boolean dead;

        Shell(int id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity queen = entity(this.id);
            if (queen != null && this.age % 2 == 0) {
                RandomSource random = level.getRandom();
                double a = random.nextDouble() * Mth.TWO_PI;
                spark(queen.getX() + Math.cos(a) * RADIUS, queen.getY() + random.nextDouble() * 3.0, queen.getZ() + Math.sin(a) * RADIUS, 0.0, 0.01, 0.0, 0.3, PALE, 8);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return !this.dead && this.age < this.duration + 2 && entity(this.id) != null;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity queen = entity(this.id);
            if (queen == null) {
                return;
            }
            float t = this.time(c);
            float form = ease(t / 8.0F);
            float pulse = 0.85F + 0.15F * Mth.sin(t * 0.7F);
            float late = Mth.clamp((t - (this.duration - 8.0F)) / 8.0F, 0.0F, 1.0F);
            Vec3 p = VfxCanvas.pos(queen, c.partial);
            double r = RADIUS * (0.6 + 0.4 * form);
            double rot = t * 0.03;
            double bottom = p.y - 0.3;
            double top = bottom + 3.3 * form;
            for (int i = 0; i < 6; i++) {
                double a0 = rot + Mth.TWO_PI * i / 6.0;
                double a1 = rot + Mth.TWO_PI * (i + 1) / 6.0;
                Vec3 v0 = new Vec3(p.x + Math.cos(a0) * r, 0.0, p.z + Math.sin(a0) * r);
                Vec3 v1 = new Vec3(p.x + Math.cos(a1) * r, 0.0, p.z + Math.sin(a1) * r);
                pane(c, v0, v1, bottom, top, GLASS, PALE, form * pulse, t * 3.0 + i * 20.0);
                c.strip(A, true, LINE, CYAN, (0.6F + 0.4F * late) * form).beam(new Vec3(v0.x, bottom, v0.z), new Vec3(v0.x, top, v0.z), 0.12);
            }
            c.sprite(A, true, SPARK, (int) (t / 2) % 4, p.x, p.y + 1.6, p.z, 0.8 + 0.4 * late, t * 0.05, WARNING, 0.4F * form + 0.4F * late);
        }
    }

    private static final class ShellBurst extends VfxEffect {
        private final Vec3 c;
        private final double radius;
        private final double ground;

        ShellBurst(ClientLevel level, Vec3 c, double radius) {
            this.c = c;
            this.radius = radius;
            this.ground = VfxCanvas.ground(level, c.x, c.z, c.y);
            RandomSource random = level.getRandom();
            shards(c, 22, 0.35, 0.15, random);
            flakes(c, 14, 0.35, PALE, random);
            for (int i = 0; i < 6; i++) {
                double a = Mth.TWO_PI * i / 6.0;
                spark(c.x + Math.cos(a) * 1.5, c.y, c.z + Math.sin(a) * 1.5, Math.cos(a) * 0.25, 0.0, Math.sin(a) * 0.25, 0.6, CYAN, 10);
            }
        }

        @Override
        protected boolean alive() {
            return this.age < 16;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float k = ease(t / 10.0F);
            float a = 1.0F - Mth.clamp(t / 14.0F, 0.0F, 1.0F);
            double r = 0.8 + this.radius * k;
            canvas.faceDecal(A, true, STAR, this.c.x, this.c.y, this.c.z, 1.0 + this.radius * 0.6 * k, t * 0.06, PALE, 0.9F * a);
            canvas.strip(A, true, SWIRL, VIOLET, a).density(4.0).scroll(t * 6.0).arc(this.c.x, this.ground, this.c.z, Math.max(0.2, r - 0.9), r, 0.0, Mth.TWO_PI, true);
            canvas.strip(A, true, LINE, CYAN, 0.8F * a).density(4.0).wall(this.c.x, this.ground, this.c.z, r, 1.2 * (1.0 - k) + 0.2, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Trace extends VfxEffect {
        private final int id;
        private final int duration;
        private final List<double[]> nodes = new ArrayList<>();
        private Vec3 head;
        private int last;

        Trace(int id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        boolean closed() {
            return this.age > this.duration + 2;
        }

        void node(ClientLevel level, Vec3 p, int delay, double radius) {
            this.nodes.add(new double[]{p.x, p.y, p.z, this.age + delay, radius, 0.0});
            this.head = p;
            this.last = this.age;
            RandomSource random = level.getRandom();
            ClientVfx.spawn(new Mote(A, MOTE, true).at(p.x, p.y + 0.15, p.z).vel((random.nextDouble() - 0.5) * 0.05, 0.04, (random.nextDouble() - 0.5) * 0.05).size(0.2).frame(random.nextInt(4)).color(CYAN).life(14));
        }

        @Override
        public boolean tick(ClientLevel level) {
            for (double[] n : this.nodes) {
                if (n[5] == 0.0 && this.age >= n[3]) {
                    n[5] = 1.0;
                    ClientVfx.add(new Bloom(level, new Vec3(n[0], n[1], n[2]), n[4]));
                }
            }
            Entity queen = entity(this.id);
            if (queen != null && this.head != null && this.age <= this.duration) {
                RandomSource random = level.getRandom();
                Vec3 mouth = queen.getEyePosition();
                Vec3 at = mouth.lerp(this.head, random.nextDouble());
                ClientVfx.spawn(new Mote(A, FLAKE, true).at(at.x, at.y, at.z).vel((random.nextDouble() - 0.5) * 0.06, -0.02, (random.nextDouble() - 0.5) * 0.06).size(0.12).frame(random.nextInt(4)).color(PALE).life(10));
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            for (double[] n : this.nodes) {
                if (n[5] == 0.0) {
                    return true;
                }
            }
            return this.age < this.duration + 4;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            List<Vec3> live = new ArrayList<>();
            for (double[] n : this.nodes) {
                if (n[5] == 0.0) {
                    live.add(new Vec3(n[0], n[1], n[2]));
                }
            }
            if (live.size() >= 2) {
                Vec3[] points = live.toArray(new Vec3[0]);
                c.strip(A, true, RIME, CYAN, 0.85F).density(3.0).fade(1.0F, 0.6F).lift(0.03).path(points, 1.3, true);
                c.strip(A, false, RIME, DEEP, 0.35F).density(3.0).lift(0.02).path(points, 1.6, true);
            }
            Entity queen = entity(this.id);
            if (queen == null || this.head == null || this.age > this.duration || this.age - this.last > 4) {
                return;
            }
            Vec3 p = VfxCanvas.pos(queen, c.partial);
            Vec3 mouth = p.add(0.0, queen.getEyeHeight(), 0.0);
            Vec3 end = this.head.add(0.0, 0.1, 0.0);
            c.strip(A, true, AURORA, GREEN, 0.4F).density(3.0).scroll(-t * 14.0).beam(mouth, end, 1.1);
            c.strip(A, true, BEAM, PALE, 0.9F).density(4.0).scroll(-t * 22.0).beam(mouth, end, 0.4);
            c.sprite(A, true, SPARK, (int) (t / 2) % 4, end.x, end.y + 0.2, end.z, 0.7, t * 0.1, CYAN, 0.9F);
        }
    }

    private static final class Bloom extends VfxEffect {
        private static final int LIFE = 24;
        private final Vec3 p;
        private final double radius;
        private final double rotation;

        Bloom(ClientLevel level, Vec3 p, double radius) {
            this.p = p;
            this.radius = radius;
            RandomSource random = level.getRandom();
            this.rotation = random.nextDouble() * Mth.TWO_PI;
            flakes(p.add(0.0, 0.5, 0.0), 4, 0.15, PALE, random);
            spark(p.x, p.y + 1.0, p.z, 0.0, 0.03, 0.0, 0.5, PALE, 8);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float open = ease(t / 5.0F);
            float wither = Mth.clamp((t - 12.0F) / 10.0F, 0.0F, 1.0F);
            float a = 1.0F - wither;
            c.flatDecal(A, true, FLOWER, this.p.x, this.p.y + 0.04, this.p.z, this.radius * (0.4 + 0.9 * open), this.rotation + t * 0.03, GREEN, 0.8F * a);
            for (int i = 0; i < 5; i++) {
                double ang = this.rotation + Mth.TWO_PI * i / 5.0;
                Vec3 dir = new Vec3(Math.cos(ang), 0.0, Math.sin(ang));
                Vec3 side = new Vec3(-dir.z, 0.0, dir.x).scale(0.22 + 0.1 * open);
                Vec3 base = this.p.add(0.0, 0.02, 0.0);
                double spread = 0.25 + 0.7 * open;
                double h = (1.0 + 0.1 * (i % 2)) * (0.3 + 0.7 * open) * (1.0 - 0.6 * wither);
                Vec3 tip = base.add(dir.scale(spread)).add(0.0, h, 0.0);
                leaf(c, base, tip, side, ICE, a);
            }
        }
    }

    private static final class Coffin extends VfxEffect {
        private static final double LOCK = 0.6;
        private final Vec3 c;
        private final double radius;
        private final int close;
        private final double angle;
        private final double[] floors = new double[6];
        private final VfxCanvas.Patch patch;

        Coffin(ClientLevel level, Vec3 c, double radius, int close, double angle) {
            this.c = c;
            this.radius = radius;
            this.close = Math.max(1, close);
            this.angle = angle;
            this.patch = VfxCanvas.patch(level, c, radius * 1.15);
            for (int i = 0; i < 6; i++) {
                double a = angle + Mth.TWO_PI * i / 6.0;
                this.floors[i] = VfxCanvas.ground(level, c.x + Math.cos(a) * radius, c.z + Math.sin(a) * radius, c.y + 1.0);
            }
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age % 3 == 0) {
                RandomSource random = level.getRandom();
                int i = random.nextInt(6);
                double a = this.angle + Mth.TWO_PI * i / 6.0;
                spark(this.c.x + Math.cos(a) * this.radius, this.floors[i] + 3.0 * this.age / this.close, this.c.z + Math.sin(a) * this.radius, 0.0, 0.02, 0.0, 0.35, PALE, 8);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.close + 1;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float k = Mth.clamp(t / this.close, 0.0F, 1.0F);
            boolean locked = k >= LOCK;
            float pulse = 0.75F + 0.25F * Mth.sin(t * (0.5F + k * 1.5F));
            int warn = CYAN;
            if (locked) {
                warn = WARNING;
            }
            canvas.decal(A, true, HEX, this.patch, this.angle, 1.0 / 1.15, 0.04, warn, (0.35F + 0.5F * k) * pulse);
            double h = 3.2 * ease(k);
            double topRadius = this.radius * (1.0 - 0.3 * k);
            for (int i = 0; i < 6; i++) {
                int j = (i + 1) % 6;
                double a0 = this.angle + Mth.TWO_PI * i / 6.0;
                double a1 = this.angle + Mth.TWO_PI * j / 6.0;
                Vec3 b0 = new Vec3(this.c.x + Math.cos(a0) * this.radius, this.floors[i] - 0.05, this.c.z + Math.sin(a0) * this.radius);
                Vec3 b1 = new Vec3(this.c.x + Math.cos(a1) * this.radius, this.floors[j] - 0.05, this.c.z + Math.sin(a1) * this.radius);
                Vec3 t0 = new Vec3(this.c.x + Math.cos(a0) * topRadius, b0.y + h, this.c.z + Math.sin(a0) * topRadius);
                Vec3 t1 = new Vec3(this.c.x + Math.cos(a1) * topRadius, b1.y + h, this.c.z + Math.sin(a1) * topRadius);
                Vec3[] outer = new Vec3[]{t0, t1};
                Vec3[] inner = new Vec3[]{b0, b1};
                double dens = 16.0 / Math.max(0.3, h);
                canvas.strip(A, false, PANEL, ICE, 0.75F).density(dens);
                canvas.band(outer, inner);
                float edge = 0.3F;
                if (locked) {
                    edge = 0.55F * pulse;
                }
                canvas.strip(A, true, PANEL, warn, edge).density(dens).scroll(t * 2.0);
                canvas.band(outer, inner);
            }
        }
    }

    private static final class CoffinShatter extends VfxEffect {
        private final Vec3 c;
        private final double radius;
        private final double angle;
        private final double ground;

        CoffinShatter(ClientLevel level, Vec3 c, double radius, double angle) {
            this.c = c;
            this.radius = radius;
            this.angle = angle;
            this.ground = VfxCanvas.ground(level, c.x, c.z, c.y + 1.0);
            RandomSource random = level.getRandom();
            for (int i = 0; i < 6; i++) {
                double a = angle + Mth.TWO_PI * (i + 0.5) / 6.0;
                Vec3 at = new Vec3(c.x + Math.cos(a) * radius * 0.85, this.ground + 1.2, c.z + Math.sin(a) * radius * 0.85);
                shards(at, 5, 0.2, 0.2, random);
            }
            flakes(c.add(0.0, 1.5, 0.0), 16, 0.3, PALE, random);
            motes(c.add(0.0, 1.5, 0.0), 2.0, 8, CYAN, random);
        }

        @Override
        protected boolean alive() {
            return this.age < 18;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float a = 1.0F - Mth.clamp(t / 16.0F, 0.0F, 1.0F);
            float k = ease(t / 8.0F);
            canvas.flatDecal(A, true, HEX, this.c.x, this.ground + 0.05, this.c.z, this.radius * (1.0 + 0.6 * k), this.angle, PALE, 0.9F * a);
            canvas.flatDecal(A, true, STAR, this.c.x, this.ground + 0.06, this.c.z, this.radius * 0.9, this.angle + t * 0.05, VIOLET, 0.6F * a);
            canvas.strip(A, true, LINE, CYAN, 0.8F * a).density(4.0).wall(this.c.x, this.ground, this.c.z, this.radius * (1.0 + 0.8 * k), 2.5 * (1.0 - k) + 0.2, this.angle, Mth.TWO_PI, true);
        }
    }

    private static final class Court extends VfxEffect {
        private final int id;
        private final int duration;

        Court(int id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity queen = entity(this.id);
            if (queen != null) {
                RandomSource random = level.getRandom();
                double a = random.nextDouble() * Mth.TWO_PI;
                ClientVfx.spawn(new Mote(A, MOTE, true).at(queen.getX() + Math.cos(a) * 2.5, queen.getY() + random.nextDouble() * 0.5, queen.getZ() + Math.sin(a) * 2.5)
                        .vel(0.0, 0.08, 0.0).drag(0.96).size(0.2).frame(random.nextInt(4)).color(tint(random.nextInt(2))).life(20));
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity queen = entity(this.id);
            if (queen == null) {
                return;
            }
            float t = this.time(c);
            float a = envelope(t, 6.0F, this.duration, 6.0F);
            Vec3 p = VfxCanvas.pos(queen, c.partial);
            c.flatDecal(A, true, SIGIL, p.x, p.y + queen.getBbHeight() + 1.2, p.z, 1.6 + 0.4 * Mth.sin(t * 0.2F), t * 0.08, VIOLET, 0.8F * a);
            c.strip(A, true, AURORA, GREEN, 0.6F * a).density(4.0).scroll(-t * 8.0).wall(p.x, p.y - 0.5, p.z, 2.6, 3.4, t * 0.04, Mth.TWO_PI, false);
            c.strip(A, true, AURORA, VIOLET, 0.45F * a).density(3.0).scroll(t * 6.0).wall(p.x, p.y + 0.2, p.z, 3.4, 2.4, -t * 0.03, Mth.TWO_PI, false);
        }
    }

    private static final class Rift extends VfxEffect {
        private static final int OPEN = 24;
        private final Vec3 p;

        Rift(Vec3 p) {
            this.p = p;
        }

        @Override
        public boolean tick(ClientLevel level) {
            RandomSource random = level.getRandom();
            if (this.age < OPEN) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double r = 1.6;
                spark(this.p.x + Math.cos(a) * r, this.p.y + (random.nextDouble() - 0.5) * 2.0, this.p.z + Math.sin(a) * r, -Math.cos(a) * 0.1, 0.0, -Math.sin(a) * 0.1, 0.3, CYAN, 10);
            }
            if (this.age == OPEN) {
                shards(this.p, 10, 0.2, 0.1, random);
                flakes(this.p, 8, 0.25, PALE, random);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < OPEN + 10;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float open = ease(t / OPEN);
            float a = 1.0F - Mth.clamp((t - OPEN) / 10.0F, 0.0F, 1.0F);
            Vec3 bottom = this.p.add(0.0, -1.1 * open, 0.0);
            Vec3 top = this.p.add(0.0, 1.1 * open, 0.0);
            c.strip(A, true, AURORA, VIOLET, 0.6F * a).density(4.0).scroll(-t * 10.0).beam(bottom, top, 0.4 + 0.8 * open);
            c.strip(A, true, BEAM, PALE, 0.9F * a).density(4.0).scroll(t * 12.0).beam(bottom, top, 0.15 + 0.15 * open);
            c.sprite(A, true, RIFT, (int) (t / 3) % 4, this.p.x, this.p.y, this.p.z, 0.8 + 0.6 * open, 0.0, CYAN, 0.8F * a);
        }
    }

    private static final class Pulse extends VfxEffect {
        private final Vec3 c;
        private final double radius;

        Pulse(ClientLevel level, Vec3 c, double radius) {
            this.c = c;
            this.radius = radius;
            motes(c.add(0.0, 0.5, 0.0), radius, 14, GREEN, level.getRandom());
        }

        @Override
        protected boolean alive() {
            return this.age < 18;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float k = ease(t / 12.0F);
            float a = 1.0F - Mth.clamp(t / 16.0F, 0.0F, 1.0F);
            double r = 0.8 + this.radius * k;
            canvas.strip(A, true, AURORA, GREEN, 0.8F * a).density(4.0).scroll(-t * 8.0).wall(this.c.x, this.c.y, this.c.z, r, 1.8 * (1.0 - 0.5 * k), 0.0, Mth.TWO_PI, true);
            canvas.strip(A, true, SWIRL, VIOLET, 0.7F * a).density(4.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.2, r - 1.0), r, t * 0.05, Mth.TWO_PI, true);
        }
    }

    private static final class Mirrors extends VfxEffect {
        private final Vec3[] mirrors = new Vec3[6];
        private final double[] facing = new double[6];
        private final double[] floors = new double[6];
        private final int life;
        private final double height;

        Mirrors(ClientLevel level, Vec3 c, double radius, double angle, double height, int life) {
            this.life = life;
            this.height = height;
            RandomSource random = level.getRandom();
            for (int i = 0; i < 6; i++) {
                double a = angle + Mth.TWO_PI * i / 6.0;
                double x = c.x + Math.cos(a) * radius;
                double z = c.z + Math.sin(a) * radius;
                this.floors[i] = VfxCanvas.ground(level, x, z, c.y + 2.0);
                this.mirrors[i] = new Vec3(x, this.floors[i], z);
                this.facing[i] = a;
                shards(new Vec3(x, this.floors[i] + 0.3, z), 4, 0.12, 0.25, random);
            }
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age % 4 == 0) {
                RandomSource random = level.getRandom();
                int i = random.nextInt(6);
                Vec3 m = this.mirrors[i];
                spark(m.x, m.y + random.nextDouble() * 2.6, m.z, 0.0, 0.01, 0.0, 0.3, PALE, 8);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.life + 8;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float rise = ease(t / 10.0F);
            float a = 1.0F - Mth.clamp((t - this.life) / 8.0F, 0.0F, 1.0F);
            for (int i = 0; i < 6; i++) {
                Vec3 m = this.mirrors[i];
                double tangent = this.facing[i] + Math.PI * 0.5;
                Vec3 side = new Vec3(Math.cos(tangent) * 0.8, 0.0, Math.sin(tangent) * 0.8);
                Vec3 l = m.add(side);
                Vec3 r = m.subtract(side);
                double bottom = m.y + 0.05;
                double top = bottom + (this.height + 1.5) * rise;
                pane(c, l, r, bottom, top, GLASS, PALE, a, t * 4.0 + i * 16.0);
                c.strip(A, true, LINE, CYAN, 0.8F * a).path(new Vec3[]{new Vec3(l.x, bottom, l.z), new Vec3(l.x, top, l.z), new Vec3(r.x, top, r.z), new Vec3(r.x, bottom, r.z)}, 0.12, false);
                c.sprite(A, true, SPARK, (int) (t / 2 + i) % 4, m.x, m.y + this.height, m.z, 0.5, 0.0, VIOLET, 0.6F * a * rise);
                c.flatDecal(A, true, SIGIL, m.x, this.floors[i] + 0.04, m.z, 1.1, t * 0.04 + i, CYAN, 0.45F * a);
            }
        }
    }

    private static final class MirrorBeam extends VfxEffect {
        private final Vec3 a;
        private final Vec3 b;
        private final int warn;
        private final int fire;
        private final int color;
        private boolean flashed;

        MirrorBeam(Vec3 a, Vec3 b, int warn, int fire, int tint) {
            this.a = a;
            this.b = b;
            this.warn = Math.max(1, warn);
            this.fire = Math.max(1, fire);
            this.color = tint(tint);
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age >= this.warn && !this.flashed) {
                this.flashed = true;
                RandomSource random = level.getRandom();
                spark(this.a.x, this.a.y, this.a.z, 0.0, 0.0, 0.0, 1.0, PALE, 8);
                spark(this.b.x, this.b.y, this.b.z, 0.0, 0.0, 0.0, 1.2, PALE, 10);
                for (int i = 0; i < ClientVfx.count(10); i++) {
                    Vec3 at = this.a.lerp(this.b, random.nextDouble());
                    motes(at, 0.6, 1, this.color, random);
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.warn + this.fire + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            if (t < this.warn) {
                float k = t / this.warn;
                float flicker = 0.6F + 0.4F * Mth.sin(t * (1.0F + k * 2.0F));
                c.strip(A, true, LINE, WARNING, (0.35F + 0.5F * k) * flicker).density(4.0).scroll(t * 8.0).beam(this.a, this.b, 0.1 + 0.15 * k);
                return;
            }
            float local = t - this.warn;
            float alpha = 1.0F - Mth.clamp((local - this.fire) / 6.0F, 0.0F, 1.0F);
            float swell = ease(local / 2.0F);
            int segments = Mth.clamp((int) (this.a.distanceTo(this.b) * 1.5), 6, 48);
            Vec3[] wave = new Vec3[segments + 1];
            for (int i = 0; i <= segments; i++) {
                double f = i / (double) segments;
                double off = Math.sin(f * 9.0 - t * 0.9) * 0.22 * Math.sin(f * Math.PI);
                wave[i] = this.a.lerp(this.b, f).add(0.0, off, 0.0);
            }
            c.strip(A, true, AURORA, this.color, 0.8F * alpha).density(3.0).scroll(-t * 16.0).path(wave, 1.4 * swell, false);
            c.strip(A, true, AURORA, CYAN, 0.4F * alpha).density(2.0).scroll(t * 10.0).path(wave, 2.0 * swell, false);
            c.strip(A, true, BEAM, PALE, alpha).density(4.0).scroll(-t * 24.0).beam(this.a, this.b, 0.4 * swell);
        }
    }
}
