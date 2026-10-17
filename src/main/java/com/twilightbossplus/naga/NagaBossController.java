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
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
    private static final int VENOM_WINDUP = 15;
    private static final int VENOM_FLIGHT = 18;
    private static final int VENOM_COUNT = 3;
    private static final float VENOM_CHANCE = 0.6F;
    private static final int SWEEP_WARN = 12;
    private static final int SWEEP_STRIKE = 6;
    private static final int SWEEP_RECOVER = 6;
    private static final float SWEEP_RADIUS = 6.0F;
    private static final float SWEEP_HALF_ARC = 60.0F;
    private static final int TAIL_LENGTH = 4;
    private static final double SWEEP_TRIGGER_RANGE = 4.0D;
    private static final float SWEEP_FRONT_ANGLE = 60.0F;
    private static final double SWEEP_KNOCKBACK = 0.8D;
    private static final int REND_WINDUP = 10;
    private static final double REND_SPEED = 0.6D;
    private static final float REND_TURN = 6.0F;
    private static final double REND_MAX = 24.0D;
    private static final double REND_MIN_RANGE = 5.0D;
    private static final int BITE_WARN = 15;
    private static final double BITE_RADIUS = 2.0D;
    private static final double BITE_KNOCKBACK = 0.6D;
    private static final int CONSTRICT_DURATION = 120;
    private static final int CONSTRICT_WARMUP = 20;
    private static final float CONSTRICT_START_RADIUS = 9.0F;
    private static final float CONSTRICT_END_RADIUS = 4.0F;
    private static final double CONSTRICT_TRIGGER_RANGE = 14.0D;
    private static final int CONSTRICT_HITS = 2;
    private static final int CONSTRICT_DAZE_TICKS = 60;
    private static final double SHED_SPEED_BONUS = 0.15D;
    private static final float RING_KIND_VENOM = 0.0F;
    private static final float RING_KIND_REND = 1.0F;

    private final Naga naga;
    private boolean shed;
    private int invulnerableTicks;
    private int globalCooldown = GLOBAL_GAP;
    private int venomCooldown;
    private int sweepCooldown = 60;
    private int rendCooldown = 60;
    private int constrictCooldown = 100;
    @Nullable
    private NagaMovementPattern.MovementState lastState;

    private int venomTicks = -1;
    private final Vec3[] venomTargets = new Vec3[VENOM_COUNT];

    private int sweepTicks = -1;
    private Vec3 sweepPivot = Vec3.ZERO;
    private float sweepYaw;
    private float sweepSign = 1.0F;

    private int rendWindup = -1;
    private boolean crackActive;
    private Vec3 crackPos = Vec3.ZERO;
    private float crackYaw;
    private double crackTravel;
    @Nullable
    private LivingEntity rendTarget;
    private int biteTicks = -1;
    private Vec3 bitePos = Vec3.ZERO;
    private float biteYaw;

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
        return NagaConfig.ENABLED.get() && (this.sweepTicks >= 0 || this.rendWindup >= 0 || this.constrictTicks >= 0);
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
            this.sweepTicks = -1;
            this.rendWindup = -1;
            this.venomTicks = -1;
            return;
        }
        if (this.invulnerableTicks > 0) {
            this.invulnerableTicks--;
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        if (this.venomCooldown > 0) {
            this.venomCooldown--;
        }
        if (this.sweepCooldown > 0) {
            this.sweepCooldown--;
        }
        if (this.rendCooldown > 0) {
            this.rendCooldown--;
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

        this.tickVenom(targetValid, target, ai, state);
        this.tickSweep();
        this.tickRendWindup(targetValid, target);
        this.tickCrack();
        this.tickBite();
        this.tickConstrict(targetValid, ai);

        if (targetValid && this.canStartSkill(state)) {
            this.chooseSkill(target, ai, state, phase);
        }
        this.lastState = state;
    }

    public void afterMove() {
        if (!NagaConfig.ENABLED.get() || this.sweepTicks < SWEEP_WARN || this.sweepTicks > SWEEP_WARN + SWEEP_STRIKE) {
            return;
        }
        List<NagaSegment> segments = this.getActiveSegments();
        int count = segments.size();
        if (count < TAIL_LENGTH + 1) {
            return;
        }
        float progress = (this.sweepTicks - SWEEP_WARN) / (float) SWEEP_STRIKE;
        float angle = this.getSweepAngle(progress);
        for (int k = 0; k < TAIL_LENGTH; k++) {
            NagaSegment segment = segments.get(count - TAIL_LENGTH + k);
            float segmentAngle = angle - this.sweepSign * k * 10.0F;
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
        this.debug("Weak scale hit {}/{}", this.weakHits, CONSTRICT_HITS);
        if (this.weakHits >= CONSTRICT_HITS) {
            this.constrictBroken = true;
        }
    }

    private boolean canStartSkill(NagaMovementPattern.MovementState state) {
        return this.sweepTicks < 0
            && this.rendWindup < 0
            && this.constrictTicks < 0
            && this.venomTicks < 0
            && this.globalCooldown <= 0
            && !this.naga.isDazed()
            && state != NagaMovementPattern.MovementState.DAZE;
    }

    private void chooseSkill(LivingEntity target, NagaMovementPattern ai, NagaMovementPattern.MovementState state, int phase) {
        double distance = this.naga.distanceTo(target);
        if (phase >= 3 && this.constrictCooldown <= 0 && state == NagaMovementPattern.MovementState.CIRCLE && distance < CONSTRICT_TRIGGER_RANGE) {
            this.startConstrict(target);
            return;
        }
        if (phase >= 2 && this.rendCooldown <= 0 && state == NagaMovementPattern.MovementState.CIRCLE && distance > REND_MIN_RANGE && distance < REND_MAX) {
            this.startRend(target);
            return;
        }
        boolean attacking = state == NagaMovementPattern.MovementState.INTIMIDATE
            || state == NagaMovementPattern.MovementState.CHARGE
            || state == NagaMovementPattern.MovementState.STUNLESS_CHARGE;
        if (this.sweepCooldown <= 0 && !attacking && this.isTargetNearTail(target)) {
            this.startSweep(target);
            return;
        }
        if (this.venomCooldown <= 0 && state == NagaMovementPattern.MovementState.INTIMIDATE && this.lastState != NagaMovementPattern.MovementState.INTIMIDATE
            && this.naga.getRandom().nextFloat() < VENOM_CHANCE) {
            this.startVenom(target, ai);
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
        for (int i = 0; i < segments.size(); i++) {
            NagaSegment segment = segments.get(i);
            int offset = (i + 1) * 6;
            data[offset] = (float) segment.getX();
            data[offset + 1] = (float) segment.getY();
            data[offset + 2] = (float) segment.getZ();
            data[offset + 3] = segment.getYRot();
            data[offset + 4] = segment.getXRot();
            data[offset + 5] = segment.getBbWidth();
        }
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.SHED, this.naga.getId(), data));
        this.naga.playSound(TFSounds.NAGA_HURT.get(), 2.0F, 0.55F);
        this.naga.playSound(SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F, 0.5F);
        this.naga.getMovementAI().forceCircle();
        this.debug("Shed triggered, invulnerable for {} ticks, {} segments", this.invulnerableTicks, segments.size());
    }

    private void startVenom(LivingEntity target, NagaMovementPattern ai) {
        this.venomTicks = 0;
        ((NagaMovementPatternAccessor) ai).setStateCounter(VENOM_WINDUP + 10);
        Vec3 predicted = target.position().add(target.getDeltaMovement().multiply(VENOM_FLIGHT * 0.6D, 0.0D, VENOM_FLIGHT * 0.6D));
        float baseAngle = this.naga.getRandom().nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < VENOM_COUNT; i++) {
            Vec3 point = predicted;
            if (i > 0) {
                float angle = baseAngle + i * Mth.PI;
                double spread = 1.8D + this.naga.getRandom().nextDouble() * 1.2D;
                point = predicted.add(Mth.cos(angle) * spread, 0.0D, Mth.sin(angle) * spread);
            }
            double ground = findGround(this.naga.level(), point.x, point.y, point.z);
            if (!Double.isNaN(ground)) {
                point = new Vec3(point.x, ground, point.z);
            }
            this.venomTargets[i] = point;
            ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.RING, this.naga.getId(),
                (float) point.x, (float) point.y, (float) point.z, VenomPool.RADIUS, VENOM_WINDUP + VENOM_FLIGHT + i * 2, RING_KIND_VENOM));
        }
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 3.0F, 1.3F);
        this.debug("Venom Spit windup toward {}", target.getName().getString());
    }

    private void tickVenom(boolean targetValid, @Nullable LivingEntity target, NagaMovementPattern ai, NagaMovementPattern.MovementState state) {
        if (this.venomTicks < 0) {
            return;
        }
        if (!targetValid || state != NagaMovementPattern.MovementState.INTIMIDATE) {
            this.venomTicks = -1;
            this.venomCooldown = NagaConfig.VENOM_SPIT_COOLDOWN_TICKS.get() / 2;
            this.debug("Venom Spit interrupted");
            return;
        }
        this.venomTicks++;
        this.naga.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (this.venomTicks < VENOM_WINDUP) {
            return;
        }
        Vec3 mouth = this.naga.position().add(0.0D, this.naga.getBbHeight() * 0.8D, 0.0D).add(directionOf(this.naga.getYRot()).scale(1.2D));
        float damage = this.getAttackDamage() * NagaConfig.VENOM_SPIT_MULTIPLIER.get().floatValue();
        for (int i = 0; i < VENOM_COUNT; i++) {
            VenomGlob glob = new VenomGlob(this.naga.level(), this.naga, damage);
            glob.setPos(mouth.x, mouth.y, mouth.z);
            glob.setDeltaMovement(VenomGlob.solveVelocity(mouth, this.venomTargets[i].add(0.0D, 0.1D, 0.0D), VENOM_FLIGHT + i * 2));
            this.naga.level().addFreshEntity(glob);
        }
        this.naga.playSound(SoundEvents.LLAMA_SPIT, 3.0F, 0.5F);
        this.venomTicks = -1;
        this.venomCooldown = NagaConfig.VENOM_SPIT_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        ai.forceCircle();
        this.debug("Venom Spit fired, damage {}", damage);
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

    private void startSweep(LivingEntity target) {
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
        this.sweepTicks = 0;
        this.naga.getNavigation().stop();
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.SWEEP, this.naga.getId(),
            (float) this.sweepPivot.x, (float) this.sweepPivot.y, (float) this.sweepPivot.z,
            this.sweepYaw, SWEEP_RADIUS, SWEEP_HALF_ARC, SWEEP_WARN, SWEEP_STRIKE, this.sweepSign));
        this.naga.playSound(TFSounds.NAGA_RATTLE.get(), 2.5F, 1.4F);
        this.debug("Tail Sweep windup, yaw {}", this.sweepYaw);
    }

    private void tickSweep() {
        if (this.sweepTicks < 0) {
            return;
        }
        this.sweepTicks++;
        this.naga.getNavigation().stop();
        if (this.sweepTicks == SWEEP_WARN) {
            this.naga.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.5F, 0.5F);
        }
        if (this.sweepTicks == SWEEP_WARN + SWEEP_STRIKE / 2) {
            float damage = this.getAttackDamage() * NagaConfig.TAIL_SWEEP_MULTIPLIER.get().floatValue();
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
                if (living.hurt(this.naga.damageSources().mobAttack(this.naga), damage)) {
                    living.knockback(SWEEP_KNOCKBACK, -dx, -dz);
                    hits++;
                }
            }
            this.debug("Tail Sweep hit {} targets for {}", hits, damage);
        }
        if (this.sweepTicks >= SWEEP_WARN + SWEEP_STRIKE + SWEEP_RECOVER) {
            this.sweepTicks = -1;
            this.sweepCooldown = NagaConfig.TAIL_SWEEP_COOLDOWN_TICKS.get();
            this.globalCooldown = GLOBAL_GAP;
        }
    }

    private float getSweepAngle(float progress) {
        return this.sweepYaw + this.sweepSign * (-SWEEP_HALF_ARC + SWEEP_HALF_ARC * 2.0F * progress);
    }

    private void startRend(LivingEntity target) {
        this.rendWindup = 0;
        this.rendTarget = target;
        this.naga.getNavigation().stop();
        Vec3 motion = this.naga.getDeltaMovement();
        this.naga.setDeltaMovement(motion.x * 0.2D, 0.55D, motion.z * 0.2D);
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 3.0F, 0.6F);
        this.debug("Earth Rend windup toward {}", target.getName().getString());
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
        this.rendCooldown = NagaConfig.EARTH_REND_COOLDOWN_TICKS.get();
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
        this.crackActive = true;
        this.rendTarget = target;
        this.naga.level().playSound(null, this.crackPos.x, this.crackPos.y, this.crackPos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 0.6F);
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.REND_POINT, this.naga.getId(),
            (float) this.crackPos.x, (float) this.crackPos.y, (float) this.crackPos.z, this.crackYaw, 1.0F));
        this.debug("Earth Rend slam at {}", this.crackPos);
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
            if (this.crackTravel % 1.8D < REND_SPEED) {
                this.naga.level().playSound(null, this.crackPos.x, this.crackPos.y, this.crackPos.z, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
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
        this.bitePos = this.crackPos;
        this.biteYaw = this.crackYaw;
        this.biteTicks = 0;
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.RING, this.naga.getId(),
            (float) this.bitePos.x, (float) this.bitePos.y, (float) this.bitePos.z, (float) BITE_RADIUS, BITE_WARN, RING_KIND_REND));
        this.debug("Earth Rend crack stopped after {} blocks", this.crackTravel);
    }

    private void tickBite() {
        if (this.biteTicks < 0) {
            return;
        }
        this.biteTicks++;
        if (this.biteTicks < BITE_WARN) {
            return;
        }
        this.biteTicks = -1;
        ModNetwork.sendTracking(this.naga, new NagaFxPacket(NagaFxPacket.BITE, this.naga.getId(),
            (float) this.bitePos.x, (float) this.bitePos.y, (float) this.bitePos.z, this.biteYaw));
        Level level = this.naga.level();
        level.playSound(null, this.bitePos.x, this.bitePos.y, this.bitePos.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 2.0F, 0.5F);
        float damage = this.getAttackDamage() * NagaConfig.EARTH_REND_MULTIPLIER.get().floatValue();
        AABB area = new AABB(this.bitePos.x - BITE_RADIUS, this.bitePos.y - 1.0D, this.bitePos.z - BITE_RADIUS,
            this.bitePos.x + BITE_RADIUS, this.bitePos.y + 3.0D, this.bitePos.z + BITE_RADIUS);
        int hits = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (living instanceof Naga || !living.isAlive()) {
                continue;
            }
            double dx = living.getX() - this.bitePos.x;
            double dz = living.getZ() - this.bitePos.z;
            if (dx * dx + dz * dz > BITE_RADIUS * BITE_RADIUS) {
                continue;
            }
            if (living.hurt(this.naga.damageSources().mobAttack(this.naga), damage)) {
                living.knockback(BITE_KNOCKBACK, -dx, -dz);
                double lift = 0.35D * (1.0D - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                if (lift > 0.0D) {
                    Vec3 motion = living.getDeltaMovement();
                    living.setDeltaMovement(motion.x, Math.max(motion.y, lift), motion.z);
                    living.hurtMarked = true;
                }
                hits++;
            }
        }
        this.debug("Earth Rend bite hit {} targets for {}", hits, damage);
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
}
