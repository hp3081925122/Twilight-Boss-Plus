package com.twilightbossplus.queen;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.naga.NagaEvents;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.SnowQueen;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SnowQueenEvents {
    private static final UUID HEALTH_UUID = UUID.fromString("3d9a6c14-7e2b-4f81-a5c3-9b0e1d6f2a47");
    private static final UUID ATTACK_UUID = UUID.fromString("c47e2a91-5b3d-4e68-8f1a-2d6c9e0b7f35");

    private SnowQueenEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof SnowQueen mob) || !SnowQueenConfig.ENABLED.get()) {
            return;
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(SnowQueenConfig.BASE_ATTACK.get());
        }
        boolean healthChanged = NagaEvents.applyMultiplier(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", SnowQueenConfig.HEALTH_MULTIPLIER.get());
        NagaEvents.applyMultiplier(attack, ATTACK_UUID, "Boss Plus attack", SnowQueenConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            mob.setHealth(mob.getMaxHealth());
        } else if (healthChanged) {
            mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[SnowQueen {}] joined level, max health {}, attack {}", mob.getId(), mob.getMaxHealth(), mob.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }
}
