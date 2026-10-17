package com.twilightbossplus.hydra;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.naga.NagaEvents;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.Hydra;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HydraEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("3a9c5e17-2b84-4d6f-9e03-71c8b2a4f590");
    private static final UUID ATTACK_UUID = UUID.fromString("b62f8d04-5e19-4a73-8c2b-9d1e06f3a7c8");

    private HydraEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Hydra mob) || !HydraConfig.ENABLED.get()) {
            return;
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(HydraConfig.BASE_ATTACK.get());
        }
        boolean healthChanged = NagaEvents.applyMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", HydraConfig.HEALTH_MULTIPLIER.get());
        NagaEvents.applyMultiplier(attack, ATTACK_UUID, "Boss Plus attack", HydraConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            mob.setHealth(mob.getMaxHealth());
        } else if (healthChanged) {
            mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Hydra {}] joined level, max health {}, attack {}", mob.getId(), mob.getMaxHealth(), mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!HydraConfig.ENABLED.get() || !(event.getEntity() instanceof Hydra hydra) || hydra.level().isClientSide()) {
            return;
        }
        float multiplier = ((HydraControllerHolder) hydra).twilightbossplus$getController().getDamageTakenMultiplier();
        if (multiplier != 1.0F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}
