package com.twilightbossplus.client.vfx;

import com.twilightbossplus.hydra.HydraFx;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHead;

public final class HydraVfx {
    private static final VfxAtlas A = VfxAtlas.HYDRA;
    private static final int FLAME_STRIP = 0;
    private static final int MAGMA_STRIP = 1;
    private static final int SCORCH_STRIP = 2;
    private static final int HEAT = 3;
    private static final int FIREWALL = 4;
    private static final int SCORCH = 0;
    private static final int POOL = 1;
    private static final int SIGIL = 2;
    private static final int JAWS = 3;
    private static final int EMBER = 0;
    private static final int FLAME = 1;
    private static final int BLOB = 2;
    private static final int SMOKE = 3;
    private static final int BURST = 4;
    private static final int ASH = 5;

    private static final int FIRE = 0xFF7A2A;
    private static final int GOLD = 0xFFD25A;
    private static final int MAGMA = 0xFF5A1E;
    private static final int WARNING = 0xFFB03A;
    private static final int CHAR = 0x2A1A14;
    private static final int SCALE = 0x3FD0A8;
    private static final double MOUTH = 3.0;

    private HydraVfx() {
    }

    static void handle(ClientLevel level, int type, CompoundTag d) {
        switch (type) {
            case HydraFx.WARN -> ClientVfx.add(new Warn(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d"), d.getInt("color")));
            case HydraFx.SHELL -> ClientVfx.add(new Shell(level, ClientVfx.vec(d, "o"), ClientVfx.vec(d, "v"), d.getDouble("g"), d.getInt("t"), ClientVfx.vec(d, "l"), d.getFloat("r")));
            case HydraFx.BURST -> burst(level, ClientVfx.vec(d, "c"), d.getFloat("r"));
            case HydraFx.POOL -> ClientVfx.add(new Pool(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case HydraFx.BREATH -> ClientVfx.add(new Breath(d.getInt("id"), d.getInt("head"), d.getInt("windup"), d.getInt("d"), d.getFloat("len")));
            case HydraFx.FAN -> ClientVfx.add(new Fan(ClientVfx.vec(d, "c"), d.getFloat("in"), d.getFloat("out"), d.getFloat("yaw"), d.getFloat("sweep"), d.getInt("d")));
            case HydraFx.BITE -> ClientVfx.add(new Bite(level, ClientVfx.vec(d, "c"), d.getFloat("r"), d.getInt("d")));
            case HydraFx.STOMP -> {
                Vec3 c = ClientVfx.vec(d, "c");
                float radius = d.getFloat("r");
                ClientVfx.add(new Scorch(level, c, radius * 1.2F, 90));
                ClientVfx.add(new Flash(level, c, radius));
                ClientVfx.add(new Pillar(c, radius * 0.55F, 8.0F, 18, FIRE));
                ClientVfx.add(new Shock(c, radius * 2.5F, 12, GOLD));
                ClientVfx.add(new Shock(c, radius * 1.6F, 16, MAGMA));
                ClientVfx.add(new Cracks(level, c, radius * 2.4F, 9, 90));
                RandomSource random = level.getRandom();
                int amount = ClientVfx.count(14);
                for (int i = 0; i < amount; i++) {
                    double angle = i * Mth.TWO_PI / amount + random.nextDouble() * 0.3;
                    double speed = 0.18 + random.nextDouble() * 0.12;
                    flame(c.x + Math.cos(angle) * radius * 0.4, c.y + 0.3, c.z + Math.sin(angle) * radius * 0.4, Math.cos(angle) * speed, 0.06, Math.sin(angle) * speed, 0.7, random);
                    smoke(c.x + Math.cos(angle) * radius * 0.7, c.y + 0.4, c.z + Math.sin(angle) * radius * 0.7, Math.cos(angle) * speed * 0.5, 0.05, Math.sin(angle) * speed * 0.5, random);
                }
                embers(level, c, radius, ClientVfx.count(12), 0.35);
            }
            case HydraFx.WAVE -> ClientVfx.add(new Wave(level, ClientVfx.vec(d, "c"), d.getFloat("speed"), d.getFloat("max"), d.getFloat("th"), d.getInt("delay")));
            case HydraFx.ROAR -> ClientVfx.add(new Roar(d.getInt("id"), d.getInt("d")));
            case HydraFx.AURA -> {
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
            case HydraFx.REGROW -> ClientVfx.add(new Regrow(d.getInt("id"), d.getInt("head")));
            case HydraFx.CROWN -> ClientVfx.add(new Crown(d.getInt("id"), d.getInt("windup"), d.getInt("d")));
            default -> {
            }
        }
    }

    @Nullable
    private static HydraHead head(ClientLevel level, int id, int index) {
        Entity entity = level.getEntity(id);
        if (!(entity instanceof Hydra hydra) || index < 0 || index >= hydra.hc.length) {
            return null;
        }
        return hydra.hc[index].headEntity;
    }

    private static void ember(double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
        ClientVfx.spawn(new Mote(A, EMBER, true).at(x, y, z).vel(vx, vy, vz).gravity(-0.004).drag(0.94).size(0.12 + random.nextDouble() * 0.08)
                .color(GOLD).animate().life(16 + random.nextInt(14)));
    }

    private static void embers(ClientLevel level, Vec3 c, double radius, int amount, double up) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * radius;
            ember(c.x + Math.cos(angle) * dist, c.y + 0.2, c.z + Math.sin(angle) * dist, Math.cos(angle) * 0.05, up * (0.4 + random.nextDouble() * 0.8), Math.sin(angle) * 0.05, random);
        }
    }

    private static void flame(double x, double y, double z, double vx, double vy, double vz, double size, RandomSource random) {
        int color = FIRE;
        if (random.nextInt(3) == 0) {
            color = GOLD;
        }
        ClientVfx.spawn(new Mote(A, FLAME, true).at(x, y, z).vel(vx, vy, vz).gravity(-0.01).drag(0.9).size(size * (0.8 + random.nextDouble() * 0.4))
                .grow(0.4).spin((random.nextDouble() - 0.5) * 0.4, 0.0).color(color).animate().life(10 + random.nextInt(6)));
    }

    private static void smoke(double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
        ClientVfx.spawn(new Mote(A, SMOKE, false).at(x, y, z).vel(vx, vy, vz).gravity(-0.006).drag(0.92).size(0.45 + random.nextDouble() * 0.25)
                .grow(0.8).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.08).color(0x9A8A80).animate().life(22 + random.nextInt(10)));
    }

    private static void ash(double x, double y, double z, RandomSource random) {
        ClientVfx.spawn(new Mote(A, ASH, false).at(x, y, z).vel((random.nextDouble() - 0.5) * 0.04, 0.03 + random.nextDouble() * 0.04, (random.nextDouble() - 0.5) * 0.04)
                .gravity(-0.002).drag(0.96).size(0.12).frame(random.nextInt(4)).spin(random.nextDouble() * Mth.TWO_PI, (random.nextDouble() - 0.5) * 0.3)
                .color(0x6A5A50).life(30 + random.nextInt(20)));
    }

    private static void burst(ClientLevel level, Vec3 c, float radius) {
        RandomSource random = level.getRandom();
        ClientVfx.add(new Scorch(level, c, radius * 1.1F, 70));
        ClientVfx.add(new Flash(level, c, radius));
        ClientVfx.add(new Pillar(c, radius * 0.5F, 4.0F, 12, FIRE));
        ClientVfx.add(new Shock(c, radius * 2.2F, 10, GOLD));
        ClientVfx.add(new Cracks(level, c, radius * 1.4F, 5, 60));
        int amount = ClientVfx.count(10);
        for (int i = 0; i < amount; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double speed = 0.08 + random.nextDouble() * 0.16;
            flame(c.x, c.y + 0.3, c.z, Math.cos(angle) * speed, 0.12 + random.nextDouble() * 0.2, Math.sin(angle) * speed, 0.6, random);
        }
        for (int i = 0; i < ClientVfx.count(4); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            smoke(c.x + Math.cos(angle) * radius * 0.4, c.y + 0.5, c.z + Math.sin(angle) * radius * 0.4, Math.cos(angle) * 0.04, 0.08, Math.sin(angle) * 0.04, random);
        }
        for (int i = 0; i < ClientVfx.count(10); i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double speed = 0.1 + random.nextDouble() * 0.2;
            ClientVfx.spawn(new Mote(A, BLOB, true).at(c.x, c.y + 0.3, c.z).vel(Math.cos(angle) * speed, 0.3 + random.nextDouble() * 0.25, Math.sin(angle) * speed)
                    .gravity(0.05).drag(0.98).size(0.1 + random.nextDouble() * 0.08).frame(random.nextInt(4)).collide().life(20 + random.nextInt(12)));
        }
    }

    private static Vec3 mouth(HydraHead head, float partial) {
        Vec3 look = head.getViewVector(partial);
        return VfxCanvas.pos(head, partial).add(0.0, 1.0, 0.0).add(look.scale(MOUTH));
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
            c.decal(A, true, SIGIL, this.patch, t * 0.02, 0.2 + 0.8 * ease(p), 0.02, this.color, (0.35F + 0.5F * p) * a);
            c.strip(A, true, FLAME_STRIP, this.color, 0.9F * a).scroll(-t * 6.0).density(10.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius - 0.7), this.radius, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, HEAT, GOLD, 0.5F * a).scroll(t * 4.0).lift(0.01).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius * p - 0.5), this.radius * p, 0.0, Mth.TWO_PI, true);
            if (t > this.duration) {
                float k = (t - this.duration) / 6.0F;
                c.decal(A, true, SIGIL, this.patch, t * 0.02, 1.0, 0.03, 0xFFFFFF, 0.7F * (1.0F - k));
            }
        }
    }

    private static final class Shell extends VfxEffect {
        private final Vec3 origin;
        private final Vec3 velocity;
        private final double gravity;
        private final int flight;
        private final Vec3 landing;
        private final float radius;
        private final VfxCanvas.Patch patch;

        Shell(ClientLevel level, Vec3 origin, Vec3 velocity, double gravity, int flight, Vec3 landing, float radius) {
            this.origin = origin;
            this.velocity = velocity;
            this.gravity = gravity;
            this.flight = Math.max(1, flight);
            this.landing = landing;
            this.radius = radius;
            this.patch = VfxCanvas.patch(level, landing, radius);
        }

        private Vec3 at(double t) {
            return this.origin.add(this.velocity.x * t, this.velocity.y * t - 0.5 * this.gravity * t * t, this.velocity.z * t);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            RandomSource random = level.getRandom();
            Vec3 p = this.at(this.age);
            if (random.nextInt(2) == 0) {
                ember(p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.05, 0.02, (random.nextDouble() - 0.5) * 0.05, random);
            }
            if (this.age % 3 == 0) {
                smoke(p.x, p.y, p.z, 0.0, 0.02, 0.0, random);
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
            float p = Mth.clamp(t / this.flight, 0.0F, 1.0F);
            c.decal(A, true, SCORCH, this.patch, t * 0.05, 0.4 + 0.6 * p, 0.02, MAGMA, 0.25F + 0.45F * p);
            c.strip(A, true, FLAME_STRIP, WARNING, 0.5F + 0.5F * p).scroll(-t * 5.0).density(12.0).arc(this.landing.x, this.landing.y, this.landing.z, Math.max(0.0, this.radius - 0.4), this.radius, 0.0, Mth.TWO_PI, true);
            Vec3 pos = this.at(t);
            Vec3[] trail = new Vec3[7];
            for (int i = 0; i < trail.length; i++) {
                trail[i] = this.at(Math.max(0.0, t - (trail.length - 1 - i) * 0.8));
            }
            c.strip(A, true, FLAME_STRIP, FIRE, 0.75F).fade(0.0F, 1.0F).scroll(-t * 8.0).path(trail, 0.7, false);
            c.strip(A, true, FLAME_STRIP, GOLD, 0.8F).fade(0.0F, 1.0F).scroll(-t * 11.0).path(trail, 0.3, false);
            c.strip(A, true, HEAT, WARNING, 0.6F * p).scroll(-t * 6.0).density(10.0).wall(this.landing.x, this.landing.y, this.landing.z, this.radius * 0.55, 0.5 + 2.5 * p, t * 0.1, Mth.TWO_PI, true);
            c.sprite(A, true, BURST, 1 + (this.age / 2) % 2, pos.x, pos.y, pos.z, 1.5, t * 0.3, FIRE, 0.55F);
            c.sprite(A, true, BURST, (this.age / 2) % 4, pos.x, pos.y, pos.z, 0.9, -t * 0.4, GOLD, 0.6F);
            c.sprite(A, false, BLOB, (this.age / 3) % 4, pos.x, pos.y, pos.z, 0.65, -t * 0.25, 0xFFFFFF, 1.0F);
        }
    }

    private static final class Flash extends VfxEffect {
        private static final int LIFE = 10;
        private final VfxCanvas.Patch patch;
        private final double rotation;

        Flash(ClientLevel level, Vec3 c, float radius) {
            this.patch = VfxCanvas.patch(level, c, radius * 1.3);
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / LIFE, 0.0F, 1.0F);
            c.decal(A, true, POOL, this.patch, this.rotation, 0.5 + 0.5 * ease(k), 0.03, 0xFFFFFF, 0.9F * (1.0F - k));
            Vec3 center = this.patch.center;
            c.sprite(A, true, BURST, Math.min(3, (int) (k * 4.0F)), center.x, center.y + 0.6, center.z, this.patch.radius * (0.5 + k), this.rotation, GOLD, 0.9F * (1.0F - k));
        }
    }

    private static final class Scorch extends VfxEffect {
        private final VfxCanvas.Patch patch;
        private final int life;
        private final double rotation;
        private final int tint;

        Scorch(ClientLevel level, Vec3 c, float radius, int life) {
            this.patch = VfxCanvas.patch(level, c, radius);
            this.life = life;
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
            this.tint = VfxCanvas.lit(level, c, 0xFFFFFF);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age % 9 == 0 && this.age < this.life * 0.6) {
                RandomSource random = level.getRandom();
                double angle = random.nextDouble() * Mth.TWO_PI;
                double dist = random.nextDouble() * this.patch.radius * 0.7;
                Vec3 c = this.patch.center;
                ash(c.x + Math.cos(angle) * dist, c.y + 0.1, c.z + Math.sin(angle) * dist, random);
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float a = envelope(t, 2.0F, this.life * 0.6F, this.life * 0.4F);
            float glow = Mth.clamp(1.0F - t / (this.life * 0.5F), 0.0F, 1.0F);
            c.decal(A, false, SCORCH, this.patch, this.rotation, 1.0, 0.015, this.tint, 0.85F * a);
            c.decal(A, true, SCORCH, this.patch, this.rotation, 1.0, 0.02, MAGMA, 0.8F * glow * a);
        }
    }

    private static final class Pool extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final VfxCanvas.Patch patch;
        private final double rotation;

        Pool(ClientLevel level, Vec3 c, float radius, int duration) {
            this.c = c;
            this.radius = radius;
            this.duration = Math.max(1, duration);
            this.patch = VfxCanvas.patch(level, c, radius);
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            RandomSource random = level.getRandom();
            if (this.age % 5 == 0 && this.age < this.duration - 6) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double dist = random.nextDouble() * this.radius * 0.8;
                double x = this.c.x + Math.cos(angle) * dist;
                double z = this.c.z + Math.sin(angle) * dist;
                if (random.nextBoolean()) {
                    ember(x, this.c.y + 0.1, z, 0.0, 0.06 + random.nextDouble() * 0.05, 0.0, random);
                } else {
                    flame(x, this.c.y + 0.2, z, 0.0, 0.03, 0.0, 0.35, random);
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
            float t = this.time(c);
            float a = envelope(t, 4.0F, this.duration - 12.0F, 12.0F);
            float pulse = 0.85F + 0.15F * Mth.sin(t * 0.3F);
            c.decal(A, false, SCORCH, this.patch, this.rotation, 1.0, 0.012, VfxCanvas.lit(c.level, this.c, 0xFFFFFF), 0.8F * a);
            c.decal(A, true, POOL, this.patch, this.rotation + t * 0.005, 0.85 * pulse, 0.02, 0xFFFFFF, 0.85F * a);
            c.strip(A, true, MAGMA_STRIP, MAGMA, 0.8F * a).scroll(t * 2.0).density(14.0).lift(0.025).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius * 0.85 - 0.4), this.radius * 0.85, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, HEAT, GOLD, 0.35F * a).scroll(-t * 3.0).wall(this.c.x, this.c.y, this.c.z, this.radius * 0.7, 0.9 + 0.2 * Mth.sin(t * 0.2F), 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Breath extends VfxEffect {
        private final int id;
        private final int head;
        private final int windup;
        private final int duration;
        private final float length;
        private Vec3 end = Vec3.ZERO;
        private boolean hitGround;

        Breath(int id, int head, int windup, int duration, float length) {
            this.id = id;
            this.head = head;
            this.windup = windup;
            this.duration = duration;
            this.length = length;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            HydraHead entity = head(level, this.id, this.head);
            if (entity == null || !entity.isAlive()) {
                return false;
            }
            RandomSource random = level.getRandom();
            Vec3 look = entity.getViewVector(1.0F);
            Vec3 from = entity.position().add(0.0, 1.0, 0.0).add(look.scale(MOUTH));
            Vec3 to = from.add(look.scale(this.length));
            HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            this.hitGround = hit.getType() != HitResult.Type.MISS;
            if (this.hitGround) {
                to = hit.getLocation();
            }
            this.end = to;
            if (this.age < this.windup) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double tilt = (random.nextDouble() - 0.5) * Math.PI;
                Vec3 offset = new Vec3(Math.cos(angle) * Math.cos(tilt), Math.sin(tilt), Math.sin(angle) * Math.cos(tilt)).scale(1.6);
                Vec3 start = from.add(offset);
                ember(start.x, start.y, start.z, -offset.x * 0.08, -offset.y * 0.08, -offset.z * 0.08, random);
                return true;
            }
            if (this.age < this.windup + this.duration) {
                for (int i = 0; i < ClientVfx.count(2); i++) {
                    double f = random.nextDouble();
                    Vec3 p = from.lerp(to, f);
                    double speed = 0.3 + random.nextDouble() * 0.3;
                    flame(p.x, p.y, p.z, look.x * speed, look.y * speed + 0.03, look.z * speed, 0.5 + f * 0.9, random);
                }
                if (this.hitGround && this.age % 4 == 0) {
                    ClientVfx.add(new Scorch(level, to, 1.4F, 60));
                    smoke(to.x, to.y + 0.3, to.z, 0.0, 0.06, 0.0, random);
                }
                if (this.hitGround) {
                    ember(to.x, to.y + 0.2, to.z, (random.nextDouble() - 0.5) * 0.25, 0.2 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.25, random);
                    flame(to.x + (random.nextDouble() - 0.5), to.y + 0.3, to.z + (random.nextDouble() - 0.5), (random.nextDouble() - 0.5) * 0.1, 0.18 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.1, 0.9, random);
                    if (random.nextInt(3) == 0) {
                        ClientVfx.spawn(new Mote(A, BLOB, true).at(to.x, to.y + 0.3, to.z).vel((random.nextDouble() - 0.5) * 0.3, 0.3 + random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3)
                                .gravity(0.05).drag(0.98).size(0.1 + random.nextDouble() * 0.06).frame(random.nextInt(4)).collide().life(18 + random.nextInt(10)));
                    }
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + this.duration + 6;
        }

        @Override
        public void render(VfxCanvas c) {
            HydraHead entity = head(c.level, this.id, this.head);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            Vec3 from = mouth(entity, c.partial);
            if (t < this.windup) {
                float k = t / Math.max(1, this.windup);
                c.sprite(A, true, BURST, (this.age / 2) % 4, from.x, from.y, from.z, 0.4 + 0.8 * k, t * 0.2, FIRE, 0.4F + 0.5F * k);
                c.sprite(A, true, EMBER, 0, from.x, from.y, from.z, 0.25 + 0.3 * k, 0.0, GOLD, 0.9F);
                return;
            }
            float a = envelope(t - this.windup, 3.0F, this.duration, 6.0F);
            if (a <= 0.0F) {
                return;
            }
            Vec3 look = entity.getViewVector(c.partial);
            double reach = Math.max(1.0, this.end.distanceTo(from));
            Vec3 to = from.add(look.scale(reach));
            Vec3[] beam = VfxCanvas.line(from, to, 1.5);
            c.strip(A, true, FLAME_STRIP, MAGMA, 0.55F * a).scroll(-t * 14.0).density(6.0).fade(1.0F, 0.6F).path(beam, 2.6, false);
            c.strip(A, true, FLAME_STRIP, FIRE, 0.85F * a).scroll(-t * 18.0 + 40.0).density(8.0).fade(1.0F, 0.75F).path(beam, 1.5, false);
            c.strip(A, true, FLAME_STRIP, GOLD, 0.95F * a).scroll(-t * 24.0 + 80.0).density(10.0).path(beam, 0.6, false);
            Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
            if (side.lengthSqr() < 1.0E-4) {
                side = new Vec3(1.0, 0.0, 0.0);
            }
            side = side.normalize();
            Vec3 lift = side.cross(look).normalize();
            int count = Math.max(2, (int) (reach * 1.6));
            for (int strand = 0; strand < 2; strand++) {
                Vec3[] helix = new Vec3[count];
                for (int i = 0; i < count; i++) {
                    double s = reach * i / (count - 1);
                    double angle = s * 0.9 - t * 0.7 + strand * Math.PI;
                    double r = 0.35 + s * 0.07;
                    helix[i] = from.add(look.scale(s)).add(side.scale(Math.cos(angle) * r)).add(lift.scale(Math.sin(angle) * r));
                }
                c.strip(A, true, FLAME_STRIP, GOLD, 0.85F * a).scroll(-t * 30.0).density(12.0).fade(1.0F, 0.25F).path(helix, 0.4, false);
            }
            for (int k = 0; k < 3; k++) {
                double phase = (t * 0.09 + k / 3.0) % 1.0;
                double s = phase * Math.min(reach, 8.0);
                double r = 0.6 + s * 0.22;
                Vec3 centre = from.add(look.scale(s));
                Vec3[] ring = new Vec3[17];
                for (int i = 0; i < ring.length; i++) {
                    double angle = i * Mth.TWO_PI / (ring.length - 1);
                    ring[i] = centre.add(side.scale(Math.cos(angle) * r)).add(lift.scale(Math.sin(angle) * r));
                }
                c.strip(A, true, HEAT, GOLD, (float) (0.9 * (1.0 - phase)) * a).scroll(t * 6.0).density(14.0).path(ring, 0.35, false);
            }
            c.sprite(A, true, BURST, (this.age / 2) % 4, from.x, from.y, from.z, 1.4, t * 0.4, GOLD, 0.85F * a);
            c.sprite(A, true, BURST, (this.age / 2 + 2) % 4, from.x, from.y, from.z, 2.2, -t * 0.25, FIRE, 0.5F * a);
            if (this.hitGround) {
                c.sprite(A, true, BURST, (this.age / 2 + 1) % 4, to.x, to.y + 0.3, to.z, 2.2, -t * 0.3, FIRE, 0.85F * a);
                c.strip(A, true, FIREWALL, FIRE, 0.85F * a).scroll(-t * 9.0).density(16.0).wall(to.x, to.y, to.z, 1.1 + 0.15 * Mth.sin(t * 0.7F), 1.8 + 0.4 * Mth.sin(t * 0.9F), t * 0.1, Mth.TWO_PI, true);
                c.flatSprite(A, true, BURST, 2, to.x, VfxCanvas.ground(c.level, to.x, to.z, to.y + 0.5) + 0.05, to.z, 2.0, t * 0.1, MAGMA, 0.7F * a);
            }
        }
    }

    private static final class Fan extends VfxEffect {
        private final Vec3 c;
        private final float inner;
        private final float outer;
        private final double start;
        private final double sweep;
        private final int duration;

        Fan(Vec3 c, float inner, float outer, float yaw, float sweep, int duration) {
            this.c = c;
            this.inner = inner;
            this.outer = outer;
            this.start = Math.toRadians(yaw) + Math.PI / 2.0;
            this.sweep = Math.toRadians(sweep);
            this.duration = Math.max(1, duration);
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 4;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float a = envelope(t, 4.0F, this.duration, 4.0F);
            c.strip(A, true, SCORCH_STRIP, WARNING, 0.28F * a).density(4.0).arc(this.c.x, this.c.y, this.c.z, this.inner, this.outer, this.start, this.sweep, true);
            c.strip(A, true, FLAME_STRIP, WARNING, 0.85F * a).scroll(-t * 6.0).density(10.0).lift(0.01).arc(this.c.x, this.c.y, this.c.z, this.outer - 0.6, this.outer, this.start, this.sweep, true);
            double edge = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            double sweepNow = this.sweep * edge;
            c.strip(A, true, FLAME_STRIP, FIRE, 0.6F * a).scroll(t * 4.0).density(10.0).lift(0.015).fade(0.2F, 1.0F).arc(this.c.x, this.c.y, this.c.z, this.inner, this.outer - 0.6, this.start, sweepNow, true);
        }
    }

    private static final class Bite extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int duration;
        private final VfxCanvas.Patch patch;
        private final double rotation;

        Bite(ClientLevel level, Vec3 c, float radius, int duration) {
            this.c = c;
            this.radius = radius;
            this.duration = Math.max(1, duration);
            this.patch = VfxCanvas.patch(level, c, radius * 1.5);
            this.rotation = level.getRandom().nextDouble() * Mth.TWO_PI;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            if (this.age == this.duration) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < ClientVfx.count(10); i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double speed = 0.12 + random.nextDouble() * 0.15;
                    ember(this.c.x, this.c.y + 0.8, this.c.z, Math.cos(angle) * speed, 0.15 + random.nextDouble() * 0.2, Math.sin(angle) * speed, random);
                }
                ClientVfx.add(new Scorch(level, this.c, this.radius, 50));
                ClientVfx.add(new Slash(this.c, this.radius * 1.8F, this.rotation));
                ClientVfx.add(new Shock(this.c, this.radius * 2.0F, 8, GOLD));
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + 8;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float p = Mth.clamp(t / this.duration, 0.0F, 1.0F);
            float a = envelope(t, 3.0F, this.duration, 8.0F);
            double close = 1.5 - 0.5 * ease(p);
            c.decal(A, true, JAWS, this.patch, this.rotation + p * 0.6, close / 1.5, 0.02, WARNING, (0.4F + 0.5F * p) * a);
            c.strip(A, true, FLAME_STRIP, FIRE, 0.7F * a).scroll(-t * 5.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, this.radius - 0.35), this.radius, 0.0, Mth.TWO_PI, true);
            if (t > this.duration) {
                float k = (t - this.duration) / 8.0F;
                c.decal(A, true, JAWS, this.patch, this.rotation + 0.6, 0.66, 0.03, 0xFFFFFF, 0.9F * (1.0F - k));
                c.sprite(A, true, BURST, Math.min(3, (int) (k * 4.0F)), this.c.x, this.c.y + 1.0, this.c.z, 1.8 * (0.6 + k), this.rotation, GOLD, 0.9F * (1.0F - k));
            }
        }
    }

    private static final class Wave extends VfxEffect {
        private final Vec3 c;
        private final float speed;
        private final float max;
        private final float thickness;
        private final int delay;

        Wave(ClientLevel level, Vec3 c, float speed, float max, float thickness, int delay) {
            this.c = c;
            this.speed = speed;
            this.max = max;
            this.thickness = thickness;
            this.delay = delay;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            int active = this.age - this.delay;
            if (active > 0 && active % 2 == 0) {
                RandomSource random = level.getRandom();
                double r = active * this.speed;
                for (int i = 0; i < ClientVfx.count(2); i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double x = this.c.x + Math.cos(angle) * r;
                    double z = this.c.z + Math.sin(angle) * r;
                    double gy = VfxCanvas.ground(level, x, z, this.c.y);
                    flame(x, gy + 0.3, z, Math.cos(angle) * 0.05, 0.08, Math.sin(angle) * 0.05, 0.5, random);
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
            c.strip(A, true, FIREWALL, MAGMA, 0.7F * f).density(10.0).scroll(active * 4.0).wall(this.c.x, this.c.y, this.c.z, outer + 0.15, 1.2 + 1.6 * f, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, FIREWALL, FIRE, 0.95F * f).density(14.0).scroll(-active * 6.0).wall(this.c.x, this.c.y, this.c.z, outer, 0.8 + 1.0 * f, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, FIREWALL, GOLD, 0.8F * f).density(18.0).scroll(-active * 9.0).wall(this.c.x, this.c.y, this.c.z, outer - 0.15, 0.4 + 0.5 * f, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, MAGMA_STRIP, MAGMA, 0.9F * f).scroll(active * 3.0).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, outer - this.thickness), outer, 0.0, Mth.TWO_PI, true);
            c.strip(A, false, SCORCH_STRIP, VfxCanvas.lit(c.level, this.c, CHAR), 0.6F * f).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, outer - this.thickness * 2.2), Math.max(0.0, outer - this.thickness), 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Roar extends VfxEffect {
        private static final int RING_EVERY = 7;
        private static final float RING_SPEED = 0.55F;
        private static final float RING_LIFE = 18.0F;
        private final int id;
        private final int duration;

        Roar(int id, int duration) {
            this.id = id;
            this.duration = Math.max(1, duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (!(entity instanceof Hydra hydra)) {
                return false;
            }
            if (this.age % 3 == 0) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < hydra.hc.length; i++) {
                    HydraHead head = hydra.hc[i].headEntity;
                    if (head == null || !hydra.hc[i].shouldRenderHead()) {
                        continue;
                    }
                    Vec3 look = head.getViewVector(1.0F);
                    Vec3 m = head.position().add(0.0, 1.0, 0.0).add(look.scale(MOUTH));
                    ember(m.x, m.y, m.z, look.x * 0.15 + (random.nextDouble() - 0.5) * 0.1, look.y * 0.15 + 0.05, look.z * 0.15 + (random.nextDouble() - 0.5) * 0.1, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.duration + (int) RING_LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            for (int k = 0; k * RING_EVERY < this.duration; k++) {
                float local = t - k * RING_EVERY;
                if (local <= 0.0F || local > RING_LIFE) {
                    continue;
                }
                float fade = 1.0F - local / RING_LIFE;
                double r = 2.0 + local * RING_SPEED * 1.6;
                c.strip(A, true, HEAT, GOLD, 0.7F * fade).scroll(t * 5.0).density(10.0).wall(pos.x, pos.y + 0.2, pos.z, r, 1.4 * fade + 0.3, 0.0, Mth.TWO_PI, true);
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
            if (!(entity instanceof Hydra hydra) || !entity.isAlive()) {
                return false;
            }
            RandomSource random = level.getRandom();
            if (this.age % 4 == 0) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double dist = 2.0 + random.nextDouble() * 3.0;
                ember(entity.getX() + Math.cos(angle) * dist, entity.getY() + 0.3, entity.getZ() + Math.sin(angle) * dist, 0.0, 0.08 + random.nextDouble() * 0.06, 0.0, random);
            }
            if (this.age % 10 == 0) {
                int index = random.nextInt(hydra.hc.length);
                HydraHead head = hydra.hc[index].headEntity;
                if (head != null && hydra.hc[index].shouldRenderHead()) {
                    Vec3 m = head.position().add(0.0, 1.0, 0.0).add(head.getViewVector(1.0F).scale(MOUTH));
                    smoke(m.x, m.y, m.z, 0.0, 0.05, 0.0, random);
                    flame(m.x, m.y, m.z, 0.0, 0.04, 0.0, 0.4, random);
                }
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
            c.strip(A, true, MAGMA_STRIP, MAGMA, 0.55F * a).scroll(t * 1.5).density(12.0).arc(pos.x, pos.y, pos.z, 4.6, 5.2, t * 0.01, Mth.TWO_PI, true);
            c.strip(A, true, HEAT, 0xFF2A55, 0.35F * a).scroll(-t * 3.0).wall(pos.x, pos.y, pos.z, 5.0, 0.7 + 0.2 * Mth.sin(t * 0.15F), 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Regrow extends VfxEffect {
        private static final int LIFE = 24;
        private final int id;
        private final int head;

        Regrow(int id, int head) {
            this.id = id;
            this.head = head;
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            HydraHead entity = head(level, this.id, this.head);
            if (entity == null) {
                return false;
            }
            if (this.age % 2 == 0) {
                RandomSource random = level.getRandom();
                Vec3 p = entity.position().add(0.0, 1.0, 0.0);
                for (int i = 0; i < ClientVfx.count(2); i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    ClientVfx.spawn(new Mote(A, EMBER, true).at(p.x + Math.cos(angle) * 1.5, p.y + (random.nextDouble() - 0.5) * 2.0, p.z + Math.sin(angle) * 1.5)
                            .vel(-Math.cos(angle) * 0.08, 0.03, -Math.sin(angle) * 0.08).drag(0.92).size(0.16).color(SCALE).animate().life(14));
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            HydraHead entity = head(c.level, this.id, this.head);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            float k = Mth.clamp(t / LIFE, 0.0F, 1.0F);
            Vec3 p = VfxCanvas.pos(entity, c.partial).add(0.0, 1.0, 0.0);
            c.sprite(A, true, BURST, Math.min(3, (int) (k * 4.0F)), p.x, p.y, p.z, 1.5 + 2.0 * k, t * 0.1, SCALE, 0.8F * (1.0F - k));
            c.sprite(A, true, BURST, (this.age / 2) % 4, p.x, p.y, p.z, 1.0 + k, -t * 0.2, 0xFFFFFF, 0.6F * (1.0F - k));
        }
    }

    private static final class Pillar extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final float height;
        private final int life;
        private final int color;

        Pillar(Vec3 c, float radius, float height, int life, int color) {
            this.c = c;
            this.radius = radius;
            this.height = height;
            this.life = Math.max(1, life);
            this.color = color;
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / this.life, 0.0F, 1.0F);
            float rise = ease(Mth.clamp(t / 3.0F, 0.0F, 1.0F));
            float a = 1.0F - k * k;
            double h = this.height * rise * (1.0 - 0.6 * k);
            double r = this.radius * (0.6 + 0.8 * k);
            c.strip(A, true, FIREWALL, MAGMA, 0.75F * a).density(10.0).scroll(-t * 8.0).wall(this.c.x, this.c.y, this.c.z, r * 1.25, h * 0.8, t * 0.2, Mth.TWO_PI, true);
            c.strip(A, true, FIREWALL, this.color, 0.95F * a).density(14.0).scroll(-t * 12.0).wall(this.c.x, this.c.y, this.c.z, r, h, -t * 0.3, Mth.TWO_PI, true);
            c.strip(A, true, FIREWALL, GOLD, 0.9F * a).density(18.0).scroll(-t * 16.0).wall(this.c.x, this.c.y, this.c.z, r * 0.55, h * 1.15, t * 0.4, Mth.TWO_PI, true);
            c.sprite(A, true, BURST, Math.min(3, (int) (k * 4.0F)), this.c.x, this.c.y + h * 0.9, this.c.z, r * 1.6, t * 0.2, GOLD, 0.7F * a);
        }
    }

    private static final class Shock extends VfxEffect {
        private final Vec3 c;
        private final float radius;
        private final int life;
        private final int color;

        Shock(Vec3 c, float radius, int life, int color) {
            this.c = c;
            this.radius = radius;
            this.life = Math.max(1, life);
            this.color = color;
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = ease(Mth.clamp(t / this.life, 0.0F, 1.0F));
            float a = 1.0F - Mth.clamp(t / this.life, 0.0F, 1.0F);
            double r = this.radius * k;
            c.strip(A, true, HEAT, this.color, 0.95F * a).scroll(t * 8.0).density(10.0).lift(0.03).arc(this.c.x, this.c.y, this.c.z, Math.max(0.0, r - 0.8), r, 0.0, Mth.TWO_PI, true);
            c.strip(A, true, HEAT, 0xFFFFFF, 0.5F * a).scroll(-t * 6.0).density(12.0).wall(this.c.x, this.c.y, this.c.z, r, 0.6 * a + 0.1, 0.0, Mth.TWO_PI, true);
        }
    }

    private static final class Cracks extends VfxEffect {
        private final Vec3[][] rays;
        private final int life;

        Cracks(ClientLevel level, Vec3 c, float length, int count, int life) {
            RandomSource random = level.getRandom();
            this.life = Math.max(1, life);
            this.rays = new Vec3[count][];
            double offset = random.nextDouble() * Mth.TWO_PI;
            for (int i = 0; i < count; i++) {
                double angle = offset + i * Mth.TWO_PI / count + (random.nextDouble() - 0.5) * 0.4;
                double reach = length * (0.6 + random.nextDouble() * 0.4);
                int steps = Math.max(2, (int) Math.ceil(reach / 0.8));
                Vec3[] ray = new Vec3[steps + 1];
                double x = c.x;
                double z = c.z;
                for (int j = 0; j <= steps; j++) {
                    ray[j] = new Vec3(x, VfxCanvas.ground(level, x, z, c.y) + 0.03, z);
                    angle += (random.nextDouble() - 0.5) * 0.6;
                    x += Math.cos(angle) * reach / steps;
                    z += Math.sin(angle) * reach / steps;
                }
                this.rays[i] = ray;
            }
        }

        @Override
        protected boolean alive() {
            return this.age < this.life;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float grow = ease(Mth.clamp(t / 6.0F, 0.0F, 1.0F));
            float a = envelope(t, 1.0F, this.life * 0.5F, this.life * 0.5F);
            for (Vec3[] ray : this.rays) {
                int shown = Math.max(2, (int) Math.ceil(ray.length * grow));
                Vec3[] part = new Vec3[shown];
                System.arraycopy(ray, 0, part, 0, shown);
                c.strip(A, false, SCORCH_STRIP, VfxCanvas.lit(c.level, ray[0], 0xFFFFFF), 0.9F * a).density(12.0).fade(0.2F, 1.0F).path(part, 0.7, false);
                c.strip(A, true, MAGMA_STRIP, MAGMA, 0.95F * a).density(12.0).lift(0.01).scroll(t * 2.0).fade(0.1F, 1.0F).path(part, 0.4, false);
            }
        }
    }

    private static final class Slash extends VfxEffect {
        private static final int LIFE = 9;
        private final Vec3 c;
        private final float length;
        private final double rotation;

        Slash(Vec3 c, float length, double rotation) {
            this.c = c.add(0.0, 1.0, 0.0);
            this.length = length;
            this.rotation = rotation;
        }

        @Override
        protected boolean alive() {
            return this.age < LIFE;
        }

        @Override
        public void render(VfxCanvas c) {
            float t = this.time(c);
            float k = Mth.clamp(t / LIFE, 0.0F, 1.0F);
            float a = 1.0F - k;
            for (int i = 0; i < 4; i++) {
                double angle = this.rotation + i * Math.PI / 4.0;
                double tilt = 0.5 - i * 0.33;
                Vec3 dir = new Vec3(Math.cos(angle), tilt, Math.sin(angle)).normalize();
                double half = this.length * (0.5 + 0.5 * ease(k * 2.0F));
                c.strip(A, true, FLAME_STRIP, GOLD, 0.95F * a).scroll(-t * 20.0).fade(0.0F, 1.0F).beam(this.c.subtract(dir.scale(half)), this.c.add(dir.scale(half)), 0.5 * a + 0.1);
                c.strip(A, true, FLAME_STRIP, 0xFFFFFF, 0.7F * a).scroll(-t * 26.0).beam(this.c.subtract(dir.scale(half * 0.7)), this.c.add(dir.scale(half * 0.7)), 0.18);
            }
        }
    }

    private static final class Crown extends VfxEffect {
        private static final int TIERS = 4;
        private final int id;
        private final int windup;
        private final int duration;

        Crown(int id, int windup, int duration) {
            this.id = id;
            this.windup = Math.max(1, windup);
            this.duration = Math.max(1, duration);
        }

        @Override
        public boolean tick(ClientLevel level) {
            this.age++;
            Entity entity = level.getEntity(this.id);
            if (entity == null || !entity.isAlive()) {
                return false;
            }
            RandomSource random = level.getRandom();
            if (this.age > this.windup / 2) {
                for (int i = 0; i < ClientVfx.count(2); i++) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    double r = 2.0 + random.nextDouble() * 2.5;
                    double y = entity.getY() + 5.0 + random.nextDouble() * 6.0;
                    flame(entity.getX() + Math.cos(angle) * r, y, entity.getZ() + Math.sin(angle) * r, -Math.sin(angle) * 0.15, 0.12, Math.cos(angle) * 0.15, 0.8, random);
                }
                if (this.age % 3 == 0) {
                    double angle = random.nextDouble() * Mth.TWO_PI;
                    smoke(entity.getX() + Math.cos(angle) * 3.0, entity.getY() + 12.0, entity.getZ() + Math.sin(angle) * 3.0, 0.0, 0.1, 0.0, random);
                }
            }
            return this.alive();
        }

        @Override
        protected boolean alive() {
            return this.age < this.windup + this.duration + 10;
        }

        @Override
        public void render(VfxCanvas c) {
            Entity entity = c.level.getEntity(this.id);
            if (entity == null) {
                return;
            }
            float t = this.time(c);
            float grow = ease(Mth.clamp(t / this.windup, 0.0F, 1.0F));
            float a = envelope(t, this.windup * 0.5F, this.windup + this.duration, 10.0F);
            Vec3 pos = VfxCanvas.pos(entity, c.partial);
            for (int k = 0; k < TIERS; k++) {
                double direction = 1.0;
                if (k % 2 == 1) {
                    direction = -1.0;
                }
                double r = (1.6 + k * 1.1) * grow;
                double y = pos.y + 4.5 + k * 1.7 * grow;
                double spin = t * 0.22 * direction + k;
                c.strip(A, true, FIREWALL, MAGMA, 0.7F * a).density(10.0).scroll(-t * 8.0).wall(pos.x, y, pos.z, r + 0.2, 2.2, spin, Mth.TWO_PI * 0.8, false);
                c.strip(A, true, FIREWALL, FIRE, 0.95F * a).density(14.0).scroll(-t * 12.0).wall(pos.x, y + 0.2, pos.z, r, 1.8, spin + 1.3, Mth.TWO_PI * 0.7, false);
                c.strip(A, true, FIREWALL, GOLD, 0.85F * a).density(18.0).scroll(-t * 16.0).wall(pos.x, y + 0.4, pos.z, r * 0.7, 1.2, -spin, Mth.TWO_PI * 0.6, false);
            }
            c.sprite(A, true, BURST, (this.age / 2) % 4, pos.x, pos.y + 6.0 + 5.0 * grow, pos.z, 3.0 * grow, t * 0.1, GOLD, 0.6F * a);
        }
    }
}
