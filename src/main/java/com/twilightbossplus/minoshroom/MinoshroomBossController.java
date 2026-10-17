package com.twilightbossplus.minoshroom;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.MinoshroomConfig;
import com.twilightbossplus.entity.MushroomPillar;
import com.twilightbossplus.entity.Puffball;
import com.twilightbossplus.entity.ThrownAxe;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.MinoFxPacket;
import com.twilightbossplus.network.ModNetwork;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.init.TFDamageTypes;
import twilightforest.init.TFSounds;

public final class MinoshroomBossController {
    public static final int KIND_RED = 0;
    public static final int KIND_BROWN = 1;
    private static final int GLOBAL_GAP = 25;
    private static final int GLOBAL_GAP_RANDOM = 6;
    private static final int ROAR_TICKS = 20;
    private static final int AXE_WINDUP = 12;
    private static final int PILLAR_WARN = 15;
    private static final int PILLAR_RAISE = 10;
    private static final int PILLAR_CAST = 20;
    private static final int PUFF_WINDUP = 10;
    private static final int PUFF_RECOVER = 6;
    private static final int PUFF_FLIGHT = 14;
    private static final int STAMPEDE_WINDUP = 8;
    private static final int SECOND_WAVE_DELAY = 10;
    private static final double SLAM_RANGE = 3.5D;
    private static final double CHARGE_MIN_RANGE = 5.0D;
    private static final double CHARGE_MAX_RANGE = 12.0D;
    private static final double CHARGE_OVERSHOOT = 2.1D;
    private static final double CHARGE_MIN_LENGTH = 3.0D;
    private static final double PUFF_MIN_RANGE = 4.0D;
    private static final double PUFF_SPREAD = 2.5D;
    private static final double SKILL_RANGE = 16.0D;
    private static final double TARGET_RANGE = 32.0D;
    private static final double PILLAR_KNOCKBACK = 0.6D;
    private static final double PILLAR_MIN_BOSS_DISTANCE = 2.2D;
    private static final double PILLAR_SPACING = 1.8D;
    private static final float BREAKABLE_HARDNESS = 5.0F;

    private enum Action {
        NONE,
        CHARGE_WINDUP,
        CHARGE_RUN,
        STUN,
        SLAM,
        AXE_WINDUP,
        PILLAR_CAST,
        PUFF_CAST,
        ROAR
    }

    private static final class Wave {
        private final Vec3 center;
        private final double maxRadius;
        private final double speed;
        private final float damage;
        private final double knockback;
        private final Set<Integer> hit = new HashSet<>();
        private int delay;
        private double radius;

        private Wave(Vec3 center, double maxRadius, double speed, float damage, double knockback, int delay) {
            this.center = center;
            this.maxRadius = maxRadius;
            this.speed = speed;
            this.damage = damage;
            this.knockback = knockback;
            this.delay = delay;
        }
    }

    private static final class PendingPillar {
        private final Vec3 pos;
        private final int kind;
        private int ticks;

        private PendingPillar(Vec3 pos, int kind, int ticks) {
            this.pos = pos;
            this.kind = kind;
            this.ticks = ticks;
        }
    }

    private final Minoshroom mob;
    private Action action = Action.NONE;
    private int actionTicks;
    private int actionDuration;
    private int globalCooldown = GLOBAL_GAP;
    private int chargeCooldown = 40;
    private int slamCooldown = 40;
    private int axeCooldown = 80;
    private int pillarCooldown = 160;
    private int puffCooldown = 120;
    private int stampedeCooldown;
    private int lastPhase;
    private boolean pendingRoar;
    private boolean roarStampede;
    private boolean stampede;
    private int stampedeLeft;
    private float chargeYaw;
    private Vec3 chargeDir = Vec3.ZERO;
    private Vec3 chargeStart = Vec3.ZERO;
    private double chargeLength;
    private boolean chargeBlocked;
    private final Set<Integer> chargeHits = new HashSet<>();
    private final List<Wave> waves = new ArrayList<>();
    private final List<PendingPillar> pendingPillars = new ArrayList<>();
    private final List<MushroomPillar> pillars = new ArrayList<>();
    @Nullable
    private ThrownAxe axe;

    public MinoshroomBossController(Minoshroom mob) {
        this.mob = mob;
    }

    public static boolean canHit(LivingEntity attacker, LivingEntity target) {
        if (target == attacker || !target.isAlive() || target instanceof Minoshroom || !target.isAttackable() || attacker.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    public boolean isBusy() {
        return this.action != Action.NONE;
    }

    public boolean isRoaring() {
        return this.action == Action.ROAR;
    }

    public boolean isAxeAirborne() {
        return this.axe != null && !this.axe.isRemoved();
    }

    public float getDamageTakenMultiplier() {
        if (!MinoshroomConfig.ENABLED.get() || this.action != Action.STUN) {
            return 1.0F;
        }
        return 1.0F + MinoshroomConfig.STUN_DAMAGE_BONUS.get().floatValue();
    }

    public float getAttackDamage() {
        return (float) this.mob.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public int getPhase() {
        float ratio = this.mob.getHealth() / this.mob.getMaxHealth();
        if (ratio <= MinoshroomConfig.PHASE3_THRESHOLD.get()) {
            return 3;
        }
        if (ratio <= MinoshroomConfig.PHASE2_THRESHOLD.get()) {
            return 2;
        }
        return 1;
    }

    public void save(CompoundTag tag) {
        tag.putInt("TwilightBossPlusMinoPhase", this.lastPhase);
    }

    public void load(CompoundTag tag) {
        this.lastPhase = tag.getInt("TwilightBossPlusMinoPhase");
    }

    public void onDeath() {
        this.mob.setGroundAttackCharge(false);
        this.mob.setCharging(false);
        if (this.axe != null) {
            this.axe.discard();
            this.axe = null;
        }
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.HORN, this.mob.getId(), 0.0F));
        this.pendingPillars.clear();
        this.waves.clear();
        for (MushroomPillar pillar : this.pillars) {
            pillar.shatter();
        }
        this.pillars.clear();
        this.action = Action.NONE;
        this.stampede = false;
        this.debug("Died, cleaned up skills");
    }

    public void tick() {
        if (!MinoshroomConfig.ENABLED.get() || this.mob.isDeadOrDying()) {
            return;
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        if (this.chargeCooldown > 0) {
            this.chargeCooldown--;
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        if (this.axeCooldown > 0) {
            this.axeCooldown--;
        }
        if (this.pillarCooldown > 0) {
            this.pillarCooldown--;
        }
        if (this.puffCooldown > 0) {
            this.puffCooldown--;
        }
        if (this.stampedeCooldown > 0) {
            this.stampedeCooldown--;
        }

        int phase = this.getPhase();
        if (this.lastPhase == 0) {
            this.lastPhase = phase;
        }
        if (phase > this.lastPhase) {
            this.debug("Phase {} -> {}", this.lastPhase, phase);
            this.lastPhase = phase;
            this.pendingRoar = true;
        }

        this.tickWaves();
        this.tickPendingPillars();
        this.pillars.removeIf(MushroomPillar::isRemoved);
        if (this.axe != null && this.axe.isRemoved()) {
            this.axe = null;
        }

        LivingEntity target = this.mob.getTarget();
        boolean targetValid = target != null && target.isAlive() && this.mob.distanceToSqr(target) < TARGET_RANGE * TARGET_RANGE;
        if (!targetValid) {
            target = null;
        }

        if (this.action != Action.NONE) {
            this.actionTicks++;
            this.mob.getNavigation().stop();
        }
        if (this.action == Action.CHARGE_WINDUP) {
            this.tickChargeWindup();
        } else if (this.action == Action.CHARGE_RUN) {
            this.tickChargeRun(target);
        } else if (this.action == Action.STUN) {
            this.tickStun();
        } else if (this.action == Action.SLAM) {
            this.tickSlam(target, phase);
        } else if (this.action == Action.AXE_WINDUP) {
            this.tickAxe(target);
        } else if (this.action == Action.PILLAR_CAST) {
            this.tickPillarCast(target);
        } else if (this.action == Action.PUFF_CAST) {
            this.tickPuffCast(target);
        } else if (this.action == Action.ROAR) {
            this.tickRoar(target);
        }

        if (this.action == Action.NONE) {
            if (this.pendingRoar) {
                this.startRoar(false);
            } else if (target != null && this.globalCooldown <= 0) {
                this.chooseSkill(target, phase);
            }
        }
    }

    private void chooseSkill(LivingEntity target, int phase) {
        double distance = this.mob.distanceTo(target);
        boolean sees = this.mob.hasLineOfSight(target);
        boolean grounded = this.mob.onGround();
        float ratio = this.mob.getHealth() / this.mob.getMaxHealth();
        if (phase >= 3 && MinoshroomConfig.STAMPEDE_ENABLED.get() && this.stampedeCooldown <= 0
            && ratio <= MinoshroomConfig.STAMPEDE_HEALTH_THRESHOLD.get() && grounded) {
            this.startRoar(true);
            return;
        }
        if (MinoshroomConfig.PILLAR_ENABLED.get() && this.pillarCooldown <= 0 && distance < SKILL_RANGE) {
            if (this.startPillars(target, phase)) {
                return;
            }
            this.pillarCooldown = 40;
        }
        if (phase >= 2 && MinoshroomConfig.PUFFBALL_ENABLED.get() && this.puffCooldown <= 0
            && distance >= PUFF_MIN_RANGE && distance < SKILL_RANGE && sees) {
            this.startAction(Action.PUFF_CAST, PUFF_WINDUP + PUFF_RECOVER);
            this.faceTarget(target);
            this.mob.playSound(SoundEvents.FUNGUS_PLACE, 1.5F, 0.5F);
            this.debug("Spore Burst windup");
            return;
        }
        if (this.slamCooldown <= 0 && distance <= SLAM_RANGE && grounded) {
            int min = MinoshroomConfig.SLAM_CHARGE_MIN.get();
            int max = Math.max(min, MinoshroomConfig.SLAM_CHARGE_MAX.get());
            int windup = min + this.mob.getRandom().nextInt(max - min + 1);
            this.mob.setMaxCharge(windup);
            this.mob.setGroundAttackCharge(true);
            this.startAction(Action.SLAM, windup);
            this.faceTarget(target);
            this.debug("Ground Slam windup {} ticks", windup);
            return;
        }
        if (MinoshroomConfig.AXE_ENABLED.get() && this.axeCooldown <= 0 && this.axe == null
            && distance >= MinoshroomConfig.AXE_MIN_RANGE.get() && distance <= MinoshroomConfig.AXE_MAX_RANGE.get() && sees) {
            this.mob.setMaxCharge(AXE_WINDUP);
            this.mob.setGroundAttackCharge(true);
            this.startAction(Action.AXE_WINDUP, AXE_WINDUP);
            this.faceTarget(target);
            this.debug("Axe Boomerang windup");
            return;
        }
        if (this.chargeCooldown <= 0 && distance >= CHARGE_MIN_RANGE && distance <= CHARGE_MAX_RANGE && sees && grounded) {
            if (!this.startCharge(target, false, phase)) {
                this.chargeCooldown = 20;
            }
        }
    }

    private void startAction(Action next, int duration) {
        this.action = next;
        this.actionTicks = 0;
        this.actionDuration = duration;
        this.mob.getNavigation().stop();
    }

    private void finishAction() {
        this.action = Action.NONE;
        this.actionTicks = 0;
        this.globalCooldown = GLOBAL_GAP + this.mob.getRandom().nextInt(GLOBAL_GAP_RANDOM);
        this.mob.setAggressive(false);
    }

    private void faceYaw(float yaw) {
        this.mob.setYRot(yaw);
        this.mob.yBodyRot = yaw;
        this.mob.yHeadRot = yaw;
    }

    private void faceTarget(@Nullable LivingEntity target) {
        if (target == null) {
            return;
        }
        this.faceYaw(NagaBossController.yawTo(this.mob.position(), target.position()));
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void holdStill() {
        Vec3 motion = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(0.0D, motion.y, 0.0D);
    }

    private boolean startCharge(LivingEntity target, boolean stampedeCharge, int phase) {
        Level level = this.mob.level();
        float yaw = NagaBossController.yawTo(this.mob.position(), target.position());
        Vec3 dir = NagaBossController.directionOf(yaw);
        double desired = Mth.clamp(this.mob.distanceTo(target) + CHARGE_OVERSHOOT, 4.0D, 16.0D);
        boolean breakBlocks = MinoshroomConfig.CHARGE_BREAK_BLOCKS.get();
        AABB box = this.mob.getBoundingBox().move(0.0D, 0.05D, 0.0D);
        double length = 0.0D;
        boolean blocked = false;
        for (double step = 0.5D; step <= desired + 1.0E-3D; step += 0.5D) {
            AABB moved = box.move(dir.x * step, 0.0D, dir.z * step);
            boolean free;
            if (breakBlocks) {
                free = level.getEntityCollisions(this.mob, moved).isEmpty();
            } else {
                free = level.noCollision(this.mob, moved);
            }
            if (!free) {
                blocked = true;
                break;
            }
            length = step;
        }
        if (length < CHARGE_MIN_LENGTH) {
            this.debug("Charge path too short ({}), skipped", length);
            return false;
        }
        int windup;
        if (stampedeCharge) {
            windup = STAMPEDE_WINDUP;
        } else {
            windup = 15 + this.mob.getRandom().nextInt(16);
            if (phase >= 2) {
                windup = Math.max(STAMPEDE_WINDUP, Math.round(windup * MinoshroomConfig.PHASE2_WINDUP_SCALE.get().floatValue()));
            }
        }
        this.chargeYaw = yaw;
        this.chargeDir = dir;
        this.chargeLength = length;
        this.chargeBlocked = blocked;
        this.stampede = stampedeCharge;
        this.chargeHits.clear();
        this.startAction(Action.CHARGE_WINDUP, windup);
        this.faceYaw(yaw);
        this.mob.setAggressive(true);
        float red = 0.0F;
        if (stampedeCharge) {
            red = 1.0F;
        }
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.CHARGE_WARN, this.mob.getId(),
            (float) this.mob.getX(), (float) this.mob.getY(), (float) this.mob.getZ(), yaw, (float) length, windup, red,
            this.mob.getBbWidth(), MinoshroomConfig.CHARGE_SPEED.get().floatValue()));
        this.mob.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.6F, 0.5F);
        this.debug("Charge windup {} ticks, length {}, blocked {}, stampede {}", windup, length, blocked, stampedeCharge);
        return true;
    }

    private void tickChargeWindup() {
        this.faceYaw(this.chargeYaw);
        this.holdStill();
        if (this.actionTicks % 6 == 0) {
            this.mob.playSound(SoundEvents.ROOTED_DIRT_STEP, 1.4F, 0.6F);
        }
        if (this.actionTicks < this.actionDuration) {
            return;
        }
        double speed = MinoshroomConfig.CHARGE_SPEED.get();
        this.chargeStart = this.mob.position();
        this.startAction(Action.CHARGE_RUN, (int) Math.ceil(this.chargeLength / speed) + 20);
        this.mob.setCharging(true);
        this.mob.playSound(TFSounds.MINOSHROOM_ATTACK.get(), 2.0F, 0.8F);
    }

    private void tickChargeRun(@Nullable LivingEntity target) {
        double speed = MinoshroomConfig.CHARGE_SPEED.get();
        Vec3 dir = this.chargeDir;
        double travelled = Math.sqrt(Mth.square(this.mob.getX() - this.chargeStart.x) + Mth.square(this.mob.getZ() - this.chargeStart.z));
        if (this.actionTicks > 1 && this.mob.horizontalCollision && this.chargeImpact(target)) {
            return;
        }
        if (this.actionTicks > this.actionDuration) {
            this.endCharge(target);
            return;
        }
        double step = speed;
        if (this.chargeBlocked) {
            if (travelled >= this.chargeLength + 1.0D) {
                this.startStun();
                return;
            }
        } else {
            double remaining = this.chargeLength - travelled;
            if (remaining <= 0.05D) {
                this.endCharge(target);
                return;
            }
            step = Math.min(speed, remaining);
        }
        this.faceYaw(this.chargeYaw);
        this.mob.setDeltaMovement(dir.x * step, this.mob.getDeltaMovement().y, dir.z * step);

        float multiplier = MinoshroomConfig.CHARGE_MULTIPLIER.get().floatValue();
        if (this.stampede) {
            multiplier = MinoshroomConfig.STAMPEDE_MULTIPLIER.get().floatValue();
        }
        float damage = this.getAttackDamage() * multiplier;
        double knockback = MinoshroomConfig.CHARGE_KNOCKBACK.get();
        AABB hitBox = this.mob.getBoundingBox().expandTowards(dir.x * step, 0.0D, dir.z * step).inflate(0.3D);
        for (LivingEntity living : this.mob.level().getEntitiesOfClass(LivingEntity.class, hitBox)) {
            if (!canHit(this.mob, living) || this.chargeHits.contains(living.getId())) {
                continue;
            }
            this.chargeHits.add(living.getId());
            if (living.hurt(this.mob.damageSources().mobAttack(this.mob), damage)) {
                living.knockback(knockback, -dir.x, -dir.z);
                this.debug("Charge hit {} for {}", living.getName().getString(), damage);
            }
        }
    }

    private boolean chargeImpact(@Nullable LivingEntity target) {
        Vec3 dir = this.chargeDir;
        AABB front = this.mob.getBoundingBox().expandTowards(dir.x, 0.0D, dir.z).inflate(0.3D);
        List<MushroomPillar> hitPillars = this.mob.level().getEntitiesOfClass(MushroomPillar.class, front);
        if (!hitPillars.isEmpty()) {
            for (MushroomPillar pillar : hitPillars) {
                pillar.shatter();
            }
            this.debug("Charge smashed {} pillars", hitPillars.size());
            this.startStun();
            return true;
        }
        if (MinoshroomConfig.CHARGE_BREAK_BLOCKS.get() && this.breakBlocksAhead()) {
            return false;
        }
        this.debug("Charge hit a wall");
        this.startStun();
        return true;
    }

    private boolean breakBlocksAhead() {
        Level level = this.mob.level();
        if (!ForgeEventFactory.getMobGriefingEvent(level, this.mob)) {
            return false;
        }
        AABB front = this.mob.getBoundingBox().move(this.chargeDir.x * 0.6D, 0.0D, this.chargeDir.z * 0.6D);
        BlockPos min = BlockPos.containing(front.minX, front.minY + 0.1D, front.minZ);
        BlockPos max = BlockPos.containing(front.maxX - 1.0E-3D, front.maxY - 1.0E-3D, front.maxZ - 1.0E-3D);
        boolean broken = false;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > BREAKABLE_HARDNESS || !state.canEntityDestroy(level, pos, this.mob)
                || !ForgeEventFactory.onEntityDestroyBlock(this.mob, pos, state)) {
                continue;
            }
            level.destroyBlock(pos.immutable(), true, this.mob);
            broken = true;
        }
        if (broken) {
            this.debug("Charge broke blocks ahead");
        }
        return broken;
    }

    private void endCharge(@Nullable LivingEntity target) {
        this.mob.setCharging(false);
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.CHARGE_END, this.mob.getId()));
        this.holdStill();
        if (this.stampede) {
            if (this.stampedeLeft > 0 && target != null) {
                this.stampedeLeft--;
                if (this.startCharge(target, true, 3)) {
                    return;
                }
            }
            this.finishStampede();
            return;
        }
        this.chargeCooldown = MinoshroomConfig.CHARGE_COOLDOWN_TICKS.get();
        this.finishAction();
    }

    private void finishStampede() {
        this.stampede = false;
        this.stampedeLeft = 0;
        this.stampedeCooldown = MinoshroomConfig.STAMPEDE_COOLDOWN_TICKS.get();
        this.chargeCooldown = MinoshroomConfig.CHARGE_COOLDOWN_TICKS.get();
        this.addWave(this.mob.position(), 0);
        this.mob.playSound(TFSounds.MINOSHROOM_SLAM.get(), 2.0F, 0.8F);
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.HORN, this.mob.getId(), 0.0F));
        this.finishAction();
        this.debug("Crimson Stampede finished");
    }

    private void startStun() {
        int ticks = MinoshroomConfig.STUN_TICKS.get();
        this.mob.setCharging(false);
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.CHARGE_END, this.mob.getId()));
        if (this.stampede) {
            this.stampede = false;
            this.stampedeLeft = 0;
            this.stampedeCooldown = MinoshroomConfig.STAMPEDE_COOLDOWN_TICKS.get();
            ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.HORN, this.mob.getId(), 0.0F));
        }
        this.chargeCooldown = MinoshroomConfig.CHARGE_COOLDOWN_TICKS.get();
        this.mob.setDeltaMovement(-this.chargeDir.x * 0.3D, 0.2D, -this.chargeDir.z * 0.3D);
        this.mob.setAggressive(false);
        this.startAction(Action.STUN, ticks);
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.STUN, this.mob.getId(), ticks));
        this.mob.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.5F);
        this.mob.playSound(TFSounds.MINOSHROOM_HURT.get(), 2.0F, 0.7F);
        this.debug("Stunned for {} ticks", ticks);
    }

    private void tickStun() {
        if (this.actionTicks > 3) {
            this.holdStill();
        }
        if (this.actionTicks >= this.actionDuration) {
            this.finishAction();
        }
    }

    private void tickSlam(@Nullable LivingEntity target, int phase) {
        this.holdStill();
        if (this.actionTicks < this.actionDuration - 4) {
            this.faceTarget(target);
        }
        if (this.actionTicks < this.actionDuration) {
            return;
        }
        this.mob.setGroundAttackCharge(false);
        Vec3 center = this.mob.position();
        this.addWave(center, 0);
        if (phase >= 3) {
            this.addWave(center, SECOND_WAVE_DELAY);
        }
        this.mob.playSound(TFSounds.MINOSHROOM_SLAM.get(), 2.0F, 1.0F);
        this.slamCooldown = MinoshroomConfig.SLAM_COOLDOWN_TICKS.get();
        this.finishAction();
        this.debug("Ground Slam released, double wave {}", phase >= 3);
    }

    private void addWave(Vec3 center, int delay) {
        double maxRadius = MinoshroomConfig.SLAM_RADIUS.get();
        double speed = MinoshroomConfig.SLAM_SPEED.get();
        float damage = this.getAttackDamage() * MinoshroomConfig.SLAM_MULTIPLIER.get().floatValue();
        this.waves.add(new Wave(center, maxRadius, speed, damage, MinoshroomConfig.SLAM_KNOCKBACK.get(), delay));
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.WAVE, this.mob.getId(),
            (float) center.x, (float) center.y, (float) center.z, (float) maxRadius, (float) speed, delay));
    }

    private void tickWaves() {
        if (this.waves.isEmpty()) {
            return;
        }
        Level level = this.mob.level();
        Iterator<Wave> iterator = this.waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            if (wave.delay > 0) {
                wave.delay--;
                continue;
            }
            double inner = wave.radius;
            wave.radius = Math.min(wave.maxRadius, wave.radius + wave.speed);
            double outer = wave.radius;
            Vec3 c = wave.center;
            AABB area = new AABB(c.x - outer - 1.0D, c.y - 1.5D, c.z - outer - 1.0D, c.x + outer + 1.0D, c.y + 2.5D, c.z + outer + 1.0D);
            DamageSource source = TFDamageTypes.getEntityDamageSource(level, TFDamageTypes.SLAM, this.mob);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!canHit(this.mob, living) || wave.hit.contains(living.getId())) {
                    continue;
                }
                double dx = living.getX() - c.x;
                double dz = living.getZ() - c.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < inner - 0.6D || distance > outer + 0.6D) {
                    continue;
                }
                if (!living.onGround() || Math.abs(living.getY() - c.y) > 1.5D) {
                    continue;
                }
                wave.hit.add(living.getId());
                if (living.hurt(source, wave.damage)) {
                    living.knockback(wave.knockback, -dx, -dz);
                    this.debug("Slam wave hit {} for {}", living.getName().getString(), wave.damage);
                }
            }
            if (wave.radius >= wave.maxRadius) {
                iterator.remove();
            }
        }
    }

    private void tickAxe(@Nullable LivingEntity target) {
        this.holdStill();
        this.faceTarget(target);
        if (this.actionTicks < this.actionDuration) {
            return;
        }
        this.mob.setGroundAttackCharge(false);
        this.axeCooldown = MinoshroomConfig.AXE_COOLDOWN_TICKS.get();
        if (target == null) {
            this.finishAction();
            return;
        }
        Vec3 hand = ThrownAxe.handPosition(this.mob);
        Vec3 aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 offset = aim.subtract(hand);
        double maxRange = MinoshroomConfig.AXE_MAX_RANGE.get();
        if (offset.length() > maxRange) {
            aim = hand.add(offset.normalize().scale(maxRange));
        }
        float damage = this.getAttackDamage() * MinoshroomConfig.AXE_MULTIPLIER.get().floatValue();
        ThrownAxe thrown = new ThrownAxe(this.mob.level(), this.mob, aim, damage);
        this.mob.level().addFreshEntity(thrown);
        this.axe = thrown;
        this.mob.swing(InteractionHand.MAIN_HAND);
        this.mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
        this.finishAction();
        this.debug("Axe Boomerang thrown at {}, damage {}", target.getName().getString(), damage);
    }

    private boolean startPillars(LivingEntity target, int phase) {
        int count = MinoshroomConfig.PILLAR_COUNT_PHASE1.get();
        if (phase >= 2) {
            count = MinoshroomConfig.PILLAR_COUNT_PHASE2.get();
        }
        Level level = this.mob.level();
        RandomSource random = this.mob.getRandom();
        List<Vec3> spots = new ArrayList<>();
        for (int attempt = 0; attempt < count * 6 && spots.size() < count; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 3.0D + random.nextDouble() * 4.0D;
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            double ground = NagaBossController.findGround(level, x, target.getY(), z);
            if (Double.isNaN(ground) || Math.abs(ground - target.getY()) > 3.0D) {
                continue;
            }
            double half = MushroomPillar.WIDTH * 0.5D;
            AABB box = new AABB(x - half, ground, z - half, x + half, ground + MushroomPillar.HEIGHT, z + half);
            if (!level.noCollision(box)) {
                continue;
            }
            if (Mth.square(x - this.mob.getX()) + Mth.square(z - this.mob.getZ()) < PILLAR_MIN_BOSS_DISTANCE * PILLAR_MIN_BOSS_DISTANCE) {
                continue;
            }
            boolean crowded = false;
            for (Vec3 spot : spots) {
                if (Mth.square(x - spot.x) + Mth.square(z - spot.z) < PILLAR_SPACING * PILLAR_SPACING) {
                    crowded = true;
                }
            }
            for (PendingPillar pending : this.pendingPillars) {
                if (Mth.square(x - pending.pos.x) + Mth.square(z - pending.pos.z) < PILLAR_SPACING * PILLAR_SPACING) {
                    crowded = true;
                }
            }
            if (crowded) {
                continue;
            }
            spots.add(new Vec3(x, ground, z));
        }
        if (spots.isEmpty()) {
            this.debug("Mycelium Maze found no valid spots");
            return false;
        }
        for (Vec3 spot : spots) {
            int kind = KIND_RED;
            if (random.nextBoolean()) {
                kind = KIND_BROWN;
            }
            this.pendingPillars.add(new PendingPillar(spot, kind, PILLAR_WARN));
            ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.PILLAR_WARN, this.mob.getId(),
                (float) spot.x, (float) spot.y, (float) spot.z, PILLAR_WARN, kind));
        }
        this.mob.setMaxCharge(PILLAR_RAISE);
        this.mob.setGroundAttackCharge(true);
        this.startAction(Action.PILLAR_CAST, PILLAR_CAST);
        this.faceTarget(target);
        this.pillarCooldown = MinoshroomConfig.PILLAR_COOLDOWN_TICKS.get();
        this.mob.playSound(SoundEvents.ROOTED_DIRT_PLACE, 2.0F, 0.5F);
        this.debug("Mycelium Maze cast with {} pillars", spots.size());
        return true;
    }

    private void tickPillarCast(@Nullable LivingEntity target) {
        this.holdStill();
        if (this.actionTicks < PILLAR_RAISE) {
            this.faceTarget(target);
        }
        if (this.actionTicks == PILLAR_RAISE) {
            this.mob.setGroundAttackCharge(false);
            this.mob.playSound(TFSounds.MINOSHROOM_SLAM.get(), 1.2F, 1.3F);
        }
        if (this.actionTicks >= this.actionDuration) {
            this.finishAction();
        }
    }

    private void tickPendingPillars() {
        if (this.pendingPillars.isEmpty()) {
            return;
        }
        Level level = this.mob.level();
        Iterator<PendingPillar> iterator = this.pendingPillars.iterator();
        while (iterator.hasNext()) {
            PendingPillar pending = iterator.next();
            pending.ticks--;
            if (pending.ticks > 0) {
                continue;
            }
            iterator.remove();
            Vec3 pos = pending.pos;
            double half = MushroomPillar.WIDTH * 0.5D + 0.3D;
            AABB area = new AABB(pos.x - half, pos.y - 0.5D, pos.z - half, pos.x + half, pos.y + MushroomPillar.HEIGHT, pos.z + half);
            float damage = this.getAttackDamage() * MinoshroomConfig.PILLAR_MULTIPLIER.get().floatValue();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
                if (!canHit(this.mob, living)) {
                    continue;
                }
                if (living.hurt(this.mob.damageSources().mobAttack(this.mob), damage)) {
                    living.knockback(PILLAR_KNOCKBACK, pos.x - living.getX(), pos.z - living.getZ());
                    this.debug("Pillar rise hit {} for {}", living.getName().getString(), damage);
                }
            }
            MushroomPillar pillar = new MushroomPillar(level, pos.x, pos.y, pos.z, pending.kind, MinoshroomConfig.PILLAR_LIFETIME.get());
            level.addFreshEntity(pillar);
            this.pillars.add(pillar);
            ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.PILLAR_RISE, this.mob.getId(),
                (float) pos.x, (float) pos.y, (float) pos.z, pending.kind));
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.8F, 0.6F);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.STEM_PLACE, SoundSource.HOSTILE, 1.8F, 0.5F);
        }
    }

    private void tickPuffCast(@Nullable LivingEntity target) {
        this.holdStill();
        if (this.actionTicks <= PUFF_WINDUP) {
            this.faceTarget(target);
        }
        if (this.actionTicks == PUFF_WINDUP && target != null) {
            Level level = this.mob.level();
            RandomSource random = this.mob.getRandom();
            Vec3 forward = NagaBossController.directionOf(this.mob.yBodyRot);
            Vec3 from = this.mob.position().add(forward.scale(0.6D)).add(0.0D, 2.2D, 0.0D);
            float damage = this.getAttackDamage() * MinoshroomConfig.PUFFBALL_MULTIPLIER.get().floatValue();
            int count = MinoshroomConfig.PUFFBALL_COUNT.get();
            int fuse = MinoshroomConfig.PUFFBALL_FUSE_TICKS.get();
            for (int i = 0; i < count; i++) {
                Vec3 aim = target.position();
                if (i > 0) {
                    double angle = (Math.PI * 2.0D * i) / Math.max(1, count - 1) + random.nextDouble() * 0.6D;
                    double distance = 1.0D + random.nextDouble() * (PUFF_SPREAD - 1.0D);
                    aim = aim.add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
                    double ground = NagaBossController.findGround(level, aim.x, target.getY(), aim.z);
                    if (!Double.isNaN(ground)) {
                        aim = new Vec3(aim.x, ground, aim.z);
                    }
                }
                double ticks = PUFF_FLIGHT + i * 2;
                double vx = (aim.x - from.x) / ticks;
                double vz = (aim.z - from.z) / ticks;
                double vy = (aim.y - from.y + Puffball.GRAVITY * ticks * (ticks - 1.0D) * 0.5D) / ticks;
                level.addFreshEntity(new Puffball(level, this.mob, from, new Vec3(vx, vy, vz), damage, fuse));
            }
            this.mob.swing(InteractionHand.MAIN_HAND);
            this.mob.playSound(SoundEvents.SNOWBALL_THROW, 2.0F, 0.4F);
            this.puffCooldown = MinoshroomConfig.PUFFBALL_COOLDOWN_TICKS.get();
            this.debug("Spore Burst threw {} puffballs, damage {}", count, damage);
        }
        if (this.actionTicks >= this.actionDuration) {
            if (this.puffCooldown <= 0) {
                this.puffCooldown = MinoshroomConfig.PUFFBALL_COOLDOWN_TICKS.get();
            }
            this.finishAction();
        }
    }

    private void startRoar(boolean forStampede) {
        this.pendingRoar = false;
        this.roarStampede = forStampede;
        this.startAction(Action.ROAR, ROAR_TICKS);
        this.mob.setAggressive(true);
        this.mob.playSound(TFSounds.MINOSHROOM_AMBIENT.get(), 3.0F, 0.6F);
        ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.ROAR, this.mob.getId(),
            (float) this.mob.getX(), (float) this.mob.getY(), (float) this.mob.getZ()));
        if (forStampede) {
            ModNetwork.sendTracking(this.mob, new MinoFxPacket(MinoFxPacket.HORN, this.mob.getId(), 1.0F));
        }
        this.debug("Roar started, stampede {}", forStampede);
    }

    private void tickRoar(@Nullable LivingEntity target) {
        this.holdStill();
        this.faceTarget(target);
        if (this.actionTicks < this.actionDuration) {
            return;
        }
        if (!this.roarStampede) {
            this.finishAction();
            return;
        }
        this.roarStampede = false;
        this.action = Action.NONE;
        this.stampede = true;
        this.stampedeLeft = Math.max(0, MinoshroomConfig.STAMPEDE_CHARGES.get() - 1);
        if (target == null || !this.startCharge(target, true, 3)) {
            this.finishStampede();
            return;
        }
        this.debug("Crimson Stampede started with {} charges", this.stampedeLeft + 1);
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Minoshroom " + this.mob.getId() + "] " + message, args);
        }
    }
}
