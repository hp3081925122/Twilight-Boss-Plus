package com.twilightbossplus.minoshroom;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.MinoshroomConfig;
import com.twilightbossplus.naga.NagaEvents;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.init.TFDamageTypes;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MinoshroomEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("7d3b9e21-4c6a-4f18-b2d5-0e9a8c1f6b43");
    private static final UUID ATTACK_UUID = UUID.fromString("e4a17c58-93d2-4b6e-8f01-2c5d7a9b3e16");

    private MinoshroomEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Minoshroom mob) || !MinoshroomConfig.ENABLED.get()) {
            return;
        }
        boolean healthChanged = NagaEvents.applyMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", MinoshroomConfig.HEALTH_MULTIPLIER.get());
        NagaEvents.applyMultiplier(mob.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_UUID, "Boss Plus attack", MinoshroomConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            mob.setHealth(mob.getMaxHealth());
        } else if (healthChanged) {
            mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Minoshroom {}] joined level, max health {}, attack {}", mob.getId(), mob.getMaxHealth(), mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!MinoshroomConfig.ENABLED.get() || event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Minoshroom victim) {
            float multiplier = ((MinoshroomControllerHolder) victim).twilightbossplus$getController().getDamageTakenMultiplier();
            if (multiplier != 1.0F) {
                event.setAmount(event.getAmount() * multiplier);
            }
        }
        if (event.getSource().getEntity() instanceof Minoshroom attacker && event.getSource().is(TFDamageTypes.AXING)
            && ((MinoshroomControllerHolder) attacker).twilightbossplus$getController().isAxeAirborne()) {
            event.setAmount(event.getAmount() * MinoshroomConfig.AXE_MELEE_PENALTY.get().floatValue());
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (MinoshroomConfig.ENABLED.get() && event.getEntity() instanceof Minoshroom mob && !mob.level().isClientSide()
            && ((MinoshroomControllerHolder) mob).twilightbossplus$getController().isRoaring()) {
            event.setCanceled(true);
        }
    }
}
