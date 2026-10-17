package com.twilightbossplus.yeti;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.AlphaYetiConfig;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.entity.YetiSnowball;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.network.YetiFxPacket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.init.TFSounds;

public class AlphaYetiBossController {
    public static final int POSE_NONE = 0;
    public static final int POSE_INHALE = 1;
    public static final int POSE_BLOW = 2;
    public static final int POSE_RIFT = 3;
    public static final int POSE_SLAM = 4;
    public static final int POSE_BOULDER = 5;
    public static final int POSE_THROW = 6;
    public static final int POSE_ROAR = 7;

    private static final int SKILL_NONE = 0;
    private static final int SKILL_BREATH = 1;
    private static final int SKILL_RIFT = 2;
    private static final int SKILL_BOULDER = 3;
    private static final int SKILL_ROAR = 4;
    private static final String PHASE_TAG = "TwilightBossPlusYetiPhase";
    private static final int SKILL_GAP = 40;
    private static final int RIFT_SPREAD = 10;
    private static final int RIFT_RECOVER = 10;
    private static final int THROW_RECOVER = 10;
    private static final int ICICLE_FALL = 6;
    private static final int WAVE_GAP = 20;
    private static final int VANILLA_TIRED_TICKS = 100;
    private static final double MOUTH_HEIGHT = 4.2D;

    private final AlphaYeti boss;
    private int phase = 1;
    private boolean phaseInit;
    private boolean pendingRoar;
    private int skill = SKILL_NONE;
    private int skillTicks;
    private int skillGap = 60;
    private int breathCooldown = 60;
    private int riftCooldown = 40;
    private int boulderCooldown = 100;
    private int roarCooldown = 200;
    private float breathStartYaw;
    private float breathEndYaw;
    private float riftYaw;
    private int riftLines;
    private double riftX;
    private double riftY;
    private double riftZ;
    private final Set<UUID> skillHits = new HashSet<>();
    private final List<Icicle> icicles = new ArrayList<>();
    private final List<Slush> slushes = new ArrayList<>();
    private final List<Wave> waves = new ArrayList<>();
    private int lastWaveTick = -WAVE_GAP;

    public AlphaYetiBossController(AlphaYeti boss) {
        this.boss = boss;
    }

    public boolean isBusy() {
        return this.skill != SKILL_NONE;
    }

    public boolean isRoaring() {
        return this.skill == SKILL_ROAR;
    }

    public int getTiredTicks() {
        if (this.phase >= 3) {
            return AlphaYetiConfig.PHASE3_TIRED_TICKS.get();
        }
        return VANILLA_TIRED_TICKS;
    }

    private float attack() {
        return (float) this.boss.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[AlphaYeti " + this.boss.getId() + "] " + message, args);
        }
    }

    private void sendCast(int pose, int ticks) {
        ModNetwork.sendTracking(this.boss, new YetiFxPacket(YetiFxPacket.CAST, this.boss.getId(), pose, ticks));
    }

    private static boolean validVictim(LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof AlphaYeti) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }
        return true;
    }

    private static float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) (Mth.atan2(toZ - fromZ, toX - fromX) * Mth.RAD_TO_DEG) - 90.0F;
    }

    private void faceYaw(float yaw) {
        this.boss.setYRot(yaw);
        this.boss.setYBodyRot(yaw);
        this.boss.setYHeadRot(yaw);
    }

    public void tick() {
        if (!AlphaYetiConfig.ENABLED.get() || !(this.boss.level() instanceof ServerLevel level) || !this.boss.isAlive()) {
            return;
        }
        this.updatePhase();
        this.tickIcicles(level);
        this.tickSlush(level);
        this.tickWaves(level);
        if (this.skillGap > 0) {
            this.skillGap--;
        }
        if (this.breathCooldown > 0) {
            this.breathCooldown--;
        }
        if (this.riftCooldown > 0) {
            this.riftCooldown--;
        }
        if (this.boulderCooldown > 0) {
            this.boulderCooldown--;
        }
        if (this.roarCooldown > 0) {
            this.roarCooldown--;
        }
        if (this.skill != SKILL_NONE) {
            this.skillTicks++;
            this.boss.getNavigation().stop();
            if (this.skill == SKILL_BREATH) {
                this.tickBreath(level);
            } else if (this.skill == SKILL_RIFT) {
                this.tickRift(level);
            } else if (this.skill == SKILL_BOULDER) {
                this.tickBoulder(level);
            } else if (this.skill == SKILL_ROAR) {
                this.tickRoar();
            }
            return;
        }
        if (this.boss.isRampaging() || this.boss.isTired() || this.boss.isVehicle()) {
            return;
        }
        if (this.pendingRoar) {
            this.pendingRoar = false;
            this.startRoar(level, true);
            return;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive() || this.skillGap > 0) {
            return;
        }
        this.chooseSkill(level, target);
    }

    private void updatePhase() {
        float ratio = this.boss.getHealth() / this.boss.getMaxHealth();
        int next = 1;
        if (ratio <= AlphaYetiConfig.PHASE3_THRESHOLD.get()) {
            next = 3;
        } else if (ratio <= AlphaYetiConfig.PHASE2_THRESHOLD.get()) {
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
        if (AlphaYetiConfig.PHASE_ROAR.get()) {
            if (this.skill != SKILL_NONE && this.skill != SKILL_ROAR) {
                this.cancelSkill();
            }
            if (this.skill != SKILL_ROAR) {
                this.pendingRoar = true;
            }
        }
    }

    private void cancelSkill() {
        this.debug("skill {} cancelled", this.skill);
        this.skill = SKILL_NONE;
        this.skillTicks = 0;
        this.sendCast(POSE_NONE, 0);
    }

    private void finishSkill() {
        this.skill = SKILL_NONE;
        this.skillTicks = 0;
        this.skillGap = SKILL_GAP;
        this.sendCast(POSE_NONE, 0);
    }

    private void chooseSkill(ServerLevel level, LivingEntity target) {
        double dist = this.boss.distanceTo(target);
        boolean sight = this.boss.hasLineOfSight(target);
        List<Integer> options = new ArrayList<>();
        if (AlphaYetiConfig.RIFT_ENABLED.get() && this.riftCooldown <= 0 && dist <= AlphaYetiConfig.RIFT_RANGE.get()) {
            options.add(SKILL_RIFT);
        }
        if (AlphaYetiConfig.BREATH_ENABLED.get() && this.breathCooldown <= 0 && dist >= 2.0D && dist <= AlphaYetiConfig.BREATH_RANGE.get() + 1.0D && sight) {
            options.add(SKILL_BREATH);
        }
        if (this.phase >= 2 && AlphaYetiConfig.BOULDER_ENABLED.get() && this.boulderCooldown <= 0 && dist >= 6.0D && dist <= 28.0D && sight) {
            options.add(SKILL_BOULDER);
        }
        if (this.phase >= 2 && AlphaYetiConfig.ROAR_ENABLED.get() && this.roarCooldown <= 0 && dist <= AlphaYetiConfig.ROAR_RANGE.get()) {
            options.add(SKILL_ROAR);
        }
        if (options.isEmpty()) {
            return;
        }
        int pick = options.get(this.boss.getRandom().nextInt(options.size()));
        if (pick == SKILL_RIFT) {
            this.startRift(target);
        } else if (pick == SKILL_BREATH) {
            this.startBreath(target);
        } else if (pick == SKILL_BOULDER) {
            this.startBoulder(target);
        } else {
            this.startRoar(level, false);
        }
    }

    private void startBreath(LivingEntity target) {
        float base = yawTo(this.boss.getX(), this.boss.getZ(), target.getX(), target.getZ());
        this.breathStartYaw = base;
        this.breathEndYaw = base;
        if (this.phase >= 3) {
            float sweep = AlphaYetiConfig.BREATH_SWEEP.get().floatValue();
            this.breathStartYaw = base - sweep;
            this.breathEndYaw = base + sweep;
        }
        this.skill = SKILL_BREATH;
        this.skillTicks = 0;
        this.skillHits.clear();
        this.breathCooldown = AlphaYetiConfig.BREATH_COOLDOWN.get();
        int inhale = AlphaYetiConfig.BREATH_INHALE.get();
        int blow = AlphaYetiConfig.BREATH_TICKS.get();
        this.faceYaw(this.breathStartYaw);
        this.sendCast(POSE_INHALE, inhale);
        ModNetwork.sendTracking(this.boss, new YetiFxPacket(YetiFxPacket.BREATH, this.boss.getId(), this.breathStartYaw, this.breathEndYaw,
            AlphaYetiConfig.BREATH_ANGLE.get().floatValue() * 0.5F, AlphaYetiConfig.BREATH_RANGE.get().floatValue(), inhale, blow, (float) this.boss.getY()));
        this.boss.playSound(TFSounds.ALPHA_YETI_GROWL.get(), 4.0F, 0.6F);
        this.debug("frost breath started, yaw {} -> {}", this.breathStartYaw, this.breathEndYaw);
    }

    private void tickBreath(ServerLevel level) {
        int inhale = AlphaYetiConfig.BREATH_INHALE.get();
        int blow = AlphaYetiConfig.BREATH_TICKS.get();
        if (this.skillTicks <= inhale) {
            this.faceYaw(this.breathStartYaw);
            if (this.skillTicks == inhale) {
                this.sendCast(POSE_BLOW, blow);
                this.boss.playSound(SoundEvents.POWDER_SNOW_STEP, 4.0F, 0.5F);
            }
            return;
        }
        int blowTick = this.skillTicks - inhale;
        float progress = Mth.clamp(blowTick / (float) blow, 0.0F, 1.0F);
        float yaw = Mth.lerp(progress, this.breathStartYaw, this.breathEndYaw);
        this.faceYaw(yaw);
        if (blowTick % 10 == 1) {
            this.breathDamage(level, yaw);
        }
        if (blowTick % 8 == 0) {
            this.boss.playSound(SoundEvents.POWDER_SNOW_FALL, 3.0F, 0.5F);
        }
        if (blowTick >= blow) {
            this.finishSkill();
        }
    }

    private void breathDamage(ServerLevel level, float yaw) {
        double range = AlphaYetiConfig.BREATH_RANGE.get();
        double half = AlphaYetiConfig.BREATH_ANGLE.get() * 0.5D;
        Vec3 mouth = new Vec3(this.boss.getX(), this.boss.getY() + MOUTH_HEIGHT, this.boss.getZ());
        float damage = (float) (this.attack() * AlphaYetiConfig.BREATH_MULTIPLIER.get());
        int freeze = AlphaYetiConfig.BREATH_FREEZE.get();
        AABB area = this.boss.getBoundingBox().inflate(range + 1.0D, 3.0D, range + 1.0D);
        int hits = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, AlphaYetiBossController::validVictim)) {
            double dx = entity.getX() - this.boss.getX();
            double dz = entity.getZ() - this.boss.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > range + entity.getBbWidth() * 0.5D || dist < 0.01D) {
                continue;
            }
            float diff = Math.abs(Mth.wrapDegrees(yawTo(this.boss.getX(), this.boss.getZ(), entity.getX(), entity.getZ()) - yaw));
            if (diff > half && dist > this.boss.getBbWidth() * 0.6D) {
                continue;
            }
            if (entity.getY() < this.boss.getY() - 2.0D || entity.getY() > this.boss.getY() + 6.0D) {
                continue;
            }
            HitResult result = level.clip(new ClipContext(mouth, entity.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.boss));
            if (result.getType() != HitResult.Type.MISS) {
                continue;
            }
            if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage)) {
                hits++;
            }
            if (freeze > 0 && entity.canFreeze()) {
                entity.setTicksFrozen(Math.min(entity.getTicksFrozen() + freeze, entity.getTicksRequiredToFreeze() + 80));
            }
        }
        if (hits > 0) {
            this.debug("frost breath hit {} entities", hits);
        }
    }

    private void startRift(LivingEntity target) {
        this.riftYaw = yawTo(this.boss.getX(), this.boss.getZ(), target.getX(), target.getZ());
        this.riftLines = AlphaYetiConfig.RIFT_PHASE1_LINES.get();
        if (this.phase >= 2) {
            this.riftLines = AlphaYetiConfig.RIFT_PHASE2_LINES.get();
        }
        this.riftX = this.boss.getX();
        this.riftY = this.boss.getY();
        this.riftZ = this.boss.getZ();
        this.skill = SKILL_RIFT;
        this.skillTicks = 0;
        this.skillHits.clear();
        this.riftCooldown = AlphaYetiConfig.RIFT_COOLDOWN.get();
        int windup = AlphaYetiConfig.RIFT_WINDUP.get();
        this.faceYaw(this.riftYaw);
        this.sendCast(POSE_RIFT, windup);
        float[] data = new float[6 + this.riftLines];
        data[0] = (float) this.riftX;
        data[1] = (float) this.riftY;
        data[2] = (float) this.riftZ;
        data[3] = AlphaYetiConfig.RIFT_LENGTH.get().floatValue();
        data[4] = windup;
        data[5] = RIFT_SPREAD;
        for (int i = 0; i < this.riftLines; i++) {
            data[6 + i] = this.riftYaw + 360.0F * i / this.riftLines;
        }
        ModNetwork.sendTracking(this.boss, new YetiFxPacket(YetiFxPacket.RIFT, this.boss.getId(), data));
        this.boss.playSound(TFSounds.ALPHA_YETI_GROWL.get(), 4.0F, 0.8F);
        this.debug("ice rift started with {} lines", this.riftLines);
    }

    private void tickRift(ServerLevel level) {
        int windup = AlphaYetiConfig.RIFT_WINDUP.get();
        if (this.skillTicks < windup) {
            this.faceYaw(this.riftYaw);
            return;
        }
        if (this.skillTicks == windup) {
            this.sendCast(POSE_SLAM, 8);
            level.playSound(null, this.riftX, this.riftY, this.riftZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
            level.playSound(null, this.riftX, this.riftY, this.riftZ, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
        }
        int step = this.skillTicks - windup + 1;
        if (step <= RIFT_SPREAD) {
            double length = AlphaYetiConfig.RIFT_LENGTH.get();
            double reach = length * step / RIFT_SPREAD;
            float damage = (float) (this.attack() * AlphaYetiConfig.RIFT_MULTIPLIER.get());
            double knockback = Math.min(1.0D, AlphaYetiConfig.RIFT_KNOCKBACK.get());
            AABB area = new AABB(this.riftX - reach - 1.0D, this.riftY - 1.0D, this.riftZ - reach - 1.0D, this.riftX + reach + 1.0D, this.riftY + 2.5D, this.riftZ + reach + 1.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, AlphaYetiBossController::validVictim)) {
                if (this.skillHits.contains(entity.getUUID())) {
                    continue;
                }
                double dx = entity.getX() - this.riftX;
                double dz = entity.getZ() - this.riftZ;
                double tolerance = 1.0D + entity.getBbWidth() * 0.5D;
                boolean onLine = false;
                for (int i = 0; i < this.riftLines && !onLine; i++) {
                    float yaw = (this.riftYaw + 360.0F * i / this.riftLines) * Mth.DEG_TO_RAD;
                    double lx = -Mth.sin(yaw);
                    double lz = Mth.cos(yaw);
                    double along = dx * lx + dz * lz;
                    double side = Math.abs(dx * lz - dz * lx);
                    if (along > 0.0D && along <= reach + 0.5D && side <= tolerance) {
                        onLine = true;
                    }
                }
                if (!onLine) {
                    continue;
                }
                this.skillHits.add(entity.getUUID());
                if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage) && knockback > 0.0D && dx * dx + dz * dz > 1.0E-4D) {
                    entity.knockback(knockback, -dx, -dz);
                }
                this.debug("ice rift hit {}", entity.getName().getString());
            }
            return;
        }
        if (this.skillTicks >= windup + RIFT_SPREAD + RIFT_RECOVER) {
            this.finishSkill();
        }
    }

    private void startBoulder(LivingEntity target) {
        this.skill = SKILL_BOULDER;
        this.skillTicks = 0;
        this.boulderCooldown = AlphaYetiConfig.BOULDER_COOLDOWN.get();
        int windup = AlphaYetiConfig.BOULDER_WINDUP.get();
        this.faceYaw(yawTo(this.boss.getX(), this.boss.getZ(), target.getX(), target.getZ()));
        this.sendCast(POSE_BOULDER, windup);
        this.boss.playSound(SoundEvents.SNOW_BREAK, 4.0F, 0.5F);
        this.debug("snow boulder wind-up");
    }

    private void tickBoulder(ServerLevel level) {
        int windup = AlphaYetiConfig.BOULDER_WINDUP.get();
        LivingEntity target = this.boss.getTarget();
        if (this.skillTicks < windup) {
            if (target != null) {
                this.faceYaw(yawTo(this.boss.getX(), this.boss.getZ(), target.getX(), target.getZ()));
            }
            return;
        }
        if (this.skillTicks == windup) {
            float yaw = this.boss.getYRot();
            if (target != null && target.isAlive()) {
                yaw = yawTo(this.boss.getX(), this.boss.getZ(), target.getX(), target.getZ());
            }
            this.faceYaw(yaw);
            double dirX = -Mth.sin(yaw * Mth.DEG_TO_RAD);
            double dirZ = Mth.cos(yaw * Mth.DEG_TO_RAD);
            Vec3 start = new Vec3(this.boss.getX() + dirX * 2.6D, this.boss.getY() + 1.2D, this.boss.getZ() + dirZ * 2.6D);
            float damage = (float) (this.attack() * AlphaYetiConfig.BOULDER_MULTIPLIER.get());
            YetiSnowball ball = new YetiSnowball(level, this.boss, start, dirX, dirZ, damage);
            level.addFreshEntity(ball);
            this.sendCast(POSE_THROW, 8);
            this.boss.playSound(TFSounds.ALPHA_YETI_THROW.get(), 4.0F, 0.6F);
            this.debug("snow boulder thrown, yaw {}", yaw);
            return;
        }
        if (this.skillTicks >= windup + THROW_RECOVER) {
            this.finishSkill();
        }
    }

    private void startRoar(ServerLevel level, boolean forced) {
        this.skill = SKILL_ROAR;
        this.skillTicks = 0;
        if (!forced) {
            this.roarCooldown = AlphaYetiConfig.ROAR_COOLDOWN.get();
        }
        int ticks = AlphaYetiConfig.ROAR_TICKS.get();
        this.sendCast(POSE_ROAR, ticks);
        this.boss.playSound(TFSounds.ALPHA_YETI_ROAR.get(), 5.0F, 0.6F);
        int count = AlphaYetiConfig.ROAR_PHASE2_COUNT.get();
        if (this.phase >= 3) {
            count = AlphaYetiConfig.ROAR_PHASE3_COUNT.get();
        }
        double range = AlphaYetiConfig.ROAR_RANGE.get();
        int total = 0;
        for (Player player : level.getEntitiesOfClass(Player.class, this.boss.getBoundingBox().inflate(range, 8.0D, range), AlphaYetiBossController::validVictim)) {
            for (int i = 0; i < count; i++) {
                double x = player.getX();
                double z = player.getZ();
                if (i > 0) {
                    double angle = this.boss.getRandom().nextDouble() * Math.PI * 2.0D;
                    double radius = 2.5D + this.boss.getRandom().nextDouble() * 2.5D;
                    x += Math.cos(angle) * radius;
                    z += Math.sin(angle) * radius;
                }
                double ground = NagaBossController.findGround(level, x, player.getY(), z);
                if (Double.isNaN(ground)) {
                    if (i > 0) {
                        continue;
                    }
                    ground = player.getY();
                }
                this.addIcicle(level, x, ground, z);
                total++;
            }
        }
        this.debug("avalanche roar (forced {}) dropped {} icicles", forced, total);
    }

    private void tickRoar() {
        if (this.skillTicks >= AlphaYetiConfig.ROAR_TICKS.get()) {
            this.finishSkill();
        }
    }

    private void addIcicle(ServerLevel level, double x, double ground, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double top = ground + 12.0D;
        for (int dy = 2; dy <= 12; dy++) {
            pos.set(Mth.floor(x), Mth.floor(ground) + dy, Mth.floor(z));
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                top = pos.getY() - 0.2D;
                break;
            }
        }
        top = Math.max(top, ground + 3.0D);
        int warning = AlphaYetiConfig.ROAR_WARNING.get();
        float radius = AlphaYetiConfig.ROAR_RADIUS.get().floatValue();
        this.icicles.add(new Icicle(x, ground, z, warning + ICICLE_FALL));
        ModNetwork.sendNear(level, x, ground, z, 96.0D, new YetiFxPacket(YetiFxPacket.ICICLE, -1, (float) x, (float) ground, (float) z, (float) top, warning, radius));
    }

    private void tickIcicles(ServerLevel level) {
        if (this.icicles.isEmpty()) {
            return;
        }
        double radius = AlphaYetiConfig.ROAR_RADIUS.get();
        float damage = (float) (this.attack() * AlphaYetiConfig.ROAR_MULTIPLIER.get());
        Iterator<Icicle> iterator = this.icicles.iterator();
        while (iterator.hasNext()) {
            Icicle icicle = iterator.next();
            icicle.ticks--;
            if (icicle.ticks > 0) {
                continue;
            }
            iterator.remove();
            AABB area = new AABB(icicle.x - radius, icicle.y - 1.0D, icicle.z - radius, icicle.x + radius, icicle.y + 3.0D, icicle.z + radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, AlphaYetiBossController::validVictim)) {
                double dx = entity.getX() - icicle.x;
                double dz = entity.getZ() - icicle.z;
                if (dx * dx + dz * dz > (radius + entity.getBbWidth() * 0.5D) * (radius + entity.getBbWidth() * 0.5D)) {
                    continue;
                }
                entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage);
                this.debug("icicle hit {}", entity.getName().getString());
            }
            level.playSound(null, icicle.x, icicle.y, icicle.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        }
    }

    public void addSlush(double x, double y, double z) {
        double radius = AlphaYetiConfig.BOULDER_SLUSH_RADIUS.get();
        int ticks = AlphaYetiConfig.BOULDER_SLUSH_TICKS.get();
        if (radius <= 0.0D || ticks <= 0 || !(this.boss.level() instanceof ServerLevel level)) {
            return;
        }
        double ground = NagaBossController.findGround(level, x, y, z);
        if (Double.isNaN(ground)) {
            ground = y;
        }
        this.slushes.add(new Slush(x, ground, z, radius, ticks));
        ModNetwork.sendNear(level, x, ground, z, 96.0D, new YetiFxPacket(YetiFxPacket.SLUSH, -1, (float) x, (float) ground, (float) z, (float) radius, ticks));
    }

    private void tickSlush(ServerLevel level) {
        if (this.slushes.isEmpty()) {
            return;
        }
        Iterator<Slush> iterator = this.slushes.iterator();
        while (iterator.hasNext()) {
            Slush slush = iterator.next();
            slush.age++;
            if (slush.age >= slush.ticks) {
                iterator.remove();
                continue;
            }
            if (slush.age % 10 != 0) {
                continue;
            }
            AABB area = new AABB(slush.x - slush.radius, slush.y - 1.0D, slush.z - slush.radius, slush.x + slush.radius, slush.y + 2.0D, slush.z + slush.radius);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, AlphaYetiBossController::validVictim)) {
                double dx = entity.getX() - slush.x;
                double dz = entity.getZ() - slush.z;
                if (dx * dx + dz * dz <= slush.radius * slush.radius) {
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), this.boss);
                }
            }
        }
    }

    public void onLanding() {
        if (!(this.boss.level() instanceof ServerLevel level)) {
            return;
        }
        float damage = (float) (this.attack() * AlphaYetiConfig.LANDING_MULTIPLIER.get());
        double knockback = Math.min(1.0D, AlphaYetiConfig.LANDING_KNOCKBACK.get());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, this.boss.getBoundingBox().inflate(5.0D, 0.0D, 5.0D), AlphaYetiBossController::validVictim)) {
            if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage) && knockback > 0.0D) {
                double dx = entity.getX() - this.boss.getX();
                double dz = entity.getZ() - this.boss.getZ();
                if (dx * dx + dz * dz > 1.0E-4D) {
                    entity.knockback(knockback, -dx, -dz);
                }
            }
        }
        if (this.phase >= 3 && AlphaYetiConfig.WAVE_ENABLED.get() && this.boss.tickCount - this.lastWaveTick >= WAVE_GAP) {
            this.lastWaveTick = this.boss.tickCount;
            Wave wave = new Wave(this.boss.getX(), this.boss.getY(), this.boss.getZ());
            this.waves.add(wave);
            ModNetwork.sendTracking(this.boss, new YetiFxPacket(YetiFxPacket.WAVE, -1, (float) wave.x, (float) wave.y, (float) wave.z,
                AlphaYetiConfig.WAVE_RADIUS.get().floatValue(), AlphaYetiConfig.WAVE_SPEED.get().floatValue()));
            this.debug("snow wave released");
        }
    }

    private void tickWaves(ServerLevel level) {
        if (this.waves.isEmpty()) {
            return;
        }
        double max = AlphaYetiConfig.WAVE_RADIUS.get();
        double speed = AlphaYetiConfig.WAVE_SPEED.get();
        float damage = (float) (this.attack() * AlphaYetiConfig.WAVE_MULTIPLIER.get());
        double knockback = Math.min(1.0D, AlphaYetiConfig.WAVE_KNOCKBACK.get());
        Iterator<Wave> iterator = this.waves.iterator();
        while (iterator.hasNext()) {
            Wave wave = iterator.next();
            wave.radius += speed;
            if (wave.radius > max) {
                iterator.remove();
                continue;
            }
            AABB area = new AABB(wave.x - wave.radius - 1.0D, wave.y - 1.0D, wave.z - wave.radius - 1.0D, wave.x + wave.radius + 1.0D, wave.y + 1.5D, wave.z + wave.radius + 1.0D);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, AlphaYetiBossController::validVictim)) {
                if (!entity.onGround() || wave.hits.contains(entity.getUUID())) {
                    continue;
                }
                double dx = entity.getX() - wave.x;
                double dz = entity.getZ() - wave.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (Math.abs(dist - wave.radius) > 0.8D + entity.getBbWidth() * 0.5D) {
                    continue;
                }
                wave.hits.add(entity.getUUID());
                if (entity.hurt(this.boss.damageSources().mobAttack(this.boss), damage) && knockback > 0.0D && dist > 0.01D) {
                    entity.knockback(knockback, -dx, -dz);
                }
            }
        }
    }

    public void onDeath() {
        this.skill = SKILL_NONE;
        this.icicles.clear();
        this.slushes.clear();
        this.waves.clear();
        this.sendCast(POSE_NONE, 0);
    }

    public void save(CompoundTag tag) {
        tag.putInt(PHASE_TAG, this.phase);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(PHASE_TAG)) {
            this.phase = Math.max(1, tag.getInt(PHASE_TAG));
        }
    }

    private static final class Icicle {
        private final double x;
        private final double y;
        private final double z;
        private int ticks;

        private Icicle(double x, double y, double z, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ticks = ticks;
        }
    }

    private static final class Slush {
        private final double x;
        private final double y;
        private final double z;
        private final double radius;
        private final int ticks;
        private int age;

        private Slush(double x, double y, double z, double radius, int ticks) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.ticks = ticks;
        }
    }

    private static final class Wave {
        private final double x;
        private final double y;
        private final double z;
        private double radius;
        private final Set<UUID> hits = new HashSet<>();

        private Wave(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
