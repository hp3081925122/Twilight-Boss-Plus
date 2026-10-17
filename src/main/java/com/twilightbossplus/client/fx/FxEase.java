package com.twilightbossplus.client.fx;

import net.minecraft.util.Mth;

public final class FxEase {
    private FxEase() {
    }

    public static float clamp(float t) {
        return Mth.clamp(t, 0.0F, 1.0F);
    }

    public static float outCubic(float t) {
        float f = 1.0F - clamp(t);
        return 1.0F - f * f * f;
    }

    public static float inCubic(float t) {
        float f = clamp(t);
        return f * f * f;
    }

    public static float outQuint(float t) {
        float f = 1.0F - clamp(t);
        return 1.0F - f * f * f * f * f;
    }

    public static float inOutSine(float t) {
        return -(Mth.cos((float) Math.PI * clamp(t)) - 1.0F) * 0.5F;
    }

    public static float outBack(float t) {
        float f = clamp(t) - 1.0F;
        float c = 1.70158F;
        return 1.0F + (c + 1.0F) * f * f * f + c * f * f;
    }

    public static float outExpo(float t) {
        float f = clamp(t);
        if (f >= 1.0F) {
            return 1.0F;
        }
        return 1.0F - (float) Math.pow(2.0D, -10.0D * f);
    }

    public static float pulse(float t) {
        float f = clamp(t);
        return Mth.sin(f * (float) Math.PI);
    }

    public static float window(float t, float in, float out) {
        float f = clamp(t);
        float a = 1.0F;
        if (in > 0.0F && f < in) {
            a = f / in;
        }
        if (out > 0.0F && f > 1.0F - out) {
            a = Math.min(a, (1.0F - f) / out);
        }
        return clamp(a);
    }
}
