package com.twilightbossplus.urghast;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.mixin.UrGhastAccessor;
import com.twilightbossplus.naga.NagaEvents;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.UrGhast;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UrGhastEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("e4b7a2c9-3f15-4d8e-a061-5c2f9b7d3e14");
    private static final UUID ATTACK_UUID = UUID.fromString("91d3f6a8-7c2e-4b05-8e4a-2f6c1d9a0b73");

    private UrGhastEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof UrGhast mob) || !UrGhastConfig.ENABLED.get()) {
            return;
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(UrGhastConfig.BASE_ATTACK.get());
        }
        boolean healthChanged = NagaEvents.applyMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", UrGhastConfig.HEALTH_MULTIPLIER.get());
        NagaEvents.applyMultiplier(attack, ATTACK_UUID, "Boss Plus attack", UrGhastConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            mob.setHealth(mob.getMaxHealth());
        } else if (healthChanged) {
            mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        }
        if (!event.loadedFromDisk()) {
            ((UrGhastAccessor) mob).twilightbossplus$setDamageUntilNextPhase((float) (mob.getMaxHealth() * UrGhastConfig.TANTRUM_FIRST_RATIO.get()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[UrGhast {}] joined level, max health {}, attack {}", mob.getId(), mob.getMaxHealth(), mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!UrGhastConfig.ENABLED.get() || !(event.getEntity() instanceof UrGhast ghast) || ghast.level().isClientSide()) {
            return;
        }
        if (UrGhastConfig.TRAP_SCALES_WITH_HEALTH.get() && event.getSource().is(DamageTypes.GENERIC) && event.getSource().getEntity() == null) {
            float scaled = (float) (event.getAmount() * UrGhastConfig.HEALTH_MULTIPLIER.get());
            if (GeneralConfig.DEBUG_LOG.get()) {
                TwilightBossPlus.LOGGER.info("[UrGhast {}] trap damage {} scaled to {}", ghast.getId(), event.getAmount(), scaled);
            }
            event.setAmount(scaled);
        }
    }
}
