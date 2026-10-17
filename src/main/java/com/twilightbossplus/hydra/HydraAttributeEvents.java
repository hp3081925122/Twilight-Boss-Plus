package com.twilightbossplus.hydra;

import com.twilightbossplus.TwilightBossPlus;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.init.TFEntities;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HydraAttributeEvents {
    public static final double DEFAULT_ATTACK = 16.0D;

    private HydraAttributeEvents() {
    }

    @SubscribeEvent
    public static void onAttributes(EntityAttributeModificationEvent event) {
        if (!event.has(TFEntities.HYDRA.get(), Attributes.ATTACK_DAMAGE)) {
            event.add(TFEntities.HYDRA.get(), Attributes.ATTACK_DAMAGE, DEFAULT_ATTACK);
        }
    }
}
