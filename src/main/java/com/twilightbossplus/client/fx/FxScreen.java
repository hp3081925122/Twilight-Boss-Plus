package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.ClientConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FxScreen {
    private static final List<float[]> PULSES = new ArrayList<>();

    private FxScreen() {
    }

    public static void pulse(double x, double y, double z, float r, float g, float b, float strength, float radius, int duration) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !ClientConfig.VFX_ENABLED.get() || !ClientConfig.SCREEN_PULSE.get() || duration <= 0) {
            return;
        }
        double distance = Math.sqrt(player.distanceToSqr(x, y, z));
        if (distance >= radius) {
            return;
        }
        float falloff = 1.0F - (float) (distance / radius);
        float amount = Math.min(1.0F, strength * falloff);
        if (amount > 0.01F && PULSES.size() < 16) {
            PULSES.add(new float[] {r, g, b, amount, duration, 0.0F});
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            PULSES.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        PULSES.removeIf(pulse -> ++pulse[5] >= pulse[4]);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (PULSES.isEmpty()) {
            return;
        }
        float partial = event.getPartialTick();
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;
        float total = 0.0F;
        for (float[] pulse : PULSES) {
            float t = Mth.clamp((pulse[5] + partial) / pulse[4], 0.0F, 1.0F);
            float a = pulse[3] * FxEase.window(t, 0.15F, 0.85F);
            r += pulse[0] * a;
            g += pulse[1] * a;
            b += pulse[2] * a;
            total += a;
        }
        if (total <= 0.005F) {
            return;
        }
        r /= total;
        g /= total;
        b /= total;
        float alpha = Math.min(0.85F, total);
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShaderColor(r, g, b, alpha);
        graphics.blit(FxWorld.VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
