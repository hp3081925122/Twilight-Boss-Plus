package com.twilightbossplus.client.vfx;

import com.twilightbossplus.knight.KnightFx;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class KnightVfx {
    private static final VfxAtlas A = VfxAtlas.KNIGHT;
    private static final int CRESCENT = 0;
    private static final int CHAIN_STRIP = 1;
    private static final int STREAK = 2;
    private static final int SPECTER = 3;
    private static final int LANE = 4;
    private static final int DOTS = 5;
    private static final int BLADE = 6;
    private static final int SHAFT = 7;
    private static final int CREST = 0;
    private static final int SHATTER = 1;
    private static final int OATH_SIGIL = 2;
    private static final int GHOST_DECAL = 3;
    private static final int WISP = 0;
    private static final int SHARD = 1;
    private static final int AXE_SPRITE = 2;
    private static final int HOOK_SPRITE = 3;
    private static final int LINK = 4;
    private static final int GLINT = 5;

    private static final int GHOST = 0xD6ECFF;
    private static final int STEEL = 0x8FB4FF;
    private static final int METAL = 0xE4F08A;
    private static final int CHAIN = 0xB8C4D8;
    private static final int OATH = 0xFFF3C4;
    private static final int WARNING = 0xF2E27A;
    private static final int PALE = 0xF4FAFF;
    private static final int SHADOW = 0x10141C;

    private KnightVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case KnightFx.REVEAL -> ClientVfx.add(new Reveal(level, d.getInt("id")));
            case KnightFx.SLASH -> ClientVfx.add(new Slash(level, ClientVfx.vec(d, "c"), d.getFloat("a"), d.getFloat("sweep"), d.getFloat("r"), d.getInt("windup"), d.getBoolean("echo"), d.getInt("dir")));
            case KnightFx.GATHER -> ClientVfx.add(new Gather(d.getInt("id"), d.getInt("d"), d.getInt("color")));
            case KnightFx.AXE -> ClientVfx.add(new Axe(ClientVfx.vec(d, "a"), ClientVfx.vec(d, "b"), ClientVfx.vec(d, "c"), ClientVfx.vec(d, "e"), d.getInt("windup"), d.getInt("out"), d.getInt("back"), d.getBoolean("twin")));
            case KnightFx.HOOK -> ClientVfx.add(new Hook(d.getInt("id"), ClientVfx.vec(d, "o"), ClientVfx.vec(d, "v"), d.getFloat("speed"), d.getFloat("range")));
            case KnightFx.TETHER -> {
                int id = d.getInt("id");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Hook hook && hook.id == id) {
                        hook.latched = true;
                    }
                }
                ClientVfx.add(new Tether(level, id, d.getInt("victim"), ClientVfx.vec(d, "p"), d.getInt("d")));
            }
            case KnightFx.IMPACT -> ClientVfx.add(new Impact(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getBoolean("heavy")));
            case KnightFx.NOOSE -> ClientVfx.add(new Noose(level, ClientVfx.vec(d, "c"), d.getIntArray("ids"), d.getInt("gather"), d.getInt("d"), d.getFloat("from"), d.getFloat("to")));
            case KnightFx.NOOSE_BREAK -> {
                int id = d.getInt("id");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Noose noose) {
                        noose.snap(level, id);
                    }
                }
            }
            case KnightFx.NOOSE_SNAP -> ClientVfx.add(new NooseSnap(level, ClientVfx.vec(d, "c"), d.getFloat("r")));
            case KnightFx.PHALANX -> {
                int n = d.getInt("n");
                float[] offsets = new float[n];
                for (int i = 0; i < n; i++) {
                    offsets[i] = d.getFloat("o" + i);
                }
                ClientVfx.add(new Phalanx(ClientVfx.vec(d, "c"), d.getFloat("a"), d.getIntArray("ids"), offsets, d.getFloat("gap"), d.getFloat("back"), d.getFloat("len"), d.getInt("gather"), d.getInt("charge")));
            }
            case KnightFx.OATH -> ClientVfx.add(new Oath(d.getIntArray("ids"), ClientVfx.vec(d, "s"), d.getInt("gather"), d.getInt("form")));
            case KnightFx.OATH_POS -> {
                Vec3 s = ClientVfx.vec(d, "s");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Oath oath && oath.lockAge < 0) {
                        oath.goal = s;
                    }
                }
            }
            case KnightFx.OATH_LOCK -> {
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Oath oath && oath.lockAge < 0) {
                        oath.lock(level, ClientVfx.vec(d, "l"), d.getFloat("r"), d.getInt("lock"), d.getInt("blades"), d.getFloat("a"), d.getFloat("range"));
                    }
                }
            }
            case KnightFx.OATH_FALL -> {
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Oath oath && oath.lockAge >= 0 && oath.fallAge < 0) {
                        oath.fall(d.getInt("fall"), d.getFloat("speed"));
                    }
                }
            }
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

    private static Vec3 hand(Entity entity, float partial) {
        return VfxCanvas.pos(entity, partial).add(0.0, entity.getBbHeight() * 0.5, 0.0);
    }

    private static void wisp(double x, double y, double z, double vx, double vy, double vz, int color, RandomSource random) {
        ClientVfx.spawn(new Mote(A, WISP, true).at(x, y, z).vel(vx, vy, vz).gravity(-0.004).drag(0.92).size(0.18 + random.nextDouble() * 0.12)
                .grow(-0.4).color(color).animate().life(16 + random.nextInt(12)));
    }

    private static void shards(Vec3 c, int amount, double speed, int color, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.5 + random.nextDouble());
            ClientVfx.spawn(new Mote(A, SHARD, true).at(c.x, c.y + 0.4, c.z).vel(Math.cos(a) * s, 0.15 + random.nextDouble() * 0.25, Math.sin(a) * s)
                    .gravity(0.045).drag(0.95).size(0.12 + random.nextDouble() * 0.1).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.5)
                    .collide().color(color).life(22 + random.nextInt(14)));
        }
    }

    private static void glint(Vec3 at, double size, int color) {
        ClientVfx.spawn(new Mote(A, GLINT, true).at(at.x, at.y, at.z).size(size).color(color).animate().life(9));
    }

    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, double t) {
        double u = 1.0 - t;
        return a.scale(u * u).add(b.scale(2.0 * u * t)).add(c.scale(t * t));
    }

    private static Vec3[] segment(Vec3 a, Vec3 b, double upTo) {
        double k = Mth.clamp(upTo, 0.0, 1.0);
        return VfxCanvas.line(a, a.lerp(b, k), 0.5);
    }

    private static final class Reveal extends VfxEffect {
        private static final int LIFE = 18;
        private final int id;

        Reveal(ClientLevel level, int id) {
            this.id = id;
            Entity knight = entity(id);
            if (knight == null) {
                return;
            }
            RandomSource random = level.getRandom();
            Vec3 p = knight.position();
            for (int i = 0; i < ClientVfx.count(10); i++) {
                double a = i * Mth.TWO_PI / 10 + random.nextDouble() * 0.4;
                wisp(p.x + Math.cos(a) * 0.8, p.y + 0.3 + random.nextDouble() * knight.getBbHeight(), p.z + Math.sin(a) * 0.8, Math.cos(a) * 0.08, 0.05, Math.sin(a) * 0.08, GHOST, random);
            }
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity knight = entity(this.id);
            if (knight == null) {
                return;
            }
            float t = this.time(c);
            float k = Mth.clamp(t / LIFE, 0.0F, 1.0F);
            Vec3 p = VfxCanvas.pos(knight, c.partial);
            float a = 1.0F - k;
            c.strip(A, true, SPECTER, GHOST, 0.85F * a).density(12.0).scroll(-t * 10.0).wall(p.x, p.y, p.z, 0.9 + k * 0.9, 3.4 * (1.0 - k * 0.7), t * 0.15, Mth.TWO_PI, false);
            c.strip(A, true, SPECTER, STEEL, 0.6F * a).density(16.0).scroll(-t * 14.0).wall(p.x, p.y, p.z, 1.3 + k * 1.4, 1.6 * (1.0 - k), -t * 0.2, Mth.TWO_PI, false);
            c.faceDecal(A, true, GHOST_DECAL, p.x, p.y + knight.getBbHeight() * 0.55, p.z, 1.4 + k * 1.2, 0.0, STEEL, 0.7F * a * a);
        }
    }

    private static final class Slash extends VfxEffect {
        private final Vec3 c;
        private final float angle;
        private final float sweep;
        private final float radius;
        private final int windup;
        private final boolean echo;
        private final int dir;
        private final ClientLevel level;

        Slash(ClientLevel level, Vec3 c, float angle, float sweep, float radius, int windup, boolean echo, int dir) {
            this.level = level;
            this.c = c;
            this.angle = angle;
            this.sweep = sweep;
            this.radius = radius;
            this.windup = Math.max(1, windup);
            this.echo = echo;
            this.dir = dir;
        }

        @Override
        public boolean tick(ClientLevel level) {
            RandomSource random = level.getRandom();
            if (this.echo && this.age < this.windup && this.age % 3 == 0) {
                wisp(this.c.x + (random.nextDouble() - 0.5) * 0.8, this.c.y + 0.6 + random.nextDouble() * 1.8, this.c.z + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.04, 0.0, STEEL, random);
            }
            if (this.age == this.windup) {
                int amount = ClientVfx.count(7);
                for (int i = 0; i < amount; i++) {
                    double a = this.angle - this.sweep * 0.5 + this.sweep * (i + 0.5) / amount;
                    double r = this.radius * (0.6 + random.nextDouble() * 0.4);
                    double x = this.c.x + Math.cos(a) * r;
                    double z = this.c.z + Math.sin(a) * r;
                    double tx = -Math.sin(a) * this.dir;
                    double tz = Math.cos(a) * this.dir;
                    ClientVfx.spawn(new Mote(A, SHARD, true).at(x, this.c.y + 1.1, z).vel(tx * 0.25 + Math.cos(a) * 0.08, 0.05, tz * 0.25 + Math.sin(a) * 0.08)
                            .gravity(0.02).drag(0.9).size(0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, 0.4).color(this.tone()).life(12 + random.nextInt(6)));
                }
            }
            return super.tick(level);
        }

        private int tone() {
            if (this.echo) {
                return STEEL;
            }
            return PALE;
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + 14;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            double start = this.angle - this.sweep * 0.5;
            if (t < this.windup) {
                float p = t / this.windup;
                int warn = WARNING;
                if (this.echo) {
                    warn = STEEL;
                }
                float a = envelope(t, 3.0F, this.windup, 1.0F);
                c.strip(A, true, DOTS, warn, (0.45F + 0.45F * p) * a).scroll(-t * 6.0).density(18.0).arc(this.c.x, this.c.y, this.c.z, this.radius - 0.3, this.radius, start, this.sweep, true);
                c.strip(A, true, CRESCENT, warn, 0.22F * a).lift(0.02).arc(this.c.x, this.c.y, this.c.z, 0.3, 0.3 + (this.radius - 0.3) * ease(p), start, this.sweep, true);
                if (this.echo) {
                    float flicker = 0.35F + 0.2F * Mth.sin(t * 0.9F);
                    c.faceDecal(A, true, GHOST_DECAL, this.c.x, this.c.y + 1.7, this.c.z, 1.6, 0.0, STEEL, flicker * a);
                }
                return;
            }
            float s = Mth.clamp((t - this.windup) / 4.0F, 0.0F, 1.0F);
            float fade = 1.0F - Mth.clamp((t - this.windup - 4.0F) / 10.0F, 0.0F, 1.0F);
            double from = start;
            double span = this.sweep * ease(s);
            if (this.dir < 0) {
                from = this.angle + this.sweep * 0.5;
                span = -span;
            }
            int tone = this.tone();
            double height = this.c.y + 1.1;
            c.strip(A, true, CRESCENT, tone, 1.0F * fade).fade(0.0F, 1.0F).arc(this.c.x, height, this.c.z, this.radius * 0.35, this.radius, from, span, false);
            c.strip(A, true, CRESCENT, STEEL, 0.6F * fade).fade(0.0F, 1.0F).arc(this.c.x, height - 0.25, this.c.z, this.radius * 0.5, this.radius * 1.05, from, span * 0.92, false);
            c.strip(A, true, CRESCENT, tone, 0.35F * fade).fade(0.0F, 1.0F).lift(0.03).arc(this.c.x, this.c.y, this.c.z, this.radius * 0.35, this.radius, from, span, true);
            double tipAngle = from + span;
            c.sprite(A, true, GLINT, 2, this.c.x + Math.cos(tipAngle) * this.radius * 0.9, height, this.c.z + Math.sin(tipAngle) * this.radius * 0.9, 0.7 * fade, t * 0.3, PALE, fade);
            if (this.echo) {
                c.faceDecal(A, true, GHOST_DECAL, this.c.x, this.c.y + 1.7, this.c.z, 1.6 + (1.0F - fade) * 0.8, 0.0, STEEL, 0.6F * fade * fade);
            }
        }
    }

    private static final class Gather extends VfxEffect {
        private final int id;
        private final int duration;
        private final int color;

        Gather(int id, int duration, int color) {
            this.id = id;
            this.duration = Math.max(1, duration);
            this.color = color;
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity knight = entity(this.id);
            if (knight != null && this.age < this.duration) {
                RandomSource random = level.getRandom();
                Vec3 hand = hand(knight, 1.0F);
                for (int i = 0; i < 2; i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double b = (random.nextDouble() - 0.5) * Mth.PI;
                    Vec3 from = hand.add(Math.cos(a) * Math.cos(b) * 2.6, Math.sin(b) * 2.0, Math.sin(a) * Math.cos(b) * 2.6);
                    Vec3 v = hand.subtract(from).scale(0.11);
                    wisp(from.x, from.y, from.z, v.x, v.y, v.z, this.color, random);
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 2;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity knight = entity(this.id);
            if (knight == null) {
                return;
            }
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            Vec3 pos = VfxCanvas.pos(knight, c.partial);
            Vec3 hand = hand(knight, c.partial);
            float a = envelope(t, 3.0F, this.duration, 2.0F);
            c.strip(A, true, DOTS, this.color, 0.7F * a).density(20.0).scroll(t * 8.0).arc(pos.x, pos.y, pos.z, 1.5, 1.8, t * 0.12, Mth.TWO_PI, true);
            c.sprite(A, true, GLINT, 2, hand.x, hand.y, hand.z, 0.2 + 0.7 * p, t * 0.2, this.color, a);
        }
    }

    private static final class Axe extends VfxEffect {
        private final Vec3 from;
        private final Vec3 out;
        private final Vec3 far;
        private final Vec3 back;
        private final int windup;
        private final int outTicks;
        private final int backTicks;
        private final boolean twin;
        private final Vec3[][] paths;

        Axe(Vec3 from, Vec3 out, Vec3 far, Vec3 back, int windup, int outTicks, int backTicks, boolean twin) {
            this.from = from;
            this.out = out;
            this.far = far;
            this.back = back;
            this.windup = Math.max(1, windup);
            this.outTicks = Math.max(1, outTicks);
            this.backTicks = Math.max(1, backTicks);
            this.twin = twin;
            int count = 1;
            if (twin) {
                count = 2;
            }
            this.paths = new Vec3[count][];
            for (int k = 0; k < count; k++) {
                Vec3[] points = new Vec3[33];
                for (int i = 0; i <= 32; i++) {
                    points[i] = this.position(i / 32.0F * (this.outTicks + this.backTicks), k == 1);
                }
                this.paths[k] = points;
            }
        }

        private Vec3 position(float tick, boolean mirrored) {
            Vec3 first = this.out;
            Vec3 second = this.back;
            if (mirrored) {
                first = this.back;
                second = this.out;
            }
            if (tick <= this.outTicks) {
                return bezier(this.from, first, this.far, Mth.clamp(tick / this.outTicks, 0.0F, 1.0F));
            }
            return bezier(this.far, second, this.from, Mth.clamp((tick - this.outTicks) / this.backTicks, 0.0F, 1.0F));
        }

        @Override
        public boolean tick(ClientLevel level) {
            int flight = this.age - this.windup;
            if (flight >= 0 && flight <= this.outTicks + this.backTicks) {
                RandomSource random = level.getRandom();
                for (int k = 0; k < this.paths.length; k++) {
                    Vec3 p = this.position(flight, k == 1);
                    wisp(p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.06, 0.02, (random.nextDouble() - 0.5) * 0.06, STEEL, random);
                    if (flight == this.outTicks) {
                        shards(p, 5, 0.12, METAL, random);
                    }
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + this.outTicks + this.backTicks + 4;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            if (t < this.windup) {
                float p = t / this.windup;
                float a = envelope(t, 4.0F, this.windup, 1.0F);
                for (Vec3[] path : this.paths) {
                    c.strip(A, true, DOTS, WARNING, 0.55F * a).density(14.0).scroll(-t * 10.0).lift(0.03).path(path, 0.55, true);
                }
                c.sprite(A, true, AXE_SPRITE, 0, this.from.x, this.from.y + 1.6, this.from.z, 1.7 * ease(p), t * 0.08, METAL, 0.95F * a);
                c.sprite(A, true, AXE_SPRITE, 3, this.from.x, this.from.y + 1.6, this.from.z, 2.1 * ease(p), t * 0.08, STEEL, 0.3F * a);
                return;
            }
            float flight = t - this.windup;
            float a = 1.0F - Mth.clamp((flight - this.outTicks - this.backTicks) / 4.0F, 0.0F, 1.0F);
            for (int k = 0; k < this.paths.length; k++) {
                boolean mirrored = k == 1;
                Vec3 p = this.position(flight, mirrored);
                double spin = -flight * 0.9;
                if (mirrored) {
                    spin = -spin;
                }
                Vec3[] trail = new Vec3[8];
                for (int i = 0; i < 8; i++) {
                    trail[i] = this.position(Math.max(0.0F, flight - (7 - i) * 0.8F), mirrored);
                }
                c.strip(A, true, STREAK, GHOST, 0.55F * a).fade(0.0F, 1.0F).scroll(-t * 12.0).path(trail, 1.5, false);
                for (int i = 4; i >= 1; i--) {
                    Vec3 ghost = this.position(Math.max(0.0F, flight - i * 1.2F), mirrored);
                    double ghostSpin = spin + i * 1.08;
                    if (mirrored) {
                        ghostSpin = spin - i * 1.08;
                    }
                    c.sprite(A, true, AXE_SPRITE, 1, ghost.x, ghost.y, ghost.z, 1.6, ghostSpin, STEEL, 0.5F * (1.0F - i / 5.0F) * a);
                }
                c.sprite(A, true, AXE_SPRITE, 3, p.x, p.y, p.z, 1.95, spin, STEEL, 0.35F * a);
                c.sprite(A, true, AXE_SPRITE, 0, p.x, p.y, p.z, 1.6, spin, PALE, a);
                double ground = VfxCanvas.ground(c.level, p.x, p.z, p.y);
                c.flatSprite(A, false, AXE_SPRITE, 3, p.x, ground + 0.03, p.z, 1.2, spin, SHADOW, 0.35F * a);
            }
        }
    }

    private static final class Hook extends VfxEffect {
        private final int id;
        private final Vec3 origin;
        private final Vec3 dir;
        private final float speed;
        private final float range;
        private boolean latched;

        Hook(int id, Vec3 origin, Vec3 dir, float speed, float range) {
            this.id = id;
            this.origin = origin;
            this.dir = dir;
            this.speed = speed;
            this.range = range;
        }

        @Override
        protected boolean alive() {
            return !this.latched && this.age < this.range / Math.max(0.1F, this.speed) + 3;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            Vec3 head = this.origin.add(this.dir.scale(Math.min(this.range, t * this.speed)));
            Vec3 start = this.origin;
            Entity knight = entity(this.id);
            if (knight != null) {
                start = hand(knight, c.partial);
            }
            Vec3[] points = VfxCanvas.line(start, head, 0.5);
            c.strip(A, true, STREAK, STEEL, 0.3F).path(points, 0.8, false);
            c.strip(A, true, CHAIN_STRIP, CHAIN, 0.95F).density(32.0).path(points, 0.32, false);
            c.sprite(A, true, HOOK_SPRITE, 0, head.x, head.y, head.z, 0.75, t * 0.6, PALE, 1.0F);
            c.sprite(A, true, GLINT, 1, head.x, head.y, head.z, 0.5, t * 0.3, STEEL, 0.8F);
        }
    }

    private static final class Tether extends VfxEffect {
        private final int id;
        private final int victim;
        private final Vec3 anchor;
        private final int duration;

        Tether(ClientLevel level, int id, int victim, Vec3 anchor, int duration) {
            this.id = id;
            this.victim = victim;
            this.anchor = anchor;
            this.duration = Math.max(1, duration);
            glint(anchor, 1.2, PALE);
            shards(anchor, 4, 0.08, CHAIN, level.getRandom());
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity knight = entity(this.id);
            if (knight == null) {
                return;
            }
            float t = this.time(c);
            Vec3 start = hand(knight, c.partial);
            Vec3 end = this.anchor;
            Entity caught = entity(this.victim);
            if (caught != null) {
                end = VfxCanvas.pos(caught, c.partial).add(0.0, caught.getBbHeight() * 0.55, 0.0);
            }
            float a = envelope(t, 1.0F, this.duration - 3.0F, 3.0F);
            int n = 12;
            Vec3[] points = new Vec3[n + 1];
            double sag = 0.35 + 0.15 * Mth.sin(t * 0.7F);
            for (int i = 0; i <= n; i++) {
                double u = i / (double) n;
                Vec3 p = start.lerp(end, u);
                double bulge = Math.sin(u * Math.PI);
                points[i] = p.add(0.0, -sag * bulge + Math.sin(t * 2.3 + u * 9.0) * 0.05 * bulge, 0.0);
            }
            c.strip(A, true, STREAK, STEEL, 0.3F * a).scroll(t * 10.0).path(points, 0.9, false);
            c.strip(A, true, CHAIN_STRIP, CHAIN, a).density(32.0).scroll(t * 10.0).path(points, 0.34, false);
            c.sprite(A, true, HOOK_SPRITE, 0, end.x, end.y, end.z, 0.75, t * 0.1, PALE, a);
            c.sprite(A, true, GLINT, (int) (t * 0.5F) % 4, start.x, start.y, start.z, 0.45, 0.0, METAL, a);
        }
    }

    private static final class Impact extends VfxEffect {
        private static final int LIFE = 26;
        private final VfxCanvas.Patch patch;
        private final float radius;
        private final boolean heavy;
        private final double rotation;

        Impact(ClientLevel level, Vec3 c, float radius, boolean heavy) {
            this.patch = VfxCanvas.patch(level, c, radius * 1.3);
            this.radius = radius;
            this.heavy = heavy;
            RandomSource random = level.getRandom();
            this.rotation = random.nextDouble() * Mth.TWO_PI;
            int amount = 6;
            if (heavy) {
                amount = 12;
            }
            shards(c, amount, 0.18, PALE, random);
            for (int i = 0; i < ClientVfx.count(amount); i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                wisp(c.x + Math.cos(a) * radius * 0.5, c.y + 0.3, c.z + Math.sin(a) * radius * 0.5, Math.cos(a) * 0.1, 0.06, Math.sin(a) * 0.1, STEEL, random);
            }
            glint(c.add(0.0, 0.8, 0.0), radius * 0.8, PALE);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / LIFE, 0.0F, 1.0F);
            Vec3 p = this.patch.center;
            float flash = 1.0F - Mth.clamp(t / 8.0F, 0.0F, 1.0F);
            c.decal(A, true, SHATTER, this.patch, this.rotation, 0.4 + 0.6 * ease(Mth.clamp(t / 4.0F, 0.0F, 1.0F)), 0.03, PALE, 0.9F * (1.0F - k));
            c.decal(A, true, SHATTER, this.patch, this.rotation + 0.4, 0.9, 0.025, STEEL, 0.5F * (1.0F - k));
            float w = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
            c.strip(A, true, SPECTER, GHOST, 0.9F * (1.0F - w)).density(12.0).scroll(-t * 12.0).wall(p.x, p.y, p.z, this.radius * (0.3 + 1.2 * ease(w)), 1.8 * (1.0F - w), 0.0, Mth.TWO_PI, true);
            if (this.heavy) {
                c.strip(A, true, CRESCENT, STEEL, 0.8F * (1.0F - w)).lift(0.04).arc(p.x, p.y, p.z, this.radius * 1.6 * ease(w), this.radius * 1.6 * ease(w) + 0.6, 0.0, Mth.TWO_PI, true);
                c.strip(A, true, STREAK, PALE, 0.9F * flash).beam(p, p.add(0.0, 5.0 * flash + 0.5, 0.0), 1.2 * flash + 0.1);
            }
        }
    }

    private static final class Noose extends VfxEffect {
        private final Vec3 c;
        private final int[] ids;
        private final int gather;
        private final int duration;
        private final float from;
        private final float to;
        private final Set<Integer> broken = new HashSet<>();
        private final VfxCanvas.Patch patch;

        Noose(ClientLevel level, Vec3 c, int[] ids, int gather, int duration, float from, float to) {
            this.c = c;
            this.ids = ids;
            this.gather = gather;
            this.duration = duration;
            this.from = from;
            this.to = to;
            this.patch = VfxCanvas.patch(level, c, from);
        }

        void snap(ClientLevel level, int id) {
            int index = -1;
            for (int i = 0; i < this.ids.length; i++) {
                if (this.ids[i] == id) {
                    index = i;
                }
            }
            if (index < 0) {
                return;
            }
            this.broken.add(id);
            RandomSource random = level.getRandom();
            Entity self = entity(id);
            if (self == null) {
                return;
            }
            int n = this.ids.length;
            for (int side : new int[]{index - 1, index + 1}) {
                Entity other = entity(this.ids[(side + n) % n]);
                if (other == null) {
                    continue;
                }
                Vec3 a = self.position();
                Vec3 b = other.position();
                for (int i = 0; i < ClientVfx.count(7); i++) {
                    Vec3 p = a.lerp(b, random.nextDouble());
                    ClientVfx.spawn(new Mote(A, LINK, true).at(p.x, this.c.y + 0.4, p.z).vel((random.nextDouble() - 0.5) * 0.25, 0.2 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.25)
                            .gravity(0.05).drag(0.96).size(0.16).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.6)
                            .collide().color(CHAIN).life(28 + random.nextInt(10)));
                }
            }
            glint(self.position().add(0.0, 1.0, 0.0), 1.4, PALE);
        }

        private double radius(float t) {
            if (t < this.gather) {
                return this.from;
            }
            float p = Mth.clamp((t - this.gather) / this.duration, 0.0F, 1.0F);
            return Mth.lerp(p * p * (3.0F - 2.0F * p), this.from, this.to);
        }

        @Override
        protected boolean alive() {
            return this.age < this.gather + this.duration + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float a = envelope(t, 8.0F, this.gather + this.duration, 6.0F);
            double r = this.radius(t);
            c.decal(A, true, CREST, this.patch, t * 0.01, Math.max(0.2, r / this.from), 0.02, STEEL, 0.35F * a);
            c.strip(A, true, DOTS, WARNING, 0.55F * a).density(16.0).scroll(-t * 6.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, r - 0.3), r, t * 0.02, Mth.TWO_PI, true);
            float draw = Mth.clamp((t - (this.gather - 8.0F)) / 8.0F, 0.0F, 1.0F);
            if (draw <= 0.0F) {
                return;
            }
            int n = this.ids.length;
            double height = this.c.y + 0.35;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                if (this.broken.contains(this.ids[i]) || this.broken.contains(this.ids[j])) {
                    continue;
                }
                Entity ka = entity(this.ids[i]);
                Entity kb = entity(this.ids[j]);
                if (ka == null || kb == null) {
                    continue;
                }
                Vec3 pa = VfxCanvas.pos(ka, c.partial);
                Vec3 pb = VfxCanvas.pos(kb, c.partial);
                Vec3 sa = new Vec3(pa.x, height, pa.z);
                Vec3 sb = new Vec3(pb.x, height, pb.z);
                Vec3[] base = segment(sa, sb, draw);
                Vec3[] points = new Vec3[base.length];
                for (int k = 0; k < base.length; k++) {
                    double u = k / (double) Math.max(1, base.length - 1);
                    double bulge = Math.sin(u * Math.PI);
                    points[k] = base[k].add(0.0, Math.sin(t * 1.7 + u * 8.0 + i) * 0.06 * bulge, 0.0);
                }
                c.strip(A, true, STREAK, STEEL, 0.35F * a).scroll(t * 8.0).path(points, 0.9, false);
                c.strip(A, true, CHAIN_STRIP, CHAIN, a).density(32.0).scroll(t * 3.0).path(points, 0.34, false);
                c.sprite(A, true, GLINT, (int) (t * 0.4F + i) % 4, sa.x, sa.y, sa.z, 0.4, 0.0, METAL, a);
            }
        }
    }

    private static final class NooseSnap extends VfxEffect {
        private final Vec3 c;
        private final float radius;

        NooseSnap(ClientLevel level, Vec3 c, float radius) {
            this.c = c;
            this.radius = radius;
            RandomSource random = level.getRandom();
            for (int i = 0; i < ClientVfx.count(14); i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double s = 0.15 + random.nextDouble() * 0.2;
                ClientVfx.spawn(new Mote(A, LINK, true).at(c.x, c.y + 0.5, c.z).vel(Math.cos(a) * s, 0.25 + random.nextDouble() * 0.2, Math.sin(a) * s)
                        .gravity(0.05).drag(0.96).size(0.18).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.6)
                        .collide().color(CHAIN).life(30 + random.nextInt(10)));
            }
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age == 5) {
                ClientVfx.add(new Impact(level, this.c, this.radius, true));
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < 8;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / 6.0F, 0.0F, 1.0F);
            double r = Mth.lerp(k * k, this.radius * 1.8, 0.3);
            c.strip(A, true, SPECTER, STEEL, 0.9F).density(12.0).scroll(-t * 16.0).wall(this.c.x, this.c.y, this.c.z, r, 1.2 + k * 1.5, t * 0.3, Mth.TWO_PI, true);
            c.strip(A, true, CHAIN_STRIP, CHAIN, 0.9F).density(32.0).lift(0.35).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, r - 0.17), r + 0.17, t * 0.3, Mth.TWO_PI, false);
        }
    }

    private static final class Afterimage extends VfxEffect {
        private final Vec3 p;
        private final double size;
        private final int life;

        Afterimage(Vec3 p, double size, int life) {
            this.p = p;
            this.size = size;
            this.life = life;
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float k = Mth.clamp(this.time(c) / this.life, 0.0F, 1.0F);
            c.faceDecal(A, true, GHOST_DECAL, this.p.x, this.p.y + this.size * 0.5, this.p.z, this.size * 0.6, 0.0, STEEL, 0.5F * (1.0F - k));
        }
    }

    private static final class Phalanx extends VfxEffect {
        private final Vec3 c;
        private final Vec3 dir;
        private final Vec3 side;
        private final int[] ids;
        private final Vec3[][] lanes;
        private final Vec3[] gapLane;
        private final int gather;
        private final int charge;

        Phalanx(Vec3 c, float angle, int[] ids, float[] offsets, float gap, float back, float length, int gather, int charge) {
            this.c = c;
            this.dir = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            this.side = new Vec3(-this.dir.z, 0.0, this.dir.x);
            this.ids = ids;
            this.gather = gather;
            this.charge = charge;
            this.lanes = new Vec3[offsets.length][];
            for (int i = 0; i < offsets.length; i++) {
                Vec3 start = c.add(this.side.scale(offsets[i])).subtract(this.dir.scale(back));
                this.lanes[i] = VfxCanvas.line(start, start.add(this.dir.scale(length)), 1.0);
            }
            Vec3 gapStart = c.add(this.side.scale(gap)).subtract(this.dir.scale(back));
            this.gapLane = VfxCanvas.line(gapStart, gapStart.add(this.dir.scale(length)), 1.0);
        }

        @Override
        public boolean tick(ClientLevel level) {
            int run = this.age - this.gather;
            if (run >= 0 && run < this.charge && run % 2 == 0) {
                RandomSource random = level.getRandom();
                for (int id : this.ids) {
                    Entity knight = entity(id);
                    if (knight == null) {
                        continue;
                    }
                    ClientVfx.add(new Afterimage(knight.position(), knight.getBbHeight(), 10));
                    wisp(knight.getX(), knight.getY() + 0.4, knight.getZ(), -this.dir.x * 0.1, 0.05, -this.dir.z * 0.1, GHOST, random);
                }
            }
            if (run == 0) {
                for (int id : this.ids) {
                    Entity knight = entity(id);
                    if (knight != null) {
                        glint(knight.position().add(0.0, 1.0, 0.0).add(this.dir.scale(2.8)), 1.0, PALE);
                    }
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.gather + this.charge + 12;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float laneAlpha;
            if (t < this.gather) {
                laneAlpha = envelope(t, 6.0F, this.gather, 1.0F) * (0.4F + 0.4F * t / this.gather);
            } else {
                laneAlpha = 0.8F * (1.0F - Mth.clamp((t - this.gather) / this.charge, 0.0F, 1.0F));
            }
            if (laneAlpha > 0.01F) {
                for (Vec3[] lane : this.lanes) {
                    c.strip(A, true, LANE, WARNING, laneAlpha).density(16.0 / 1.7).scroll(-t * 14.0).lift(0.03).path(lane, 1.7, true);
                }
                c.strip(A, true, DOTS, GHOST, laneAlpha * 0.8F).density(16.0).scroll(t * 4.0).lift(0.03).path(this.gapLane, 0.5, true);
            }
            if (t < this.gather - 6.0F) {
                return;
            }
            float lance = Mth.clamp((t - this.gather + 6.0F) / 6.0F, 0.0F, 1.0F);
            float end = 1.0F - Mth.clamp((t - this.gather - this.charge) / 10.0F, 0.0F, 1.0F);
            boolean running = t >= this.gather && t < this.gather + this.charge;
            for (int id : this.ids) {
                Entity knight = entity(id);
                if (knight == null) {
                    continue;
                }
                Vec3 p = VfxCanvas.pos(knight, c.partial);
                Vec3 base = p.add(0.0, 1.0, 0.0).subtract(this.dir.scale(0.4));
                Vec3 tip = base.add(this.dir.scale(3.4 * lance));
                c.strip(A, true, SHAFT, PALE, lance * end).density(118.0 / Math.max(0.1, 3.4 * lance)).beam(base, tip, 0.75);
                c.strip(A, true, STREAK, STEEL, 0.3F * lance * end).beam(base, tip.add(this.dir.scale(0.6)), 1.4);
                if (running) {
                    Vec3[] trail = VfxCanvas.line(p.add(0.0, 1.2, 0.0).subtract(this.dir.scale(7.0)), p.add(0.0, 1.2, 0.0), 1.0);
                    c.strip(A, true, STREAK, STEEL, 0.65F).fade(0.0F, 1.0F).scroll(-t * 20.0).path(trail, 2.2, false);
                    Vec3[] ground = VfxCanvas.line(p.subtract(this.dir.scale(9.0)), p, 1.0);
                    c.strip(A, true, STREAK, GHOST, 0.45F).fade(0.0F, 0.9F).scroll(-t * 20.0).lift(0.04).path(ground, 1.4, true);
                }
            }
        }
    }

    private static final class Oath extends VfxEffect {
        private static final double LENGTH = 8.0;
        private static final int PLANTED = 50;
        private final int[] ids;
        private final int gather;
        private final int form;
        private Vec3 goal;
        private Vec3 shown;
        private Vec3 previous;
        private Vec3 landing = Vec3.ZERO;
        private VfxCanvas.Patch patch;
        private float radius;
        private int lock;
        private int blades;
        private float bladeAngle;
        private float range;
        private float speed;
        int lockAge = -1;
        int fallAge = -1;
        private int fall;

        Oath(int[] ids, Vec3 sword, int gather, int form) {
            this.ids = ids;
            this.goal = sword;
            this.shown = sword;
            this.previous = sword;
            this.gather = gather;
            this.form = form;
        }

        void lock(ClientLevel level, Vec3 landing, float radius, int lock, int blades, float angle, float range) {
            this.lockAge = this.age;
            this.landing = landing;
            this.radius = radius;
            this.lock = lock;
            this.blades = blades;
            this.bladeAngle = angle;
            this.range = range;
            this.patch = VfxCanvas.patch(level, landing, radius + 0.5);
            glint(this.shown, 2.0, OATH);
        }

        void fall(int fall, float speed) {
            this.fallAge = this.age;
            this.fall = Math.max(1, fall);
            this.speed = speed;
        }

        private float impactAge() {
            return this.fallAge + this.fall;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.previous = this.shown;
            this.shown = this.shown.lerp(this.goal, 0.35);
            RandomSource random = level.getRandom();
            if (this.fallAge < 0 && this.age > this.gather) {
                Vec3 s = this.shown;
                double a = random.nextDouble() * Mth.TWO_PI;
                wisp(s.x + Math.cos(a) * 1.2, s.y - random.nextDouble() * 4.0, s.z + Math.sin(a) * 1.2, 0.0, 0.03, 0.0, OATH, random);
            }
            if (this.fallAge >= 0 && this.age == (int) this.impactAge()) {
                ClientVfx.add(new Impact(level, this.landing, this.radius, true));
                shards(this.landing, 16, 0.25, OATH, random);
                for (int i = 0; i < this.blades; i++) {
                    double a = this.bladeAngle + i * Mth.TWO_PI / this.blades;
                    Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
                    Vec3 origin = this.landing.add(0.0, 0.8, 0.0).add(dir.scale(1.2));
                    double reach = this.range;
                    HitResult hit = level.clip(new ClipContext(origin, origin.add(dir.scale(this.range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
                    if (hit.getType() != HitResult.Type.MISS) {
                        reach = hit.getLocation().distanceTo(origin);
                    }
                    ClientVfx.add(new FlyingBlade(origin, dir, this.speed, reach));
                }
            }
            if (this.fallAge >= 0 && this.age == (int) this.impactAge() + PLANTED) {
                shards(this.landing.add(0.0, 2.0, 0.0), 14, 0.15, OATH, random);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            if (this.fallAge >= 0) {
                return this.age < this.impactAge() + PLANTED + 10;
            }
            return this.age < this.gather + this.form + 120;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float formed = Mth.clamp((t - this.gather) / this.form, 0.0F, 1.0F);
            Vec3 s = this.previous.lerp(this.shown, c.partial);
            float fade = 1.0F;
            Vec3 hiltAt = s.add(0.0, LENGTH * 0.3 * ease(formed), 0.0);
            if (this.fallAge >= 0) {
                float f = Mth.clamp((t - this.fallAge) / this.fall, 0.0F, 1.0F);
                Vec3 plantedHilt = this.landing.add(0.0, LENGTH * 0.3 + LENGTH * 0.7 - 1.6, 0.0);
                Vec3 lockedHilt = new Vec3(this.landing.x, s.y + LENGTH * 0.3, this.landing.z);
                hiltAt = lockedHilt.lerp(plantedHilt, f * f);
                float after = t - this.impactAge();
                if (after > PLANTED) {
                    fade = 1.0F - Mth.clamp((after - PLANTED) / 10.0F, 0.0F, 1.0F);
                }
            }
            float beam = Mth.clamp((t - this.gather * 0.5F) / (this.gather * 0.5F), 0.0F, 1.0F);
            if (this.fallAge >= 0) {
                beam *= 1.0F - Mth.clamp((t - this.fallAge) / 4.0F, 0.0F, 1.0F);
            }
            if (beam > 0.01F) {
                Vec3 focus = hiltAt.subtract(0.0, LENGTH * 0.3 * ease(formed), 0.0);
                for (int id : this.ids) {
                    Entity knight = entity(id);
                    if (knight == null) {
                        continue;
                    }
                    Vec3 from = hand(knight, c.partial);
                    c.strip(A, true, STREAK, OATH, 0.8F * beam).scroll(-t * 16.0).beam(from, focus, 0.35);
                    c.strip(A, true, STREAK, STEEL, 0.35F * beam).scroll(-t * 10.0).beam(from, focus, 0.9);
                    c.sprite(A, true, GLINT, (int) (t * 0.4F) % 4, from.x, from.y, from.z, 0.4, 0.0, OATH, beam);
                }
                c.sprite(A, true, GLINT, 2, focus.x, focus.y, focus.z, 0.6 + 1.6 * formed, t * 0.1, OATH, beam);
            }
            if (formed > 0.0F) {
                double l = LENGTH * ease(formed);
                Vec3 tip = hiltAt.subtract(0.0, l, 0.0);
                Vec3 pommel = hiltAt.add(0.0, l * 0.12, 0.0);
                double width = 1.9 * ease(formed);
                c.strip(A, true, BLADE, STEEL, 0.35F * fade).density(128.0 / Math.max(0.1, pommel.distanceTo(tip))).beam(pommel, tip, width * 1.5);
                c.strip(A, true, BLADE, OATH, fade).density(128.0 / Math.max(0.1, pommel.distanceTo(tip))).beam(pommel, tip, width);
                c.strip(A, true, DOTS, OATH, 0.7F * formed * fade).density(16.0).scroll(t * 6.0).arc(hiltAt.x, hiltAt.y - l * 0.18, hiltAt.z, 1.2, 1.5, t * 0.1, Mth.TWO_PI, false);
                c.strip(A, true, SPECTER, GHOST, 0.4F * formed * fade).density(14.0).scroll(-t * 10.0).wall(hiltAt.x, tip.y, hiltAt.z, 0.9, l * 0.8, -t * 0.2, Mth.TWO_PI, false);
            }
            if (this.lockAge >= 0 && this.patch != null && (this.fallAge < 0 || t < this.impactAge() + 4.0F)) {
                float lt = t - this.lockAge;
                float a = envelope(lt, 3.0F, this.lock + this.fall, 4.0F);
                c.decal(A, true, OATH_SIGIL, this.patch, t * 0.03, 1.0, 0.03, WARNING, (0.55F + 0.3F * Mth.sin(t * 0.8F)) * a);
                c.strip(A, true, DOTS, WARNING, 0.7F * a).density(16.0).scroll(-t * 8.0).arc(this.landing.x, this.landing.y, this.landing.z, this.radius - 0.3, this.radius, 0.0, Mth.TWO_PI, true);
                for (int i = 0; i < this.blades; i++) {
                    double ang = this.bladeAngle + i * Mth.TWO_PI / this.blades;
                    Vec3 dir = new Vec3(Math.cos(ang), 0.0, Math.sin(ang));
                    Vec3[] lane = VfxCanvas.line(this.landing.add(dir.scale(1.2)), this.landing.add(dir.scale(this.range)), 1.0);
                    c.strip(A, true, LANE, WARNING, 0.4F * a).density(16.0 / 0.9).scroll(-t * 12.0).lift(0.04).path(lane, 0.9, true);
                }
            }
        }
    }

    private static final class FlyingBlade extends VfxEffect {
        private final Vec3 origin;
        private final Vec3 dir;
        private final float speed;
        private final double reach;
        private boolean burst;

        FlyingBlade(Vec3 origin, Vec3 dir, float speed, double reach) {
            this.origin = origin;
            this.dir = dir;
            this.speed = Math.max(0.05F, speed);
            this.reach = reach;
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (!this.burst && this.age * this.speed >= this.reach) {
                this.burst = true;
                shards(this.origin.add(this.dir.scale(this.reach)), 4, 0.1, OATH, level.getRandom());
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.reach / this.speed + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            double travelled = Math.min(this.reach, t * this.speed);
            float a = 1.0F - Mth.clamp((float) ((t * this.speed - this.reach) / (this.speed * 6.0)), 0.0F, 1.0F);
            Vec3 tip = this.origin.add(this.dir.scale(travelled));
            Vec3 pommel = tip.subtract(this.dir.scale(1.7));
            Vec3[] trail = VfxCanvas.line(this.origin.add(this.dir.scale(Math.max(0.0, travelled - 6.0))), pommel, 0.8);
            c.strip(A, true, STREAK, STEEL, 0.6F * a).fade(0.0F, 1.0F).scroll(-t * 16.0).path(trail, 0.7, false);
            c.strip(A, true, BLADE, OATH, a).density(128.0 / 1.7).beam(pommel, tip, 0.55);
        }
    }
}
