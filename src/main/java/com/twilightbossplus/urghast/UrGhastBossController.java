package com.twilightbossplus.urghast;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.entity.PrimalTear;
import com.twilightbossplus.mixin.UrGhastAccessor;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.network.UrGhastFxPacket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;
import twilightforest.entity.monster.CarminiteGhastling;
import twilightforest.entity.projectile.UrGhastFireball;
import twilightforest.init.TFDamageTypes;
import twilightforest.init.TFEntities;
import twilightforest.init.TFSounds;

public class UrGhastBossController {
    private static final UUID GRIEF_UUID = UUID.fromString("2c7e9f14-8a3b-4d61-b5f0-7e1a3c9d2b48");
    private static final String PHASE_TAG = "TwilightBossPlusUrGhastPhase";
    private static final int SKILL_GAP = 60;
    private static final int POOL_INTERVAL = 20;
    private static final double RAIN_DEPTH = 40.0D;
    private static final double RAIN_SEEK_RANGE = 32.0D;
    private static final double SHOT_DISTANCE = 8.5D;

    private final UrGhast boss;
    private boolean initialized;
    private int phase = 1;
    private int tantrumTicks;
    private boolean rainActive;
    private double rainOffsetX;
    private double rainOffsetZ;
    private int gazeCooldown = 100;
    private int gazeTicks;
    private final List<LivingEntity> gazeTargets = new ArrayList<>();
    private int broodCooldown = 200;
    private int broodTicks;
    private final List<CarminiteGhastling> recalled = new ArrayList<>();
    private int stacks;
    private int stackTimer;
    private int tearCooldown = 100;
    private int tearCharge;
    private int skillGap;
    private final List<Pool> pools = new ArrayList<>();

    public UrGhastBossController(UrGhast boss) {
        this.boss = boss;
    }

    public void tick() {
        if (!UrGhastConfig.ENABLED.get() || !(this.boss.level() instanceof ServerLevel level) || !this.boss.isAlive()) {
            return;
        }
        this.updatePhase();
        this.tickStacks();
        this.tickPools(level);
        this.tickBrood();
        if (this.boss.isInTantrum()) {
            this.cancelCasts();
            this.tickTantrum(level);
        } else {
            if (this.rainActive) {
                this.endRain();
            }
            this.tantrumTicks = 0;
            this.tickSkills(level);
        }
    }

    public boolean isCasting() {
        return this.gazeTicks > 0 || this.tearCharge > 0;
    }

    public int getPhase() {
        return this.phase;
    }

    private float attack() {
        return (float) this.boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[UrGhast " + this.boss.getId() + "] " + message, args);
        }
    }

    private void updatePhase() {
        float ratio = this.boss.getHealth() / this.boss.getMaxHealth();
        int target = 1;
        if (ratio <= UrGhastConfig.PHASE3_THRESHOLD.get()) {
            target = 3;
        } else if (ratio <= UrGhastConfig.PHASE2_THRESHOLD.get()) {
            target = 2;
        }
        if (!this.initialized) {
            this.initialized = true;
            this.phase = Math.max(this.phase, target);
            return;
        }
        if (target <= this.phase) {
            return;
        }
        this.phase = target;
        this.debug("entered phase {} at health ratio {}", target, ratio);
        if (UrGhastConfig.PHASE_TANTRUM.get() && !this.boss.isInTantrum()) {
            this.cancelCasts();
            ((UrGhastAccessor) this.boss).twilightbossplus$startTantrum();
            this.debug("phase change forced a tantrum");
        }
    }

    private void tickTantrum(ServerLevel level) {
        this.tantrumTicks++;
        int limit = UrGhastConfig.TANTRUM_NO_TRAP_TICKS.get();
        if (limit > 0 && this.boss.getTrapLocations() != null && this.boss.getTrapLocations().isEmpty() && this.tantrumTicks >= limit) {
            this.debug("no usable trap, tantrum auto-ended after {} ticks", this.tantrumTicks);
            this.boss.setInTantrum(false);
            this.endRain();
            this.tantrumTicks = 0;
            return;
        }
        if (!this.rainActive) {
            this.rainActive = true;
            this.rainOffsetX = 0.0D;
            this.rainOffsetZ = 0.0D;
        }
        double radius = this.rainRadius();
        double speed = UrGhastConfig.RAIN_DRIFT_SPEED.get();
        double goalX = 0.0D;
        double goalZ = 0.0D;
        if (this.phase >= 2 && speed > 0.0D) {
            Player seek = this.nearestPlayerBelow(level, RAIN_SEEK_RANGE);
            if (seek != null) {
                goalX = seek.getX() - this.boss.getX();
                goalZ = seek.getZ() - this.boss.getZ();
            }
            double dx = goalX - this.rainOffsetX;
            double dz = goalZ - this.rainOffsetZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > speed) {
                this.rainOffsetX += dx / dist * speed;
                this.rainOffsetZ += dz / dist * speed;
            } else {
                this.rainOffsetX = goalX;
                this.rainOffsetZ = goalZ;
            }
            double max = UrGhastConfig.RAIN_DRIFT_MAX.get();
            double offset = Math.sqrt(this.rainOffsetX * this.rainOffsetX + this.rainOffsetZ * this.rainOffsetZ);
            if (offset > max && offset > 1.0E-4D) {
                this.rainOffsetX = this.rainOffsetX / offset * max;
                this.rainOffsetZ = this.rainOffsetZ / offset * max;
            }
        } else {
            this.rainOffsetX = 0.0D;
            this.rainOffsetZ = 0.0D;
        }
        double cx = this.boss.getX() + this.rainOffsetX;
        double cz = this.boss.getZ() + this.rainOffsetZ;
        if (this.boss.tickCount % 5 == 0) {
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.RAIN, this.boss.getId(), (float) cx, (float) cz, (float) radius));
        }
        if (this.boss.tickCount % 10 != 0) {
            return;
        }
        double top = this.boss.getY() + this.boss.getBbHeight() * 0.5D;
        AABB zone = new AABB(cx - radius, this.boss.getY() - RAIN_DEPTH, cz - radius, cx + radius, top, cz + radius);
        float damage = (float) (this.attack() * UrGhastConfig.RAIN_MULTIPLIER.get());
        int slow = UrGhastConfig.RAIN_SLOW_TICKS.get();
        for (Player player : level.getEntitiesOfClass(Player.class, zone)) {
            double dx = player.getX() - cx;
            double dz = player.getZ() - cz;
            if (dx * dx + dz * dz > radius * radius || player.isSpectator() || player.isCreative() || !level.canSeeSkyFromBelowWater(player.blockPosition())) {
                continue;
            }
            player.hurt(TFDamageTypes.getEntityDamageSource(level, TFDamageTypes.GHAST_TEAR, this.boss, TFEntities.UR_GHAST.get()), damage);
            if (slow > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 0), this.boss);
            }
        }
        AABB below = this.boss.getBoundingBox().move(0.0D, -16.0D, 0.0D).inflate(0.0D, 16.0D, 0.0D);
        for (CarminiteGhastling ghast : level.getEntitiesOfClass(CarminiteGhastling.class, below)) {
            ghast.push(0.0D, 1.0D, 0.0D);
        }
    }

    private double rainRadius() {
        double radius = UrGhastConfig.RAIN_RADIUS.get();
        if (this.phase >= 3) {
            radius *= UrGhastConfig.RAIN_PHASE3_SCALE.get();
        }
        return radius;
    }

    @Nullable
    private Player nearestPlayerBelow(ServerLevel level, double range) {
        Player best = null;
        double bestDist = range * range;
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.isCreative() || player.getY() > this.boss.getY() + this.boss.getBbHeight() * 0.5D) {
                continue;
            }
            double dx = player.getX() - this.boss.getX();
            double dz = player.getZ() - this.boss.getZ();
            double dist = dx * dx + dz * dz;
            if (dist < bestDist && this.boss.getY() - player.getY() < RAIN_DEPTH) {
                bestDist = dist;
                best = player;
            }
        }
        return best;
    }

    private void endRain() {
        this.rainActive = false;
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.RAIN_END, this.boss.getId()));
    }

    private void cancelCasts() {
        if (this.gazeTicks > 0) {
            this.gazeTicks = 0;
            this.gazeTargets.clear();
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.GAZE_END, this.boss.getId()));
            this.debug("gaze cancelled");
        }
        if (this.tearCharge > 0) {
            this.tearCharge = 0;
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.TEAR_CHARGE, this.boss.getId(), 0.0F));
            this.debug("primal tear cancelled");
        }
    }

    private void tickSkills(ServerLevel level) {
        if (this.skillGap > 0) {
            this.skillGap--;
        }
        if (this.gazeCooldown > 0) {
            this.gazeCooldown--;
        }
        if (this.broodCooldown > 0) {
            this.broodCooldown--;
        }
        if (this.tearCooldown > 0) {
            this.tearCooldown--;
        }
        if (this.gazeTicks > 0) {
            this.tickGaze(level);
            return;
        }
        if (this.tearCharge > 0) {
            this.tickTearCharge(level);
            return;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive() || this.skillGap > 0) {
            return;
        }
        double dist = this.boss.distanceTo(target);
        if (this.phase >= 3 && UrGhastConfig.TEAR_ENABLED.get() && this.tearCooldown <= 0 && dist < 64.0D && this.boss.hasLineOfSight(target)) {
            this.startTear();
            return;
        }
        if (UrGhastConfig.GAZE_ENABLED.get() && this.gazeCooldown <= 0 && dist <= UrGhastConfig.GAZE_RANGE.get() && this.boss.hasLineOfSight(target)) {
            this.startGaze(level, target);
            return;
        }
        if (this.phase >= 2 && UrGhastConfig.BROOD_ENABLED.get() && this.broodCooldown <= 0 && this.broodTicks <= 0) {
            this.startBrood(level);
        }
    }

    private Vec3 faceOrigin(double forward, double drop) {
        Vec3 look = this.boss.getViewVector(1.0F);
        return new Vec3(this.boss.getX() + look.x * forward, this.boss.getY() + this.boss.getBbHeight() * 0.5D + look.y * forward - drop, this.boss.getZ() + look.z * forward);
    }

    private void startGaze(ServerLevel level, LivingEntity target) {
        this.gazeTargets.clear();
        this.gazeTargets.add(target);
        double range = UrGhastConfig.GAZE_RANGE.get();
        if (this.phase >= 3) {
            Player second = null;
            double best = range * range;
            for (Player player : level.players()) {
                if (player == target || !player.isAlive() || player.isSpectator() || player.isCreative()) {
                    continue;
                }
                double dist = player.distanceToSqr(this.boss);
                if (dist < best) {
                    best = dist;
                    second = player;
                }
            }
            if (second != null) {
                this.gazeTargets.add(second);
            }
        }
        int duration = UrGhastConfig.GAZE_DURATION_TICKS.get();
        this.gazeTicks = duration;
        this.gazeCooldown = UrGhastConfig.GAZE_COOLDOWN_TICKS.get();
        this.skillGap = duration + SKILL_GAP;
        float[] data = new float[1 + this.gazeTargets.size()];
        data[0] = duration;
        for (int i = 0; i < this.gazeTargets.size(); i++) {
            data[i + 1] = this.gazeTargets.get(i).getId();
        }
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.GAZE, this.boss.getId(), data));
        level.playSound(null, this.boss.getX(), this.boss.getY(), this.boss.getZ(), TFSounds.UR_GHAST_AMBIENT.get(), SoundSource.HOSTILE, 6.0F, 0.4F);
        this.debug("gaze started on {} target(s), duration {}", this.gazeTargets.size(), duration);
    }

    private void tickGaze(ServerLevel level) {
        this.gazeTicks--;
        this.gazeTargets.removeIf(entity -> !entity.isAlive() || entity.isRemoved());
        if (this.gazeTargets.isEmpty()) {
            this.gazeTicks = 0;
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.GAZE_END, this.boss.getId()));
            return;
        }
        this.boss.getLookControl().setLookAt(this.gazeTargets.get(0), 10.0F, this.boss.getMaxHeadXRot());
        if (this.gazeTicks > 0) {
            return;
        }
        Vec3 origin = this.faceOrigin(SHOT_DISTANCE * 0.8D, 0.0D);
        double range = UrGhastConfig.GAZE_RANGE.get() * 1.5D;
        float damage = (float) (this.attack() * UrGhastConfig.GAZE_MULTIPLIER.get());
        int slow = UrGhastConfig.GAZE_SLOW_TICKS.get();
        float[] data = new float[this.gazeTargets.size() * 2];
        for (int i = 0; i < this.gazeTargets.size(); i++) {
            LivingEntity target = this.gazeTargets.get(i);
            Vec3 eye = target.getEyePosition();
            boolean hit = false;
            if (target.distanceTo(this.boss) <= range) {
                HitResult result = level.clip(new ClipContext(origin, eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.boss));
                hit = result.getType() == HitResult.Type.MISS;
            }
            if (hit) {
                target.hurt(this.boss.damageSources().mobAttack(this.boss), damage);
                if (slow > 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow, 1), this.boss);
                }
            }
            data[i * 2] = target.getId();
            if (hit) {
                data[i * 2 + 1] = 1.0F;
            }
            this.debug("gaze resolved on {}, line of sight hit {}", target.getName().getString(), hit);
        }
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.GAZE_END, this.boss.getId(), data));
        this.gazeTargets.clear();
    }

    private void startBrood(ServerLevel level) {
        List<CarminiteGhastling> found = level.getEntitiesOfClass(CarminiteGhastling.class, this.boss.getBoundingBox().inflate(48.0D), Entity::isAlive);
        if (found.size() < 2) {
            this.broodCooldown = 40;
            return;
        }
        found.sort(Comparator.comparingDouble(ghast -> ghast.distanceToSqr(this.boss)));
        int max = Math.min(found.size(), UrGhastConfig.BROOD_MAX_GHASTLINGS.get());
        this.recalled.clear();
        this.recalled.addAll(found.subList(0, max));
        this.broodTicks = UrGhastConfig.BROOD_MAX_TICKS.get();
        this.broodCooldown = UrGhastConfig.BROOD_COOLDOWN_TICKS.get();
        this.skillGap = SKILL_GAP;
        float[] data = new float[1 + this.recalled.size()];
        data[0] = this.broodTicks;
        for (int i = 0; i < this.recalled.size(); i++) {
            data[i + 1] = this.recalled.get(i).getId();
        }
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.RECALL, this.boss.getId(), data));
        level.playSound(null, this.boss.getX(), this.boss.getY(), this.boss.getZ(), TFSounds.UR_GHAST_TANTRUM.get(), SoundSource.HOSTILE, 8.0F, 0.35F);
        this.debug("brood call recalled {} ghastling(s)", this.recalled.size());
    }

    private void tickBrood() {
        if (this.broodTicks <= 0) {
            return;
        }
        this.broodTicks--;
        this.recalled.removeIf(ghast -> !ghast.isAlive() || ghast.isRemoved());
        Vec3 center = this.boss.position().add(0.0D, this.boss.getBbHeight() * 0.5D, 0.0D);
        double speed = UrGhastConfig.BROOD_SPEED.get();
        for (CarminiteGhastling ghast : this.recalled) {
            Vec3 dir = center.subtract(ghast.position());
            double length = dir.length();
            if (length < 0.01D) {
                continue;
            }
            ghast.setTarget(null);
            ghast.setDeltaMovement(dir.scale(speed / length));
        }
        if (this.recalled.isEmpty() || this.broodTicks <= 0) {
            this.broodTicks = 0;
            this.recalled.clear();
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.RECALL_END, this.boss.getId()));
            this.debug("brood call ended");
        }
    }

    public float onAbsorb() {
        float heal = (float) (this.boss.getMaxHealth() * UrGhastConfig.ABSORB_HEAL_RATIO.get());
        if (this.phase >= 2 && UrGhastConfig.BROOD_MAX_STACKS.get() > 0) {
            this.stacks = Math.min(UrGhastConfig.BROOD_MAX_STACKS.get(), this.stacks + 1);
            this.stackTimer = UrGhastConfig.BROOD_STACK_TICKS.get();
            this.applyGrief();
        }
        this.debug("absorbed a ghastling, heal {}, grief stacks {}", heal, this.stacks);
        return heal;
    }

    private void tickStacks() {
        if (this.stacks <= 0) {
            return;
        }
        this.stackTimer--;
        if (this.stackTimer <= 0) {
            this.stacks = 0;
            this.applyGrief();
            this.debug("grief stacks expired");
        }
    }

    private void applyGrief() {
        AttributeInstance attack = this.boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.removeModifier(GRIEF_UUID);
            if (this.stacks > 0) {
                attack.addTransientModifier(new AttributeModifier(GRIEF_UUID, "Boss Plus grief", this.stacks * UrGhastConfig.BROOD_ATTACK_PER_STACK.get(), AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.STACKS, this.boss.getId(), this.stacks));
    }

    private void startTear() {
        int charge = UrGhastConfig.TEAR_CHARGE_TICKS.get();
        this.tearCharge = charge;
        this.tearCooldown = UrGhastConfig.TEAR_COOLDOWN_TICKS.get();
        this.skillGap = charge + SKILL_GAP;
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.TEAR_CHARGE, this.boss.getId(), charge));
        this.boss.playSound(TFSounds.UR_GHAST_WARN.get(), 8.0F, 0.5F);
        this.debug("primal tear charging for {} ticks", charge);
    }

    private void tickTearCharge(ServerLevel level) {
        this.tearCharge--;
        LivingEntity target = this.boss.getTarget();
        if (target != null) {
            this.boss.getLookControl().setLookAt(target, 10.0F, this.boss.getMaxHeadXRot());
        }
        if (this.tearCharge > 0) {
            return;
        }
        if (target == null || !target.isAlive()) {
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.TEAR_CHARGE, this.boss.getId(), 0.0F));
            this.debug("primal tear released without a target, cancelled");
            return;
        }
        Vec3 start = this.faceOrigin(SHOT_DISTANCE, 3.0D);
        double ground = NagaBossController.findGround(level, target.getX(), target.getY() + 1.0D, target.getZ());
        if (Double.isNaN(ground)) {
            ground = target.getY();
        }
        Vec3 end = new Vec3(target.getX(), ground, target.getZ());
        float damage = (float) (this.attack() * UrGhastConfig.TEAR_MULTIPLIER.get());
        float popDamage = (float) (this.boss.getMaxHealth() * UrGhastConfig.TEAR_POP_DAMAGE_RATIO.get());
        PrimalTear tear = new PrimalTear(level, this.boss, start, end, UrGhastConfig.TEAR_FALL_TICKS.get(), damage, popDamage);
        level.addFreshEntity(tear);
        ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.TEAR_CHARGE, this.boss.getId(), 0.0F));
        this.boss.playSound(TFSounds.UR_GHAST_SHOOT.get(), 8.0F, 0.4F);
        this.debug("primal tear released toward {} {} {}", end.x, end.y, end.z);
    }

    public void addPool(double x, double y, double z) {
        double radius = UrGhastConfig.TEAR_POOL_RADIUS.get();
        int ticks = UrGhastConfig.TEAR_POOL_TICKS.get();
        if (radius <= 0.0D || ticks <= 0) {
            return;
        }
        this.pools.add(new Pool(x, y, z, radius, ticks));
        ModNetwork.sendNear(this.boss.level(), x, y, z, 96.0D, new UrGhastFxPacket(UrGhastFxPacket.POOL, -1, (float) x, (float) y, (float) z, (float) radius, ticks));
    }

    private void tickPools(ServerLevel level) {
        if (this.pools.isEmpty()) {
            return;
        }
        float damage = (float) (this.attack() * UrGhastConfig.TEAR_POOL_MULTIPLIER.get());
        Iterator<Pool> iterator = this.pools.iterator();
        while (iterator.hasNext()) {
            Pool pool = iterator.next();
            pool.age++;
            if (pool.age >= pool.ticks) {
                iterator.remove();
                continue;
            }
            if (pool.age % POOL_INTERVAL != 0) {
                continue;
            }
            AABB box = new AABB(pool.x - pool.radius, pool.y - 1.0D, pool.z - pool.radius, pool.x + pool.radius, pool.y + 2.0D, pool.z + pool.radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (entity instanceof CarminiteGhastguard || !entity.isAlive()) {
                    continue;
                }
                double dx = entity.getX() - pool.x;
                double dz = entity.getZ() - pool.z;
                if (dx * dx + dz * dz > pool.radius * pool.radius) {
                    continue;
                }
                entity.hurt(this.boss.damageSources().indirectMagic(this.boss, this.boss), damage);
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), this.boss);
            }
        }
    }

    public void fireVolley() {
        LivingEntity target = this.boss.getTarget();
        if (target == null || !(this.boss.level() instanceof ServerLevel level)) {
            return;
        }
        double offsetX = target.getX() - this.boss.getX();
        double offsetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (this.boss.getY() + this.boss.getBbHeight() / 2.0F);
        double offsetZ = target.getZ() - this.boss.getZ();
        int count = UrGhastConfig.FIREBALL_PHASE1_COUNT.get();
        if (this.phase >= 2) {
            count = UrGhastConfig.FIREBALL_PHASE2_COUNT.get();
        }
        double spread = Math.toRadians(UrGhastConfig.FIREBALL_SPREAD.get());
        Vec3 look = this.boss.getViewVector(1.0F);
        double sx = this.boss.getX() + look.x * SHOT_DISTANCE;
        double sy = this.boss.getY() + this.boss.getBbHeight() / 2.0F + look.y * SHOT_DISTANCE;
        double sz = this.boss.getZ() + look.z * SHOT_DISTANCE;
        for (int i = 0; i < count; i++) {
            double angle = (i - (count - 1) * 0.5D) * spread;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double rx = offsetX * cos - offsetZ * sin;
            double rz = offsetX * sin + offsetZ * cos;
            UrGhastFireball fireball = new UrGhastFireball(level, this.boss, rx, offsetY, rz, 1);
            fireball.setPos(sx, sy, sz);
            level.addFreshEntity(fireball);
        }
        this.debug("fired a volley of {} fireball(s) in phase {}", count, this.phase);
    }

    public static float fireballDamage(@Nullable Entity owner, Entity hit, float original) {
        if (!UrGhastConfig.ENABLED.get()) {
            return original;
        }
        if (owner instanceof UrGhast ghast) {
            return (float) (ghast.getAttributeValue(Attributes.ATTACK_DAMAGE) * UrGhastConfig.FIREBALL_MULTIPLIER.get());
        }
        if (hit instanceof UrGhast && UrGhastConfig.REFLECT_SCALES_WITH_HEALTH.get()) {
            return (float) (original * UrGhastConfig.HEALTH_MULTIPLIER.get());
        }
        return original;
    }

    public void onDeath() {
        this.cancelCasts();
        if (this.rainActive) {
            this.endRain();
        }
        if (this.broodTicks > 0) {
            this.broodTicks = 0;
            this.recalled.clear();
            ModNetwork.sendTracking(this.boss, new UrGhastFxPacket(UrGhastFxPacket.RECALL_END, this.boss.getId()));
        }
        if (this.stacks > 0) {
            this.stacks = 0;
            this.applyGrief();
        }
        this.pools.clear();
    }

    public void save(CompoundTag tag) {
        tag.putInt(PHASE_TAG, this.phase);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(PHASE_TAG)) {
            this.phase = Math.max(1, tag.getInt(PHASE_TAG));
            this.initialized = true;
        }
    }

    private static final class Pool {
        private final double x;
        private final double y;
        private final double z;
        private final double radius;
        private final int ticks;
        private int age;

        private Pool(double x, double y, double z, double radius, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ticks = ticks;
        }
    }
}
