package com.twilightbossplus.client.vfx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

public abstract class VfxEffect {
    protected int age;

    public boolean tick(ClientLevel level) {
        this.age++;
        return this.alive();
    }

    protected abstract boolean alive();

    public abstract void render(VfxCanvas c);

    protected float time(VfxCanvas c) {
        return this.age + c.partial;
    }

    protected static float envelope(float t, float in, float hold, float out) {
        if (t < in) {
            return Mth.clamp(t / Math.max(0.001F, in), 0.0F, 1.0F);
        }
        if (t < hold) {
            return 1.0F;
        }
        return Mth.clamp(1.0F - (t - hold) / Math.max(0.001F, out), 0.0F, 1.0F);
    }

    protected static float ease(float t) {
        float k = Mth.clamp(t, 0.0F, 1.0F);
        return 1.0F - (1.0F - k) * (1.0F - k);
    }
}
