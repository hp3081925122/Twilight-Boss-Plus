package com.twilightbossplus.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.twilightbossplus.TwilightBossPlus;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FxShaders {
    private static ShaderInstance flow;
    private static ShaderInstance flowAdd;
    private static boolean shaderPack;
    private static boolean irisChecked;
    private static Object irisApi;
    private static Method irisInUse;

    private FxShaders() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(TwilightBossPlus.MOD_ID, "fx_flow"),
                DefaultVertexFormat.NEW_ENTITY), shader -> flow = shader);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(TwilightBossPlus.MOD_ID, "fx_flow_add"),
                DefaultVertexFormat.NEW_ENTITY), shader -> flowAdd = shader);
        } catch (Exception exception) {
            flow = null;
            flowAdd = null;
            TwilightBossPlus.LOGGER.warn("Failed to load fx_flow shader, falling back to vanilla effects", exception);
        }
    }

    public static ShaderInstance flow() {
        return flow;
    }

    public static ShaderInstance flowAdd() {
        return flowAdd;
    }

    public static boolean fancy() {
        return flow != null && flowAdd != null && !shaderPack;
    }

    public static void refreshShaderPack() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                irisInUse = api.getMethod("isShaderPackInUse");
            } catch (Throwable throwable) {
                irisApi = null;
                irisInUse = null;
            }
        }
        if (irisApi == null || irisInUse == null) {
            shaderPack = false;
            return;
        }
        try {
            shaderPack = Boolean.TRUE.equals(irisInUse.invoke(irisApi));
        } catch (Throwable throwable) {
            shaderPack = false;
            irisApi = null;
        }
    }
}
