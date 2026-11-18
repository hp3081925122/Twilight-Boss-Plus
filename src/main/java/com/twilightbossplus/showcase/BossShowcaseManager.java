package com.twilightbossplus.showcase;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.monster.IceCrystal;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.monster.LichMinion;
import twilightforest.init.TFEntities;

@Mod.EventBusSubscriber(modid = TwilightBossPlus.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BossShowcaseManager {
    public enum BossKind {
        NAGA("naga"),
        LICH("lich"),
        MINOSHROOM("minoshroom"),
        HYDRA("hydra"),
        KNIGHT_PHANTOM("knight_phantom"),
        UR_GHAST("ur_ghast"),
        ALPHA_YETI("alpha_yeti"),
        SNOW_QUEEN("snow_queen");

        private final String id;

        BossKind(String id) {
            this.id = id;
        }

        public String id() {
            return this.id;
        }

        @Nullable
        public static BossKind byId(String id) {
            for (BossKind kind : values()) {
                if (kind.id.equalsIgnoreCase(id)) {
                    return kind;
                }
            }
            return null;
        }
    }

    private static final int CROWD_TARGET = 24;
    private static final int CROWD_REPLENISH_THRESHOLD = 18;
    private static final int SKILL_GAP_TICKS = 25;
    private static final int BOSS_GAP_TICKS = 35;
    private static final int SKILL_TIMEOUT_TICKS = 260;

    private static final EntityType<?>[] VANILLA_MOBS = {
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.VINDICATOR,
            EntityType.PILLAGER
    };

    private static boolean active;
    @Nullable
    private static ServerLevel level;
    @Nullable
    private static UUID playerUuid;
    private static boolean restoreHudOnStop;
    private static Vec3 center = Vec3.ZERO;
    private static final Deque<BossKind> queue = new ArrayDeque<>();
    private static final List<UUID> bossUuids = new ArrayList<>();
    private static final List<UUID> mobUuids = new ArrayList<>();
    @Nullable
    private static BossKind currentKind;
    @Nullable
    private static UUID leadBossUuid;
    private static int skillIndex;
    private static int waitTicks;
    private static int skillTicks;
    private static int betweenBossTicks;

    private BossShowcaseManager() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void start(ServerLevel targetLevel, ServerPlayer player, @Nullable BossKind single, boolean hideGui) {
        stop(false);
        active = true;
        level = targetLevel;
        playerUuid = player.getUUID();
        restoreHudOnStop = hideGui;
        center = findArenaCenter(targetLevel, player);
        queue.clear();
        if (single != null) {
            queue.add(single);
        } else {
            queue.addAll(Arrays.asList(BossKind.values()));
        }
        if (hideGui) {
            sendHudState(player, true);
        }
        replenishCrowd();
        spawnNextBoss();
    }

    public static void stop(boolean restoreHud) {
        if (restoreHud && restoreHudOnStop && level != null && playerUuid != null) {
            Entity entity = level.getServer().getPlayerList().getPlayer(playerUuid);
            if (entity instanceof ServerPlayer player) {
                sendHudState(player, false);
            }
        }
        clearCurrentBosses();
        if (level != null) {
            for (UUID uuid : mobUuids) {
                Entity entity = level.getEntity(uuid);
                if (entity != null) {
                    entity.discard();
                }
            }
        }
        mobUuids.clear();
        queue.clear();
        currentKind = null;
        leadBossUuid = null;
        level = null;
        playerUuid = null;
        restoreHudOnStop = false;
        active = false;
        skillIndex = 0;
        waitTicks = 0;
        skillTicks = 0;
        betweenBossTicks = 0;
    }

    public static void setPlayerHudHidden(ServerPlayer player, boolean hide) {
        sendHudState(player, hide);
    }

    private static void sendHudState(ServerPlayer player, boolean hide) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("hide", hide);
        BossNetwork.sendToPlayer(player, new VfxPacket(VfxPacket.SHOWCASE_HUD, data));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active || level == null) {
            return;
        }
        pruneDeadMobs();
        Mob leadBoss = getLeadBoss();
        BossBrain<?> leadBrain = getBrain(leadBoss);
        if (leadBoss == null || leadBrain == null || !leadBoss.isAlive()) {
            clearCurrentBosses();
            if (!spawnNextBoss()) {
                stop(true);
            }
            return;
        }
        maintainBosses(leadBoss);
        maintainMobAggro(leadBoss);

        if (leadBrain.isSkillRunning()) {
            skillTicks++;
            if (skillTicks > SKILL_TIMEOUT_TICKS) {
                stopBrain(leadBrain);
                skillTicks = 0;
                waitTicks = SKILL_GAP_TICKS;
            }
            return;
        }

        skillTicks = 0;
        if (skillIndex < leadBrain.showcaseSkillCount()) {
            if (waitTicks > 0) {
                waitTicks--;
                return;
            }
            replenishCrowd();
            if (currentKind == BossKind.LICH && leadBoss instanceof Lich lich) {
                ensureLichMinions(lich);
            }
            recenterIfNeeded(leadBoss);
            Mob target = nearestMob(leadBoss.position());
            if (target == null) {
                replenishCrowd();
                target = nearestMob(leadBoss.position());
            }
            if (target != null) {
                for (UUID uuid : bossUuids) {
                    Entity entity = level.getEntity(uuid);
                    if (entity instanceof Mob mob) {
                        mob.setTarget(target);
                    }
                }
            }
            TwilightBossPlus.LOGGER.info("Showcase casting {} skill {}/{}: {}",
                    currentKind != null ? currentKind.id() : "unknown",
                    skillIndex + 1,
                    leadBrain.showcaseSkillCount(),
                    leadBrain.showcaseSkillName(skillIndex));
            leadBrain.startShowcaseSkill(skillIndex);
            skillIndex++;
            waitTicks = SKILL_GAP_TICKS;
            return;
        }

        if (betweenBossTicks < BOSS_GAP_TICKS) {
            betweenBossTicks++;
            return;
        }
        clearCurrentBosses();
        replenishCrowd();
        if (!spawnNextBoss()) {
            stop(true);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        stop(false);
    }

    private static boolean spawnNextBoss() {
        if (level == null || queue.isEmpty()) {
            return false;
        }
        currentKind = queue.poll();
        skillIndex = 0;
        waitTicks = 20;
        skillTicks = 0;
        betweenBossTicks = 0;
        bossUuids.clear();
        leadBossUuid = null;

        if (currentKind == BossKind.KNIGHT_PHANTOM) {
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2.0 * i) / 6.0;
                double x = center.x + Math.cos(angle) * 4.0;
                double z = center.z + Math.sin(angle) * 4.0;
                KnightPhantom knight = TFEntities.KNIGHT_PHANTOM.get().create(level);
                if (knight == null) {
                    continue;
                }
                knight.moveTo(x, center.y + 1.5, z, (float) Math.toDegrees(angle) + 90.0F, 0.0F);
                knight.setNumber(i);
                knight.finalizeSpawn(level, level.getCurrentDifficultyAt(knight.blockPosition()), MobSpawnType.COMMAND, null, null);
                knight.setPersistenceRequired();
                level.addFreshEntity(knight);
                bossUuids.add(knight.getUUID());
                BossBrain<?> brain = getBrain(knight);
                if (brain != null) {
                    brain.setShowcaseMode(true);
                }
                if (leadBossUuid == null) {
                    leadBossUuid = knight.getUUID();
                }
            }
            return leadBossUuid != null;
        }

        Mob boss = createBoss(currentKind);
        if (boss == null) {
            return spawnNextBoss();
        }
        double yOffset = switch (currentKind) {
            case UR_GHAST -> 8.0;
            case SNOW_QUEEN -> 3.5;
            default -> 0.0;
        };
        boss.moveTo(center.x, center.y + yOffset, center.z, 0.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), MobSpawnType.COMMAND, null, null);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);
        bossUuids.add(boss.getUUID());
        leadBossUuid = boss.getUUID();
        BossBrain<?> brain = getBrain(boss);
        if (brain != null) {
            brain.setShowcaseMode(true);
        }
        if (currentKind == BossKind.LICH && boss instanceof Lich lich) {
            ensureLichMinions(lich);
        }
        return true;
    }

    @Nullable
    private static Mob createBoss(BossKind kind) {
        if (level == null) {
            return null;
        }
        return switch (kind) {
            case NAGA -> TFEntities.NAGA.get().create(level);
            case LICH -> TFEntities.LICH.get().create(level);
            case MINOSHROOM -> TFEntities.MINOSHROOM.get().create(level);
            case HYDRA -> TFEntities.HYDRA.get().create(level);
            case KNIGHT_PHANTOM -> TFEntities.KNIGHT_PHANTOM.get().create(level);
            case UR_GHAST -> TFEntities.UR_GHAST.get().create(level);
            case ALPHA_YETI -> TFEntities.ALPHA_YETI.get().create(level);
            case SNOW_QUEEN -> TFEntities.SNOW_QUEEN.get().create(level);
        };
    }

    private static void ensureLichMinions(Lich lich) {
        if (level == null) {
            return;
        }
        List<LichMinion> existing = level.getEntitiesOfClass(LichMinion.class, lich.getBoundingBox().inflate(24.0),
                minion -> minion.master == lich && minion.isAlive());
        int needed = 3 - existing.size();
        RandomSource random = level.getRandom();
        for (int i = 0; i < needed; i++) {
            LichMinion minion = TFEntities.LICH_MINION.get().create(level);
            if (minion == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 3.5 + random.nextDouble() * 3.0;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = groundY(level, x, z, center.y + 4.0);
            minion.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
            minion.master = lich;
            minion.setPersistenceRequired();
            level.addFreshEntity(minion);
            bossUuids.add(minion.getUUID());
        }
    }

    private static void maintainBosses(Mob leadBoss) {
        if (level == null) {
            return;
        }
        Mob target = nearestMob(leadBoss.position());
        for (UUID uuid : bossUuids) {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            mob.setHealth(mob.getMaxHealth());
            mob.clearFire();
            if (target != null && (mob.getTarget() == null || !mob.getTarget().isAlive())) {
                mob.setTarget(target);
            }
        }
    }

    private static void maintainMobAggro(Mob leadBoss) {
        if (level == null) {
            return;
        }
        for (UUID uuid : mobUuids) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob && mob.isAlive()) {
                if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                    mob.setTarget(leadBoss);
                }
            }
        }
    }

    private static void recenterIfNeeded(Mob leadBoss) {
        double dx = leadBoss.getX() - center.x;
        double dz = leadBoss.getZ() - center.z;
        if (dx * dx + dz * dz <= 16.0 * 16.0) {
            return;
        }
        double yOffset = switch (currentKind != null ? currentKind : BossKind.NAGA) {
            case UR_GHAST -> 8.0;
            case SNOW_QUEEN -> 3.5;
            default -> 0.0;
        };
        leadBoss.setPos(center.x, center.y + yOffset, center.z);
        leadBoss.setDeltaMovement(Vec3.ZERO);
    }

    private static void clearCurrentBosses() {
        if (level == null) {
            bossUuids.clear();
            return;
        }
        for (UUID uuid : bossUuids) {
            Entity entity = level.getEntity(uuid);
            if (entity != null) {
                BossBrain<?> brain = getBrain(entity);
                if (brain != null) {
                    stopBrain(brain);
                    brain.setShowcaseMode(false);
                }
                entity.discard();
            }
        }
        bossUuids.clear();
        AABB box = new AABB(center.x - 40.0, center.y - 20.0, center.z - 40.0, center.x + 40.0, center.y + 30.0, center.z + 40.0);
        for (IceCrystal crystal : level.getEntitiesOfClass(IceCrystal.class, box)) {
            crystal.discard();
        }
        for (LichMinion minion : level.getEntitiesOfClass(LichMinion.class, box)) {
            minion.discard();
        }
    }

    private static void stopBrain(BossBrain<?> brain) {
        if (brain instanceof com.twilightbossplus.naga.NagaBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.lich.LichBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.minoshroom.MinoBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.hydra.HydraBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.knight.KnightBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.urghast.UrGhastBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.yeti.YetiBrain b) {
            b.stopAll();
        } else if (brain instanceof com.twilightbossplus.queen.QueenBrain b) {
            b.stopAll();
        }
    }

    private static void pruneDeadMobs() {
        if (level == null) {
            mobUuids.clear();
            return;
        }
        Iterator<UUID> it = mobUuids.iterator();
        while (it.hasNext()) {
            Entity entity = level.getEntity(it.next());
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                it.remove();
            }
        }
    }

    private static void replenishCrowd() {
        if (level == null || mobUuids.size() >= CROWD_REPLENISH_THRESHOLD) {
            return;
        }
        RandomSource random = level.getRandom();
        Mob leadBoss = getLeadBoss();
        int toSpawn = CROWD_TARGET - mobUuids.size();
        for (int i = 0; i < toSpawn; i++) {
            EntityType<?> type = VANILLA_MOBS[random.nextInt(VANILLA_MOBS.length)];
            Entity created = type.create(level);
            if (!(created instanceof Mob mob)) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = 5.0 + random.nextDouble() * 6.0;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = groundY(level, x, z, center.y + 6.0);
            float yaw = (float) (Math.toDegrees(Math.atan2(center.z - z, center.x - x)) - 90.0);
            mob.moveTo(x, y, z, yaw, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null, null);
            mob.setPersistenceRequired();
            mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 600, 0, false, false));
            if (leadBoss != null) {
                mob.setTarget(leadBoss);
            }
            level.addFreshEntity(mob);
            mobUuids.add(mob.getUUID());
        }
    }

    @Nullable
    private static Mob getLeadBoss() {
        if (level == null || leadBossUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(leadBossUuid);
        return entity instanceof Mob mob ? mob : null;
    }

    @Nullable
    private static Mob nearestMob(Vec3 from) {
        if (level == null) {
            return null;
        }
        Mob best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID uuid : mobUuids) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof Mob mob && mob.isAlive()) {
                double dist = mob.position().distanceToSqr(from);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = mob;
                }
            }
        }
        return best;
    }

    @Nullable
    private static BossBrain<?> getBrain(@Nullable Entity entity) {
        if (entity instanceof NagaBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof LichBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof MinoBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof HydraBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof KnightBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof UrGhastBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof YetiBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        if (entity instanceof QueenBrainHolder holder) {
            return holder.tbp$getBrain();
        }
        return null;
    }

    private static Vec3 findArenaCenter(ServerLevel targetLevel, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4) {
            flat = new Vec3(0.0, 0.0, 1.0);
        } else {
            flat = flat.normalize();
        }
        double x = player.getX() + flat.x * 14.0;
        double z = player.getZ() + flat.z * 14.0;
        double y = groundY(targetLevel, x, z, player.getY() + 4.0);
        return new Vec3(x, y, z);
    }

    private static double groundY(ServerLevel targetLevel, double x, double z, double startY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = Mth.floor(startY);
        int bottom = top - 24;
        for (int y = top; y >= bottom; y--) {
            pos.set(Mth.floor(x), y, Mth.floor(z));
            BlockState state = targetLevel.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(targetLevel, pos);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        return startY - 4.0;
    }
}
