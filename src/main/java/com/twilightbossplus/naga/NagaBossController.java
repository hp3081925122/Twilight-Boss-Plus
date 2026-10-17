package com.twilightbossplus.naga;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.entity.VenomGlob;
import com.twilightbossplus.entity.VenomPool;
import com.twilightbossplus.mixin.NagaMovementPatternAccessor;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.network.NagaFxPacket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;
import twilightforest.entity.boss.NagaSegment;
import twilightforest.init.TFSounds;

public final class NagaBossController {
    private static final UUID SHED_SPEED_UUID = UUID.fromString("6c2f3b1e-8a4d-4f0e-9b57-2d1c7e5a9f31");
    private static final int GLOBAL_GAP = 40;

    private static final int FANG_WINDUP = 12;
    private static final int FANG_LUNGE = 5;
    private static final int FANG_RECOIL = 7;
    private static final double FANG_MIN_RANGE = 3.0D;
    private static final double FANG_MAX_RANGE = 14.0D;
    private static final float FANG_CHANCE = 0.55F;
    private static final double FANG_REACH = 2.4D;
    private static final float FANG_HALF_CONE = 35.0F;
    private static final double FANG_LUNGE_SPEED = 0.75D;
    private static final double FANG_KNOCKBACK = 0.35D;
    private static final int FANG_POISON_TICKS = 60;

    private static final int BARRAGE_WINDUP = 15;
    private static final int BARRAGE_FLIGHT = 18;
    private static final double BARRAGE_MIN_RANGE = 4.0D;
    private static final double BARRAGE_MAX_RANGE = 24.0D;

    private static final int SWEEP_WARN = 12;
    private static final int SWEEP_STRIKE = 6;
    private static final int SWEEP_PAUSE = 3;
    private static final int SWEEP_RECOVER = 6;
    private static final float SWEEP_RADIUS = 6.0F;
    private static final float SWEEP_HALF_ARC = 60.0F;
    private static final int TAIL_LENGTH = 4;
    private static final double SWEEP_TRIGGER_RANGE = 4.0D;
    private static final float SWEEP_FRONT_ANGLE = 60.0F;
    private static final double SWEEP_KNOCKBACK = 0.8D;

    private static final int MOLT_DELAY = 30;
    private static final double MOLT_SPEED = 0.35D;
    private static final double MOLT_RADIUS = 9.0D;
    private static final double MOLT_KNOCKBACK = 0.8D;
    private static final double SHED_SPEED_BONUS = 0.15D;

    private static final int REND_WINDUP = 10;
    private static final double REND_SPEED = 0.6D;
    private static final float REND_TURN = 6.0F;
    private static final double REND_MAX = 24.0D;
    private static final double REND_MIN_RANGE = 5.0D;
    private static final double SPIKE_SPACING = 1.8D;
    private static final int SPIKE_DELAY = 10;
    private static final double SPIKE_RADIUS = 1.0D;
    private static final double SPIKE_LIFT = 0.4D;
    private static final int JAW_WARN = 15;
    private static final double JAW_RADIUS = 2.0D;
    private static final double JAW_KNOCKBACK = 0.6D;
    private static final double JAW_LIFT = 0.6D;

    private static final int TEMPEST_WINDUP = 20;
    private static final int TEMPEST_RINGS = 3;
    private static final int TEMPEST_INTERVAL = 14;
    private static final double TEMPEST_RANGE = 16.0D;
    private static final double TEMPEST_SPEED = 0.45D;
    private static final double TEMPEST_START = 1.0D;
    private static final double TEMPEST_MAX = 16.0D;
    private static final float TEMPEST_GAP_HALF = 15.0F;
    private static final double TEMPEST_HEIGHT = 1.2D;
    private static final double TEMPEST_KNOCKBACK = 0.5D;

    private static final int CONSTRICT_DURATION = 120;
    private static final int CONSTRICT_WARMUP = 20;
    private static final float CONSTRICT_START_RADIUS = 9.0F;
    private static final float CONSTRICT_END_RADIUS = 4.0F;
    private static final double CONSTRICT_TRIGGER_RANGE = 14.0D;
    private static final int CONSTRICT_HITS = 2;
    private static final int CONSTRICT_DAZE_TICKS = 60;

    private static final float DECAL_VENOM = 0.0F;
    private static final float DECAL_JAW = 1.0F;
    private static final float IMPACT_FANG = 0.0F;
    private static final float IMPACT_SPIKE = 1.0F;
    private static final float IMPACT_TEMPEST = 2.0F;
    private static final float IMPACT_WAVE = 3.0F;

    private final Naga naga;
    private boolean shed;
    private int invulnerableTicks;
    private int globalCooldown = GLOBAL_GAP;
    private int fangCooldown = 40;
    private int barrageCooldown;
    private int sweepCooldown = 60;
    private int devourerCooldown = 60;
    private int tempestCooldown = 80;
    private int constrictCooldown = 100;
    private int clock;
    @Nullable
    private NagaMovementPattern.MovementState lastState;

    private int fangTicks = -1;
    private int fangBites;
    private Vec3 fangDir = Vec3.ZERO;
    private final Set<Integer> fangHit = new HashSet<>();
    @Nullable
    private LivingEntity fangTarget;

    private int barrageTicks = -1;
    private final List<Vec3> barrageTargets = new ArrayList<>();
    private final List<Integer> barrageFlights = new ArrayList<>();

    private int sweepTicks = -1;
    private boolean sweepDouble;
    private Vec3 sweepPivot = Vec3.ZERO;
    private float sweepYaw;
    private float sweepSign = 1.0F;

    private int moltTicks = -1;
    private int moltWaveTicks = -1;
    private Vec3 moltCenter = Vec3.ZERO;
    private final Set<Integer> moltHit = new HashSet<>();

    private int rendWindup = -1;
    private boolean crackActive;
    private Vec3 crackPos = Vec3.ZERO;
    private float crackYaw;
    private double crackTravel;
    private double nextSpikeAt;
    @Nullable
    private LivingEntity rendTarget;
    private final List<double[]> spikes = new ArrayList<>();
    private int jawTicks = -1;
    private Vec3 jawPos = Vec3.ZERO;
    private float jawYaw;

    private int tempestTicks = -1;
    private Vec3 tempestCenter = Vec3.ZERO;
    private final List<TempestRing> tempestRings = new ArrayList<>();

    private int constrictTicks = -1;
    private Vec3 constrictCenter = Vec3.ZERO;
    private double constrictAngle;
    private int weakIndex = -1;
    private int weakHits;
    private boolean constrictBroken;

    public NagaBossController(Naga naga) {
        this.naga = naga;
    }

    public boolean isInvulnerable() {
        return NagaConfig.ENABLED.get() && this.invulnerableTicks > 0;
    }

    public boolean isMovementLocked() {
        return NagaConfig.ENABLED.get() && (this.sweepTicks >= 0 || this.rendWindup >= 0 || this.constrictTicks >= 0 || this.fangTicks >= 0
            || this.tempestTicks >= 0);
    }

    public int getPhase() {
        float ratio = this.naga.getHealth() / this.naga.getMaxHealth();
        if (ratio <= NagaConfig.PHASE_THREE_HEALTH.get()) {
            return 3;
        }
        if (ratio <= NagaConfig.PHASE_TWO_HEALTH.get()) {
            return 2;
        }
        return 1;
    }

    public float getAttackDamage() {
        return (float) this.naga.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("TwilightBossPlusShed", this.shed);
    }

    public void load(CompoundTag tag) {
        this.shed = tag.getBoolean("TwilightBossPlusShed");
    }

    public void tick() {
        if (!NagaConfig.ENABLED.get()) {
            return;
        }
        if (this.naga.isDeadOrDying()) {
            if (this.constrictTicks >= 0) {
                this.endConstrict(false);
            }
            this.resetSkills();
            return;
        }
        this.clock++;
        if (this.invulnerableTicks > 0) {
            this.invulnerableTicks--;
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        if (this.fangCooldown > 0) {
            this.fangCooldown--;
        }
        if (this.barrageCooldown > 0) {
            this.barrageCooldown--;
        }
        if (this.sweepCooldown > 0) {
            this.sweepCooldown--;
        }
        if (this.devourerCooldown > 0) {
            this.devourerCooldown--;
        }
        if (this.tempestCooldown > 0) {
            this.tempestCooldown--;
        }
        if (this.constrictCooldown > 0) {
            this.constrictCooldown--;
        }

        int phase = this.getPhase();
        if (!this.shed && phase >= 2) {
            this.startShed();
        }

        LivingEntity target = this.naga.getTarget();
        boolean targetValid = target != null && target.isAlive() && this.naga.areSelfAndTargetInHome(target);
        NagaMovementPattern ai = this.naga.getMovementAI();
        NagaMovementPattern.MovementState state = ai.getState();

        this.tickFang(targetValid, target, ai);
        this.tickBarrage(targetValid, target, ai, state);
        this.tickSweep();
        this.tickMolt();
        this.tickRendWindup(targetValid, target);
        this.tickCrack();
        this.tickSpikes();
        this.tickJaw();
        this.tickTempest(targetValid, target, ai);
        this.tickTempestRings();
        this.tickConstrict(targetValid, ai);

        if (targetValid && this.canStartSkill(state)) {
            this.chooseSkill(target, ai, state, phase);
        }
        this.lastState = state;
    }

    public void afterMove() {
        if (!NagaConfig.ENABLED.get() || this.sweepTicks < SWEEP_WARN) {
            return;
        }
        float progress = this.sweepProgress();
        if (progress < 0.0F) {
            return;
        }
        List<NagaSegment> segments = this.getActiveSegments();
        int count = segments.size();
        if (count < TAIL_LENGTH + 1) {
            return;
        }
        float angle = this.sweepYaw + this.sweepSign * (-SWEEP_HALF_ARC + SWEEP_HALF_ARC * 2.0F * progress);
        float trail = this.sweepSign;
        if (this.sweepDouble && this.sweepTicks > SWEEP_WARN + SWEEP_STRIKE) {
            trail = -this.sweepSign;
        }
        for (int k = 0; k < TAIL_LENGTH; k++) {
            NagaSegment segment = segments.get(count - TAIL_LENGTH + k);
            float segmentAngle = angle - trail * k * 10.0F;
            Vec3 dir = directionOf(segmentAngle);
            double distance = 2.0D * (k + 1);
            segment.setPos(this.sweepPivot.x + dir.x * distance, segment.getY(), this.sweepPivot.z + dir.z * distance);
            segment.setRot(segmentAngle + 180.0F, 0.0F);
        }
    }

    public void onSegmentHurt(NagaSegment segment) {
        if (!NagaConfig.ENABLED.get() || this.constrictTicks < 0 || this.constrictBroken || this.weakIndex < 0) {
            return;
        }
        PartEntity<?>[] parts = this.naga.getParts();
        if (parts == null || this.weakIndex >= parts.length || parts[this.weakIndex] != segment) {
            return;
        }
        this.weakHits++;
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.WEAK_HIT, this.naga.getId(), this.weakHits));
        this.naga.level().playSound(null, segment.getX(), segment.getY() + 1.0D, segment.getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        this.naga.level().playSound(null, segment.getX(), segment.getY() + 1.0D, segment.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5F, 0.8F);
        this.debug("Weak scale hit {}/{}", this.weakHits, CONSTRICT_HITS);
        if (this.weakHits >= CONSTRICT_HITS) {
            this.constrictBroken = true;
        }
    }

    private void resetSkills() {
        this.fangTicks = -1;
        this.fangTarget = null;
        this.barrageTicks = -1;
        this.sweepTicks = -1;
        this.moltTicks = -1;
        this.moltWaveTicks = -1;
        this.rendWindup = -1;
        this.crackActive = false;
        this.rendTarget = null;
        this.spikes.clear();
        this.jawTicks = -1;
        this.tempestTicks = -1;
        this.tempestRings.clear();
    }

    private boolean canStartSkill(NagaMovementPattern.MovementState state) {
        return this.sweepTicks < 0
            && this.rendWindup < 0
            && this.constrictTicks < 0
            && this.barrageTicks < 0
            && this.fangTicks < 0
            && this.tempestTicks < 0
            && this.globalCooldown <= 0
            && !this.naga.isDazed()
            && state != NagaMovementPattern.MovementState.DAZE;
    }

    private void chooseSkill(LivingEntity target, NagaMovementPattern ai, NagaMovementPattern.MovementState state, int phase) {
        double distance = this.naga.distanceTo(target);
        if (state == NagaMovementPattern.MovementState.CIRCLE) {
            if (phase >= 3 && this.constrictCooldown <= 0 && distance < CONSTRICT_TRIGGER_RANGE) {
                this.startConstrict(target);
                return;
            }
            boolean tempestReady = phase >= 2 && this.tempestCooldown <= 0 && distance < TEMPEST_RANGE;
            boolean devourerReady = phase >= 2 && this.devourerCooldown <= 0 && !this.crackActive && this.jawTicks < 0
                && distance > REND_MIN_RANGE && distance < REND_MAX;
            if (tempestReady && devourerReady) {
                if (this.naga.getRandom().nextBoolean()) {
                    devourerReady = false;
                } else {
                    tempestReady = false;
                }
            }
            if (tempestReady) {
                this.startTempest(target);
                return;
            }
            if (devourerReady) {
                this.startRend(target);
                return;
            }
        }
        boolean attacking = state == NagaMovementPattern.MovementState.INTIMIDATE
            || state == NagaMovementPattern.MovementState.CHARGE
            || state == NagaMovementPattern.MovementState.STUNLESS_CHARGE;
        if (this.sweepCooldown <= 0 && !attacking && this.isTargetNearTail(target)) {
            this.startSweep(target, phase);
            return;
        }
        if (state != NagaMovementPattern.MovementState.INTIMIDATE || this.lastState == NagaMovementPattern.MovementState.INTIMIDATE) {
            return;
        }
        if (this.fangCooldown <= 0 && distance > FANG_MIN_RANGE && distance < FANG_MAX_RANGE && this.naga.getRandom().nextFloat() < FANG_CHANCE) {
            this.startFang(target, phase);
            return;
        }
        if (this.barrageCooldown <= 0 && distance > BARRAGE_MIN_RANGE && distance < BARRAGE_MAX_RANGE) {
            this.startBarrage(target, ai, phase);
        }
    }

    private Vec3 mouthPos() {
        Vec3 dir = directionOf(this.naga.getYRot());
        return this.naga.position().add(0.0D, this.naga.getBbHeight() * 0.55D, 0.0D).add(dir.scale(this.naga.getBbWidth() * 0.6D));
    }

    private void face(Vec3 dir) {
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.naga.setYRot(yaw);
        this.naga.yBodyRot = yaw;
        this.naga.yHeadRot = yaw;
    }

    private boolean strike(LivingEntity living, float damage, double dirX, double dirZ, double knockback, double lift) {
        if (!living.hurt(this.naga.damageSources().mobAttack(this.naga), damage)) {
            return false;
        }
        if (knockback > 0.0D && dirX * dirX + dirZ * dirZ > 1.0E-6D) {
            living.knockback(knockback, -dirX, -dirZ);
        }
        double scaled = lift * (1.0D - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (scaled > 0.0D) {
            Vec3 motion = living.getDeltaMovement();
            living.setDeltaMovement(motion.x, Math.max(motion.y, scaled), motion.z);
            living.hurtMarked = true;
        }
        return true;
    }

    private void impact(Vec3 pos, float kind) {
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.IMPACT, this.naga.getId(), (float) pos.x, (float) pos.y, (float) pos.z, kind));
    }

    private void sound(Vec3 pos, SoundEvent sound, float volume, float pitch) {
        this.naga.level().playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    private void startFang(LivingEntity target, int phase) {
        this.fangTicks = 0;
        this.fangBites = 3;
        if (phase >= 3) {
            this.fangBites = 4;
        }
        this.fangTarget = target;
        this.naga.getNavigation().stop();
        this.naga.getLookControl().setLookAt(target, 30.0F, 30.0F);
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.FANG_WINDUP, this.naga.getId(), FANG_WINDUP, this.fangBites));
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 3.0F, 1.5F);
        this.debug("Fang Rush windup toward {}, {} bites", target.getName().getString(), this.fangBites);
    }

    private void tickFang(boolean targetValid, @Nullable LivingEntity target, NagaMovementPattern ai) {
        if (this.fangTicks < 0) {
            return;
        }
        if (!targetValid || target != this.fangTarget || this.naga.isDazed()) {
            this.endFang(ai);
            this.debug("Fang Rush interrupted");
            return;
        }
        this.naga.getNavigation().stop();
        int tick = this.fangTicks;
        this.fangTicks++;
        Vec3 toTarget = new Vec3(target.getX() - this.naga.getX(), 0.0D, target.getZ() - this.naga.getZ());
        if (tick < FANG_WINDUP) {
            if (toTarget.lengthSqr() > 1.0E-4D) {
                this.face(toTarget.normalize());
            }
            Vec3 motion = this.naga.getDeltaMovement();
            this.naga.setDeltaMovement(motion.x * 0.5D, motion.y, motion.z * 0.5D);
            return;
        }
        int local = tick - FANG_WINDUP;
        int bite = local / (FANG_LUNGE + FANG_RECOIL);
        int step = local % (FANG_LUNGE + FANG_RECOIL);
        if (bite >= this.fangBites) {
            this.endFang(ai);
            this.debug("Fang Rush finished");
            return;
        }
        if (step == 0) {
            if (toTarget.lengthSqr() > 1.0E-4D) {
                this.fangDir = toTarget.normalize();
            } else {
                this.fangDir = directionOf(this.naga.getYRot());
            }
            this.face(this.fangDir);
            this.fangHit.clear();
            Vec3 mouth = this.mouthPos();
            ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.FANG_BITE, this.naga.getId(),
                (float) mouth.x, (float) mouth.y, (float) mouth.z, this.naga.getYRot(), bite));
            this.naga.playSound(SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.7F + bite * 0.12F);
            this.naga.playSound(TFSounds.NAGA_HISS.get(), 1.5F, 1.8F);
        }
        if (step < FANG_LUNGE) {
            Vec3 motion = this.naga.getDeltaMovement();
            this.naga.setDeltaMovement(this.fangDir.x * FANG_LUNGE_SPEED, motion.y, this.fangDir.z * FANG_LUNGE_SPEED);
            this.fangStrike();
        } else {
            Vec3 motion = this.naga.getDeltaMovement();
            this.naga.setDeltaMovement(-this.fangDir.x * 0.12D, motion.y, -this.fangDir.z * 0.12D);
            if (toTarget.lengthSqr() > 1.0E-4D) {
                Vec3 look = directionOf(this.naga.getYRot());
                Vec3 desired = toTarget.normalize();
                this.face(look.add(desired.subtract(look).scale(0.35D)).normalize());
            }
        }
    }

    private void fangStrike() {
        Vec3 mouth = this.mouthPos();
        float damage = this.getAttackDamage() * NagaConfig.FANG_RUSH_MULTIPLIER.get().floatValue();
        AABB area = new AABB(mouth, mouth).inflate(FANG_REACH + 1.0D, 2.0D, FANG_REACH + 1.0D);
        for (LivingEntity living : this.naga.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (living instanceof Naga || !living.isAlive() || this.fangHit.contains(living.getId())) {
                continue;
            }
            double dx = living.getX() - mouth.x;
            double dz = living.getZ() - mouth.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > FANG_REACH + living.getBbWidth() * 0.5D) {
                continue;
            }
            if (distance > 1.2D) {
                double cos = (dx * this.fangDir.x + dz * this.fangDir.z) / distance;
                if (cos < Mth.cos(FANG_HALF_CONE * Mth.DEG_TO_RAD)) {
                    continue;
                }
            }
            this.fangHit.add(living.getId());
            if (this.strike(living, damage, this.fangDir.x, this.fangDir.z, FANG_KNOCKBACK, 0.0D)) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, FANG_POISON_TICKS, 0), this.naga);
                this.impact(living.position().add(0.0D, living.getBbHeight() * 0.6D, 0.0D), IMPACT_FANG);
                this.debug("Fang Rush bit {} for {}", living.getName().getString(), damage);
            }
        }
    }

    private void endFang(NagaMovementPattern ai) {
        this.fangTicks = -1;
        this.fangTarget = null;
        this.fangCooldown = NagaConfig.FANG_RUSH_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        ai.forceCircle();
    }

    private void startBarrage(LivingEntity target, NagaMovementPattern ai, int phase) {
        int count = 5;
        if (phase >= 2) {
            count = 7;
        }
        this.barrageTicks = 0;
        this.barrageTargets.clear();
        this.barrageFlights.clear();
        ((NagaMovementPatternAccessor) ai).setStateCounter(BARRAGE_WINDUP + 10);
        Vec3 predicted = target.position().add(target.getDeltaMovement().multiply(BARRAGE_FLIGHT * 0.6D, 0.0D, BARRAGE_FLIGHT * 0.6D));
        Vec3 forward = new Vec3(predicted.x - this.naga.getX(), 0.0D, predicted.z - this.naga.getZ());
        if (forward.lengthSqr() < 1.0E-4D) {
            forward = directionOf(this.naga.getYRot());
        }
        forward = forward.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        float middle = (count - 1) * 0.5F;
        for (int i = 0; i < count; i++) {
            Vec3 point = predicted;
            if (i > 0) {
                double lateral = (i - middle) * 1.7D + (this.naga.getRandom().nextDouble() - 0.5D) * 0.8D;
                double depth = (this.naga.getRandom().nextDouble() - 0.5D) * 2.4D;
                point = predicted.add(side.scale(lateral)).add(forward.scale(depth));
            }
            double ground = findGround(this.naga.level(), point.x, point.y, point.z);
            if (!Double.isNaN(ground)) {
                point = new Vec3(point.x, ground, point.z);
            }
            int flight = BARRAGE_FLIGHT + Math.round(Math.abs(i - middle) * 2.0F);
            this.barrageTargets.add(point);
            this.barrageFlights.add(flight);
            ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.DECAL, this.naga.getId(),
                (float) point.x, (float) point.y, (float) point.z, VenomPool.RADIUS, BARRAGE_WINDUP + flight, DECAL_VENOM));
        }
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.BARRAGE_WINDUP, this.naga.getId(), BARRAGE_WINDUP));
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 3.0F, 1.3F);
        this.debug("Venom Barrage windup toward {}, {} globs", target.getName().getString(), count);
    }

    private void tickBarrage(boolean targetValid, @Nullable LivingEntity target, NagaMovementPattern ai, NagaMovementPattern.MovementState state) {
        if (this.barrageTicks < 0) {
            return;
        }
        if (!targetValid || state != NagaMovementPattern.MovementState.INTIMIDATE) {
            this.barrageTicks = -1;
            this.barrageCooldown = NagaConfig.VENOM_BARRAGE_COOLDOWN_TICKS.get() / 2;
            this.debug("Venom Barrage interrupted");
            return;
        }
        this.barrageTicks++;
        this.naga.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.barrageTicks < BARRAGE_WINDUP) {
            return;
        }
        Vec3 mouth = this.mouthPos();
        float damage = this.getAttackDamage() * NagaConfig.VENOM_BARRAGE_MULTIPLIER.get().floatValue();
        for (int i = 0; i < this.barrageTargets.size(); i++) {
            VenomGlob glob = new VenomGlob(this.naga.level(), this.naga, damage);
            glob.setPos(mouth.x, mouth.y, mouth.z);
            glob.setDeltaMovement(VenomGlob.solveVelocity(mouth, this.barrageTargets.get(i).add(0.0D, 0.1D, 0.0D), this.barrageFlights.get(i)));
            this.naga.level().addFreshEntity(glob);
        }
        this.naga.playSound(SoundEvents.LLAMA_SPIT, 3.0F, 0.5F);
        this.naga.playSound(SoundEvents.SLIME_SQUISH, 2.0F, 0.6F);
        this.barrageTicks = -1;
        this.barrageCooldown = NagaConfig.VENOM_BARRAGE_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        ai.forceCircle();
        this.debug("Venom Barrage fired {} globs, damage {}", this.barrageTargets.size(), damage);
    }

    private boolean isTargetNearTail(LivingEntity target) {
        List<NagaSegment> segments = this.getActiveSegments();
        int count = segments.size();
        if (count < TAIL_LENGTH + 1) {
            return false;
        }
        float toTarget = yawTo(this.naga.position(), target.position());
        if (Math.abs(Mth.wrapDegrees(toTarget - this.naga.getYRot())) < SWEEP_FRONT_ANGLE) {
            return false;
        }
        for (int i = count / 2; i < count; i++) {
            if (segments.get(i).distanceToSqr(target) < SWEEP_TRIGGER_RANGE * SWEEP_TRIGGER_RANGE) {
                return true;
            }
        }
        return false;
    }

    private void startSweep(LivingEntity target, int phase) {
        List<NagaSegment> segments = this.getActiveSegments();
        int count = segments.size();
        this.sweepPivot = segments.get(count - TAIL_LENGTH - 1).position();
        this.sweepYaw = yawTo(this.sweepPivot, target.position());
        float tailYaw = yawTo(this.sweepPivot, segments.get(count - 1).position());
        if (Mth.wrapDegrees(tailYaw - this.sweepYaw) < 0.0F) {
            this.sweepSign = 1.0F;
        } else {
            this.sweepSign = -1.0F;
        }
        this.sweepDouble = phase >= 2;
        this.sweepTicks = 0;
        this.naga.getNavigation().stop();
        float doubleFlag = 0.0F;
        if (this.sweepDouble) {
            doubleFlag = 1.0F;
        }
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.SWEEP, this.naga.getId(),
            (float) this.sweepPivot.x, (float) this.sweepPivot.y, (float) this.sweepPivot.z,
            this.sweepYaw, SWEEP_RADIUS, SWEEP_HALF_ARC, SWEEP_WARN, SWEEP_STRIKE, this.sweepSign, doubleFlag, SWEEP_PAUSE));
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 2.5F, 1.4F);
        this.debug("Tail Reaver windup, yaw {}, double {}", this.sweepYaw, this.sweepDouble);
    }

    private float sweepProgress() {
        int t = this.sweepTicks - SWEEP_WARN;
        if (t < 0) {
            return -1.0F;
        }
        if (t <= SWEEP_STRIKE) {
            return t / (float) SWEEP_STRIKE;
        }
        if (!this.sweepDouble) {
            return -1.0F;
        }
        t -= SWEEP_STRIKE;
        if (t <= SWEEP_PAUSE) {
            return 1.0F;
        }
        t -= SWEEP_PAUSE;
        if (t <= SWEEP_STRIKE) {
            return 1.0F - t / (float) SWEEP_STRIKE;
        }
        return -1.0F;
    }

    private void tickSweep() {
        if (this.sweepTicks < 0) {
            return;
        }
        this.sweepTicks++;
        this.naga.getNavigation().stop();
        int secondStart = SWEEP_WARN + SWEEP_STRIKE + SWEEP_PAUSE;
        if (this.sweepTicks == SWEEP_WARN || (this.sweepDouble && this.sweepTicks == secondStart)) {
            this.naga.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5F, 0.5F);
        }
        if (this.sweepTicks == SWEEP_WARN + SWEEP_STRIKE / 2 || (this.sweepDouble && this.sweepTicks == secondStart + SWEEP_STRIKE / 2)) {
            float damage = this.getAttackDamage() * NagaConfig.TAIL_REAVER_MULTIPLIER.get().floatValue();
            AABB area = new AABB(this.sweepPivot, this.sweepPivot).inflate(SWEEP_RADIUS, 3.0D, SWEEP_RADIUS);
            int hits = 0;
            for (LivingEntity living : this.naga.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (living instanceof Naga || !living.isAlive()) {
                    continue;
                }
                double dx = living.getX() - this.sweepPivot.x;
                double dz = living.getZ() - this.sweepPivot.z;
                if (dx * dx + dz * dz > SWEEP_RADIUS * SWEEP_RADIUS) {
                    continue;
                }
                float angle = yawTo(this.sweepPivot, living.position());
                if (Math.abs(Mth.wrapDegrees(angle - this.sweepYaw)) > SWEEP_HALF_ARC + 10.0F) {
                    continue;
                }
                living.invulnerableTime = 0;
                if (this.strike(living, damage, dx, dz, SWEEP_KNOCKBACK, 0.0D)) {
                    hits++;
                }
            }
            this.debug("Tail Reaver hit {} targets for {}", hits, damage);
        }
        int total = SWEEP_WARN + SWEEP_STRIKE + SWEEP_RECOVER;
        if (this.sweepDouble) {
            total += SWEEP_PAUSE + SWEEP_STRIKE;
        }
        if (this.sweepTicks >= total) {
            this.sweepTicks = -1;
            this.sweepCooldown = NagaConfig.TAIL_REAVER_COOLDOWN_TICKS.get();
            this.globalCooldown = GLOBAL_GAP;
        }
    }

    private void startShed() {
        this.shed = true;
        this.invulnerableTicks = NagaConfig.PHASE_TRANSITION_INVULNERABLE_TICKS.get();
        if (this.constrictTicks >= 0) {
            this.endConstrict(false);
        }
        this.sweepTicks = -1;
        this.rendWindup = -1;
        this.fangTicks = -1;
        this.fangTarget = null;
        this.barrageTicks = -1;
        this.tempestTicks = -1;

        AttributeInstance speed = this.naga.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SHED_SPEED_UUID) == null) {
            speed.addPermanentModifier(new AttributeModifier(SHED_SPEED_UUID, "Naga shed speed boost", SHED_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        }

        List<NagaSegment> segments = this.getActiveSegments();
        float[] data = new float[(segments.size() + 1) * 6];
        data[0] = (float) this.naga.getX();
        data[1] = (float) this.naga.getY();
        data[2] = (float) this.naga.getZ();
        data[3] = this.naga.getYRot();
        data[4] = 0.0F;
        data[5] = this.naga.getBbWidth();
        double sumX = this.naga.getX();
        double sumZ = this.naga.getZ();
        for (int i = 0; i < segments.size(); i++) {
            NagaSegment segment = segments.get(i);
            int offset = (i + 1) * 6;
            data[offset] = (float) segment.getX();
            data[offset + 1] = (float) segment.getY();
            data[offset + 2] = (float) segment.getZ();
            data[offset + 3] = segment.getYRot();
            data[offset + 4] = segment.getXRot();
            data[offset + 5] = segment.getBbWidth();
            sumX += segment.getX();
            sumZ += segment.getZ();
        }
        int pieces = segments.size() + 1;
        double centerX = sumX / pieces;
        double centerZ = sumZ / pieces;
        double ground = findGround(this.naga.level(), centerX, this.naga.getY() + 1.0D, centerZ);
        if (Double.isNaN(ground)) {
            ground = this.naga.getY();
        }
        this.moltCenter = new Vec3(centerX, ground, centerZ);
        this.moltTicks = 0;
        this.moltWaveTicks = -1;
        this.moltHit.clear();
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.SHED, this.naga.getId(), data));
        this.naga.playSound(TFSounds.NAGA_HURT.get(), 2.0F, 0.55F);
        this.naga.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.5F);
        this.naga.getMovementAI().forceCircle();
        this.debug("Molt Burst shed, invulnerable for {} ticks, {} segments", this.invulnerableTicks, segments.size());
    }

    private void tickMolt() {
        if (this.moltTicks >= 0) {
            this.moltTicks++;
            if (this.moltTicks >= MOLT_DELAY) {
                this.moltTicks = -1;
                this.moltWaveTicks = 0;
                ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.MOLT_BURST, this.naga.getId(),
                    (float) this.moltCenter.x, (float) this.moltCenter.y, (float) this.moltCenter.z, (float) MOLT_RADIUS, (float) MOLT_SPEED));
                this.sound(this.moltCenter, SoundEvents.GENERIC_EXPLODE, 2.0F, 0.7F);
                this.sound(this.moltCenter, SoundEvents.SLIME_SQUISH, 3.0F, 0.5F);
                this.debug("Molt Burst wave released at {}", this.moltCenter);
            }
        }
        if (this.moltWaveTicks < 0) {
            return;
        }
        this.moltWaveTicks++;
        double radius = 0.5D + this.moltWaveTicks * MOLT_SPEED;
        if (radius > MOLT_RADIUS + 0.2D) {
            this.moltWaveTicks = -1;
            return;
        }
        float damage = this.getAttackDamage() * NagaConfig.MOLT_BURST_MULTIPLIER.get().floatValue();
        AABB area = new AABB(this.moltCenter, this.moltCenter).inflate(radius + 1.0D, 3.0D, radius + 1.0D);
        for (LivingEntity living : this.naga.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (living instanceof Naga || !living.isAlive() || this.moltHit.contains(living.getId())) {
                continue;
            }
            double dx = living.getX() - this.moltCenter.x;
            double dz = living.getZ() - this.moltCenter.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < radius - 0.8D || distance > radius + 0.2D) {
                continue;
            }
            if (living.getY() > this.moltCenter.y + 1.0D || living.getY() < this.moltCenter.y - 2.0D) {
                continue;
            }
            this.moltHit.add(living.getId());
            if (this.strike(living, damage, dx, dz, MOLT_KNOCKBACK, 0.0D)) {
                this.impact(living.position().add(0.0D, 0.4D, 0.0D), IMPACT_WAVE);
                this.debug("Molt Burst wave hit {} for {}", living.getName().getString(), damage);
            }
        }
    }

    private void startRend(LivingEntity target) {
        this.rendWindup = 0;
        this.rendTarget = target;
        this.naga.getNavigation().stop();
        Vec3 motion = this.naga.getDeltaMovement();
        this.naga.setDeltaMovement(motion.x * 0.2D, 0.55D, motion.z * 0.2D);
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 3.0F, 0.6F);
        this.debug("Earth Devourer windup toward {}", target.getName().getString());
    }

    private void tickRendWindup(boolean targetValid, @Nullable LivingEntity target) {
        if (this.rendWindup < 0) {
            return;
        }
        this.rendWindup++;
        this.naga.getNavigation().stop();
        if (targetValid) {
            this.naga.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.rendWindup < REND_WINDUP || (!this.naga.onGround() && this.rendWindup < REND_WINDUP + 15)) {
            return;
        }
        this.rendWindup = -1;
        this.devourerCooldown = NagaConfig.EARTH_DEVOURER_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        if (!targetValid) {
            return;
        }
        Vec3 start = this.naga.position().add(directionOf(this.naga.getYRot()).scale(1.5D));
        double ground = findGround(this.naga.level(), start.x, this.naga.getY(), start.z);
        if (Double.isNaN(ground)) {
            ground = this.naga.getY();
        }
        this.crackPos = new Vec3(start.x, ground, start.z);
        this.crackYaw = yawTo(this.crackPos, target.position());
        this.crackTravel = 0.0D;
        this.nextSpikeAt = SPIKE_SPACING;
        this.crackActive = true;
        this.rendTarget = target;
        this.sound(this.crackPos, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.6F);
        this.sound(this.crackPos, SoundEvents.RAVAGER_ROAR, 1.0F, 1.4F);
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.SLAM, this.naga.getId(),
            (float) this.crackPos.x, (float) this.crackPos.y, (float) this.crackPos.z, this.crackYaw));
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.REND_POINT, this.naga.getId(),
            (float) this.crackPos.x, (float) this.crackPos.y, (float) this.crackPos.z, this.crackYaw, 1.0F));
        this.debug("Earth Devourer slam at {}", this.crackPos);
    }

    private void tickCrack() {
        if (!this.crackActive) {
            return;
        }
        boolean targetAlive = this.rendTarget != null && this.rendTarget.isAlive();
        if (targetAlive) {
            float desired = yawTo(this.crackPos, this.rendTarget.position());
            this.crackYaw += Mth.clamp(Mth.wrapDegrees(desired - this.crackYaw), -REND_TURN, REND_TURN);
        }
        Vec3 dir = directionOf(this.crackYaw);
        double nextX = this.crackPos.x + dir.x * REND_SPEED;
        double nextZ = this.crackPos.z + dir.z * REND_SPEED;
        double ground = findGround(this.naga.level(), nextX, this.crackPos.y, nextZ);
        boolean finished = false;
        if (Double.isNaN(ground)) {
            finished = true;
        } else {
            this.crackPos = new Vec3(nextX, ground, nextZ);
            this.crackTravel += REND_SPEED;
            ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.REND_POINT, this.naga.getId(),
                (float) this.crackPos.x, (float) this.crackPos.y, (float) this.crackPos.z, this.crackYaw, 0.0F));
            if (this.crackTravel >= this.nextSpikeAt) {
                this.nextSpikeAt += SPIKE_SPACING;
                this.spikes.add(new double[] {this.crackPos.x, this.crackPos.y, this.crackPos.z, this.clock + SPIKE_DELAY});
                ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.SPIKE, this.naga.getId(),
                    (float) this.crackPos.x, (float) this.crackPos.y, (float) this.crackPos.z, SPIKE_DELAY, this.crackYaw));
                this.sound(this.crackPos, SoundEvents.ROOTED_DIRT_BREAK, 1.2F, 0.6F);
            }
        }
        if (targetAlive) {
            double dx = this.rendTarget.getX() - this.crackPos.x;
            double dz = this.rendTarget.getZ() - this.crackPos.z;
            if (dx * dx + dz * dz < 1.0D) {
                finished = true;
            }
        }
        if (this.crackTravel >= REND_MAX) {
            finished = true;
        }
        if (!finished) {
            return;
        }
        this.crackActive = false;
        this.rendTarget = null;
        this.jawPos = this.crackPos;
        this.jawYaw = this.crackYaw;
        this.jawTicks = 0;
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.DECAL, this.naga.getId(),
            (float) this.jawPos.x, (float) this.jawPos.y, (float) this.jawPos.z, (float) JAW_RADIUS, JAW_WARN, DECAL_JAW));
        this.sound(this.jawPos, TFSounds.NAGA_RATTLE.get(), 2.0F, 0.5F);
        this.debug("Earth Devourer tremor stopped after {} blocks", this.crackTravel);
    }

    private void tickSpikes() {
        if (this.spikes.isEmpty()) {
            return;
        }
        float damage = this.getAttackDamage() * NagaConfig.EARTH_DEVOURER_SPIKE_MULTIPLIER.get().floatValue();
        Iterator<double[]> iterator = this.spikes.iterator();
        while (iterator.hasNext()) {
            double[] spike = iterator.next();
            if (this.clock < spike[3]) {
                continue;
            }
            iterator.remove();
            Vec3 pos = new Vec3(spike[0], spike[1], spike[2]);
            this.sound(pos, SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 1.2F);
            AABB area = new AABB(pos.x - SPIKE_RADIUS - 0.5D, pos.y - 0.5D, pos.z - SPIKE_RADIUS - 0.5D,
                pos.x + SPIKE_RADIUS + 0.5D, pos.y + 2.5D, pos.z + SPIKE_RADIUS + 0.5D);
            for (LivingEntity living : this.naga.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (living instanceof Naga || !living.isAlive()) {
                    continue;
                }
                double dx = living.getX() - pos.x;
                double dz = living.getZ() - pos.z;
                if (dx * dx + dz * dz > (SPIKE_RADIUS + living.getBbWidth() * 0.5D) * (SPIKE_RADIUS + living.getBbWidth() * 0.5D)) {
                    continue;
                }
                if (this.strike(living, damage, dx, dz, 0.2D, SPIKE_LIFT)) {
                    this.impact(living.position().add(0.0D, 0.5D, 0.0D), IMPACT_SPIKE);
                    this.debug("Earth Devourer spike hit {} for {}", living.getName().getString(), damage);
                }
            }
        }
    }

    private void tickJaw() {
        if (this.jawTicks < 0) {
            return;
        }
        this.jawTicks++;
        if (this.jawTicks < JAW_WARN) {
            return;
        }
        this.jawTicks = -1;
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.BITE, this.naga.getId(),
            (float) this.jawPos.x, (float) this.jawPos.y, (float) this.jawPos.z, this.jawYaw));
        Level level = this.naga.level();
        this.sound(this.jawPos, SoundEvents.EVOKER_FANGS_ATTACK, 2.0F, 0.5F);
        this.sound(this.jawPos, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.8F);
        float damage = this.getAttackDamage() * NagaConfig.EARTH_DEVOURER_JAW_MULTIPLIER.get().floatValue();
        AABB area = new AABB(this.jawPos.x - JAW_RADIUS, this.jawPos.y - 1.0D, this.jawPos.z - JAW_RADIUS,
            this.jawPos.x + JAW_RADIUS, this.jawPos.y + 3.0D, this.jawPos.z + JAW_RADIUS);
        int hits = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (living instanceof Naga || !living.isAlive()) {
                continue;
            }
            double dx = living.getX() - this.jawPos.x;
            double dz = living.getZ() - this.jawPos.z;
            if (dx * dx + dz * dz > JAW_RADIUS * JAW_RADIUS) {
                continue;
            }
            living.invulnerableTime = 0;
            if (this.strike(living, damage, dx, dz, JAW_KNOCKBACK, JAW_LIFT)) {
                hits++;
            }
        }
        this.debug("Earth Devourer jaw hit {} targets for {}", hits, damage);
    }

    private void startTempest(LivingEntity target) {
        this.tempestTicks = 0;
        this.tempestCenter = this.naga.position();
        this.naga.getNavigation().stop();
        this.naga.getLookControl().setLookAt(target, 30.0F, 30.0F);
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.TEMPEST_WINDUP, this.naga.getId(), TEMPEST_WINDUP));
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 3.0F, 1.0F);
        this.naga.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.6F);
        this.debug("Scale Tempest windup toward {}", target.getName().getString());
    }

    private void tickTempest(boolean targetValid, @Nullable LivingEntity target, NagaMovementPattern ai) {
        if (this.tempestTicks < 0) {
            return;
        }
        if (this.naga.isDazed()) {
            this.endTempest(ai);
            this.debug("Scale Tempest interrupted");
            return;
        }
        this.tempestTicks++;
        this.naga.getNavigation().stop();
        Vec3 motion = this.naga.getDeltaMovement();
        this.naga.setDeltaMovement(motion.x * 0.3D, motion.y, motion.z * 0.3D);
        if (targetValid) {
            this.naga.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        int local = this.tempestTicks - TEMPEST_WINDUP;
        if (local >= 0 && local % TEMPEST_INTERVAL == 0 && local / TEMPEST_INTERVAL < TEMPEST_RINGS) {
            Vec3 center = this.naga.position();
            double ground = findGround(this.naga.level(), center.x, center.y + 1.0D, center.z);
            if (!Double.isNaN(ground)) {
                center = new Vec3(center.x, ground, center.z);
            }
            float base = this.naga.getRandom().nextFloat() * 360.0F;
            this.tempestRings.add(new TempestRing(center, base));
            ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.TEMPEST_WAVE, this.naga.getId(),
                (float) center.x, (float) center.y, (float) center.z, base, (float) TEMPEST_SPEED, (float) TEMPEST_MAX, TEMPEST_GAP_HALF,
                (float) TEMPEST_START));
            this.naga.playSound(SoundEvents.TRIDENT_THROW, 3.0F, 0.6F);
            this.naga.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.7F);
        }
        if (local >= TEMPEST_INTERVAL * (TEMPEST_RINGS - 1) + 6) {
            this.endTempest(ai);
            this.debug("Scale Tempest finished");
        }
    }

    private void endTempest(NagaMovementPattern ai) {
        this.tempestTicks = -1;
        this.tempestCooldown = NagaConfig.SCALE_TEMPEST_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        ai.forceCircle();
    }

    private void tickTempestRings() {
        if (this.tempestRings.isEmpty()) {
            return;
        }
        float damage = this.getAttackDamage() * NagaConfig.SCALE_TEMPEST_MULTIPLIER.get().floatValue();
        Iterator<TempestRing> iterator = this.tempestRings.iterator();
        while (iterator.hasNext()) {
            TempestRing ring = iterator.next();
            ring.age++;
            double radius = TEMPEST_START + ring.age * TEMPEST_SPEED;
            if (radius > TEMPEST_MAX + 0.3D) {
                iterator.remove();
                continue;
            }
            AABB area = new AABB(ring.center, ring.center).inflate(radius + 1.0D, 3.0D, radius + 1.0D);
            for (LivingEntity living : this.naga.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (living instanceof Naga || !living.isAlive() || ring.hit.contains(living.getId())) {
                    continue;
                }
                double dx = living.getX() - ring.center.x;
                double dz = living.getZ() - ring.center.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < radius - 0.6D || distance > radius + 0.3D) {
                    continue;
                }
                if (living.getY() >= ring.center.y + TEMPEST_HEIGHT || living.getY() < ring.center.y - 1.5D) {
                    continue;
                }
                float angle = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG);
                boolean inGap = false;
                for (int i = 0; i < 3; i++) {
                    if (Math.abs(Mth.wrapDegrees(angle - (ring.base + i * 120.0F))) < TEMPEST_GAP_HALF) {
                        inGap = true;
                    }
                }
                if (inGap) {
                    continue;
                }
                ring.hit.add(living.getId());
                if (this.strike(living, damage, dx, dz, TEMPEST_KNOCKBACK, 0.0D)) {
                    this.impact(living.position().add(0.0D, 0.6D, 0.0D), IMPACT_TEMPEST);
                    this.debug("Scale Tempest ring hit {} for {}", living.getName().getString(), damage);
                }
            }
        }
    }

    private void startConstrict(LivingEntity target) {
        this.constrictTicks = 0;
        this.constrictCenter = target.position();
        this.constrictAngle = Math.atan2(this.naga.getZ() - this.constrictCenter.z, this.naga.getX() - this.constrictCenter.x);
        List<NagaSegment> segments = this.getActiveSegments();
        this.weakIndex = segments.size() / 2;
        this.weakHits = 0;
        this.constrictBroken = false;
        this.naga.getNavigation().stop();
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.CONSTRICT_START, this.naga.getId(),
            (float) this.constrictCenter.x, (float) this.constrictCenter.y, (float) this.constrictCenter.z,
            CONSTRICT_START_RADIUS, CONSTRICT_END_RADIUS, CONSTRICT_DURATION, CONSTRICT_WARMUP, this.weakIndex));
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 4.0F, 0.7F);
        this.debug("Constrict started around {}, weak segment {}", this.constrictCenter, this.weakIndex);
    }

    private void tickConstrict(boolean targetValid, NagaMovementPattern ai) {
        if (this.constrictTicks < 0) {
            return;
        }
        if (this.constrictBroken) {
            this.endConstrict(true);
            this.naga.invulnerableTime = 0;
            float damage = this.naga.getMaxHealth() * NagaConfig.CONSTRICT_BREAK_DAMAGE_RATIO.get().floatValue();
            this.naga.hurt(this.naga.damageSources().generic(), damage);
            ai.doDaze();
            ((NagaMovementPatternAccessor) ai).setStateCounter(CONSTRICT_DAZE_TICKS);
            this.naga.playSound(TFSounds.NAGA_HURT.get(), 3.0F, 0.6F);
            this.debug("Constrict broken, Naga took {} damage", damage);
            return;
        }
        if (!targetValid || this.naga.isDazed()) {
            this.endConstrict(false);
            return;
        }
        this.constrictTicks++;
        float progress = Math.min(1.0F, this.constrictTicks / (float) CONSTRICT_DURATION);
        double radius = CONSTRICT_START_RADIUS + (CONSTRICT_END_RADIUS - CONSTRICT_START_RADIUS) * progress;
        this.constrictAngle += 0.75D / radius;
        double lead = this.constrictAngle + 0.5D;
        this.naga.getNavigation().moveTo(this.constrictCenter.x + Math.cos(lead) * radius, this.constrictCenter.y, this.constrictCenter.z + Math.sin(lead) * radius, 1.5D);
        if (this.constrictTicks > CONSTRICT_WARMUP) {
            boolean damageTick = this.constrictTicks % 20 == 0;
            float damage = this.getAttackDamage() * NagaConfig.CONSTRICT_MULTIPLIER.get().floatValue();
            AABB area = new AABB(this.constrictCenter, this.constrictCenter).inflate(radius + 2.0D, 3.0D, radius + 2.0D);
            for (LivingEntity living : this.naga.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (living instanceof Naga || !living.isAlive()) {
                    continue;
                }
                double dx = living.getX() - this.constrictCenter.x;
                double dz = living.getZ() - this.constrictCenter.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (damageTick && distance < radius) {
                    living.hurt(this.naga.damageSources().mobAttack(this.naga), damage);
                }
                if (distance > radius - 0.8D && distance < radius + 0.5D && distance > 0.01D) {
                    Vec3 motion = living.getDeltaMovement();
                    double pull = 0.3D / distance;
                    living.setDeltaMovement(motion.x * 0.5D - dx * pull, motion.y, motion.z * 0.5D - dz * pull);
                    living.hurtMarked = true;
                }
            }
        }
        if (this.constrictTicks >= CONSTRICT_DURATION) {
            this.endConstrict(false);
        }
    }

    private void endConstrict(boolean broken) {
        this.constrictTicks = -1;
        this.constrictBroken = false;
        this.weakIndex = -1;
        this.constrictCooldown = NagaConfig.CONSTRICT_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        this.naga.getNavigation().stop();
        float brokenFlag = 0.0F;
        if (broken) {
            brokenFlag = 1.0F;
        }
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.CONSTRICT_END, this.naga.getId(), brokenFlag));
    }

    private List<NagaSegment> getActiveSegments() {
        List<NagaSegment> list = new ArrayList<>();
        PartEntity<?>[] parts = this.naga.getParts();
        if (parts == null) {
            return list;
        }
        for (PartEntity<?> part : parts) {
            if (part instanceof NagaSegment segment && segment.getBbWidth() > 0.0F) {
                list.add(segment);
            }
        }
        return list;
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Naga " + this.naga.getId() + "] " + message, args);
        }
    }

    public static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    public static Vec3 directionOf(float yaw) {
        float radians = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
    }

    public static double findGround(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        int startY = Mth.floor(y) + 3;
        for (int checkY = startY; checkY >= startY - 8; checkY--) {
            below.set(Mth.floor(x), checkY - 1, Mth.floor(z));
            above.set(Mth.floor(x), checkY, Mth.floor(z));
            VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
            if (shape.isEmpty()) {
                continue;
            }
            if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                continue;
            }
            return below.getY() + shape.max(Direction.Axis.Y);
        }
        return Double.NaN;
    }

    private static final class TempestRing {
        private final Vec3 center;
        private final float base;
        private final Set<Integer> hit = new HashSet<>();
        private int age;

        private TempestRing(Vec3 center, float base) {
            this.center = center;
            this.base = base;
        }
    }
}
