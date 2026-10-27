package com.twilightbossplus.client.vfx;

import com.twilightbossplus.yeti.YetiColors;
import com.twilightbossplus.yeti.YetiFx;
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

public final class YetiVfx {
    private static final VfxAtlas A = VfxAtlas.YETI;
    private static final int MIST = 0;
    private static final int RIME = 1;
    private static final int TRACK = 2;
    private static final int WIND = 3;
    private static final int DASH = 4;
    private static final int FACET = 5;
    private static final int ICE = 6;
    private static final int FLAKE_DECAL = 0;
    private static final int TILE = 1;
    private static final int CRACK = 2;
    private static final int BALL = 3;
    private static final int FLAKE = 0;
    private static final int SHARD = 2;
    private static final int PUFF = 3;
    private static final int GLINT = 4;
    private static final int CHUNK = 5;

    private static final int FROST = YetiColors.FROST;
    private static final int GLACIER = YetiColors.GLACIER;
    private static final int DEEP = YetiColors.DEEP;
    private static final int SNOW = YetiColors.SNOW;
    private static final int SHADE = YetiColors.SHADE;
    private static final int WARNING = YetiColors.WARNING;
    private static final int ICE_BODY = 0x9ED8F5;

    private YetiVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case YetiFx.CLAP -> ClientVfx.add(new Clap(d.getInt("id"), d.getFloat("reach"), d.getFloat("half"), d.getInt("windup")));
            case YetiFx.MARK -> ClientVfx.add(new Mark(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("delay")));
            case YetiFx.LEAP -> ClientVfx.add(new Trail(d.getInt("id"), d.getInt("d")));
            case YetiFx.LANDING -> ClientVfx.add(new Landing(level, ClientVfx.vec(d, "c"), d.getFloat("r")));
            case YetiFx.SNOWBALL -> ClientVfx.add(new Ball(level, d.getInt("bid"), ClientVfx.vec(d, "p"), ClientVfx.vec(d, "v"), d.getFloat("speed"), d.getFloat("from"), d.getFloat("to"), d.getInt("life")));
            case YetiFx.SNOWBALL_BREAK -> {
                int bid = d.getInt("bid");
                for (VfxEffect effect : ClientVfx.effects()) {
                    if (effect instanceof Ball ball && ball.id == bid) {
                        ball.dead = true;
                    }
                }
                ClientVfx.add(new Burst(level, ClientVfx.vec(d, "p"), d.getFloat("r")));
            }
            case YetiFx.TILES -> ClientVfx.add(new Tiles(level, ClientVfx.vec(d, "c"), d.getFloat("size"), d.getInt("half"), d.getInt("parity"), d.getInt("warn")));
            case YetiFx.SPIKES -> ClientVfx.add(new Spikes(level, ClientVfx.vec(d, "c"), d.getFloat("size"), d.getInt("half"), d.getInt("parity")));
            case YetiFx.ROAR -> ClientVfx.add(new Blizzard(level, d.getInt("id"), d.getInt("d")));
            case YetiFx.SHARDS -> {
                Vec3 c = ClientVfx.vec(d, "c");
                int n = d.getInt("n");
                double offset = d.getFloat("a");
                for (int i = 0; i < n; i++) {
                    double a = offset + Mth.TWO_PI * i / n;
                    Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
                    ClientVfx.add(new Shard(level, c.add(dir.scale(1.5)), dir, d.getFloat("speed"), d.getFloat("range")));
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

    private static boolean lit(int ix, int iz, int parity) {
        return Math.floorMod(ix + iz, 2) == parity || (ix == 0 && iz == 0);
    }

    private static void crystal(VfxCanvas c, double x, double y, double z, double height, double width, double yaw, double lean, float alpha) {
        if (height < 0.05 || alpha <= 0.01F) {
            return;
        }
        Vec3 base = new Vec3(x, y - 0.1, z);
        Vec3 tip = base.add(Math.cos(yaw) * lean * height, height, Math.sin(yaw) * lean * height);
        double dens = 128.0 / height;
        for (int k = 0; k < 2; k++) {
            double a = yaw + Math.PI * 0.25 + k * Math.PI * 0.5;
            Vec3 side = new Vec3(Math.cos(a) * width * 0.5, 0.0, Math.sin(a) * width * 0.5);
            Vec3[] outer = new Vec3[]{base.add(side), tip};
            Vec3[] inner = new Vec3[]{base.subtract(side), tip};
            c.strip(A, false, ICE, ICE_BODY, 0.88F * alpha).density(dens);
            c.band(outer, inner);
            c.strip(A, true, ICE, FROST, 0.3F * alpha).density(dens);
            c.band(outer, inner);
        }
    }

    private static void puff(double x, double y, double z, double vx, double vy, double vz, double size, int color, RandomSource random) {
        ClientVfx.spawn(new Mote(A, PUFF, false).at(x, y, z).vel(vx, vy, vz).gravity(-0.002).drag(0.9).size(size * (0.7 + random.nextDouble() * 0.6))
                .grow(0.6).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.1).color(color).life(18 + random.nextInt(12)));
    }

    private static void chunks(Vec3 c, int amount, double speed, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.4 + random.nextDouble());
            ClientVfx.spawn(new Mote(A, CHUNK, false).at(c.x, c.y + 0.3, c.z).vel(Math.cos(a) * s, 0.2 + random.nextDouble() * 0.3, Math.sin(a) * s)
                    .gravity(0.05).drag(0.96).size(0.18 + random.nextDouble() * 0.15).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.4)
                    .collide().color(SNOW).life(26 + random.nextInt(14)));
        }
    }

    private static void flakes(Vec3 c, int amount, double speed, RandomSource random) {
        for (int i = 0; i < ClientVfx.count(amount); i++) {
            double a = random.nextDouble() * Mth.TWO_PI;
            double s = speed * (0.4 + random.nextDouble());
            ClientVfx.spawn(new Mote(A, FLAKE, true).at(c.x, c.y, c.z).vel(Math.cos(a) * s, 0.05 + random.nextDouble() * 0.15, Math.sin(a) * s)
                    .gravity(0.004).drag(0.93).size(0.16 + random.nextDouble() * 0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.3)
                    .color(FROST).life(20 + random.nextInt(14)));
        }
    }

    private static final class Clap extends VfxEffect {
        private final int id;
        private final double reach;
        private final double half;
        private final int windup;
        private Vec3 origin = Vec3.ZERO;
        private double yaw;
        private boolean fired;

        Clap(int id, double reach, double half, int windup) {
            this.id = id;
            this.reach = reach;
            this.half = Math.toRadians(half);
            this.windup = windup;
        }

        private Vec3 hand(Entity yeti, float partial, int side) {
            Vec3 p = VfxCanvas.pos(yeti, partial);
            double yawRad = Math.toRadians(yeti.getViewYRot(partial));
            Vec3 forward = new Vec3(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
            Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
            float k = Mth.clamp(this.age / (float) this.windup, 0.0F, 1.0F);
            double spread = Mth.lerp(k * k, 2.6, 0.4);
            return p.add(forward.scale(2.0)).add(right.scale(spread * side)).add(0.0, yeti.getBbHeight() * 0.55, 0.0);
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity yeti = entity(this.id);
            RandomSource random = level.getRandom();
            if (yeti != null && this.age < this.windup) {
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 h = this.hand(yeti, 1.0F, side);
                    double a = random.nextDouble() * Mth.TWO_PI;
                    Vec3 off = new Vec3(Math.cos(a), random.nextDouble() - 0.5, Math.sin(a)).scale(1.6);
                    ClientVfx.spawn(new Mote(A, FLAKE, true).at(h.x + off.x, h.y + off.y, h.z + off.z).vel(-off.x * 0.12, -off.y * 0.12, -off.z * 0.12).drag(0.9)
                            .size(0.18).frame(random.nextInt(4)).color(FROST).life(8));
                }
            }
            if (yeti != null && this.age == this.windup && !this.fired) {
                this.fired = true;
                this.origin = yeti.position();
                this.yaw = Math.toRadians(yeti.getYRot());
                Vec3 forward = new Vec3(-Math.sin(this.yaw), 0.0, Math.cos(this.yaw));
                for (int i = 0; i < ClientVfx.count(18); i++) {
                    double a = (random.nextDouble() - 0.5) * 2.0 * this.half;
                    Vec3 dir = forward.yRot((float) a);
                    double s = 0.35 + random.nextDouble() * 0.35;
                    puff(this.origin.x + dir.x * 1.5, this.origin.y + 1.0 + random.nextDouble() * 1.5, this.origin.z + dir.z * 1.5, dir.x * s, 0.02, dir.z * s, 0.9, SNOW, random);
                }
                flakes(this.origin.add(forward.scale(2.0)).add(0.0, 2.5, 0.0), 14, 0.3, random);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + 16;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            Entity yeti = entity(this.id);
            if (t < this.windup && yeti != null) {
                float k = t / this.windup;
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 h = this.hand(yeti, c.partial, side);
                    c.sprite(A, true, GLINT, (int) (t / 2) % 4, h.x, h.y, h.z, 0.8 + k, t * 0.1 * side, FROST, 0.5F + 0.5F * k);
                }
                return;
            }
            if (!this.fired) {
                return;
            }
            float local = t - this.windup;
            float k = Mth.clamp(local / 8.0F, 0.0F, 1.0F);
            float a = 1.0F - Mth.clamp((local - 4.0F) / 10.0F, 0.0F, 1.0F);
            double facing = this.yaw + Math.PI / 2.0;
            double outer = 1.5 + (this.reach - 1.5) * ease(k);
            c.strip(A, true, MIST, FROST, 0.9F * a).density(4.0).scroll(-local * 8.0).arc(this.origin.x, this.origin.y, this.origin.z, Math.max(0.8, outer - 2.2), outer, facing - this.half, this.half * 2.0, true);
            c.strip(A, true, RIME, GLACIER, a).density(3.0).arc(this.origin.x, this.origin.y, this.origin.z, outer - 0.5, outer + 0.4, facing - this.half, this.half * 2.0, true);
        }
    }

    private static final class Mark extends VfxEffect {
        private final Vec3 c;
        private final double radius;
        private final int delay;
        private final VfxCanvas.Patch patch;

        Mark(ClientLevel level, Vec3 c, double radius, int delay) {
            this.c = c;
            this.radius = radius;
            this.delay = Math.max(1, delay);
            this.patch = VfxCanvas.patch(level, c, radius);
        }

        @Override
        protected boolean alive() {
            return this.age < this.delay + 2;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float k = Mth.clamp(t / this.delay, 0.0F, 1.0F);
            float pulse = 0.75F + 0.25F * Mth.sin(t * 0.8F);
            canvas.decal(A, true, FLAKE_DECAL, this.patch, t * 0.05, 0.5 + 0.5 * ease(k), 0.03, WARNING, (0.35F + 0.5F * k) * pulse);
            double r = this.radius * (1.5 - 0.5 * k);
            canvas.strip(A, true, DASH, WARNING, 0.5F + 0.4F * k).density(3.0).scroll(t * 5.0).arc(this.c.x, this.c.y, this.c.z, r - 0.2, r + 0.2, -t * 0.08, Mth.TWO_PI, true);
        }
    }

    private static final class Trail extends VfxEffect {
        private final int id;
        private final int duration;

        Trail(int id, int duration) {
            this.id = id;
            this.duration = duration;
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity yeti = entity(this.id);
            if (yeti != null) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < ClientVfx.count(3); i++) {
                    puff(yeti.getX() + (random.nextDouble() - 0.5) * 2.5, yeti.getY() + random.nextDouble() * 2.0, yeti.getZ() + (random.nextDouble() - 0.5) * 2.5, 0.0, 0.0, 0.0, 0.8, SHADE, random);
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity yeti = entity(this.id);
            if (yeti == null) {
                return;
            }
            Vec3 p = VfxCanvas.pos(yeti, c.partial);
            float t = this.time(c);
            c.strip(A, true, WIND, FROST, 0.55F).density(6.0).scroll(-t * 30.0).wall(p.x, p.y, p.z, yeti.getBbWidth() * 0.7, yeti.getBbHeight() * 0.8, t * 0.3, Mth.TWO_PI, false);
        }
    }

    private static final class Landing extends VfxEffect {
        private static final int LIFE = 46;
        private final Vec3 c;
        private final double radius;
        private final double rotation;
        private final VfxCanvas.Patch patch;
        private final double[] spikes;

        Landing(ClientLevel level, Vec3 c, double radius) {
            this.c = c;
            this.radius = radius;
            RandomSource random = level.getRandom();
            this.rotation = random.nextDouble() * Mth.TWO_PI;
            this.patch = VfxCanvas.patch(level, c, radius * 1.2);
            this.spikes = new double[8];
            for (int i = 0; i < this.spikes.length; i++) {
                double a = Mth.TWO_PI * i / this.spikes.length + random.nextDouble() * 0.4;
                this.spikes[i] = a;
            }
            for (int i = 0; i < ClientVfx.count(16); i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double s = 0.25 + random.nextDouble() * 0.3;
                puff(c.x + Math.cos(a), c.y + 0.4, c.z + Math.sin(a), Math.cos(a) * s, 0.04, Math.sin(a) * s, 1.1, SNOW, random);
            }
            chunks(c, 12, 0.25, random);
            flakes(c.add(0.0, 1.0, 0.0), 10, 0.25, random);
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas canvas) {
            float t = this.time(canvas);
            float k = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
            float a = 1.0F - k;
            double r = 0.8 + this.radius * 1.3 * ease(k);
            if (t < 12.0F) {
                canvas.strip(A, true, RIME, GLACIER, a).density(3.0).arc(this.c.x, this.c.y, this.c.z, r - 0.6, r + 0.4, this.rotation, Mth.TWO_PI, true);
                canvas.strip(A, true, MIST, FROST, 0.8F * a).density(4.0).scroll(t * 6.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.3, r - 2.2), r - 0.4, -this.rotation, Mth.TWO_PI, true);
            }
            float scar = envelope(t, 1.0F, 14.0F, LIFE - 14.0F);
            canvas.decal(A, true, CRACK, this.patch, this.rotation, 1.0, 0.03, FROST, 0.8F * scar);
            canvas.decal(A, false, CRACK, this.patch, this.rotation, 1.08, 0.02, DEEP, 0.45F * scar);
            float grow = Mth.clamp(t / 4.0F, 0.0F, 1.0F) * (1.0F - Mth.clamp((t - 14.0F) / 10.0F, 0.0F, 1.0F));
            for (int i = 0; i < this.spikes.length; i++) {
                double ang = this.spikes[i];
                double x = this.c.x + Math.cos(ang) * this.radius * 0.75;
                double z = this.c.z + Math.sin(ang) * this.radius * 0.75;
                double y = VfxCanvas.ground(canvas.level, x, z, this.c.y + 1.0);
                double h = (1.1 + (i % 3) * 0.3) * grow;
                crystal(canvas, x, y, z, h, h * 0.45, ang, 0.35, 1.0F);
            }
        }
    }

    private static final class Ball extends VfxEffect {
        private static final int TRAIL = 18;
        private final int id;
        private final ClientLevel level;
        private final Vec3 dir;
        private final double speed;
        private final double from;
        private final double to;
        private final int life;
        private final List<Vec3> trail = new ArrayList<>();
        private Vec3 pos;
        private Vec3 prev;
        private double travelled;
        private boolean dead;

        Ball(ClientLevel level, int id, Vec3 start, Vec3 dir, double speed, double from, double to, int life) {
            this.level = level;
            this.id = id;
            this.pos = start;
            this.prev = start;
            this.dir = new Vec3(dir.x, 0.0, dir.z).normalize();
            this.speed = speed;
            this.from = from;
            this.to = to;
            this.life = life;
            this.trail.add(start);
            chunks(start, 8, 0.2, level.getRandom());
        }

        private double radius(float t) {
            return Mth.lerp(Math.min(1.0, t / this.life), this.from, this.to);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.prev = this.pos;
            Vec3 next = this.pos.add(this.dir.scale(this.speed));
            double r = this.radius(this.age);
            double y = VfxCanvas.ground(level, next.x, next.z, this.pos.y + r * 0.5) - 0.04;
            if (y > this.pos.y + 1.1 + r * 0.3) {
                this.dead = true;
            } else {
                this.pos = new Vec3(next.x, y, next.z);
                this.travelled += this.speed;
            }
            this.trail.add(this.pos);
            if (this.trail.size() > TRAIL) {
                this.trail.remove(0);
            }
            RandomSource random = level.getRandom();
            double side = this.radius(this.age);
            for (int i = 0; i < 1 + (int) side; i++) {
                double lateral = (random.nextDouble() - 0.5) * side * 1.6;
                Vec3 back = this.pos.subtract(this.dir.scale(side * 0.8)).add(-this.dir.z * lateral, 0.0, this.dir.x * lateral);
                ClientVfx.spawn(new Mote(A, CHUNK, false).at(back.x, back.y + 0.2, back.z).vel((random.nextDouble() - 0.5) * 0.12, 0.12 + side * 0.04, (random.nextDouble() - 0.5) * 0.12)
                        .gravity(0.05).size(0.12 + side * 0.06 + random.nextDouble() * 0.1).frame(random.nextInt(4)).collide().color(SNOW).life(18));
            }
            if (this.age % 4 == 0) {
                Vec3 front = this.pos.add(this.dir.scale(side * 0.9));
                puff(front.x, front.y + 0.2, front.z, this.dir.x * 0.1, 0.05, this.dir.z * 0.1, 0.4 + side * 0.25, SNOW, random);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return !this.dead && this.age < this.life + 4;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            Vec3 p = this.prev.lerp(this.pos, c.partial);
            double r = this.radius(t);
            if (this.trail.size() >= 2) {
                Vec3[] points = this.trail.toArray(new Vec3[0]);
                points[points.length - 1] = p;
                c.strip(A, false, TRACK, SNOW, 0.85F).density(3.0).fade(0.0F, 1.0F).lift(0.02).path(points, r * 1.4, true);
            }
            double travel = this.travelled + this.speed * c.partial;
            double side = this.dir.x * c.left.x() + this.dir.z * c.left.z();
            double roll = -travel / r * Math.signum(side);
            c.faceDecal(A, false, BALL, p.x, p.y + r, p.z, r * 1.08, roll, SNOW, 1.0F);
            c.faceDecal(A, true, BALL, p.x, p.y + r, p.z, r * 1.12, roll, FROST, 0.25F);
        }
    }

    private static final class Burst extends VfxEffect {
        private final Vec3 at;
        private final double radius;

        Burst(ClientLevel level, Vec3 at, double radius) {
            this.at = at;
            this.radius = radius;
            RandomSource random = level.getRandom();
            for (int i = 0; i < ClientVfx.count(18); i++) {
                double a = random.nextDouble() * Mth.TWO_PI;
                double s = 0.15 + random.nextDouble() * 0.25;
                puff(at.x + Math.cos(a) * radius * 0.6, at.y + (random.nextDouble() - 0.5) * radius, at.z + Math.sin(a) * radius * 0.6, Math.cos(a) * s, 0.05, Math.sin(a) * s, radius * 0.7, SNOW, random);
            }
            chunks(at, 20, 0.3, random);
            flakes(at, 12, 0.3, random);
        }

        @Override
        protected boolean alive() {
            return this.age < 12;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / 10.0F, 0.0F, 1.0F);
            double ground = VfxCanvas.ground(c.level, this.at.x, this.at.z, this.at.y);
            double r = this.radius * (0.6 + 1.2 * ease(k));
            c.strip(A, true, RIME, FROST, 1.0F - k).density(3.0).arc(this.at.x, ground, this.at.z, r - 0.5, r + 0.3, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Tiles extends VfxEffect {
        private final double size;
        private final int half;
        private final int parity;
        private final int warn;
        private final double[] xs;
        private final double[] ys;
        private final double[] zs;
        private final boolean[] lit;

        Tiles(ClientLevel level, Vec3 c, double size, int half, int parity, int warn) {
            this.size = size;
            this.half = half;
            this.parity = parity;
            this.warn = Math.max(1, warn);
            int n = (half * 2 + 1) * (half * 2 + 1);
            this.xs = new double[n];
            this.ys = new double[n];
            this.zs = new double[n];
            this.lit = new boolean[n];
            int k = 0;
            for (int ix = -half; ix <= half; ix++) {
                for (int iz = -half; iz <= half; iz++) {
                    this.xs[k] = c.x + ix * size;
                    this.zs[k] = c.z + iz * size;
                    this.ys[k] = VfxCanvas.ground(level, this.xs[k], this.zs[k], c.y + 1.0);
                    this.lit[k] = lit(ix, iz, parity);
                    k++;
                }
            }
        }

        @Override
        protected boolean alive() {
            return this.age < this.warn + 8;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / this.warn, 0.0F, 1.0F);
            float fade = envelope(t, 4.0F, this.warn, 8.0F);
            float flicker = 0.8F + 0.2F * Mth.sin(t * (0.6F + k * 1.4F));
            for (int i = 0; i < this.xs.length; i++) {
                if (this.lit[i]) {
                    c.flatDecal(A, true, TILE, this.xs[i], this.ys[i] + 0.03, this.zs[i], this.size * 0.48, 0.0, WARNING, (0.3F + 0.6F * k) * flicker * fade);
                } else {
                    c.flatDecal(A, true, TILE, this.xs[i], this.ys[i] + 0.02, this.zs[i], this.size * 0.46, 0.0, DEEP, 0.35F * fade);
                }
            }
        }
    }

    private static final class Spikes extends VfxEffect {
        private static final int LIFE = 22;
        private final double size;
        private final List<double[]> spikes = new ArrayList<>();
        private final List<double[]> tiles = new ArrayList<>();

        Spikes(ClientLevel level, Vec3 c, double size, int half, int parity) {
            this.size = size;
            RandomSource random = level.getRandom();
            for (int ix = -half; ix <= half; ix++) {
                for (int iz = -half; iz <= half; iz++) {
                    if (!lit(ix, iz, parity)) {
                        continue;
                    }
                    double x = c.x + ix * size;
                    double z = c.z + iz * size;
                    double y = VfxCanvas.ground(level, x, z, c.y + 1.0);
                    this.tiles.add(new double[]{x, y, z});
                    double delay = random.nextDouble() * 1.5;
                    this.spikes.add(new double[]{x + (random.nextDouble() - 0.5) * 0.6, y, z + (random.nextDouble() - 0.5) * 0.6, 1.5 + random.nextDouble() * 0.7,
                            random.nextDouble() * Mth.TWO_PI, 0.06 + random.nextDouble() * 0.1, delay});
                    for (int s = 0; s < 2; s++) {
                        double a = random.nextDouble() * Mth.TWO_PI;
                        double r = size * (0.22 + random.nextDouble() * 0.15);
                        this.spikes.add(new double[]{x + Math.cos(a) * r, y, z + Math.sin(a) * r, 0.6 + random.nextDouble() * 0.5, a, 0.25 + random.nextDouble() * 0.2, delay + 0.5});
                    }
                    if (random.nextFloat() < 0.4F) {
                        ClientVfx.spawn(new Mote(A, GLINT, true).at(x, y + 1.0, z).vel(0.0, 0.05, 0.0).size(0.4).animate().color(FROST).life(10));
                    }
                }
            }
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float flash = 1.0F - Mth.clamp(t / 6.0F, 0.0F, 1.0F);
            for (double[] tile : this.tiles) {
                c.flatDecal(A, true, TILE, tile[0], tile[1] + 0.04, tile[2], this.size * 0.5, 0.0, FROST, 0.4F * flash);
            }
            for (double[] s : this.spikes) {
                float local = (float) (t - s[6]);
                float rise = Mth.clamp(local / 2.5F, 0.0F, 1.0F);
                float sink = Mth.clamp((local - 12.0F) / 7.0F, 0.0F, 1.0F);
                if (rise <= 0.01F || sink >= 0.99F) {
                    continue;
                }
                float overshoot = 1.0F + 0.15F * Mth.sin(rise * Mth.PI);
                double h = s[3] * ease(rise) * overshoot * (1.0F - sink);
                crystal(c, s[0], s[1], s[2], h, s[3] * 0.42, s[4], s[5], 1.0F - sink * 0.5F);
            }
        }
    }

    private static final class Blizzard extends VfxEffect {
        private final int id;
        private final int duration;

        Blizzard(ClientLevel level, int id, int duration) {
            this.id = id;
            this.duration = duration;
            Entity yeti = entity(id);
            if (yeti != null) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < ClientVfx.count(24); i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double s = 0.3 + random.nextDouble() * 0.3;
                    puff(yeti.getX() + Math.cos(a) * 2.0, yeti.getY() + 0.5 + random.nextDouble() * 2.0, yeti.getZ() + Math.sin(a) * 2.0, Math.cos(a) * s, 0.02, Math.sin(a) * s, 1.2, SNOW, random);
                }
            }
        }

        @Override
        public boolean tick(ClientLevel level) {
            Entity yeti = entity(this.id);
            if (yeti != null) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < ClientVfx.count(4); i++) {
                    double a = random.nextDouble() * Mth.TWO_PI;
                    double r = 3.0 + random.nextDouble() * 5.0;
                    double x = yeti.getX() + Math.cos(a) * r;
                    double z = yeti.getZ() + Math.sin(a) * r;
                    ClientVfx.spawn(new Mote(A, FLAKE, true).at(x, yeti.getY() + random.nextDouble() * 5.0, z).vel(-Math.sin(a) * 0.35, -0.02, Math.cos(a) * 0.35).drag(0.97)
                            .size(0.18 + random.nextDouble() * 0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, 0.2).color(FROST).life(20));
                }
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity yeti = entity(this.id);
            if (yeti == null) {
                return;
            }
            float t = this.time(c);
            float a = envelope(t, 8.0F, this.duration - 10.0F, 10.0F);
            Vec3 p = VfxCanvas.pos(yeti, c.partial);
            c.strip(A, true, WIND, FROST, 0.6F * a).density(5.0).scroll(-t * 24.0).wall(p.x, p.y, p.z, 4.5, 4.5, t * 0.12, Mth.TWO_PI, false);
            c.strip(A, true, WIND, SNOW, 0.45F * a).density(4.0).scroll(-t * 18.0).wall(p.x, p.y + 0.5, p.z, 6.5, 3.5, t * 0.09 + 1.0, Mth.TWO_PI, false);
            double ground = VfxCanvas.ground(c.level, p.x, p.z, p.y + 0.5);
            c.flatDecal(A, true, FLAKE_DECAL, p.x, ground + 0.05, p.z, 5.5, t * 0.04, GLACIER, 0.45F * a);
        }
    }

    private static final class Shard extends VfxEffect {
        private final Vec3 dir;
        private final double speed;
        private final double range;
        private final List<Vec3> trail = new ArrayList<>();
        private Vec3 pos;
        private Vec3 prev;
        private double travelled;
        private boolean dead;

        Shard(ClientLevel level, Vec3 start, Vec3 dir, double speed, double range) {
            this.pos = new Vec3(start.x, VfxCanvas.ground(level, start.x, start.z, start.y + 0.5), start.z);
            this.prev = this.pos;
            this.dir = dir;
            this.speed = speed;
            this.range = range;
            this.trail.add(this.pos);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.prev = this.pos;
            Vec3 next = this.pos.add(this.dir.scale(this.speed));
            double y = VfxCanvas.ground(level, next.x, next.z, this.pos.y + 0.1);
            if (y > this.pos.y + 1.1 || this.travelled >= this.range) {
                this.dead = true;
                ClientVfx.spawn(new Mote(A, GLINT, true).at(this.pos.x, this.pos.y + 0.5, this.pos.z).size(0.6).animate().color(FROST).life(8));
            } else {
                this.pos = new Vec3(next.x, y, next.z);
                this.travelled += this.speed;
            }
            this.trail.add(this.pos);
            if (this.trail.size() > 7) {
                this.trail.remove(0);
            }
            return super.tick(level);
        }

        @Override
        protected boolean alive() {
            return !this.dead;
        }

        @Override
        public void render(VfxCanvas c) {
            Vec3 p = this.prev.lerp(this.pos, c.partial);
            if (this.trail.size() >= 2) {
                Vec3[] points = this.trail.toArray(new Vec3[0]);
                points[points.length - 1] = p;
                c.strip(A, true, FACET, FROST, 0.8F).density(4.0).fade(0.0F, 1.0F).lift(0.05).path(points, 0.9, true);
            }
            crystal(c, p.x, p.y, p.z, 1.0, 0.45, Math.atan2(this.dir.z, this.dir.x), 0.55, 1.0F);
            c.sprite(A, true, GLINT, (int) (this.travelled * 2) % 4, p.x, p.y + 0.9, p.z, 0.5, 0.0, SNOW, 0.6F);
        }
    }
}
