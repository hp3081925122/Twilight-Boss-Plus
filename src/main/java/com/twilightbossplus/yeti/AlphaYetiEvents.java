package com.twilightbossplus.yeti;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.naga.NagaEvents;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.init.TFDamageTypes;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AlphaYetiEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("5f2c8e31-9a7d-4b64-8c15-3e0d7b2a9f68");
    private static final UUID ATTACK_UUID = UUID.fromString("b81e4d27-6c3f-4a95-9e02-7d5a1c8f3b46");
    private static final double ICE_CAP_RANGE = 48.0D;

    private AlphaYetiEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AlphaYeti mob) || !AlphaYetiConfig.ENABLED.get()) {
            return;
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(AlphaYetiConfig.BASE_ATTACK.get());
        }
        boolean healthChanged = NagaEvents.applyMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", AlphaYetiConfig.HEALTH_MULTIPLIER.get());
        NagaEvents.applyMultiplier(attack, ATTACK_UUID, "Boss Plus attack", AlphaYetiConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            mob.setHealth(mob.getMaxHealth());
        } else if (healthChanged) {
            mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[AlphaYeti {}] joined level, max health {}, attack {}", mob.getId(), mob.getMaxHealth(), mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!AlphaYetiConfig.ENABLED.get() || !(event.getEntity() instanceof Player player) || player.level().isClientSide()
            || !event.getSource().is(TFDamageTypes.FALLING_ICE) || AlphaYetiConfig.FALLING_ICE_CAP.get() <= 0.0D) {
            return;
        }
        AlphaYeti nearest = null;
        double best = ICE_CAP_RANGE * ICE_CAP_RANGE;
        for (AlphaYeti yeti : player.level().getEntitiesOfClass(AlphaYeti.class, player.getBoundingBox().inflate(ICE_CAP_RANGE), AlphaYeti::isAlive)) {
            double dist = yeti.distanceToSqr(player);
            if (dist < best) {
                best = dist;
                nearest = yeti;
            }
        }
        if (nearest == null) {
            return;
        }
        float cap = (float) (nearest.getAttributeValue(Attributes.ATTACK_DAMAGE) * AlphaYetiConfig.FALLING_ICE_CAP.get());
        if (event.getAmount() > cap) {
            if (GeneralConfig.DEBUG_LOG.get()) {
                TwilightBossPlus.LOGGER.info("[AlphaYeti {}] falling ice damage {} capped to {}", nearest.getId(), event.getAmount(), cap);
            }
            event.setAmount(cap);
        }
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (AlphaYetiConfig.ENABLED.get() && event.getEntity() instanceof AlphaYeti mob && !mob.level().isClientSide()
            && ((AlphaYetiControllerHolder) mob).twilightbossplus$getController().isRoaring()) {
            event.setCanceled(true);
        }
    }
}
