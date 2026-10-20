package com.twilightbossplus.client.vfx;

import com.twilightbossplus.urghast.UrGhastColors;
import com.twilightbossplus.urghast.UrGhastFx;
import java.util.ArrayList;
import java.util.List;
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

public final class UrGhastVfx {
    private static final VfxAtlas A = VfxAtlas.URGHAST;
    private static final int RIPPLE = 0;
    private static final int RAIN = 1;
    private static final int BOLT = 2;
    private static final int BEAM = 3;
    private static final int CLOUD = 4;
    private static final int DASH = 5;
    private static final int FACET = 6;
    private static final int SPLASH = 0;
    private static final int SCAR = 1;
    private static final int SIGIL = 2;
    private static final int EYE = 3;
    private static final int TEAR = 0;
    private static final int ORB = 1;
    private static final int CRYSTAL = 2;
    private static final int SPARK = 3;
    private static final int PUFF = 4;
    private static final int RING = 5;
    private static final int DROP = 6;

    private static final int CRIMSON = UrGhastColors.CRIMSON;
    private static final int WINE = UrGhastColors.WINE;
    private static final int TEARS = UrGhastColors.TEAR;
    private static final int BLUSH = UrGhastColors.BLUSH;
    private static final int STORM = UrGhastColors.STORM;
    private static final int WARNING = UrGhastColors.WARNING;

    private static final double CURTAIN_HEIGHT = 6.0;
    private static final double PRISM_HEIGHT = 1.1;

    private UrGhastVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case UrGhastFx.ORBIT -> ClientVfx.add(new Orbit(d.getInt("id"), d.getInt("n"), d.getInt("d"), d.getInt("gap")));
            case UrGhastFx.TEAR -> ClientVfx.add(new Tear(d.getInt("tid"), ClientVfx.vec(d, "p"), ClientVfx.vec(d, "v"), d.getInt("target"), d.getFloat("speed"), d.getFloat("turn"), d.getInt("life")));
            case UrGhastFx.TEAR_BURST -> {
                int tid = d.getInt("tid");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Tear tear && tear.id == tid) {
                        tear.dead = true;
                    }
                }
                ClientVfx.add(new Splash(level, ClientVfx.vec(d, "p"), d.getFloat("r")));
            }
            case UrGhastFx.WAIL -> ClientVfx.add(new Wail(level, ClientVfx.vec(d, "o"), ClientVfx.vec(d, "v"), d.getFloat("half"), d.getFloat("speed"), d.getFloat("max"), d.getInt("delay")));
            case UrGhastFx.INHALE -> ClientVfx.add(new Inhale(d.getInt("id"), ClientVfx.vec(d, "m"), d.getInt("d")));
            case UrGhastFx.STORM -> {
                int id = d.getInt("id");
                boolean extended = false;
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Storm storm && storm.id == id) {
                        storm.extend(d.getInt("d"));
                        extended = true;
                    }
                }
                if (!extended) {
                    ClientVfx.add(new Storm(id, d.getInt("d")));
                }
            }
            case UrGhastFx.CURTAIN -> ClientVfx.add(new Curtain(level, ClientVfx.vec(d, "p"), ClientVfx.vec(d, "v"), d.getFloat("len"), d.getFloat("gap"), d.getFloat("gw"), d.getFloat("speed"), d.getInt("life")));
            case UrGhastFx.PRISM -> ClientVfx.add(new Prism(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getFloat("a"), d.getInt("d")));
            case UrGhastFx.PRISM_CYCLE -> {
                Vec3 c = ClientVfx.vec(d, "c");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Prism prism && prism.center.distanceToSqr(c) < 1.0) {
                        prism.cycle(d.getFloat("a"), d.getInt("warn"), d.getInt("fire"));
                    }
                }
            }
            case UrGhastFx.PRISM_SHATTER -> {
                Vec3 c = ClientVfx.vec(d, "c");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Prism prism && prism.center.distanceToSqr(c) < 1.0) {
                        prism.shatter(level);
                    }
                }
            }
            case UrGhastFx.BOLT_WARN -> ClientVfx.add(new BoltWarn(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("delay")));
            case UrGhastFx.BOLT -> ClientVfx.add(new Bolt(level, ClientVfx.vec(d, "c"), ClientVfx.vec(d, "s"), d.getFloat("r")));
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

    private static Vec3 side(Vec3 normal) {
        Vec3 right = normal.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        return right.normalize();
    }

    private static void ring(VfxCanvas c, Vec3 center, Vec3 normal, double radius, double width, double spin) {
        Vec3 n = normal.normalize();
        Vec3 right = side(n);
        Vec3 up = right.cross(n).normalize();
        int steps = Mth.clamp((int) (radius * 4.0), 16, 64);
        Vec3[] outer = new Vec3[steps + 1];
        Vec3[] inner = new Vec3[steps + 1];
        double half = width * 0.5;
        for (int i = 0; i <= steps; i++) {
            double a = spin + Mth.TWO_PI * i / steps;
            Vec3 dir = right.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            outer[i] = center.add(dir.scale(radius + half));
            inner[i] = center.add(dir.scale(Math.max(0.0, radius - half)));
        }
        c.band(outer, inner);
    }

    private static double tearRoll(VfxCanvas c, Vec3 dir) {
        double mx = dir.x * c.left.x() + dir.y * c.left.y() + dir.z * c.left.z();
        double my = dir.x * c.up.x() + dir.y * c.up.y() + dir.z * c.up.z();
        return Math.atan2(mx, -my);
    }

    private static void drip(double x, double y, double z, RandomSource random) {
        ClientVfx.spawn(new Mote(A, DROP, true).at(x, y, z).vel((random.nextDouble() - 0.5) * 0.04, -0.05, (random.nextDouble() - 0.5) * 0.04)
                .gravity(0.04).drag(0.98).size(0.22 + random.nextDouble() * 0.1).frame(random.nextInt(4)).collide().color(TEARS).life(18 + random.nextInt(10)));
    }

    private static void droplets(Vec3 at, int amount, double speed, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.4 + random.nextDouble());
            ClientVfx.spawn(new Mote(A, TEAR, true).at(at.x, at.y, at.z).vel(Math.cos(a) * s, 0.12 + random.nextDouble() * 0.22, Math.sin(a) * s)
                    .gravity(0.05).drag(0.95).size(0.14 + random.nextDouble() * 0.12).frame(random.nextInt(4)).collide().color(TEARS).life(20 + random.nextInt(12)));
        }
    }

    private static void sparks(Vec3 at, int amount, double speed, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.5 + random.nextDouble());
            ClientVfx.spawn(new Mote(A, SPARK, true).at(at.x, at.y + 0.2, at.z).vel(Math.cos(a) * s, 0.1 + random.nextDouble() * 0.3, Math.sin(a) * s)
                    .gravity(0.03).drag(0.9).size(0.25 + random.nextDouble() * 0.2).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, 0.0)
                    .color(BLUSH).life(8 + random.nextInt(8)));
        }
    }

    private static final class Orbit extends VfxEffect {
        private final int id;
        private final int count;
        private final int duration;
        private final int gap;
        private final int charge;

        Orbit(int id, int count, int duration, int gap) {
            this.id = id;
            this.count = Math.max(1, count);
            this.duration = duration;
            this.gap = Math.max(1, gap);
            this.charge = Math.max(1, duration - count * gap);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 2;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity ghast = entity(this.id);
            if (ghast == null) {
                return;
            }
            float t = this.time(c);
            float k = Mth.clamp(t / this.charge, 0.0F, 1.0F);
            Vec3 look = ghast.getViewVector(c.partial);
            Vec3 body = VfxCanvas.pos(ghast, c.partial).add(0.0, ghast.getBbHeight() * 0.5, 0.0);
            Vec3 mouth = body.add(look.scale(ghast.getBbWidth() * 0.45));
            double radius = ghast.getBbWidth() * 0.3;
            Vec3 right = side(look);
            Vec3 up = right.cross(look).normalize();
            int fired = 0;
            if (t >= this.charge) {
                fired = Math.min(this.count, (int) ((t - this.charge) / this.gap) + 1);
            }
            int shown = Math.min(this.count, (int) Math.ceil(k * this.count + 0.001));
            double spin = (1.0 - k) * (1.0 - k) * 3.0;
            float fade = 1.0F;
            if (fired >= this.count) {
                fade = Mth.clamp(1.0F - (t - this.charge - (this.count - 1) * this.gap) / 6.0F, 0.0F, 1.0F);
            }
            c.strip(A, true, FACET, TEARS, 0.45F * k * fade).density(3.0).scroll(t * 4.0);
            ring(c, mouth, look, radius, 0.7, spin);
            c.faceDecal(A, true, EYE, mouth.x, mouth.y, mouth.z, ghast.getBbWidth() * 0.22 * (0.8 + 0.2 * k), 0.0, CRIMSON, (0.35F + 0.25F * Mth.sin(t * 0.5F)) * k * fade);
            for (int i = fired; i < shown; i++) {
                double a = Mth.TWO_PI * i / this.count + spin;
                Vec3 p = mouth.add(right.scale(Math.cos(a) * radius)).add(up.scale(Math.sin(a) * radius));
                float bob = 0.12F * Mth.sin(t * 0.3F + i);
                c.sprite(A, true, ORB, (int) (t / 3 + i) % 4, p.x, p.y + bob, p.z, 1.3, 0.0, CRIMSON, 0.5F);
                c.sprite(A, true, TEAR, (int) (t / 4 + i) % 4, p.x, p.y + bob, p.z, 0.6, 0.0, TEARS, 0.95F);
            }
        }
    }

    private static final class Tear extends VfxEffect {
        private static final int TRAIL = 9;
        private final int id;
        private final int target;
        private final double speed;
        private final double turn;
        private final int life;
        private final List<Vec3> trail = new ArrayList<>();
        private Vec3 pos;
        private Vec3 prev;
        private Vec3 dir;
        private boolean dead;

        Tear(int id, Vec3 pos, Vec3 dir, int target, double speed, double turn, int life) {
            this.id = id;
            this.pos = pos;
            this.prev = pos;
            this.dir = dir.normalize();
            this.target = target;
            this.speed = speed;
            this.turn = turn;
            this.life = life;
            this.trail.add(pos);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.prev = this.pos;
            Entity victim = entity(this.target);
            if (victim != null) {
                Vec3 want = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0).subtract(this.pos);
                if (want.lengthSqr() > 1.0E-6) {
                    Vec3 steered = this.dir.lerp(want.normalize(), this.turn);
                    if (steered.lengthSqr() > 1.0E-6) {
                        this.dir = steered.normalize();
                    }
                }
            }
            Vec3 next = this.pos.add(this.dir.scale(this.speed));
            HitResult hit = level.clip(new ClipContext(this.pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
            if (hit.getType() != HitResult.Type.MISS) {
                next = hit.getLocation();
                this.dead = true;
            }
            this.pos = next;
            this.trail.add(next);
            if (this.trail.size() > TRAIL) {
                this.trail.remove(0);
            }
            if (this.age % 3 == 0) {
                drip(this.pos.x, this.pos.y, this.pos.z, level.getRandom());
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return !this.dead && this.age < this.life + 4;
        }

        @Override
        public void render(VfxCanvas c) {
            Vec3 p = this.prev.lerp(this.pos, c.partial);
            float t = this.time(c);
            if (this.trail.size() >= 2) {
                Vec3[] points = new Vec3[this.trail.size()];
                for (int i = 0; i < points.length - 1; i++) {
                    points[i] = this.trail.get(i);
                }
                points[points.length - 1] = p;
                c.strip(A, true, BEAM, CRIMSON, 0.65F).density(3.0).scroll(-t * 12.0).fade(0.0F, 1.0F).path(points, 0.45, false);
            }
            c.sprite(A, true, ORB, (int) (t / 2) % 4, p.x, p.y, p.z, 1.0, 0.0, CRIMSON, 0.55F);
            c.sprite(A, true, TEAR, (int) (t / 3) % 4, p.x, p.y, p.z, 0.55, tearRoll(c, this.dir), TEARS, 1.0F);
        }
    }

    private static final class Splash extends VfxEffect {
        private static final int LIFE = 34;
        private final Vec3 at;
        private final double radius;
        private final double rotation;
        @Nullable
        private final VfxCanvas.Patch puddle;

        Splash(ClientLevel level, Vec3 at, double radius) {
            this.at = at;
            this.radius = radius;
            RandomSource random = level.getRandom();
            this.rotation = random.nextDouble() * Mth.TWO_PI;
            double ground = VfxCanvas.ground(level, at.x, at.z, at.y + 0.5);
            if (at.y - ground < 2.5) {
                this.puddle = VfxCanvas.patch(level, new Vec3(at.x, ground, at.z), radius);
            } else {
                this.puddle = null;
            }
            droplets(at, 12, 0.18, random);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
            if (t < 10.0F) {
                c.faceDecal(A, true, SPLASH, this.at.x, this.at.y, this.at.z, this.radius * (0.4 + 0.8 * ease(k)), this.rotation, TEARS, 1.0F - k);
                c.sprite(A, true, RING, Math.min(3, (int) (k * 4)), this.at.x, this.at.y, this.at.z, this.radius * 1.2, 0.0, BLUSH, 1.0F - k);
            }
            if (this.puddle != null) {
                float a = envelope(t, 2.0F, 14.0F, 20.0F);
                c.decal(A, true, SPLASH, this.puddle, this.rotation, 0.6 + 0.4 * ease(k), 0.03, CRIMSON, 0.7F * a);
            }
        }
    }

    private static final class Wail extends VfxEffect {
        private final Vec3 origin;
        private final Vec3 dir;
        private final double tan;
        private final double speed;
        private final double max;
        private final int delay;

        Wail(ClientLevel level, Vec3 origin, Vec3 dir, float half, double speed, double max, int delay) {
            this.origin = origin;
            this.dir = dir.normalize();
            this.tan = Math.tan(half);
            this.speed = speed;
            this.max = max;
            this.delay = delay;
            RandomSource random = level.getRandom();
            for (int i = 0; i < ClientVfx.count(6); i++) {
                Vec3 v = this.dir.scale(0.3 + random.nextDouble() * 0.3).add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
                ClientVfx.spawn(new Mote(A, RING, true).at(origin.x, origin.y, origin.z).vel(v.x, v.y, v.z).drag(0.9).size(0.6).grow(0.12).animate().color(BLUSH).life(12));
            }
        }

        @Override
        protected boolean alive() {
            return (this.age - this.delay) * this.speed < this.max + 6.0;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c) - this.delay;
            if (t <= 0.0F) {
                return;
            }
            double front = t * this.speed;
            for (int k = 0; k < 3; k++) {
                double r = front - k * 1.8;
                if (r <= 0.6 || r > this.max) {
                    continue;
                }
                float a = (float) (1.0 - r / this.max) * (1.0F - k * 0.3F);
                int color = CRIMSON;
                if (k == 0) {
                    color = BLUSH;
                }
                Vec3 center = this.origin.add(this.dir.scale(r));
                c.strip(A, true, RIPPLE, color, 0.9F * a).density(4.0).scroll(t * 6.0 + k * 20.0);
                ring(c, center, this.dir, Math.max(0.4, r * this.tan), 1.0 + r * 0.05, t * 0.05 + k);
            }
        }
    }

    private static final class Inhale extends VfxEffect {
        private final int id;
        private final Vec3 mouth;
        private final int duration;

        Inhale(int id, Vec3 mouth, int duration) {
            this.id = id;
            this.mouth = mouth;
            this.duration = duration;
        }

        private Vec3 mouth(float partial) {
            Entity ghast = entity(this.id);
            if (ghast == null) {
                return this.mouth;
            }
            return VfxCanvas.pos(ghast, partial).add(0.0, ghast.getBbHeight() * 0.5, 0.0).add(ghast.getViewVector(partial).scale(ghast.getBbWidth() * 0.45));
        }

        @Override
        public boolean tick(ClientLevel level) {
            RandomSource random = level.getRandom();
            Vec3 m = this.mouth(1.0F);
            for (int i = 0; i < ClientVfx.count(3); i++) {
                double yaw = random.nextDouble() * Mth.TWO_PI;
                double pitch = (random.nextDouble() - 0.5) * Math.PI;
                double dist = 7.0 + random.nextDouble() * 4.0;
                Vec3 off = new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch)).scale(dist);
                Vec3 v = off.scale(-1.0 / 14.0);
                ClientVfx.spawn(new Mote(A, ORB, true).at(m.x + off.x, m.y + off.y, m.z + off.z).vel(v.x, v.y, v.z).drag(1.0).size(0.35).grow(-0.5)
                        .frame(random.nextInt(4)).color(TEARS).life(14));
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 2;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity ghast = entity(this.id);
            Vec3 m = this.mouth(c.partial);
            Vec3 look = new Vec3(0.0, 0.0, 1.0);
            if (ghast != null) {
                look = ghast.getViewVector(c.partial);
            }
            float t = this.time(c);
            float k = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            for (int i = 0; i < 3; i++) {
                float phase = (t * 0.09F + i / 3.0F) % 1.0F;
                double radius = 0.6 + 6.0 * (1.0 - phase);
                c.strip(A, true, RIPPLE, CRIMSON, 0.85F * phase).density(4.0).scroll(-t * 8.0);
                ring(c, m, look, radius, 0.9, i);
            }
            c.sprite(A, true, ORB, (int) (t / 2) % 4, m.x, m.y, m.z, 1.5 + 2.0 * k, 0.0, CRIMSON, 0.4F + 0.5F * k);
        }
    }

    private static final class Storm extends VfxEffect {
        private final int id;
        private int end;

        Storm(int id, int duration) {
            this.id = id;
            this.end = duration;
        }

        void extend(int duration) {
            this.end = Math.max(this.end, this.age + duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity ghast = entity(this.id);
            if (ghast != null && this.age < this.end) {
                RandomSource random = level.getRandom();
                double w = ghast.getBbWidth();
                for (int i = 0; i < ClientVfx.count(3); i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double r = Math.sqrt(random.nextDouble()) * w * 0.55;
                    drip(ghast.getX() + Math.cos(a) * r, ghast.getY() - 0.8, ghast.getZ() + Math.sin(a) * r, random);
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.end + 10;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity ghast = entity(this.id);
            if (ghast == null) {
                return;
            }
            float t = this.time(c);
            float a = Mth.clamp(t / 8.0F, 0.0F, 1.0F) * Mth.clamp((this.end + 10 - t) / 10.0F, 0.0F, 1.0F);
            Vec3 p = VfxCanvas.pos(ghast, c.partial);
            double w = ghast.getBbWidth();
            double y = p.y - 0.6;
            c.strip(A, false, CLOUD, STORM, 0.85F * a).density(3.0).scroll(t * 1.5).wall(p.x, y - 2.2, p.z, w * 0.55, 2.6, t * 0.01, Mth.TWO_PI, false);
            c.strip(A, false, CLOUD, WINE, 0.7F * a).density(3.0).scroll(-t * 1.0).wall(p.x, y - 1.4, p.z, w * 0.4, 1.8, -t * 0.015, Mth.TWO_PI, false);
            for (int i = 0; i < 12; i++) {
                double ang = Mth.TWO_PI * i / 12 + t * 0.01;
                double r = w * (0.2 + 0.13 * (i % 3));
                c.sprite(A, false, PUFF, i % 4, p.x + Math.cos(ang) * r, y - 0.6 - (i % 2) * 0.8, p.z + Math.sin(ang) * r, w * 0.13, i, STORM, 0.9F * a);
            }
            if ((int) t % 23 < 2) {
                c.sprite(A, true, SPARK, (int) t % 4, p.x + Math.sin(t) * w * 0.3, y - 1.5, p.z + Math.cos(t) * w * 0.3, 1.6, 0.0, CRIMSON, 0.9F * a);
            }
        }
    }

    private static final class Curtain extends VfxEffect {
        private final ClientLevel level;
        private final Vec3 start;
        private final Vec3 motion;
        private final Vec3 side;
        private final double length;
        private final double gap;
        private final double gapWidth;
        private final double speed;
        private final int life;
        private final double[] phases;

        Curtain(ClientLevel level, Vec3 start, Vec3 motion, double length, double gap, double gapWidth, double speed, int life) {
            this.level = level;
            this.start = start;
            this.motion = new Vec3(motion.x, 0.0, motion.z).normalize();
            this.side = new Vec3(-this.motion.z, 0.0, this.motion.x);
            this.length = length;
            this.gap = gap;
            this.gapWidth = gapWidth;
            this.speed = speed;
            this.life = life;
            int columns = (int) Math.ceil(length / 0.55) + 1;
            this.phases = new double[columns];
            RandomSource random = level.getRandom();
            for (int i = 0; i < columns; i++) {
                this.phases[i] = random.nextDouble() * 128.0;
            }
        }

        private Vec3 center(float t) {
            return this.start.add(this.motion.scale(t * this.speed));
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age % 2 == 0 && this.age < this.life - 6) {
                RandomSource random = level.getRandom();
                Vec3 c = this.center(this.age);
                double along = (random.nextDouble() - 0.5) * this.length;
                if (Math.abs(along - this.gap) > this.gapWidth * 0.5) {
                    Vec3 p = c.add(this.side.scale(along));
                    double y = VfxCanvas.ground(level, p.x, p.z, this.start.y + 2.0);
                    droplets(new Vec3(p.x, y + 0.1, p.z), 1, 0.06, random);
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float a = envelope(t, 8.0F, this.life - 10.0F, 10.0F);
            if (a <= 0.01F) {
                return;
            }
            Vec3 center = this.center(t);
            double half = this.length * 0.5;
            for (int i = 0; i < this.phases.length; i++) {
                double along = -half + this.length * i / (this.phases.length - 1);
                if (Math.abs(along - this.gap) < this.gapWidth * 0.5) {
                    continue;
                }
                Vec3 p = center.add(this.side.scale(along));
                double y = VfxCanvas.ground(this.level, p.x, p.z, this.start.y + 2.0);
                float edge = (float) Mth.clamp((half - Math.abs(along)) / 1.5, 0.25, 1.0);
                Vec3 top = new Vec3(p.x, y + CURTAIN_HEIGHT, p.z);
                Vec3 bottom = new Vec3(p.x, y, p.z);
                c.strip(A, true, RAIN, TEARS, 0.85F * a * edge).density(9.0).scroll(-t * 26.0 + this.phases[i]).fade(0.2F, 1.0F).path(new Vec3[]{top, bottom}, 0.75, false);
                if ((i + (int) (t / 3)) % 3 == 0) {
                    c.flatSprite(A, true, RING, ((int) t + i) % 4, p.x, y + 0.03, p.z, 0.55, 0.0, BLUSH, 0.6F * a);
                }
            }
            Vec3 left = center.add(this.side.scale(-half));
            Vec3 right = center.add(this.side.scale(half));
            Vec3[] cloud = VfxCanvas.line(left.add(0.0, CURTAIN_HEIGHT + 0.4, 0.0), right.add(0.0, CURTAIN_HEIGHT + 0.4, 0.0), 1.0);
            c.strip(A, false, CLOUD, STORM, 0.8F * a).density(5.0).scroll(t * 2.0).path(cloud, 1.4, false);
            for (int s = -1; s <= 1; s += 2) {
                Vec3 p = center.add(this.side.scale(this.gap + s * this.gapWidth * 0.5));
                double y = VfxCanvas.ground(this.level, p.x, p.z, this.start.y + 2.0);
                float pulse = 0.6F + 0.4F * Mth.sin(t * 0.4F);
                c.sprite(A, true, ORB, (int) (t / 3) % 4, p.x, y + 0.6, p.z, 0.9, 0.0, BLUSH, pulse * a);
                c.sprite(A, true, CRYSTAL, (int) (t / 4) % 4, p.x, y + 0.6, p.z, 0.45, 0.0, BLUSH, a);
            }
        }
    }

    private static final class Prism extends VfxEffect {
        private final Vec3 center;
        private final double radius;
        private final int duration;
        private final VfxCanvas.Patch patch;
        private double angle;
        private double from;
        private double to;
        private boolean cycling;
        private int cycleStart;
        private int warn = 1;
        private int fire = 1;
        private int shatterAge = -1;

        Prism(ClientLevel level, Vec3 center, double radius, double angle, int duration) {
            this.center = center;
            this.from = angle;
            this.to = angle;
            this.radius = radius;
            this.duration = duration;
            this.patch = VfxCanvas.patch(level, center, radius + 1.0);
            RandomSource random = level.getRandom();
            for (int k = 0; k < 6; k++) {
                double a = angle + Mth.TWO_PI * k / 6;
                Vec3 p = center.add(Math.cos(a) * radius, 0.2, Math.sin(a) * radius);
                droplets(p, 3, 0.08, random);
            }
        }

        void cycle(double angle, int warn, int fire) {
            this.from = this.displayAngle(this.age);
            this.to = angle;
            this.cycling = true;
            this.cycleStart = this.age;
            this.warn = Math.max(1, warn);
            this.fire = Math.max(1, fire);
        }

        void shatter(ClientLevel level) {
            if (this.shatterAge >= 0) {
                return;
            }
            this.shatterAge = this.age;
            RandomSource random = level.getRandom();
            double a0 = this.displayAngle(this.age);
            for (int k = 0; k < 6; k++) {
                Vec3 p = this.crystal(a0, k, 0.0F);
                for (int i = 0; i < ClientVfx.count(5); i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double s = 0.08 + random.nextDouble() * 0.12;
                    ClientVfx.spawn(new Mote(A, CRYSTAL, true).at(p.x, p.y, p.z).vel(Math.cos(a) * s, 0.15 + random.nextDouble() * 0.2, Math.sin(a) * s)
                            .gravity(0.05).drag(0.95).size(0.18 + random.nextDouble() * 0.12).frame(random.nextInt(4))
                            .spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.6).collide().color(BLUSH).life(24 + random.nextInt(12)));
                }
                ClientVfx.spawn(new Mote(A, RING, true).at(p.x, p.y, p.z).size(0.8).grow(0.25).animate().color(CRIMSON).life(10));
            }
        }

        private double displayAngle(float t) {
            if (!this.cycling) {
                return this.to;
            }
            float k = Mth.clamp((t - this.cycleStart) / 6.0F, 0.0F, 1.0F);
            return Mth.lerp(ease(k), this.from, this.to);
        }

        private Vec3 crystal(double angle, int k, float t) {
            double a = angle + Math.PI / 3.0 * k;
            float rise = Mth.clamp(t / 20.0F, 0.0F, 1.0F);
            if (this.shatterAge >= 0 || t <= 0.0F) {
                rise = 1.0F;
            }
            double bob = 0.12 * Math.sin(t * 0.15 + k);
            return this.center.add(Math.cos(a) * this.radius, PRISM_HEIGHT * ease(rise) + bob, Math.sin(a) * this.radius);
        }

        @Override
        protected boolean alive() {
            if (this.shatterAge >= 0) {
                return this.age < this.shatterAge + 14;
            }
            return this.age < this.duration + 30;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float a = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
            if (this.shatterAge >= 0) {
                a *= Mth.clamp(1.0F - (t - this.shatterAge) / 12.0F, 0.0F, 1.0F);
            }
            c.decal(A, true, SIGIL, this.patch, t * 0.01, 1.0, 0.03, CRIMSON, 0.3F * a);
            c.strip(A, true, FACET, TEARS, 0.55F * a).density(3.0).scroll(t * 3.0).arc(this.center.x, this.center.y, this.center.z, this.radius - 0.35, this.radius + 0.35, t * 0.02, Mth.TWO_PI, true);
            if (this.shatterAge < 0) {
                double shown = this.displayAngle(t);
                for (int k = 0; k < 6; k++) {
                    Vec3 p = this.crystal(shown, k, t);
                    c.sprite(A, true, ORB, (int) (t / 3 + k) % 4, p.x, p.y, p.z, 1.6, 0.0, CRIMSON, 0.45F * a);
                    c.sprite(A, true, CRYSTAL, (int) (t / 4 + k) % 4, p.x, p.y, p.z, 0.75, 0.0, BLUSH, a);
                }
            }
            if (!this.cycling || this.shatterAge >= 0) {
                return;
            }
            float local = t - this.cycleStart;
            for (int i = 0; i < 3; i++) {
                double ang = this.to + Math.PI / 3.0 * i;
                Vec3 offset = new Vec3(Math.cos(ang) * this.radius, 0.0, Math.sin(ang) * this.radius);
                Vec3 p0 = this.center.add(offset.x, PRISM_HEIGHT, offset.z);
                Vec3 p1 = this.center.add(-offset.x, PRISM_HEIGHT, -offset.z);
                if (local < this.warn) {
                    float k = local / this.warn;
                    float flicker = 0.55F + 0.45F * Mth.sin(local * 1.6F);
                    c.strip(A, true, DASH, WARNING, (0.35F + 0.5F * k) * flicker).density(2.0).scroll(-t * 6.0).beam(p0, p1, 0.18 + 0.12 * k);
                    c.strip(A, true, DASH, WARNING, 0.45F * k).density(1.2).scroll(t * 4.0).path(VfxCanvas.line(this.center.add(offset), this.center.subtract(offset), 1.0), 0.9, true);
                } else if (local < this.warn + this.fire) {
                    float k = (local - this.warn) / this.fire;
                    double width = 1.3 * (1.0 - 0.5 * k);
                    c.strip(A, true, BEAM, CRIMSON, 0.95F).density(2.0).scroll(t * 20.0).beam(p0, p1, width);
                    c.strip(A, true, BEAM, BLUSH, 1.0F).density(2.5).scroll(-t * 28.0).beam(p0, p1, width * 0.4);
                    c.strip(A, true, BEAM, CRIMSON, 0.5F * (1.0F - k)).density(1.0).scroll(t * 10.0).path(VfxCanvas.line(this.center.add(offset), this.center.subtract(offset), 1.0), 1.6, true);
                }
            }
            if (local >= this.warn && local < this.warn + this.fire) {
                c.sprite(A, true, RING, Math.min(3, (int) ((local - this.warn) / this.fire * 4)), this.center.x, this.center.y + PRISM_HEIGHT, this.center.z, 2.4, 0.0, BLUSH, 0.8F);
            }
        }
    }

    private static final class BoltWarn extends VfxEffect {
        private final Vec3 c;
        private final double radius;
        private final int delay;
        private final double rotation;
        private final VfxCanvas.Patch patch;

        BoltWarn(ClientLevel level, Vec3 c, double radius, int delay) {
            this.c = c;
            this.radius = radius;
            this.delay = Math.max(1, delay);
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.patch = VfxCanvas.patch(level, c, radius * 1.3);
        }

        @Override
        public boolean tick(ClientLevel level) {
            RandomSource random = level.getRandom();
            if (random.nextFloat() < 0.4F) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double r = random.nextDouble() * this.radius;
                ClientVfx.spawn(new Mote(A, SPARK, true).at(this.c.x + Math.cos(a) * r, this.c.y + 0.1, this.c.z + Math.sin(a) * r).vel(0.0, 0.08, 0.0)
                        .drag(0.95).size(0.25).frame(random.nextInt(4)).color(WARNING).life(8));
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.delay + 1;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float k = Mth.clamp(t / this.delay, 0.0F, 1.0F);
            double r = this.radius * (1.6 - 0.6 * k);
            canvas.strip(A, true, DASH, WARNING, 0.5F + 0.5F * k).density(3.0).scroll(t * 4.0).arc(this.c.x, this.c.y, this.c.z, r - 0.22, r + 0.22, t * 0.12, Mth.TWO_PI, true);
            canvas.decal(A, true, SCAR, this.patch, this.rotation, 0.6 + 0.4 * k, 0.03, CRIMSON, 0.2F + 0.55F * k * k);
        }
    }

    private static final class Bolt extends VfxEffect {
        private static final int FLASH = 8;
        private static final int LIFE = 50;
        private final Vec3 c;
        private final Vec3 sky;
        private final double radius;
        private final double rotation;
        private final VfxCanvas.Patch patch;
        private final RandomSource random;
        private Vec3[] main = new Vec3[0];
        private final List<Vec3[]> forks = new ArrayList<>();

        Bolt(ClientLevel level, Vec3 c, Vec3 sky, double radius) {
            this.c = c;
            this.sky = sky;
            this.radius = radius;
            this.random = level.getRandom();
            this.rotation = this.random.nextDouble() * Mth.TWO_PI;
            this.patch = VfxCanvas.patch(level, c, radius * 1.4);
            this.shape();
            sparks(c, 14, 0.25, this.random);
            droplets(c.add(0.0, 0.2, 0.0), 8, 0.2, this.random);
            ClientVfx.spawn(new Mote(A, RING, true).at(c.x, c.y + 0.4, c.z).size(radius).grow(0.15).animate().color(BLUSH).life(10));
        }

        private void shape() {
            Vec3 span = this.c.subtract(this.sky);
            double length = span.length();
            int n = Mth.clamp((int) (length / 1.6), 4, 40);
            Vec3 right = side(span.normalize());
            Vec3 up = right.cross(span.normalize()).normalize();
            this.main = new Vec3[n + 1];
            for (int i = 0; i <= n; i++) {
                double f = i / (double) n;
                double amp = 1.4 * Math.sin(f * Math.PI);
                this.main[i] = this.sky.add(span.scale(f)).add(right.scale((this.random.nextDouble() - 0.5) * 2.0 * amp)).add(up.scale((this.random.nextDouble() - 0.5) * 2.0 * amp));
            }
            this.main[0] = this.sky;
            this.main[n] = this.c;
            this.forks.clear();
            for (int b = 0; b < 2; b++) {
                int at = n / 3 + this.random.nextInt(Math.max(1, n / 2));
                Vec3 p = this.main[Math.min(at, n)];
                double ang = this.random.nextDouble() * Mth.TWO_PI;
                Vec3[] fork = new Vec3[5];
                fork[0] = p;
                for (int i = 1; i < 5; i++) {
                    fork[i] = fork[i - 1].add(Math.cos(ang) * 0.9 + (this.random.nextDouble() - 0.5), -1.0 - this.random.nextDouble() * 0.6, Math.sin(ang) * 0.9 + (this.random.nextDouble() - 0.5));
                }
                this.forks.add(fork);
            }
        }

        @Override
        public boolean tick(ClientLevel level) {
            if (this.age < FLASH && this.age % 2 == 1) {
                this.shape();
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            if (t < FLASH) {
                float a = 1.0F - t / FLASH;
                canvas.strip(A, true, BOLT, CRIMSON, a).density(2.0).scroll(t * 16.0).path(this.main, 1.6, false);
                canvas.strip(A, true, BOLT, BLUSH, a).density(3.0).scroll(-t * 20.0).path(this.main, 0.55, false);
                for (Vec3[] fork : this.forks) {
                    canvas.strip(A, true, BOLT, CRIMSON, 0.8F * a).density(3.0).scroll(t * 12.0).fade(1.0F, 0.0F).path(fork, 0.7, false);
                }
                canvas.faceDecal(A, true, SPLASH, this.c.x, this.c.y + 0.6, this.c.z, this.radius * (0.8 + t * 0.15), this.rotation, BLUSH, 0.9F * a);
            }
            float scar = envelope(t, 1.0F, 12.0F, LIFE - 12.0F);
            canvas.decal(A, false, SCAR, this.patch, this.rotation, 1.1, 0.02, WINE, 0.65F * scar);
            canvas.decal(A, true, SCAR, this.patch, this.rotation, 1.0, 0.035, CRIMSON, 0.85F * scar * scar);
        }
    }
}
