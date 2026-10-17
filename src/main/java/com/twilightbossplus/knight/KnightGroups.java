package com.twilightbossplus.knight;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.KnightPhantomConfig;
import com.twilightbossplus.naga.NagaEvents;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.boss.KnightPhantom;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KnightGroups {
    private static final UUID HEALTH_UUID = UUID.fromString("5d1e8a37-0c64-4b92-a7f1-3e9b26d4c815");
    private static final UUID ATTACK_UUID = UUID.fromString("c84b2f19-73ad-4e05-9b6c-12f7e0a5d3b6");
    private static final Map<ServerLevel, Map<BlockPos, KnightGroup>> GROUPS = new IdentityHashMap<>();
    private static int nextEchoId = 1;

    private KnightGroups() {
    }

    public static int nextEchoId() {
        nextEchoId++;
        return nextEchoId;
    }

    @Nullable
    public static KnightGroup of(KnightPhantom knight) {
        if (!KnightPhantomConfig.ENABLED.get() || !(knight.level() instanceof ServerLevel level)) {
            return null;
        }
        GlobalPos home = knight.getRestrictionPoint();
        if (home == null || !home.dimension().equals(level.dimension())) {
            return null;
        }
        Map<BlockPos, KnightGroup> groups = GROUPS.computeIfAbsent(level, key -> new HashMap<>());
        KnightGroup group = groups.get(home.pos());
        if (group == null) {
            group = new KnightGroup(level, home.pos().immutable());
            groups.put(home.pos().immutable(), group);
            if (GeneralConfig.DEBUG_LOG.get()) {
                TwilightBossPlus.LOGGER.info("[KnightPhantom] created group at {}", home.pos());
            }
        }
        return group;
    }

    public static float speedFactor(KnightPhantom knight) {
        KnightGroup group = of(knight);
        if (group == null) {
            return 1.0F;
        }
        return group.getSpeedFactor();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, KnightGroup> groups = GROUPS.get(level);
        if (groups == null || groups.isEmpty()) {
            return;
        }
        Iterator<KnightGroup> iterator = groups.values().iterator();
        while (iterator.hasNext()) {
            KnightGroup group = iterator.next();
            if (!KnightPhantomConfig.ENABLED.get()) {
                iterator.remove();
                continue;
            }
            group.tick();
            if (group.isFinished()) {
                iterator.remove();
                if (GeneralConfig.DEBUG_LOG.get()) {
                    TwilightBossPlus.LOGGER.info("[KnightPhantom] removed empty group at {}", group.getHome());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            GROUPS.remove(level);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof KnightPhantom knight) || !KnightPhantomConfig.ENABLED.get()) {
            return;
        }
        AttributeInstance attack = knight.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(KnightPhantomConfig.BASE_ATTACK.get());
        }
        boolean healthChanged = NagaEvents.applyMultiplier(knight.getAttribute(Attributes.MAX_HEALTH), HEALTH_UUID, "Boss Plus health", KnightPhantomConfig.HEALTH_MULTIPLIER.get());
        NagaEvents.applyMultiplier(attack, ATTACK_UUID, "Boss Plus attack", KnightPhantomConfig.ATTACK_MULTIPLIER.get());
        if (healthChanged && !event.loadedFromDisk()) {
            knight.setHealth(knight.getMaxHealth());
        } else if (healthChanged) {
            knight.setHealth(Math.min(knight.getHealth(), knight.getMaxHealth()));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom {}] joined level, max health {}, attack {}", knight.getId(), knight.getMaxHealth(), knight.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof KnightPhantom knight) || knight.level().isClientSide()) {
            return;
        }
        KnightGroup group = of(knight);
        if (group != null && group.isRegrouping()) {
            event.setAmount(event.getAmount() * (float) (1.0D - KnightPhantomConfig.REGROUP_DAMAGE_REDUCTION.get()));
        }
    }
}
