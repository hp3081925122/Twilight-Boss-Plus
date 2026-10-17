package com.twilightbossplus.client;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.client.render.MinoHornLayer;
import com.twilightbossplus.client.render.MushroomPillarRenderer;
import com.twilightbossplus.client.render.PrimalTearRenderer;
import com.twilightbossplus.client.render.YetiSnowballRenderer;
import com.twilightbossplus.client.render.PuffballRenderer;
import com.twilightbossplus.client.render.SoulCandleRenderer;
import com.twilightbossplus.client.render.ThrownAxeRenderer;
import com.twilightbossplus.client.render.VenomGlobRenderer;
import com.twilightbossplus.client.render.VenomPoolRenderer;
import com.twilightbossplus.client.render.VoidOrbRenderer;
import com.twilightbossplus.registry.ModEntities;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.init.TFEntities;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VENOM_GLOB.get(), VenomGlobRenderer::new);
        event.registerEntityRenderer(ModEntities.VENOM_POOL.get(), VenomPoolRenderer::new);
        event.registerEntityRenderer(ModEntities.VOID_ORB.get(), VoidOrbRenderer::new);
        event.registerEntityRenderer(ModEntities.SOUL_CANDLE.get(), SoulCandleRenderer::new);
        event.registerEntityRenderer(ModEntities.MUSHROOM_PILLAR.get(), MushroomPillarRenderer::new);
        event.registerEntityRenderer(ModEntities.PUFFBALL.get(), PuffballRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_AXE.get(), ThrownAxeRenderer::new);
        event.registerEntityRenderer(ModEntities.PRIMAL_TEAR.get(), PrimalTearRenderer::new);
        event.registerEntityRenderer(ModEntities.YETI_SNOWBALL.get(), YetiSnowballRenderer::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        LivingEntityRenderer renderer = event.getRenderer(TFEntities.MINOSHROOM.get());
        if (renderer != null && renderer.getModel() instanceof HumanoidModel) {
            renderer.addLayer(new MinoHornLayer((RenderLayerParent) renderer));
        }
    }
}
