package com.twilightbossplus.common;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.hydra.HydraBrainHolder;
import com.twilightbossplus.knight.KnightBrainHolder;
import com.twilightbossplus.lich.LichBrainHolder;
import com.twilightbossplus.minoshroom.MinoBrainHolder;
import com.twilightbossplus.naga.NagaBrainHolder;
import com.twilightbossplus.network.BossNetwork;
import com.twilightbossplus.network.VfxPacket;
import com.twilightbossplus.queen.QueenBrainHolder;
import com.twilightbossplus.urghast.UrGhastBrainHolder;
import com.twilightbossplus.yeti.YetiBrainHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.entity.boss.Naga;
import twilightforest.entity.boss.SnowQueen;
import twilightforest.entity.boss.UrGhast;

public final class BossActivation {
    public static final String ACTIVATED_TAG = "tbp_activated";
    private static final Set<Integer> CLIENT_AWAKENED = ConcurrentHashMap.newKeySet();

    private BossActivation() {
    }

    public static void markClientAwakened(int entityId) {
        if (entityId >= 0) {
            CLIENT_AWAKENED.add(entityId);
        }
    }

    public static void clearClientAwakened() {
        CLIENT_AWAKENED.clear();
    }

    public static boolean isEnhanced(@Nullable Entity entity, boolean bossConfigEnabled) {
        if (!bossConfigEnabled || entity == null) {
            return false;
        }
        if (!GeneralConfig.REQUIRE_TROPHY_ACTIVATION.get()) {
            return true;
        }
        if (entity.level().isClientSide()) {
            return CLIENT_AWAKENED.contains(entity.getId());
        }
        return entity.entityTags().contains(ACTIVATED_TAG);
    }

    public static void syncClient(Mob mob) {
        CompoundTag data = new CompoundTag();
        data.putInt("id", mob.getId());
        BossNetwork.sendAround(mob, new VfxPacket(VfxPacket.AWAKEN, data));
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!GeneralConfig.REQUIRE_TROPHY_ACTIVATION.get()) {
            return;
        }
        Mob boss = resolveBoss(event.getTarget());
        if (boss == null || !boss.isAlive()) {
            return;
        }
        if (!isBossReworkEnabled(boss)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !matchesActivationItem(boss, stack)) {
            return;
        }

        boolean alreadyActivated = boss.level().isClientSide()
                ? CLIENT_AWAKENED.contains(boss.getId())
                : boss.entityTags().contains(ACTIVATED_TAG);
        if (alreadyActivated) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        if (boss.level().isClientSide() || !(boss.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Player player = event.getEntity();
        List<Mob> targets = collectTargets(boss);
        for (Mob member : targets) {
            member.addTag(ACTIVATED_TAG);
            syncClient(member);
        }

        String itemId = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (GeneralConfig.CONSUME_ACTIVATION_ITEM.get() && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        double cx = boss.getX();
        double cy = boss.getY() + boss.getBbHeight() * 0.5;
        double cz = boss.getZ();
        double spread = Math.max(0.6, boss.getBbWidth() * 0.45);
        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy, cz, 45, spread, boss.getBbHeight() * 0.4, spread, 0.25);
        serverLevel.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 30, spread, boss.getBbHeight() * 0.4, spread, 0.12);
        serverLevel.playSound(null, cx, cy, cz, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0F, 0.6F);
        serverLevel.playSound(null, cx, cy, cz, SoundEvents.TRIDENT_THUNDER, SoundSource.HOSTILE, 1.6F, 1.15F);

        BossBrain<?> brain = findBrain(boss);
        if (brain != null) {
            brain.shake(boss.position(), 0.7F, 24.0F, 16);
        }
        TwilightBossPlus.debug("Boss {} awakened to enhanced form by {} using {}", boss.getId(), player.getScoreboardName(), itemId);
    }

    @Nullable
    private static Mob resolveBoss(@Nullable Entity raw) {
        Entity current = raw;
        if (current instanceof PartEntity<?> part) {
            current = part.getParent();
        }
        if (current instanceof Lich lich && lich.isShadowClone() && lich.getMaster() != null) {
            current = lich.getMaster();
        }
        if (current instanceof Naga
                || current instanceof Lich
                || current instanceof Minoshroom
                || current instanceof Hydra
                || current instanceof KnightPhantom
                || current instanceof UrGhast
                || current instanceof AlphaYeti
                || current instanceof SnowQueen) {
            return (Mob) current;
        }
        return null;
    }

    private static List<Mob> collectTargets(Mob boss) {
        List<Mob> result = new ArrayList<>();
        if (boss instanceof KnightPhantom phantom) {
            for (KnightPhantom other : phantom.getNearbyKnights()) {
                if (other.isAlive() && !result.contains(other)) {
                    result.add(other);
                }
            }
        }
        if (!result.contains(boss)) {
            result.add(boss);
        }
        return result;
    }

    private static boolean isBossReworkEnabled(Mob boss) {
        if (boss instanceof Naga) {
            return NagaConfig.ENABLED.get();
        }
        if (boss instanceof Lich) {
            return LichConfig.ENABLED.get();
        }
        if (boss instanceof Minoshroom) {
            return MinoConfig.ENABLED.get();
        }
        if (boss instanceof Hydra) {
            return HydraConfig.ENABLED.get();
        }
        if (boss instanceof KnightPhantom) {
            return KnightConfig.ENABLED.get();
        }
        if (boss instanceof UrGhast) {
            return UrGhastConfig.ENABLED.get();
        }
        if (boss instanceof AlphaYeti) {
            return YetiConfig.ENABLED.get();
        }
        if (boss instanceof SnowQueen) {
            return SnowQueenConfig.ENABLED.get();
        }
        return false;
    }

    private static boolean matchesActivationItem(Mob boss, ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<? extends String> allowed = allowedItems(boss);
        for (String entry : allowed) {
            if (id.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    private static List<? extends String> allowedItems(Mob boss) {
        if (boss instanceof Naga) {
            return GeneralConfig.NAGA_ITEMS.get();
        }
        if (boss instanceof Lich) {
            return GeneralConfig.LICH_ITEMS.get();
        }
        if (boss instanceof Minoshroom) {
            return GeneralConfig.MINOSHROOM_ITEMS.get();
        }
        if (boss instanceof Hydra) {
            return GeneralConfig.HYDRA_ITEMS.get();
        }
        if (boss instanceof KnightPhantom) {
            return GeneralConfig.KNIGHT_PHANTOM_ITEMS.get();
        }
        if (boss instanceof UrGhast) {
            return GeneralConfig.UR_GHAST_ITEMS.get();
        }
        if (boss instanceof AlphaYeti) {
            return GeneralConfig.ALPHA_YETI_ITEMS.get();
        }
        if (boss instanceof SnowQueen) {
            return GeneralConfig.SNOW_QUEEN_ITEMS.get();
        }
        return List.of();
    }

    @Nullable
    private static BossBrain<?> findBrain(Mob boss) {
        if (boss instanceof NagaBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof LichBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof MinoBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof HydraBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof KnightBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof UrGhastBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof YetiBrainHolder h) {
            return h.tbp$getBrain();
        }
        if (boss instanceof QueenBrainHolder h) {
            return h.tbp$getBrain();
        }
        return null;
    }
}
