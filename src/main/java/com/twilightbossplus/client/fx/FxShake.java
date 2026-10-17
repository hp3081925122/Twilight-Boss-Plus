package com.twilightbossplus.client.fx;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FxShake {
    private static final float MAX_ANGLE = 4.0F;
    private static final List<float[]> SHAKES = new ArrayList<>();
    private static int clock;

    private FxShake() {
    }

    public static void at(double x, double y, double z, float strength, float radius, int duration) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !ClientConfig.SCREEN_SHAKE.get() || duration <= 0) {
            return;
        }
        double distance = Math.sqrt(player.distanceToSqr(x, y, z));
        if (distance >= radius) {
            return;
        }
        float falloff = 1.0F - (float) (distance / radius);
        float amount = strength * falloff * falloff * ClientConfig.SHAKE_STRENGTH.get().floatValue();
        if (amount > 0.01F) {
            SHAKES.add(new float[] {amount, duration, 0.0F});
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            SHAKES.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        clock++;
        SHAKES.removeIf(shake -> ++shake[2] >= shake[1]);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (SHAKES.isEmpty()) {
            return;
        }
        float partial = (float) event.getPartialTick();
        float total = 0.0F;
        for (float[] shake : SHAKES) {
            float t = Mth.clamp((shake[2] + partial) / shake[1], 0.0F, 1.0F);
            total += shake[0] * (1.0F - t) * (1.0F - t);
        }
        total = Math.min(total, MAX_ANGLE);
        if (total <= 0.001F) {
            return;
        }
        float time = clock + partial;
        float yaw = (Mth.sin(time * 2.1F) + 0.5F * Mth.sin(time * 5.3F + 1.3F)) * total * 0.6F;
        float pitch = (Mth.cos(time * 2.7F + 0.7F) + 0.5F * Mth.sin(time * 6.1F)) * total * 0.6F;
        float roll = Mth.sin(time * 3.3F + 2.1F) * total * 0.4F;
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }
}
