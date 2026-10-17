package com.twilightbossplus.queen;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.network.QueenFxPacket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import twilightforest.entity.boss.SnowQueen;
import twilightforest.init.TFDamageTypes;
import twilightforest.init.TFEntities;
import twilightforest.init.TFSounds;

public class SnowQueenBossController {
    public static final int POSE_NONE = 0;
    public static final int POSE_SIGIL = 1;
    public static final int POSE_LANCE = 2;
    public static final int POSE_AURORA = 3;

    private static final int SKILL_NONE = 0;
    private static final int SKILL_SIGIL = 1;
    private static final int SKILL_LANCE = 2;
    private static final int SKILL_AURORA = 3;
    private static final String PHASE_TAG = "TwilightBossPlusQueenPhase";
    private static final int SKILL_GAP = 30;
    private static final int SIGIL_RECOVER = 8;
    private static final int LANCE_CAST = 12;
    private static final int AURORA_CAST = 20;
    private static final int GROUND_DEPTH = 16;
    private static final double BEAM_RANGE = 20.0D;
    private static final double AURORA_HEIGHT = 8.0D;
    private static final int AURORA_PULSE = 10;

    private final SnowQueen boss;
    private int phase = 1;
    private boolean phaseInit;
    private boolean pendingAurora;
    private boolean wasBreathing;
    private int skill = SKILL_NONE;
    private int skillTicks;
    private int skillDuration;
    private int skillGap = 60;
    private int sigilCooldown = 40;
    private int lanceCooldown = 80;
    private int auroraCooldown = 200;
    private double sigilX;
    private double sigilY;
    private double sigilZ;
    private float sigilAngle;
    private final List<LanceChase> chases = new ArrayList<>();
    private final List<Spike> spikes = new ArrayList<>();
    private final List<Curtain> curtains = new ArrayList<>();

    public SnowQueenBossController(SnowQueen boss) {
        this.boss = boss;
    }

    public boolean isBusy() {
        return this.skill != SKILL_NONE;
    }

    public int getPhase() {
        return this.phase;
    }

    private float attack() {
        return (float) this.boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[SnowQueen " + this.boss.getId() + "] " + message, args);
        }
    }

    private void sendCast(int pose, int ticks) {
        ModNetwork.sendTracking(this.boss, new QueenFxPacket(QueenFxPacket.CAST, this.boss.getId(), pose, ticks));
    }

    private static boolean validVictim(LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof SnowQueen) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    private static double resistance(LivingEntity entity) {
        return Mth.clamp(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
    }

    private static Vec3 forward(float yaw) {
        float rad = yaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(rad), 0.0D, Mth.cos(rad));
    }

    public static double groundBelow(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        int startY = Mth.floor(y) + 1;
        for (int checkY = startY; checkY >= startY - GROUND_DEPTH; checkY--) {
            below.set(Mth.floor(x), checkY - 1, Mth.floor(z));
            above.set(Mth.floor(x), checkY, Mth.floor(z));
            VoxelShape shape = level.getBlockState(below).getCollisionShape(level, below);
            if (shape.isEmpty() || !level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
                continue;
            }
            return below.getY() + shape.max(Direction.Axis.Y);
        }
        return Double.NaN;
    }

    private List<Player> players(ServerLevel level, double range) {
        List<Player> result = new ArrayList<>();
        for (Player player : level.getEntitiesOfClass(Player.class, this.boss.getBoundingBox().inflate(range), SnowQueenBossController::validVictim)) {
            if (player.distanceToSqr(this.boss) <= range * range) {
                result.add(player);
            }
        }
        return result;
    }

    public void tick() {
        if (!SnowQueenConfig.ENABLED.get() || !(this.boss.level() instanceof ServerLevel level) || !this.boss.isAlive()) {
            return;
        }
        this.updatePhase();
        this.tickRefraction(level);
        this.tickChases(level);
        this.tickSpikes(level);
        this.tickCurtains(level);
        if (this.skillGap > 0) {
            this.skillGap--;
        }
        if (this.sigilCooldown > 0) {
            this.sigilCooldown--;
        }
        if (this.lanceCooldown > 0) {
            this.lanceCooldown--;
        }
        if (this.auroraCooldown > 0) {
            this.auroraCooldown--;
        }
        if (this.skill != SKILL_NONE) {
            this.skillTicks++;
            if (this.skill == SKILL_SIGIL) {
                this.tickSigil(level);
            }
            if (this.skillTicks >= this.skillDuration) {
                this.finishSkill();
            }
            return;
        }
        if (this.boss.isBreathing() || this.boss.getCurrentPhase() == SnowQueen.Phase.DROP) {
            return;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        if (this.pendingAurora) {
            this.pendingAurora = false;
            this.startAurora(level, target, true);
            return;
        }
        if (this.skillGap > 0) {
            return;
        }
        this.chooseSkill(level, target);
    }

    private void updatePhase() {
        float ratio = this.boss.getHealth() / this.boss.getMaxHealth();
        int next = 1;
        if (ratio <= SnowQueenConfig.PHASE3_THRESHOLD.get()) {
            next = 3;
        } else if (ratio <= SnowQueenConfig.PHASE2_THRESHOLD.get()) {
            next = 2;
        }
        if (!this.phaseInit) {
            this.phaseInit = true;
            this.phase = Math.max(this.phase, next);
            return;
        }
        if (next <= this.phase) {
            return;
        }
        this.phase = next;
        this.debug("entered phase {}", next);
        if (SnowQueenConfig.PHASE_AURORA.get() && SnowQueenConfig.AURORA_ENABLED.get() && this.skill != SKILL_AURORA) {
            this.pendingAurora = true;
        }
    }

    private void finishSkill() {
        this.skill = SKILL_NONE;
        this.skillTicks = 0;
        this.skillGap = SKILL_GAP;
        this.sendCast(POSE_NONE, 0);
    }

    private void beginSkill(int skill, int pose, int duration) {
        this.skill = skill;
        this.skillTicks = 0;
        this.skillDuration = duration;
        this.sendCast(pose, duration);
    }

    private void chooseSkill(ServerLevel level, LivingEntity target) {
        double dx = target.getX() - this.boss.getX();
        double dz = target.getZ() - this.boss.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dist = this.boss.distanceTo(target);
        List<Integer> options = new ArrayList<>();
        if (SnowQueenConfig.SIGIL_ENABLED.get() && this.sigilCooldown <= 0 && horizontal <= SnowQueenConfig.SIGIL_RANGE.get()) {
            options.add(SKILL_SIGIL);
        }
        if (SnowQueenConfig.LANCE_ENABLED.get() && this.lanceCooldown <= 0 && dist <= SnowQueenConfig.LANCE_RANGE.get()) {
            options.add(SKILL_LANCE);
        }
        if (this.phase >= 2 && SnowQueenConfig.AURORA_ENABLED.get() && this.auroraCooldown <= 0 && dist <= SnowQueenConfig.AURORA_RANGE.get()) {
            options.add(SKILL_AURORA);
        }
        if (options.isEmpty()) {
            return;
        }
        int pick = options.get(this.boss.getRandom().nextInt(options.size()));
        if (pick == SKILL_SIGIL) {
            this.startSigil(level);
        } else if (pick == SKILL_LANCE) {
            this.startLance(level, target);
        } else {
            this.startAurora(level, target, false);
        }
    }

    private void startSigil(ServerLevel level) {
        double ground = groundBelow(level, this.boss.getX(), this.boss.getY(), this.boss.getZ());
        this.sigilCooldown = SnowQueenConfig.SIGIL_COOLDOWN.get();
        if (Double.isNaN(ground)) {
            this.debug("sigil skipped, no ground below");
            return;
        }
        int windup = SnowQueenConfig.SIGIL_WINDUP.get();
        float spin = 0.0F;
        if (this.phase >= 3) {
            spin = 30.0F;
        }
        float start = this.boss.getRandom().nextFloat() * 60.0F;
        this.sigilX = this.boss.getX();
        this.sigilY = ground;
        this.sigilZ = this.boss.getZ();
        this.sigilAngle = start + spin;
        int duration = windup + SIGIL_RECOVER;
        if (this.phase >= 2) {
            duration += SnowQueenConfig.SIGIL_SECOND_DELAY.get();
        }
        this.beginSkill(SKILL_SIGIL, POSE_SIGIL, duration);
        this.sendSigil(level, windup, start, spin);
        this.boss.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.6F);
        this.debug("sigil at {} {} {} angle {}", this.sigilX, this.sigilY, this.sigilZ, this.sigilAngle);
    }

    private void sendSigil(ServerLevel level, int windup, float start, float spin) {
        ModNetwork.sendNear(level, this.sigilX, this.sigilY, this.sigilZ, 96.0D, new QueenFxPacket(QueenFxPacket.SIGIL, this.boss.getId(),
            (float) this.sigilX, (float) this.sigilY, (float) this.sigilZ, SnowQueenConfig.SIGIL_RADIUS.get().floatValue(), windup, start, spin,
            SnowQueenConfig.SIGIL_WIDTH.get().floatValue()));
    }

    private void tickSigil(ServerLevel level) {
        int windup = SnowQueenConfig.SIGIL_WINDUP.get();
        if (this.skillTicks == windup) {
            this.fireSigil(level, this.sigilAngle);
            if (this.phase >= 2) {
                this.sigilAngle += 30.0F;
                this.sendSigil(level, SnowQueenConfig.SIGIL_SECOND_DELAY.get(), this.sigilAngle, 0.0F);
            }
        } else if (this.phase >= 2 && this.skillTicks == windup + SnowQueenConfig.SIGIL_SECOND_DELAY.get()) {
            this.fireSigil(level, this.sigilAngle);
        }
    }

    private void fireSigil(ServerLevel level, float angle) {
        double radius = SnowQueenConfig.SIGIL_RADIUS.get();
        double half = SnowQueenConfig.SIGIL_WIDTH.get() * 0.5D;
        float damage = (float) (this.attack() * SnowQueenConfig.SIGIL_MULTIPLIER.get());
        double knockback = SnowQueenConfig.SIGIL_KNOCKBACK.get();
        AABB area = new AABB(this.sigilX - radius, this.sigilY - 1.0D, this.sigilZ - radius, this.sigilX + radius, this.sigilY + 3.0D, this.sigilZ + radius);
        level.playSound(null, this.sigilX, this.sigilY, this.sigilZ, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
        level.playSound(null, this.sigilX, this.sigilY, this.sigilZ, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        int hits = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, SnowQueenBossController::validVictim)) {
            double dx = entity.getX() - this.sigilX;
            double dz = entity.getZ() - this.sigilZ;
            double tolerance = half + entity.getBbWidth() * 0.5D;
            if (dx * dx + dz * dz > (radius + tolerance) * (radius + tolerance)) {
                continue;
            }
            boolean onArm = false;
            for (int i = 0; i < 6 && !onArm; i++) {
                Vec3 dir = forward(angle + i * 60.0F);
                double along = dx * dir.x + dz * dir.z;
                double side = Math.abs(dx * dir.z - dz * dir.x);
                onArm = along >= -tolerance && along <= radius && side <= tolerance;
            }
            if (!onArm) {
                continue;
            }
            if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage)) {
                hits++;
                if (knockback > 0.0D && dx * dx + dz * dz > 1.0E-4D) {
                    entity.knockback(knockback, -dx, -dz);
                }
            }
        }
        this.debug("sigil fired at angle {}, hits {}", angle, hits);
    }

    private void startLance(ServerLevel level, LivingEntity target) {
        this.lanceCooldown = SnowQueenConfig.LANCE_COOLDOWN.get();
        int count = SnowQueenConfig.LANCE_PHASE1_COUNT.get();
        if (this.phase >= 3) {
            count = SnowQueenConfig.LANCE_PHASE3_COUNT.get();
        }
        Set<UUID> chased = new HashSet<>();
        for (Player player : this.players(level, SnowQueenConfig.LANCE_RANGE.get())) {
            this.chases.add(new LanceChase(player.getUUID(), count));
            chased.add(player.getUUID());
        }
        if (validVictim(target) && !chased.contains(target.getUUID())) {
            this.chases.add(new LanceChase(target.getUUID(), count));
        }
        this.beginSkill(SKILL_LANCE, POSE_LANCE, LANCE_CAST);
        this.boss.playSound(TFSounds.SNOW_QUEEN_ATTACK.get(), 1.5F, 1.2F);
        this.debug("lance started, chases {}", this.chases.size());
    }

    private void tickChases(ServerLevel level) {
        Iterator<LanceChase> iterator = this.chases.iterator();
        while (iterator.hasNext()) {
            LanceChase chase = iterator.next();
            if (chase.delay > 0) {
                chase.delay--;
                continue;
            }
            Entity entity = level.getEntity(chase.target);
            if (!(entity instanceof LivingEntity living) || !validVictim(living) || chase.remaining <= 0) {
                iterator.remove();
                continue;
            }
            double ground = groundBelow(level, living.getX(), living.getY() + 0.5D, living.getZ());
            if (!Double.isNaN(ground)) {
                int warning = SnowQueenConfig.LANCE_WARNING.get();
                float radius = SnowQueenConfig.LANCE_RADIUS.get().floatValue();
                this.spikes.add(new Spike(living.getX(), ground, living.getZ(), warning));
                ModNetwork.sendNear(level, living.getX(), ground, living.getZ(), 96.0D, new QueenFxPacket(QueenFxPacket.LANCE, -1,
                    (float) living.getX(), (float) ground, (float) living.getZ(), warning, radius));
            }
            chase.remaining--;
            chase.delay = SnowQueenConfig.LANCE_INTERVAL.get();
        }
    }

    private void tickSpikes(ServerLevel level) {
        if (this.spikes.isEmpty()) {
            return;
        }
        double radius = SnowQueenConfig.LANCE_RADIUS.get();
        float damage = (float) (this.attack() * SnowQueenConfig.LANCE_MULTIPLIER.get());
        double lift = SnowQueenConfig.LANCE_LIFT.get();
        Iterator<Spike> iterator = this.spikes.iterator();
        while (iterator.hasNext()) {
            Spike spike = iterator.next();
            if (--spike.ticks > 0) {
                continue;
            }
            iterator.remove();
            level.playSound(null, spike.x, spike.y, spike.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.5F, 0.8F);
            AABB area = new AABB(spike.x - radius, spike.y - 0.5D, spike.z - radius, spike.x + radius, spike.y + 3.0D, spike.z + radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, SnowQueenBossController::validVictim)) {
                double dx = entity.getX() - spike.x;
                double dz = entity.getZ() - spike.z;
                double reach = radius + entity.getBbWidth() * 0.5D;
                if (dx * dx + dz * dz > reach * reach) {
                    continue;
                }
                if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage) && lift > 0.0D) {
                    double up = lift * (1.0D - resistance(entity));
                    if (up > 0.0D) {
                        Vec3 motion = entity.getDeltaMovement();
                        entity.setDeltaMovement(motion.x, Math.max(motion.y, up), motion.z);
                        entity.hurtMarked = true;
                    }
                }
            }
        }
    }

    private void startAurora(ServerLevel level, LivingEntity target, boolean forced) {
        this.auroraCooldown = SnowQueenConfig.AURORA_COOLDOWN.get();
        List<LivingEntity> anchors = new ArrayList<>(this.players(level, SnowQueenConfig.AURORA_RANGE.get()));
        if (anchors.isEmpty()) {
            anchors.add(target);
        }
        int count = SnowQueenConfig.AURORA_COUNT.get();
        int warning = SnowQueenConfig.AURORA_WARNING.get();
        int active = SnowQueenConfig.AURORA_ACTIVE.get();
        double length = SnowQueenConfig.AURORA_LENGTH.get();
        double width = SnowQueenConfig.AURORA_WIDTH.get();
        double drift = SnowQueenConfig.AURORA_DRIFT.get();
        for (int i = 0; i < count; i++) {
            LivingEntity anchor = anchors.get(i % anchors.size());
            double offset = 1.5D + this.boss.getRandom().nextDouble() * 2.5D;
            float yaw = this.boss.getRandom().nextFloat() * 180.0F;
            Vec3 axis = forward(yaw);
            double sideSign = 1.0D;
            if (this.boss.getRandom().nextBoolean()) {
                sideSign = -1.0D;
            }
            double x = anchor.getX() - axis.z * offset * sideSign;
            double z = anchor.getZ() + axis.x * offset * sideSign;
            if (i < anchors.size()) {
                x = anchor.getX();
                z = anchor.getZ();
            }
            double ground = groundBelow(level, x, anchor.getY() + 1.0D, z);
            if (Double.isNaN(ground)) {
                ground = anchor.getY();
            }
            double driftSpeed = drift * sideSign;
            this.curtains.add(new Curtain(x, ground, z, yaw, length, width, warning, active, driftSpeed));
            ModNetwork.sendNear(level, x, ground, z, 128.0D, new QueenFxPacket(QueenFxPacket.AURORA, -1, (float) x, (float) ground, (float) z, yaw,
                (float) length, (float) width, (float) AURORA_HEIGHT, warning, active, (float) driftSpeed));
        }
        this.beginSkill(SKILL_AURORA, POSE_AURORA, AURORA_CAST);
        this.boss.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3.0F, 0.5F);
        this.boss.playSound(TFSounds.SNOW_QUEEN_AMBIENT.get(), 2.0F, 0.8F);
        this.debug("aurora curtain started, count {}, forced {}", count, forced);
    }

    private void tickCurtains(ServerLevel level) {
        if (this.curtains.isEmpty()) {
            return;
        }
        float damage = (float) (this.attack() * SnowQueenConfig.AURORA_MULTIPLIER.get());
        Iterator<Curtain> iterator = this.curtains.iterator();
        while (iterator.hasNext()) {
            Curtain curtain = iterator.next();
            curtain.age++;
            if (curtain.age > curtain.warning + curtain.active) {
                iterator.remove();
                continue;
            }
            if (curtain.age <= curtain.warning) {
                continue;
            }
            Vec3 axis = forward(curtain.yaw);
            curtain.x += -axis.z * curtain.drift;
            curtain.z += axis.x * curtain.drift;
            if ((curtain.age - curtain.warning) % AURORA_PULSE != 1) {
                continue;
            }
            double halfLength = curtain.length * 0.5D;
            double reach = halfLength + curtain.width;
            AABB area = new AABB(curtain.x - reach, curtain.y - 0.5D, curtain.z - reach, curtain.x + reach, curtain.y + AURORA_HEIGHT, curtain.z + reach);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, SnowQueenBossController::validVictim)) {
                double dx = entity.getX() - curtain.x;
                double dz = entity.getZ() - curtain.z;
                double along = Math.abs(dx * axis.x + dz * axis.z);
                double side = Math.abs(dx * axis.z - dz * axis.x);
                if (along > halfLength || side > curtain.width * 0.5D + entity.getBbWidth() * 0.5D) {
                    continue;
                }
                if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0), this.boss);
                }
            }
        }
    }

    private void tickRefraction(ServerLevel level) {
        boolean breathing = this.boss.isBreathing();
        double angle = SnowQueenConfig.REFRACT_ANGLE.get();
        boolean refract = breathing && this.phase >= 3 && angle > 0.0D;
        if (refract && !this.wasBreathing) {
            ModNetwork.sendTracking(this.boss, new QueenFxPacket(QueenFxPacket.REFRACT, this.boss.getId(), (float) angle));
            this.debug("beam refracted by {} degrees", angle);
        }
        this.wasBreathing = breathing;
        if (!refract) {
            return;
        }
        Vec3 src = new Vec3(this.boss.getX(), this.boss.getY() + 0.25D, this.boss.getZ());
        Vec3 look = this.boss.getViewVector(1.0F);
        List<Entity> parts = Arrays.asList(this.boss.iceArray);
        for (int sign = -1; sign <= 1; sign += 2) {
            Vec3 dir = look.yRot((float) (sign * angle * Mth.DEG_TO_RAD));
            Vec3 dest = src.add(dir.scale(BEAM_RANGE));
            AABB area = new AABB(src, dest).inflate(1.0D);
            for (Entity entity : level.getEntities(this.boss, area)) {
                if (!entity.isPickable() || parts.contains(entity) || !(entity instanceof LivingEntity living) || !validVictim(living)) {
                    continue;
                }
                AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
                if (box.contains(src) || box.clip(src, dest).isPresent()) {
                    this.boss.doBreathAttack(entity);
                }
            }
        }
    }

    public float beamDamage() {
        return (float) (this.attack() * SnowQueenConfig.BEAM_MULTIPLIER.get());
    }

    public void onShieldHit(Entity shield, Entity collided) {
        if (collided == this.boss) {
            return;
        }
        if (!(collided instanceof LivingEntity living) || !validVictim(living)) {
            collided.push(shield);
            return;
        }
        boolean dropping = this.boss.getCurrentPhase() == SnowQueen.Phase.DROP;
        DamageSource source = this.boss.damageSources().mobAttack(this.boss);
        double multiplier = SnowQueenConfig.SHIELD_MULTIPLIER.get();
        if (dropping) {
            source = TFDamageTypes.getEntityDamageSource(this.boss.level(), TFDamageTypes.SQUISH, this.boss, TFEntities.SNOW_QUEEN.get());
            multiplier = SnowQueenConfig.DROP_MULTIPLIER.get();
        }
        if (!living.hurt(source, (float) (this.attack() * multiplier))) {
            return;
        }
        double dx = living.getX() - this.boss.getX();
        double dz = living.getZ() - this.boss.getZ();
        double knockback = SnowQueenConfig.SHIELD_KNOCKBACK.get();
        if (knockback > 0.0D && dx * dx + dz * dz > 1.0E-4D) {
            living.knockback(knockback, -dx, -dz);
        }
        double up = 0.25D * (1.0D - resistance(living));
        if (up > 0.0D) {
            Vec3 motion = living.getDeltaMovement();
            living.setDeltaMovement(motion.x, Math.max(motion.y, up), motion.z);
            living.hurtMarked = true;
        }
        this.boss.playSound(TFSounds.SNOW_QUEEN_ATTACK.get(), 1.0F, 1.0F);
        this.debug("shield hit {} dropping {}", living.getName().getString(), dropping);
    }

    public void onDeath() {
        this.skill = SKILL_NONE;
        this.chases.clear();
        this.spikes.clear();
        this.curtains.clear();
        this.pendingAurora = false;
    }

    public void save(CompoundTag tag) {
        tag.putInt(PHASE_TAG, this.phase);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(PHASE_TAG)) {
            this.phase = Math.max(1, tag.getInt(PHASE_TAG));
        }
    }

    private static final class LanceChase {
        private final UUID target;
        private int remaining;
        private int delay;

        private LanceChase(UUID target, int remaining) {
            this.target = target;
            this.remaining = remaining;
        }
    }

    private static final class Spike {
        private final double x;
        private final double y;
        private final double z;
        private int ticks;

        private Spike(double x, double y, double z, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticks = ticks;
        }
    }

    private static final class Curtain {
        private double x;
        private final double y;
        private double z;
        private final float yaw;
        private final double length;
        private final double width;
        private final int warning;
        private final int active;
        private final double drift;
        private int age;

        private Curtain(double x, double y, double z, float yaw, double length, double width, int warning, int active, double drift) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.length = length;
            this.width = width;
            this.warning = warning;
            this.active = active;
            this.drift = drift;
        }
    }
}
