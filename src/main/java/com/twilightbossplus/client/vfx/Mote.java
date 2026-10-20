package com.twilightbossplus.client.vfx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;

public final class Mote {
    private final VfxAtlas atlas;
    private final int row;
    private final boolean glow;
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
    private double drag = 0.96;
    private double size = 0.25;
    private double grow;
    private double roll;
    private double spin;
    private int color = 0xFFFFFF;
    private int life = 20;
    private int age;
    private int frame;
    private boolean animate;
    private boolean collide;
    private boolean resting;
    private float alpha = 1.0F;
    private float light = 1.0F;

    public Mote(VfxAtlas atlas, int row, boolean glow) {
        this.atlas = atlas;
        this.row = row;
        this.glow = glow;
    }

    public Mote at(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.px = x;
        this.py = y;
        this.pz = z;
        return this;
    }

    public Mote vel(double vx, double vy, double vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        return this;
    }

    public Mote gravity(double gravity) {
        this.gravity = gravity;
        return this;
    }

    public Mote drag(double drag) {
        this.drag = drag;
        return this;
    }

    public Mote size(double size) {
        this.size = size;
        return this;
    }

    public Mote grow(double grow) {
        this.grow = grow;
        return this;
    }

    public Mote spin(double roll, double spin) {
        this.roll = roll;
        this.spin = spin;
        return this;
    }

    public Mote color(int color) {
        this.color = color;
        return this;
    }

    public Mote life(int life) {
        this.life = Math.max(1, life);
        return this;
    }

    public Mote frame(int frame) {
        this.frame = Mth.clamp(frame, 0, 3);
        return this;
    }

    public Mote animate() {
        this.animate = true;
        return this;
    }

    public Mote collide() {
        this.collide = true;
        return this;
    }

    public Mote alpha(float alpha) {
        this.alpha = alpha;
        return this;
    }

    public boolean tick(ClientLevel level) {
        this.px = this.x;
        this.py = this.y;
        this.pz = this.z;
        this.age++;
        if (!this.glow && (this.age == 1 || this.age % 5 == 0)) {
            BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
            int block = level.getBrightness(LightLayer.BLOCK, pos);
            int sky = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
            this.light = 0.3F + 0.7F * Math.max(block, sky) / 15.0F;
        }
        if (this.resting) {
            return this.age < this.life;
        }
        this.vy -= this.gravity;
        this.vx *= this.drag;
        this.vy *= this.drag;
        this.vz *= this.drag;
        double nx = this.x + this.vx;
        double ny = this.y + this.vy;
        double nz = this.z + this.vz;
        if (this.collide && this.vy < 0.0) {
            BlockPos pos = BlockPos.containing(nx, ny - this.size * 0.5, nz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                this.resting = true;
                return this.age < this.life;
            }
        }
        this.x = nx;
        this.y = ny;
        this.z = nz;
        this.roll += this.spin;
        return this.age < this.life;
    }

    public void render(VfxCanvas c) {
        float t = (this.age + c.partial) / this.life;
        float a = Mth.clamp((1.0F - t) * 3.5F, 0.0F, 1.0F) * this.alpha;
        int shown = this.frame;
        if (this.animate) {
            shown = Mth.clamp((int) (t * 4.0F), 0, 3);
        }
        int tint = this.color;
        if (!this.glow && this.light < 0.999F) {
            int r = (int) ((tint >> 16 & 255) * this.light);
            int g = (int) ((tint >> 8 & 255) * this.light);
            int b = (int) ((tint & 255) * this.light);
            tint = r << 16 | g << 8 | b;
        }
        double s = this.size * Math.max(0.05, 1.0 + this.grow * t);
        double rx = Mth.lerp(c.partial, this.px, this.x);
        double ry = Mth.lerp(c.partial, this.py, this.y);
        double rz = Mth.lerp(c.partial, this.pz, this.z);
        c.sprite(this.atlas, this.glow, this.row, shown, rx, ry, rz, s, this.roll + this.spin * c.partial, tint, a);
    }
}
