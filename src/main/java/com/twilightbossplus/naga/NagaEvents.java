package com.twilightbossplus.naga;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.NagaConfig;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.Naga;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NagaEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("3f8a2c71-5d4e-4b9a-a1c6-7e0b9d2f4a18");
    private static final UUID ATTACK_UUID = UUID.fromString("9b1e4d62-0c7a-4f35-8e29-c5a7f3d1b064");

    private NagaEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Naga naga) || !NagaConfig.ENABLED.get()) {
            return;
        }
        boolean healthChanged = applyMultiplier(naga.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", NagaConfig.HEALTH_MULTIPLIER.get());
        applyMultiplier(naga.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_UUID, "Boss Plus attack", NagaConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            naga.setHealth(naga.getMaxHealth());
        } else if (healthChanged) {
            naga.setHealth(Math.min(naga.getHealth(), naga.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Naga {}] joined level, max health {}, attack {}", naga.getId(), naga.getMaxHealth(), naga.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    public static boolean applyMultiplier(AttributeInstance instance, UUID id, String name, double multiplier) {
        if (instance == null) {
            return false;
        }
        double amount = multiplier - 1.0D;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.getAmount() - amount) < 1.0E-6D) {
            return false;
        }
        if (existing != null) {
            instance.removeModifier(id);
        }
        if (Math.abs(amount) < 1.0E-6D) {
            return existing != null;
        }
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
        return true;
    }
}
