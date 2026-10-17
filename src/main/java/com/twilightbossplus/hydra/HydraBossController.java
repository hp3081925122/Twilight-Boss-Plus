package com.twilightbossplus.hydra;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.mixin.HydraHeadContainerAccessor;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.HydraFxPacket;
import com.twilightbossplus.network.ModNetwork;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHead;
import twilightforest.entity.boss.HydraHeadContainer;
import twilightforest.init.TFDamageTypes;
import twilightforest.init.TFSounds;

public final class HydraBossController {
    private static final int HEADS = 7;
    private static final int GLOBAL_GAP = 40;
    private static final int GLOBAL_GAP_RANDOM = 20;
    private static final int ROAR_TICKS = 60;
    private static final int WAVE_GAP = 20;
    private static final double WAVE_HEIGHT = 2.5D;
    private static final double TARGET_RANGE = 48.0D;
    private static final double CLOSE_RANGE = 6.0D;
    private static final float REAR_HALF_ANGLE = 60.0F;
    private static final float TAIL_OVERSHOOT = 30.0F;
    private static final float TAIL_MAX_SWEEP = 210.0F;
    private static final float TAIL_DEGREES_PER_TICK = 12.5F;
    private static final double TAIL_INNER = 4.0D;
    private static final double TAIL_OUTER = 13.5D;
    private static final double TAIL_HALF_WIDTH = 3.0D;
    private static final double TAIL_HEIGHT = 3.5D;
    private static final int SWEEP_WINDUP = 41;
    private static final int SWEEP_RECOVER = 30;
    private static final float[] SWEEP_OFFSETS = {0.0F, 12.0F, -12.0F};
    private static final float SWEEP_SPREAD = 12.0F;
    private static final double SWEEP_RANGE = 20.0D;
    private static final double SWEEP_MIN_DIST = 6.0D;
    private static final double SWEEP_MAX_DIST = 18.0D;
    private static final int SCORCH_INTERVAL = 4;
    private static final int SCORCH_DAMAGE_INTERVAL = 10;
    private static final int SCORCH_BURN = 2;
    private static final double SCORCH_RADIUS = 1.2D;
    private static final double SCORCH_SPACING = 1.0D;
    private static final int MAX_SCORCHES = 80;
    private static final double SLAG_MIN_RANGE = 8.0D;
    private static final double SLAG_MAX_RANGE = 32.0D;
    private static final double SLAG_SPREAD_MIN = 2.0D;
    private static final double SLAG_SPREAD_MAX = 7.0D;
    private static final int SLAG_HEADS = 2;
    private static final int SLAG_SHOTS_PER_HEAD = 3;
    private static final int SLAG_TIMEOUT = 120;
    private static final int SLAG_BURN = 3;
    private static final int GEYSER_ROAR = 30;
    private static final int GEYSER_RING_INTERVAL = 15;
    private static final int GEYSER_DURATION = 10;
    private static final double GEYSER_FIRST_RADIUS = 5.0D;
    private static final double GEYSER_COLUMN_RADIUS = 1.2D;
    private static final double GEYSER_HEIGHT = 4.0D;
    private static final double GEYSER_SPACING = 2.4D;
    private static final double GEYSER_GAP_HALF = 2.2D;
    private static final float GEYSER_GAP_SEPARATION = 25.0F * Mth.DEG_TO_RAD;
    private static final float[] HEAD_YAW = {0.0F, 45.0F, -45.0F, 90.0F, -90.0F, 135.0F, -135.0F};

    private enum Action {
        NONE,
        ROAR,
        TAIL_WARN,
        TAIL_SWEEP,
        FLAME_SWEEP,
        SLAG_RAIN,
        GEYSER
    }

    private static final class Wave {
        private final Vec3 center;
        private final double maxRadius;
        private final int expandTicks;
        private final float damage;
        private final double knockback;
        private final Set<Integer> hit = new HashSet<>();
        private int age;
        private double radius;

        private Wave(Vec3 center, double maxRadius, int expandTicks, float damage, double knockback) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.expandTicks = expandTicks;
            this.damage = damage;
            this.knockback = knockback;
        }
    }

    private static final class Scorch {
        private final Vec3 pos;
        private int ticks;

        private Scorch(Vec3 pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    private static final class Slag {
        private final Vec3 pos;
        private final double radius;
        private final float damage;
        private int ticks;

        private Slag(Vec3 pos, int ticks, double radius, float damage) {
            this.pos = pos;
            this.ticks = ticks;
            this.radius = radius;
            this.damage = damage;
        }
    }

    private static final class GeyserRing {
        private final Vec3 center;
        private final double radius;
        private final List<Vec3> points;
        private final long eruptAt;
        private final float damage;
        private final Set<Integer> hit = new HashSet<>();

        private GeyserRing(Vec3 center, double radius, List<Vec3> points, long eruptAt, float damage) {
            this.center = center;
            this.radius = radius;
            this.points = points;
            this.eruptAt = eruptAt;
            this.damage = damage;
        }
    }

    private final Hydra hydra;
    private Action action = Action.NONE;
    private int actionTicks;
    private int actionDuration;
    private long clock;
    private int globalCooldown = 60;
    private int tailCooldown = 100;
    private int sweepCooldown = 120;
    private int slagCooldown = 200;
    private int geyserCooldown = 100;
    private int lastPhase;
    private boolean pendingRoar;
    private boolean pendingRevive;
    private int closeTicks;
    private long lastWaveClock = -1000L;
    private final int[] prevStates = {-1, -1, -1, -1, -1, -1, -1};
    private float tailStartYaw;
    private float tailSweep;
    private int tailTicks;
    private final Set<Integer> tailHits = new HashSet<>();
    private final boolean[] sweepHeads = new boolean[HEADS];
    private Vec3 sweepCenter = Vec3.ZERO;
    private float sweepStartYaw;
    private float sweepAngle;
    private double sweepDist;
    private double sweepY;
    private final boolean[] slagHeads = new boolean[HEADS];
    private int slagLeft;
    private int slagTotal;
    private int slagPerShot;
    private Vec3 slagTarget = Vec3.ZERO;
    private final boolean[] geyserHeads = new boolean[HEADS];
    private final List<Wave> waves = new ArrayList<>();
    private final List<Scorch> scorches = new ArrayList<>();
    private final List<Slag> slags = new ArrayList<>();
    private final List<GeyserRing> geysers = new ArrayList<>();
    private final Map<Integer, Long> collisionHits = new HashMap<>();

    public HydraBossController(Hydra hydra) {
        this.hydra = hydra;
    }

    public static boolean canHit(Hydra hydra, LivingEntity target) {
        if (target == hydra || !target.isAlive() || target instanceof Hydra || !target.isAttackable() || hydra.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    public static float attackOf(Hydra hydra) {
        AttributeInstance attack = hydra.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return (float) HydraAttributeEvents.DEFAULT_ATTACK;
        }
        return (float) attack.getValue();
    }

    private static boolean enabled() {
        return HydraConfig.ENABLED.get();
    }

    public boolean isBusy() {
        return enabled() && this.action != Action.NONE;
    }

    public float getDamageTakenMultiplier() {
        if (!enabled() || this.action != Action.ROAR) {
            return 1.0F;
        }
        return 1.0F - HydraConfig.ROAR_DAMAGE_REDUCTION.get().floatValue();
    }

    public int getPhase() {
        float ratio = this.hydra.getHealth() / this.hydra.getMaxHealth();
        if (ratio <= HydraConfig.PHASE3_THRESHOLD.get()) {
            return 3;
        }
        if (ratio <= HydraConfig.PHASE2_THRESHOLD.get()) {
            return 2;
        }
        return 1;
    }

    public int getSeverThreshold(int original) {
        if (!enabled()) {
            return original;
        }
        return Math.max(1, Math.round(this.hydra.getMaxHealth() * HydraConfig.SEVER_RATIO.get().floatValue()));
    }

    public int getRespawnTicks(int original) {
        if (!enabled()) {
            return original;
        }
        if (this.getPhase() >= 3) {
            return HydraConfig.PHASE3_RESPAWN_TICKS.get();
        }
        return HydraConfig.RESPAWN_TICKS.get();
    }

    public float getBodyDivisor(float original) {
        if (!enabled()) {
            return original;
        }
        return HydraConfig.BODY_DAMAGE_DIVISOR.get().floatValue();
    }

    public int getRegenDelay(int original) {
        if (!enabled()) {
            return original;
        }
        return HydraConfig.REGEN_DELAY_TICKS.get();
    }

    public int getRegenInterval(int original) {
        if (!enabled()) {
            return original;
        }
        return HydraConfig.REGEN_INTERVAL_TICKS.get();
    }

    public float getRegenAmount(float original) {
        if (!enabled()) {
            return original;
        }
        return HydraConfig.REGEN_AMOUNT.get().floatValue();
    }

    public float getBiteDamage(float original) {
        if (!enabled()) {
            return original;
        }
        return attackOf(this.hydra) * HydraConfig.BITE_MULTIPLIER.get().floatValue();
    }

    public void applyBiteKnockback(LivingEntity living) {
        double dx = living.getX() - this.hydra.getX();
        double dz = living.getZ() - this.hydra.getZ();
        if (dx * dx + dz * dz < 1.0E-4D) {
            Vec3 forward = NagaBossController.directionOf(this.hydra.yBodyRot);
            dx = forward.x;
            dz = forward.z;
        }
        living.knockback(HydraConfig.BITE_KNOCKBACK.get(), -dx, -dz);
    }

    public float getFlameDamage(int headNum, float original) {
        if (!enabled()) {
            return original;
        }
        float multiplier = HydraConfig.FLAME_MULTIPLIER.get().floatValue();
        if (headNum >= 0 && headNum < HEADS && this.sweepHeads[headNum]) {
            multiplier = HydraConfig.SWEEP_MULTIPLIER.get().floatValue();
        }
        return attackOf(this.hydra) * multiplier;
    }

    public int getFlameBurnSeconds(int original) {
        if (!enabled()) {
            return original;
        }
        return HydraConfig.FLAME_BURN_SECONDS.get();
    }

    public double getTrackingScale() {
        if (!enabled() || this.getPhase() < 3) {
            return 1.0D;
        }
        return HydraConfig.FLAME_PHASE3_TRACKING_SCALE.get();
    }

    public boolean consumeSlagShot(int headNum) {
        if (!enabled() || headNum < 0 || headNum >= HEADS || !this.slagHeads[headNum]) {
            return false;
        }
        int count = Math.min(this.slagPerShot, this.slagLeft);
        HydraHead head = this.hydra.hc[headNum].headEntity;
        for (int i = 0; i < count; i++) {
            this.launchSlag(head);
        }
        return true;
    }

    public void collide(List<Entity> entities, Entity part) {
        AABB box = part.getBoundingBox();
        double cx = (box.minX + box.maxX) * 0.5D;
        double cz = (box.minZ + box.maxZ) * 0.5D;
        int interval = HydraConfig.COLLISION_HIT_INTERVAL.get();
        float damage = attackOf(this.hydra) * HydraConfig.COLLISION_MULTIPLIER.get().floatValue();
        double knockback = HydraConfig.COLLISION_KNOCKBACK.get();
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living) || !canHit(this.hydra, living)) {
                continue;
            }
            double dx = living.getX() - cx;
            double dz = living.getZ() - cz;
            double distance = Math.max(Math.sqrt(dx * dx + dz * dz), 0.1D);
            dx /= distance;
            dz /= distance;
            Long last = this.collisionHits.get(living.getId());
            if (last == null || this.clock - last >= interval) {
                this.collisionHits.put(living.getId(), this.clock);
                if (damage > 0.0F && living.hurt(this.hydra.damageSources().mobAttack(this.hydra), damage)) {
                    living.knockback(knockback, -dx, -dz);
                    this.debug("Body collision hit {} for {}", living.getName().getString(), damage);
                    continue;
                }
            }
            double resist = 1.0D - Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
            if (resist <= 0.0D) {
                continue;
            }
            living.push(dx * 0.15D * resist, 0.0D, dz * 0.15D * resist);
            if (living instanceof Player) {
                living.hurtMarked = true;
            }
        }
    }

    public void save(CompoundTag tag) {
        tag.putInt("TwilightBossPlusHydraPhase", this.lastPhase);
    }

    public void load(CompoundTag tag) {
        this.lastPhase = tag.getInt("TwilightBossPlusHydraPhase");
    }

    public void onDeath() {
        this.action = Action.NONE;
        this.waves.clear();
        this.scorches.clear();
        this.slags.clear();
        this.geysers.clear();
        for (int i = 0; i < HEADS; i++) {
            this.sweepHeads[i] = false;
            this.slagHeads[i] = false;
            this.geyserHeads[i] = false;
        }
        this.debug("Died, cleaned up skills");
    }

    public void tick() {
        if (!enabled() || this.hydra.isDeadOrDying()) {
            return;
        }
        this.clock++;
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        if (this.tailCooldown > 0) {
            this.tailCooldown--;
        }
        if (this.sweepCooldown > 0) {
            this.sweepCooldown--;
        }
        if (this.slagCooldown > 0) {
            this.slagCooldown--;
        }
        if (this.geyserCooldown > 0) {
            this.geyserCooldown--;
        }

        int phase = this.getPhase();
        if (this.lastPhase == 0) {
            this.lastPhase = phase;
        }
        if (phase > this.lastPhase) {
            this.debug("Phase {} -> {}", this.lastPhase, phase);
            if (this.lastPhase < 2) {
                this.pendingRevive = true;
            }
            this.lastPhase = phase;
            this.pendingRoar = true;
        }

        this.tickWaves();
        this.tickScorches();
        this.tickSlags();
        this.tickGeysers();
        if (this.clock % 100L == 0L) {
            long expire = HydraConfig.COLLISION_HIT_INTERVAL.get() * 2L;
            this.collisionHits.entrySet().removeIf(entry -> this.clock - entry.getValue() > expire);
        }
        if (!HydraHeads.isAvailable()) {
            return;
        }
        this.tickHeadEvents();

        LivingEntity target = this.hydra.getTarget();
        if (target != null && (!canHit(this.hydra, target) || this.hydra.distanceToSqr(target) > TARGET_RANGE * TARGET_RANGE)) {
            target = null;
        }
        if (target != null) {
            this.slagTarget = target.position();
        }
        this.tickCloseTracking(target);

        if (this.action != Action.NONE) {
            this.actionTicks++;
        }
        if (this.action == Action.ROAR) {
            this.tickRoar();
        } else if (this.action == Action.TAIL_WARN) {
            this.tickTailWarn();
        } else if (this.action == Action.TAIL_SWEEP) {
            this.tickTailSweep();
        } else if (this.action == Action.FLAME_SWEEP) {
            this.tickFlameSweep();
        } else if (this.action == Action.SLAG_RAIN) {
            this.tickSlagRain();
        } else if (this.action == Action.GEYSER) {
            this.tickGeyserCast();
        }

        if (this.action == Action.NONE) {
            if (this.pendingRoar) {
                this.startRoar();
            } else if (target != null && this.globalCooldown <= 0) {
                this.chooseSkill(target, phase);
            }
        }
    }

    private void chooseSkill(LivingEntity target, int phase) {
        double distance = this.horizontalDistance(target.position());
        boolean sees = this.hydra.getSensing().hasLineOfSight(target);
        if (HydraConfig.TAIL_ENABLED.get() && this.tailCooldown <= 0 && this.closeTicks >= HydraConfig.TAIL_TRIGGER_TICKS.get()) {
            this.startTailWarn(target);
            return;
        }
        double geyserReach = GEYSER_FIRST_RADIUS + (HydraConfig.GEYSER_RINGS.get() - 1) * HydraConfig.GEYSER_RING_SPACING.get() + 2.0D;
        if (phase >= 3 && HydraConfig.GEYSER_ENABLED.get() && this.geyserCooldown <= 0 && distance < geyserReach) {
            if (this.startGeyser()) {
                return;
            }
            this.geyserCooldown = 40;
        }
        if (phase >= 2 && HydraConfig.SWEEP_ENABLED.get() && this.sweepCooldown <= 0 && distance < SWEEP_RANGE && sees) {
            if (this.startFlameSweep(target)) {
                return;
            }
            this.sweepCooldown = 40;
        }
        if (phase >= 2 && HydraConfig.SLAG_ENABLED.get() && this.slagCooldown <= 0 && distance >= SLAG_MIN_RANGE && distance <= SLAG_MAX_RANGE) {
            if (this.startSlagRain(target)) {
                return;
            }
            this.slagCooldown = 40;
        }
    }

    private void startAction(Action next, int duration) {
        this.action = next;
        this.actionTicks = 0;
        this.actionDuration = duration;
    }

    private void finishAction() {
        this.action = Action.NONE;
        this.actionTicks = 0;
        this.globalCooldown = GLOBAL_GAP + this.hydra.getRandom().nextInt(GLOBAL_GAP_RANDOM);
    }

    private double horizontalDistance(Vec3 pos) {
        double dx = pos.x - this.hydra.getX();
        double dz = pos.z - this.hydra.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private void setBodyYaw(float yaw) {
        this.hydra.setYRot(yaw);
        this.hydra.yBodyRot = yaw;
        this.hydra.yHeadRot = yaw;
    }

    private boolean isFree(int state) {
        return state == HydraHeads.IDLE || state == HydraHeads.ATTACK_COOLDOWN;
    }

    private void aimContainer(HydraHeadContainer container, double x, double y, double z) {
        HydraHeadContainerAccessor accessor = (HydraHeadContainerAccessor) container;
        accessor.twilightbossplus$setTargetX(x);
        accessor.twilightbossplus$setTargetY(y);
        accessor.twilightbossplus$setTargetZ(z);
    }

    private DamageSource fireSource() {
        return TFDamageTypes.getEntityDamageSource(this.hydra.level(), TFDamageTypes.HYDRA_FIRE, this.hydra);
    }

    private void tickHeadEvents() {
        for (int i = 0; i < HEADS; i++) {
            int state = HydraHeads.stateOf(this.hydra.hc[i].headEntity);
            if (state == HydraHeads.BORN && this.prevStates[i] != HydraHeads.BORN && this.prevStates[i] != -1) {
                this.debug("Head {} reborn", i);
                if (HydraConfig.WAVE_ENABLED.get() && this.clock - this.lastWaveClock >= WAVE_GAP) {
                    this.spawnWave();
                }
            }
            this.prevStates[i] = state;
        }
    }

    private void tickCloseTracking(@Nullable LivingEntity target) {
        if (target == null || !HydraConfig.TAIL_ENABLED.get() || this.action != Action.NONE) {
            this.closeTicks = Math.max(0, this.closeTicks - 1);
            return;
        }
        double distance = this.horizontalDistance(target.position());
        float relative = Mth.wrapDegrees(NagaBossController.yawTo(this.hydra.position(), target.position()) - this.hydra.yBodyRot);
        boolean behind = Math.abs(relative) >= 180.0F - REAR_HALF_ANGLE && distance < HydraConfig.TAIL_RANGE.get();
        boolean close = distance < CLOSE_RANGE;
        if (behind || close) {
            this.closeTicks++;
        } else {
            this.closeTicks = Math.max(0, this.closeTicks - 1);
        }
    }

    private void spawnWave() {
        Vec3 center = this.hydra.position();
        double radius = HydraConfig.WAVE_RADIUS.get();
        int expand = HydraConfig.WAVE_EXPAND_TICKS.get();
        float damage = attackOf(this.hydra) * HydraConfig.WAVE_MULTIPLIER.get().floatValue();
        this.waves.add(new Wave(center, radius, expand, damage, HydraConfig.WAVE_KNOCKBACK.get()));
        this.lastWaveClock = this.clock;
        ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.WAVE, this.hydra.getId(),
            (float) center.x, (float) center.y, (float) center.z, (float) radius, expand));
        this.hydra.playSound(SoundEvents.BLAZE_SHOOT, 2.5F, 0.5F);
        this.debug("Heat wave radius {}, damage {}", radius, damage);
    }

    private void tickWaves() {
        if (this.waves.isEmpty()) {
            return;
        }
        Level level = this.hydra.level();
        Iterator<Wave> iterator = this.waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.age++;
            double inner = wave.radius;
            wave.radius = wave.maxRadius * Math.min(1.0D, (double) wave.age / wave.expandTicks);
            double outer = wave.radius;
            Vec3 c = wave.center;
            AABB area = new AABB(c.x - outer - 1.0D, c.y - 1.0D, c.z - outer - 1.0D, c.x + outer + 1.0D, c.y + WAVE_HEIGHT + 1.0D, c.z + outer + 1.0D);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!canHit(this.hydra, living) || wave.hit.contains(living.getId())) {
                    continue;
                }
                double dx = living.getX() - c.x;
                double dz = living.getZ() - c.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < inner - 0.6D || distance > outer + 0.6D) {
                    continue;
                }
                if (living.getY() > c.y + WAVE_HEIGHT || living.getY() + living.getBbHeight() < c.y - 1.0D) {
                    continue;
                }
                wave.hit.add(living.getId());
                if (living.hurt(this.fireSource(), wave.damage) && distance > 0.01D) {
                    living.knockback(wave.knockback, -dx, -dz);
                    this.debug("Heat wave hit {} for {}", living.getName().getString(), wave.damage);
                }
            }
            if (wave.age >= wave.expandTicks) {
                iterator.remove();
            }
        }
    }

    private void startRoar() {
        this.pendingRoar = false;
        if (this.pendingRevive) {
            this.pendingRevive = false;
            this.reviveHeads();
        }
        float bodyYaw = this.hydra.yBodyRot;
        for (int i = 0; i < HEADS; i++) {
            HydraHeadContainer container = this.hydra.hc[i];
            int state = HydraHeads.stateOf(container.headEntity);
            if (!container.isActive() || state == HydraHeads.BORN || state == HydraHeads.ROAR_START || state == HydraHeads.ROAR_RAWR) {
                continue;
            }
            container.isSecondaryAttacking = false;
            Vec3 look = this.hydra.position().add(NagaBossController.directionOf(bodyYaw + HEAD_YAW[i]).scale(8.0D));
            this.aimContainer(container, look.x, this.hydra.getY() + 9.0D, look.z);
            HydraHeads.force(container, HydraHeads.ROAR_START);
        }
        this.startAction(Action.ROAR, ROAR_TICKS);
        if (HydraConfig.WAVE_ENABLED.get()) {
            this.spawnWave();
        }
        this.debug("Phase roar started");
    }

    private void reviveHeads() {
        int alive = 0;
        for (int i = 0; i < HEADS; i++) {
            HydraHeadContainer container = this.hydra.hc[i];
            int state = HydraHeads.stateOf(container.headEntity);
            if (container.isActive()) {
                alive++;
            } else if (state == HydraHeads.DYING || (state == HydraHeads.DEAD && !container.canRespawn())) {
                container.setRespawnCounter(1);
                alive++;
            }
        }
        int minimum = HydraConfig.PHASE2_MIN_HEADS.get();
        for (int i = 0; i < HEADS && alive < minimum; i++) {
            HydraHeadContainer container = this.hydra.hc[i];
            if (container.canRespawn()) {
                container.setRespawnCounter(1);
                alive++;
            }
        }
        this.debug("Revived heads, {} heads alive or regrowing", alive);
    }

    private void tickRoar() {
        if (this.actionTicks >= this.actionDuration) {
            this.finishAction();
        }
    }

    private void startTailWarn(LivingEntity target) {
        float bodyYaw = this.hydra.yBodyRot;
        float relative = Mth.wrapDegrees(NagaBossController.yawTo(this.hydra.position(), target.position()) - bodyYaw);
        float delta = Mth.wrapDegrees(relative - 180.0F);
        if (Math.abs(delta) < 1.0F) {
            delta = 1.0F;
            if (this.hydra.getRandom().nextBoolean()) {
                delta = -1.0F;
            }
        }
        float sweep = delta + Math.signum(delta) * TAIL_OVERSHOOT;
        sweep = Mth.clamp(sweep, -TAIL_MAX_SWEEP, TAIL_MAX_SWEEP);
        this.tailStartYaw = bodyYaw;
        this.tailSweep = sweep;
        this.tailTicks = Math.max(8, Mth.ceil(Math.abs(sweep) / TAIL_DEGREES_PER_TICK));
        this.tailHits.clear();
        this.closeTicks = 0;
        int warn = HydraConfig.TAIL_WARNING_TICKS.get();
        this.startAction(Action.TAIL_WARN, warn);
        ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.TAIL_WARN, this.hydra.getId(),
            (float) this.hydra.getX(), (float) this.hydra.getY(), (float) this.hydra.getZ(), bodyYaw + 180.0F, sweep,
            (float) TAIL_INNER, (float) TAIL_OUTER, warn, this.tailTicks));
        this.hydra.playSound(TFSounds.HYDRA_GROWL.get(), 2.5F, 0.6F);
        this.debug("Tail sweep warning, sweep {} degrees over {} ticks", sweep, this.tailTicks);
    }

    private void tickTailWarn() {
        this.setBodyYaw(this.tailStartYaw);
        if (this.actionTicks >= this.actionDuration) {
            this.startAction(Action.TAIL_SWEEP, this.tailTicks);
            this.hydra.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 3.0F, 0.4F);
        }
    }

    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    private void tickTailSweep() {
        float prevProgress = Mth.clamp((float) (this.actionTicks - 1) / this.tailTicks, 0.0F, 1.0F);
        float progress = Mth.clamp((float) this.actionTicks / this.tailTicks, 0.0F, 1.0F);
        float prevYaw = this.tailStartYaw + this.tailSweep * ease(prevProgress);
        float yaw = this.tailStartYaw + this.tailSweep * ease(progress);
        this.setBodyYaw(yaw);
        float span = yaw - prevYaw;
        float from = prevYaw + 180.0F;
        float sign = Math.signum(this.tailSweep);
        Vec3 c = this.hydra.position();
        AABB area = new AABB(c.x - TAIL_OUTER - 1.0D, c.y - 1.0D, c.z - TAIL_OUTER - 1.0D, c.x + TAIL_OUTER + 1.0D, c.y + TAIL_HEIGHT + 1.0D, c.z + TAIL_OUTER + 1.0D);
        float damage = attackOf(this.hydra) * HydraConfig.TAIL_MULTIPLIER.get().floatValue();
        double knockback = HydraConfig.TAIL_KNOCKBACK.get();
        for (LivingEntity living : this.hydra.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!canHit(this.hydra, living) || this.tailHits.contains(living.getId())) {
                continue;
            }
            double dx = living.getX() - c.x;
            double dz = living.getZ() - c.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < TAIL_INNER - 0.5D || distance > TAIL_OUTER + 0.5D) {
                continue;
            }
            if (living.getY() > c.y + TAIL_HEIGHT || living.getY() + living.getBbHeight() < c.y - 0.5D) {
                continue;
            }
            float angle = NagaBossController.yawTo(c, living.position());
            float relative = Mth.wrapDegrees(angle - from);
            float tolerance = (float) Math.toDegrees(Math.atan2(TAIL_HALF_WIDTH, Math.max(distance, 1.0D)));
            boolean inside;
            if (span >= 0.0F) {
                inside = relative >= -tolerance && relative <= span + tolerance;
            } else {
                inside = relative <= tolerance && relative >= span - tolerance;
            }
            if (!inside) {
                continue;
            }
            this.tailHits.add(living.getId());
            if (living.hurt(this.hydra.damageSources().mobAttack(this.hydra), damage)) {
                Vec3 push = NagaBossController.directionOf(angle + sign * 90.0F).add(NagaBossController.directionOf(angle).scale(0.5D));
                living.knockback(knockback, -push.x, -push.z);
                this.debug("Tail sweep hit {} for {}", living.getName().getString(), damage);
            }
        }
        if (this.actionTicks >= this.tailTicks) {
            this.tailCooldown = HydraConfig.TAIL_COOLDOWN_TICKS.get();
            this.finishAction();
        }
    }

    private boolean startFlameSweep(LivingEntity target) {
        List<Integer> heads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            HydraHeadContainer container = this.hydra.hc[i];
            if (container.isActive() && this.isFree(HydraHeads.stateOf(container.headEntity))) {
                heads.add(i);
            }
        }
        if (heads.size() < 2) {
            this.debug("Flame sweep skipped, only {} free front heads", heads.size());
            return false;
        }
        float angle = HydraConfig.SWEEP_ANGLE.get().floatValue();
        float direction = 1.0F;
        if (this.hydra.getRandom().nextBoolean()) {
            direction = -1.0F;
        }
        float centerYaw = NagaBossController.yawTo(this.hydra.position(), target.position());
        this.sweepStartYaw = centerYaw - direction * angle * 0.5F;
        this.sweepAngle = direction * angle;
        this.sweepDist = Mth.clamp(this.horizontalDistance(target.position()), SWEEP_MIN_DIST, SWEEP_MAX_DIST);
        this.sweepY = target.getY();
        this.sweepCenter = this.hydra.position();
        for (int index : heads) {
            HydraHeadContainer container = this.hydra.hc[index];
            this.sweepHeads[index] = true;
            container.isSecondaryAttacking = false;
            container.setTargetEntity(null);
            this.aimSweepHead(index, 0.0F);
            HydraHeads.force(container, HydraHeads.FLAME_BEGINNING);
        }
        int sweepTicks = HydraConfig.SWEEP_TICKS.get();
        this.startAction(Action.FLAME_SWEEP, SWEEP_WINDUP + sweepTicks + SWEEP_RECOVER);
        ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.SWEEP_WARN, this.hydra.getId(),
            (float) this.sweepCenter.x, (float) this.sweepCenter.y, (float) this.sweepCenter.z,
            this.sweepStartYaw - direction * SWEEP_SPREAD, this.sweepAngle + direction * SWEEP_SPREAD * 2.0F,
            4.0F, (float) this.sweepDist + 3.0F, SWEEP_WINDUP, sweepTicks, (float) this.sweepY));
        this.hydra.playSound(TFSounds.HYDRA_WARN.get(), 2.5F, 0.8F);
        this.debug("Flame sweep with {} heads, angle {}, distance {}", heads.size(), this.sweepAngle, this.sweepDist);
        return true;
    }

    private void aimSweepHead(int index, float progress) {
        float yaw = this.sweepStartYaw + this.sweepAngle * progress + SWEEP_OFFSETS[index];
        Vec3 point = this.sweepCenter.add(NagaBossController.directionOf(yaw).scale(this.sweepDist));
        this.aimContainer(this.hydra.hc[index], point.x, this.sweepY, point.z);
    }

    private void tickFlameSweep() {
        int sweepTicks = HydraConfig.SWEEP_TICKS.get();
        int flameTick = this.actionTicks - SWEEP_WINDUP;
        float progress = Mth.clamp((float) flameTick / sweepTicks, 0.0F, 1.0F);
        boolean any = false;
        for (int i = 0; i < 3; i++) {
            if (!this.sweepHeads[i]) {
                continue;
            }
            HydraHeadContainer container = this.hydra.hc[i];
            int state = HydraHeads.stateOf(container.headEntity);
            if (!container.isActive() || (flameTick > 0 && state != HydraHeads.FLAME_BEGINNING && state != HydraHeads.FLAMING)) {
                this.sweepHeads[i] = false;
                continue;
            }
            any = true;
            this.aimSweepHead(i, progress);
            if (state == HydraHeads.FLAMING) {
                if (flameTick >= sweepTicks) {
                    container.endCurrentAction();
                } else if (this.actionTicks % SCORCH_INTERVAL == 0) {
                    this.scorchFromHead(container.headEntity);
                }
            }
        }
        if (this.actionTicks >= this.actionDuration || (!any && flameTick > 0)) {
            for (int i = 0; i < HEADS; i++) {
                this.sweepHeads[i] = false;
            }
            this.sweepCooldown = HydraConfig.SWEEP_COOLDOWN_TICKS.get();
            this.finishAction();
            this.debug("Flame sweep finished");
        }
    }

    private void scorchFromHead(HydraHead head) {
        int ticks = HydraConfig.SCORCH_TICKS.get();
        if (ticks <= 0) {
            return;
        }
        Level level = this.hydra.level();
        Vec3 source = new Vec3(head.getX(), head.getY() + 1.0D, head.getZ());
        Vec3 end = source.add(head.getLookAngle().scale(30.0D));
        BlockHitResult hit = level.clip(new ClipContext(source, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, head));
        if (hit.getType() == HitResult.Type.MISS) {
            return;
        }
        Vec3 location = hit.getLocation();
        double ground = NagaBossController.findGround(level, location.x, location.y + 0.5D, location.z);
        if (Double.isNaN(ground)) {
            return;
        }
        Vec3 pos = new Vec3(location.x, ground, location.z);
        for (Scorch scorch : this.scorches) {
            if (Mth.square(scorch.pos.x - pos.x) + Mth.square(scorch.pos.z - pos.z) < SCORCH_SPACING * SCORCH_SPACING && Math.abs(scorch.pos.y - pos.y) < 1.0D) {
                scorch.ticks = Math.max(scorch.ticks, ticks);
                return;
            }
        }
        if (this.scorches.size() >= MAX_SCORCHES) {
            return;
        }
        this.scorches.add(new Scorch(pos, ticks));
        ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.SCORCH, this.hydra.getId(),
            (float) pos.x, (float) pos.y, (float) pos.z, (float) SCORCH_RADIUS, ticks));
    }

    private void tickScorches() {
        if (this.scorches.isEmpty()) {
            return;
        }
        boolean damageTick = this.clock % SCORCH_DAMAGE_INTERVAL == 0L;
        Set<Integer> burned = new HashSet<>();
        float damage = attackOf(this.hydra) * HydraConfig.SCORCH_MULTIPLIER.get().floatValue();
        Level level = this.hydra.level();
        Iterator<Scorch> iterator = this.scorches.iterator();
        while (iterator.hasNext()) {
            Scorch scorch = iterator.next();
            scorch.ticks--;
            if (scorch.ticks <= 0) {
                iterator.remove();
                continue;
            }
            if (!damageTick) {
                continue;
            }
            Vec3 p = scorch.pos;
            AABB area = new AABB(p.x - SCORCH_RADIUS, p.y - 0.3D, p.z - SCORCH_RADIUS, p.x + SCORCH_RADIUS, p.y + 1.0D, p.z + SCORCH_RADIUS);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!canHit(this.hydra, living) || burned.contains(living.getId())) {
                    continue;
                }
                if (Mth.square(living.getX() - p.x) + Mth.square(living.getZ() - p.z) > SCORCH_RADIUS * SCORCH_RADIUS) {
                    continue;
                }
                burned.add(living.getId());
                if (living.hurt(this.fireSource(), damage)) {
                    living.setSecondsOnFire(SCORCH_BURN);
                }
            }
        }
    }

    private boolean startSlagRain(LivingEntity target) {
        List<Integer> heads = new ArrayList<>();
        int[] order = {3, 4, 5, 6, 0, 1, 2};
        for (int index : order) {
            if (heads.size() >= SLAG_HEADS) {
                break;
            }
            HydraHeadContainer container = this.hydra.hc[index];
            if (container.isActive() && this.isFree(HydraHeads.stateOf(container.headEntity))) {
                heads.add(index);
            }
        }
        if (heads.isEmpty()) {
            this.debug("Slag rain skipped, no free heads");
            return false;
        }
        this.slagTotal = HydraConfig.SLAG_COUNT.get();
        this.slagLeft = this.slagTotal;
        this.slagPerShot = Mth.ceil((float) this.slagTotal / (heads.size() * SLAG_SHOTS_PER_HEAD));
        this.slagTarget = target.position();
        for (int index : heads) {
            HydraHeadContainer container = this.hydra.hc[index];
            this.slagHeads[index] = true;
            container.isSecondaryAttacking = false;
            container.setTargetEntity(target);
            HydraHeads.force(container, HydraHeads.MORTAR_BEGINNING);
        }
        this.startAction(Action.SLAG_RAIN, SLAG_TIMEOUT);
        this.hydra.playSound(TFSounds.HYDRA_GROWL.get(), 2.5F, 1.2F);
        this.debug("Slag rain with {} heads, {} slag", heads.size(), this.slagTotal);
        return true;
    }

    private void launchSlag(HydraHead head) {
        if (this.slagLeft <= 0) {
            return;
        }
        Level level = this.hydra.level();
        RandomSource random = this.hydra.getRandom();
        Vec3 look = head.getLookAngle();
        Vec3 from = new Vec3(head.getX() + look.x * 3.5D, head.getY() + 1.0D + look.y * 3.5D, head.getZ() + look.z * 3.5D);
        Vec3 base = this.slagTarget;
        LivingEntity target = this.hydra.getTarget();
        if (target != null && target.isAlive()) {
            base = target.position();
        }
        Vec3 aim = base;
        if (this.slagLeft != this.slagTotal) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = SLAG_SPREAD_MIN + random.nextDouble() * (SLAG_SPREAD_MAX - SLAG_SPREAD_MIN);
            double x = base.x + Math.cos(angle) * distance;
            double z = base.z + Math.sin(angle) * distance;
            double ground = NagaBossController.findGround(level, x, base.y, z);
            double y = base.y;
            if (!Double.isNaN(ground)) {
                y = ground;
            }
            aim = new Vec3(x, y, z);
        }
        this.slagLeft--;
        int warn = HydraConfig.SLAG_WARNING_TICKS.get();
        double radius = HydraConfig.SLAG_RADIUS.get();
        float damage = attackOf(this.hydra) * HydraConfig.SLAG_MULTIPLIER.get().floatValue();
        this.slags.add(new Slag(aim, warn, radius, damage));
        ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.SLAG, this.hydra.getId(),
            (float) from.x, (float) from.y, (float) from.z, (float) aim.x, (float) aim.y, (float) aim.z, warn, (float) radius));
    }

    private void tickSlagRain() {
        boolean any = false;
        for (int i = 0; i < HEADS; i++) {
            if (!this.slagHeads[i]) {
                continue;
            }
            HydraHeadContainer container = this.hydra.hc[i];
            int state = HydraHeads.stateOf(container.headEntity);
            if (!container.isActive() || (this.actionTicks > 3 && !HydraHeads.isMortarState(state))) {
                this.slagHeads[i] = false;
                continue;
            }
            any = true;
        }
        if ((!any && this.actionTicks > 3) || this.actionTicks >= this.actionDuration) {
            for (int i = 0; i < HEADS; i++) {
                this.slagHeads[i] = false;
            }
            this.slagCooldown = HydraConfig.SLAG_COOLDOWN_TICKS.get();
            this.finishAction();
            this.debug("Slag rain finished, {} slag unused", this.slagLeft);
        }
    }

    private void tickSlags() {
        if (this.slags.isEmpty()) {
            return;
        }
        Level level = this.hydra.level();
        Iterator<Slag> iterator = this.slags.iterator();
        while (iterator.hasNext()) {
            Slag slag = iterator.next();
            slag.ticks--;
            if (slag.ticks > 0) {
                continue;
            }
            iterator.remove();
            Vec3 p = slag.pos;
            double r = slag.radius;
            AABB area = new AABB(p.x - r - 1.0D, p.y - 1.0D, p.z - r - 1.0D, p.x + r + 1.0D, p.y + r + 1.0D, p.z + r + 1.0D);
            DamageSource source = TFDamageTypes.getEntityDamageSource(level, TFDamageTypes.HYDRA_MORTAR, this.hydra);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!canHit(this.hydra, living)) {
                    continue;
                }
                double reach = r + living.getBbWidth() * 0.5D;
                if (Mth.square(living.getX() - p.x) + Mth.square(living.getZ() - p.z) > reach * reach) {
                    continue;
                }
                if (living.hurt(source, slag.damage)) {
                    living.setSecondsOnFire(SLAG_BURN);
                    this.debug("Slag hit {} for {}", living.getName().getString(), slag.damage);
                }
            }
            level.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.5F);
            level.playSound(null, p.x, p.y, p.z, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.5F, 0.8F);
            ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.SLAG_IMPACT, this.hydra.getId(),
                (float) p.x, (float) p.y, (float) p.z, (float) r));
        }
    }

    private boolean startGeyser() {
        Level level = this.hydra.level();
        RandomSource random = this.hydra.getRandom();
        Vec3 center = this.hydra.position();
        int rings = HydraConfig.GEYSER_RINGS.get();
        double spacing = HydraConfig.GEYSER_RING_SPACING.get();
        int warn = HydraConfig.GEYSER_WARNING_TICKS.get();
        float damage = attackOf(this.hydra) * HydraConfig.GEYSER_MULTIPLIER.get().floatValue();
        List<Float> previousGaps = new ArrayList<>();
        int total = 0;
        for (int k = 0; k < rings; k++) {
            double radius = GEYSER_FIRST_RADIUS + k * spacing;
            int count = Math.max(8, (int) Math.round(Math.PI * 2.0D * radius / GEYSER_SPACING));
            int gapCount = 2 + random.nextInt(2);
            List<Float> gaps = new ArrayList<>();
            float minGapDistance = Mth.TWO_PI / (gapCount * 2);
            for (int attempt = 0; attempt < 40 && gaps.size() < gapCount; attempt++) {
                float candidate = random.nextFloat() * Mth.TWO_PI;
                boolean ok = true;
                for (float gap : previousGaps) {
                    if (Math.abs(Mth.wrapDegrees((candidate - gap) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD < GEYSER_GAP_SEPARATION) {
                        ok = false;
                    }
                }
                for (float gap : gaps) {
                    if (Math.abs(Mth.wrapDegrees((candidate - gap) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD < minGapDistance) {
                        ok = false;
                    }
                }
                if (ok) {
                    gaps.add(candidate);
                }
            }
            double gapHalf = GEYSER_GAP_HALF / radius;
            float offset = random.nextFloat() * Mth.TWO_PI;
            List<Vec3> points = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                float angle = offset + Mth.TWO_PI * j / count;
                boolean inGap = false;
                for (float gap : gaps) {
                    if (Math.abs(Mth.wrapDegrees((angle - gap) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD < gapHalf) {
                        inGap = true;
                    }
                }
                if (inGap) {
                    continue;
                }
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;
                double ground = NagaBossController.findGround(level, x, center.y, z);
                if (Double.isNaN(ground) || Math.abs(ground - center.y) > 4.0D) {
                    continue;
                }
                points.add(new Vec3(x, ground, z));
            }
            previousGaps = gaps;
            if (points.isEmpty()) {
                continue;
            }
            int delay = GEYSER_ROAR + k * GEYSER_RING_INTERVAL;
            this.geysers.add(new GeyserRing(center, radius, points, this.clock + delay, damage));
            total += points.size();
            float[] data = new float[5 + points.size() * 3];
            data[0] = delay;
            data[1] = warn;
            data[2] = (float) GEYSER_COLUMN_RADIUS;
            data[3] = (float) GEYSER_HEIGHT;
            data[4] = GEYSER_DURATION;
            for (int j = 0; j < points.size(); j++) {
                Vec3 p = points.get(j);
                data[5 + j * 3] = (float) p.x;
                data[6 + j * 3] = (float) p.y;
                data[7 + j * 3] = (float) p.z;
            }
            ModNetwork.sendTracking(this.hydra, new HydraFxPacket(HydraFxPacket.GEYSER, this.hydra.getId(), data));
        }
        if (total == 0) {
            this.debug("Lava geyser found no valid ground");
            return false;
        }
        float bodyYaw = this.hydra.yBodyRot;
        for (int i = 0; i < HEADS; i++) {
            HydraHeadContainer container = this.hydra.hc[i];
            int state = HydraHeads.stateOf(container.headEntity);
            if (!container.isActive() || state == HydraHeads.BORN || state == HydraHeads.ROAR_START || state == HydraHeads.ROAR_RAWR) {
                continue;
            }
            this.geyserHeads[i] = true;
            container.isSecondaryAttacking = false;
            container.setTargetEntity(null);
            Vec3 point = center.add(NagaBossController.directionOf(bodyYaw + HEAD_YAW[i]).scale(GEYSER_FIRST_RADIUS));
            this.aimContainer(container, point.x, center.y - 1.0D, point.z);
            HydraHeads.force(container, HydraHeads.FLAME_BEGINNING);
        }
        this.startAction(Action.GEYSER, GEYSER_ROAR + (rings - 1) * GEYSER_RING_INTERVAL + GEYSER_DURATION);
        this.hydra.playSound(TFSounds.HYDRA_ROAR.get(), 3.0F, 0.5F);
        this.debug("Lava geyser with {} rings, {} columns", rings, total);
        return true;
    }

    private void tickGeyserCast() {
        for (int i = 0; i < HEADS; i++) {
            if (!this.geyserHeads[i]) {
                continue;
            }
            HydraHeadContainer container = this.hydra.hc[i];
            int state = HydraHeads.stateOf(container.headEntity);
            if (!container.isActive()) {
                this.geyserHeads[i] = false;
                continue;
            }
            if (this.actionTicks >= GEYSER_ROAR || state == HydraHeads.FLAMING) {
                if (state == HydraHeads.FLAME_BEGINNING || state == HydraHeads.FLAMING) {
                    HydraHeads.force(container, HydraHeads.IDLE);
                }
                this.geyserHeads[i] = false;
            }
        }
        if (this.actionTicks >= this.actionDuration) {
            for (int i = 0; i < HEADS; i++) {
                this.geyserHeads[i] = false;
            }
            this.geyserCooldown = HydraConfig.GEYSER_COOLDOWN_TICKS.get();
            this.finishAction();
        }
    }

    private void tickGeysers() {
        if (this.geysers.isEmpty()) {
            return;
        }
        Level level = this.hydra.level();
        Iterator<GeyserRing> iterator = this.geysers.iterator();
        while (iterator.hasNext()) {
            GeyserRing ring = iterator.next();
            if (this.clock < ring.eruptAt) {
                continue;
            }
            if (this.clock >= ring.eruptAt + GEYSER_DURATION) {
                iterator.remove();
                continue;
            }
            if (this.clock == ring.eruptAt) {
                int step = Math.max(1, ring.points.size() / 4);
                for (int j = 0; j < ring.points.size(); j += step) {
                    Vec3 p = ring.points.get(j);
                    level.playSound(null, p.x, p.y, p.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.5F);
                }
                level.playSound(null, ring.center.x, ring.center.y, ring.center.z, SoundEvents.LAVA_AMBIENT, SoundSource.HOSTILE, 2.5F, 0.7F);
            }
            Vec3 c = ring.center;
            double reach = ring.radius + GEYSER_COLUMN_RADIUS + 1.0D;
            AABB area = new AABB(c.x - reach, c.y - 5.0D, c.z - reach, c.x + reach, c.y + GEYSER_HEIGHT + 5.0D, c.z + reach);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!canHit(this.hydra, living) || ring.hit.contains(living.getId())) {
                    continue;
                }
                double size = GEYSER_COLUMN_RADIUS + living.getBbWidth() * 0.5D;
                for (Vec3 p : ring.points) {
                    if (Mth.square(living.getX() - p.x) + Mth.square(living.getZ() - p.z) > size * size) {
                        continue;
                    }
                    if (living.getY() > p.y + GEYSER_HEIGHT || living.getY() + living.getBbHeight() < p.y - 0.5D) {
                        continue;
                    }
                    ring.hit.add(living.getId());
                    if (living.hurt(this.fireSource(), ring.damage)) {
                        living.setSecondsOnFire(HydraConfig.GEYSER_BURN_SECONDS.get());
                        this.debug("Lava geyser hit {} for {}", living.getName().getString(), ring.damage);
                    }
                    break;
                }
            }
        }
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Hydra " + this.hydra.getId() + "] " + message, args);
        }
    }
}
