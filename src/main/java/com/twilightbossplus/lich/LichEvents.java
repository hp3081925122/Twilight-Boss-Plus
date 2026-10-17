package com.twilightbossplus.lich;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.naga.NagaEvents;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.Lich;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LichEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("5a7c1e93-2b6d-4f08-9c3e-81d4a6f0b2c5");
    private static final UUID ATTACK_UUID = UUID.fromString("c2e8f415-7a3b-4d96-b0e1-4f9d2c6a8e37");

    private LichEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Lich lich) || !LichConfig.ENABLED.get()) {
            return;
        }
        NagaEvents.applyMultiplier(lich.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_UUID, "Boss Plus attack", LichConfig.ATTACK_MULTIPLIER.get());
        if (lich.isShadowClone()) {
            return;
        }
        boolean healthChanged = NagaEvents.applyMultiplier(lich.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", LichConfig.HEALTH_MULTIPLIER.get());
        if (!event.loadedFromDisk()) {
            lich.setShieldStrength(LichConfig.SHIELD_STRENGTH.get());
            lich.setMinionsToSummon(LichConfig.MINIONS_TO_SUMMON.get());
            if (healthChanged) {
                lich.setHealth(lich.getMaxHealth());
            }
        } else if (healthChanged) {
            lich.setHealth(Math.min(lich.getHealth(), lich.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Lich {}] joined level, max health {}, attack {}, shield {}, minions {}", lich.getId(), lich.getMaxHealth(),
                lich.getAttributeValue(Attributes.ATTACK_DAMAGE), lich.getShieldStrength(), lich.getMinionsToSummon());
        }
    }
}
