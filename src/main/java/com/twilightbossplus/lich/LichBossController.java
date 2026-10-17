package com.twilightbossplus.lich;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.entity.SoulCandle;
import com.twilightbossplus.entity.VoidOrb;
import com.twilightbossplus.naga.NagaBossController;
import com.twilightbossplus.network.LichFxPacket;
import com.twilightbossplus.network.ModNetwork;
import com.twilightbossplus.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.monster.LichMinion;
import twilightforest.entity.projectile.LichBolt;
import twilightforest.init.TFSounds;
import twilightforest.item.LifedrainScepterItem;
import twilightforest.util.EntityUtil;

public final class LichBossController {
    public static final int KIND_SIPHON = 0;
    public static final int KIND_ABSORB = 1;
    public static final int SIGIL_MIRROR = 0;
    public static final int SIGIL_BLINK = 1;
    private static final int GLOBAL_GAP = 30;
    private static final int MIRROR_WARN = 15;
    private static final int MIRROR_FIRST_SHOT = 6;
    private static final int MIRROR_INTERVAL = 12;
    private static final int CANDLE_CHECK_INTERVAL = 5;
    private static final int SIPHON_WINDUP = 10;
    private static final int CROWN_DURATION = 60;
    private static final double CROWN_RISE = 2.0D;
    private static final double BLINK_BEHIND = 2.0D;
    private static final double BLINK_REACH = 2.5D;
    private static final float BLINK_HALF_ARC = 45.0F;
    private static final double BLINK_TRIGGER_RANGE = 16.0D;
    private static final double ORB_TRIGGER_RANGE = 24.0D;

    private final Lich lich;
    private int initialShield;
    private int lastShield = -1;
    private boolean mirrorDone;
    private int globalCooldown = GLOBAL_GAP;
    private int orbCooldown = 60;
    private int siphonCooldown = 100;
    private int blinkCooldown = 60;
    private int crownCooldown;
    private int candleCooldown;
    private int absorbCooldown;

    private int mirrorTicks = -1;
    private final List<Lich> mirrorCasters = new ArrayList<>();
    private final List<Vec3> mirrorSpots = new ArrayList<>();

    private int candleCheck;
    private int litCandles;
    private int staggerTicks;

    private int siphonTicks = -1;
    @Nullable
    private LivingEntity siphonTarget;

    private int absorbTicks = -1;
    @Nullable
    private LichMinion absorbMinion;

    private int blinkTicks = -1;
    private Vec3 blinkSpot = Vec3.ZERO;
    private float blinkYaw;

    private int crownTicks = -1;
    private double crownBaseY;

    public LichBossController(Lich lich) {
        this.lich = lich;
    }

    public float getDamageTakenMultiplier() {
        if (!LichConfig.ENABLED.get()) {
            return 1.0F;
        }
        float multiplier = 1.0F;
        if (this.staggerTicks > 0) {
            multiplier += LichConfig.CANDLE_STAGGER_DAMAGE_BONUS.get().floatValue();
        }
        if (this.crownTicks >= 0) {
            multiplier += LichConfig.CROWN_VULNERABLE_BONUS.get().floatValue();
        }
        return multiplier;
    }

    public float getAttackDamage() {
        return (float) this.lich.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("TwilightBossPlusMirror", this.mirrorDone);
        tag.putInt("TwilightBossPlusInitialShield", this.initialShield);
    }

    public void load(CompoundTag tag) {
        this.mirrorDone = tag.getBoolean("TwilightBossPlusMirror");
        this.initialShield = tag.getInt("TwilightBossPlusInitialShield");
        this.lich.setNoGravity(false);
    }

    public void onDeath() {
        if (this.siphonTicks >= 0) {
            this.endSiphon(true);
        }
        if (this.absorbTicks >= 0) {
            this.endAbsorb(true);
        }
        if (this.mirrorTicks >= 0) {
            this.endMirror();
        }
        this.blinkTicks = -1;
        if (this.crownTicks >= 0) {
            this.crownTicks = -1;
            this.lich.setNoGravity(false);
        }
    }

    public void tick() {
        if (!LichConfig.ENABLED.get() || this.lich.isShadowClone()) {
            return;
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        if (this.orbCooldown > 0) {
            this.orbCooldown--;
        }
        if (this.siphonCooldown > 0) {
            this.siphonCooldown--;
        }
        if (this.blinkCooldown > 0) {
            this.blinkCooldown--;
        }
        if (this.crownCooldown > 0) {
            this.crownCooldown--;
        }
        if (this.absorbCooldown > 0) {
            this.absorbCooldown--;
        }

        int phase = this.lich.getPhase();
        LivingEntity target = this.lich.getTarget();
        boolean targetValid = target != null && target.isAlive() && this.lich.distanceToSqr(target) < 48.0D * 48.0D;

        this.tickShield(targetValid, target);
        this.tickMirror();
        this.tickCandles(phase, targetValid);
        this.tickSiphon();
        this.tickAbsorb();
        this.tickBlink();
        this.tickCrown();
        if (this.staggerTicks > 0) {
            this.staggerTicks--;
            this.lich.getNavigation().stop();
            this.lich.setAttackCooldown(Math.max(this.lich.getAttackCooldown(), 20));
        }

        if (this.absorbTicks < 0 && this.siphonTicks < 0 && this.absorbCooldown <= 0 && this.lich.getHealth() < this.lich.getMaxHealth() * 0.5F) {
            this.tryStartAbsorb();
        }
        if (targetValid && this.canStartSkill()) {
            this.chooseSkill(target, phase);
        }
    }

    private boolean canStartSkill() {
        return this.mirrorTicks < 0
            && this.siphonTicks < 0
            && this.absorbTicks < 0
            && this.blinkTicks < 0
            && this.crownTicks < 0
            && this.staggerTicks <= 0
            && this.globalCooldown <= 0;
    }

    private void chooseSkill(LivingEntity target, int phase) {
        double distance = this.lich.distanceTo(target);
        boolean sees = this.lich.hasLineOfSight(target);
        float ratio = this.lich.getHealth() / this.lich.getMaxHealth();
        if (phase == 3 && LichConfig.CROWN_ENABLED.get() && this.crownCooldown <= 0 && ratio <= LichConfig.CROWN_HEALTH_THRESHOLD.get()) {
            this.startCrown();
            return;
        }
        if (phase == 3 && LichConfig.BLINK_ENABLED.get() && this.blinkCooldown <= 0 && distance < BLINK_TRIGGER_RANGE && sees) {
            if (this.startBlink(target)) {
                return;
            }
            this.blinkCooldown = 20;
        }
        if (phase >= 2 && LichConfig.SIPHON_ENABLED.get() && this.siphonCooldown <= 0 && distance < LichConfig.SIPHON_BREAK_RANGE.get() - 2.0D && sees) {
            this.startSiphon(target);
            return;
        }
        if (LichConfig.VOID_ORB_ENABLED.get() && this.orbCooldown <= 0 && distance < ORB_TRIGGER_RANGE && sees) {
            this.castVoidOrb(target);
        }
    }

    private void tickShield(boolean targetValid, @Nullable LivingEntity target) {
        int shield = this.lich.getShieldStrength();
        if (shield > this.initialShield) {
            this.initialShield = shield;
        }
        if (this.lastShield >= 0 && shield < this.lastShield) {
            ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.SHIELD_BREAK, this.lich.getId(), shield, this.lastShield));
            this.debug("Shield broken {} -> {}", this.lastShield, shield);
        }
        this.lastShield = shield;
        if (!this.mirrorDone && LichConfig.MIRROR_ENABLED.get() && targetValid && this.mirrorTicks < 0 && shield > 0 && shield * 2 <= this.initialShield) {
            this.startMirror(target);
        }
    }

    private void castVoidOrb(LivingEntity target) {
        float damage = this.getAttackDamage() * LichConfig.VOID_ORB_MULTIPLIER.get().floatValue();
        VoidOrb orb = new VoidOrb(this.lich.level(), this.lich, target, damage);
        this.lich.level().addFreshEntity(orb);
        this.lich.swing(InteractionHand.MAIN_HAND);
        this.lich.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.0F, 0.6F);
        this.orbCooldown = LichConfig.VOID_ORB_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        this.debug("Void Orb cast at {}, damage {}", target.getName().getString(), damage);
    }

    private void startMirror(LivingEntity target) {
        this.mirrorDone = true;
        this.mirrorCasters.clear();
        this.mirrorSpots.clear();
        this.mirrorCasters.add(this.lich);
        for (Lich other : this.lich.getNearbyLiches()) {
            if (this.mirrorCasters.size() >= 3) {
                break;
            }
            if (other.isShadowClone() && other.getMasterLich() == this.lich && other.isAlive()) {
                this.mirrorCasters.add(other);
            }
        }
        Level level = this.lich.level();
        float baseYaw = NagaBossController.yawTo(target.position(), this.lich.position());
        double radius = LichConfig.MIRROR_RADIUS.get();
        for (int i = 0; i < this.mirrorCasters.size(); i++) {
            Lich caster = this.mirrorCasters.get(i);
            Vec3 dir = NagaBossController.directionOf(baseYaw + i * 120.0F);
            Vec3 spot = null;
            for (int attempt = 0; attempt < 3 && spot == null; attempt++) {
                double distance = radius * (1.0D - attempt * 0.25D);
                double x = target.getX() + dir.x * distance;
                double z = target.getZ() + dir.z * distance;
                double ground = NagaBossController.findGround(level, x, target.getY(), z);
                if (Double.isNaN(ground)) {
                    continue;
                }
                AABB box = caster.getType().getDimensions().makeBoundingBox(x, ground, z);
                if (level.noCollision(caster, box)) {
                    spot = new Vec3(x, ground, z);
                }
            }
            if (spot == null) {
                spot = caster.position();
            }
            this.mirrorSpots.add(spot);
            float facing = NagaBossController.yawTo(spot, target.position());
            ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.SIGIL, this.lich.getId(),
                (float) spot.x, (float) spot.y + 1.1F, (float) spot.z, facing, MIRROR_WARN + 4, SIGIL_MIRROR));
        }
        this.mirrorTicks = 0;
        this.lich.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 3.0F, 0.8F);
        this.debug("Mirror Corridor started with {} casters", this.mirrorCasters.size());
    }

    private void tickMirror() {
        if (this.mirrorTicks < 0) {
            return;
        }
        this.mirrorTicks++;
        for (Lich caster : this.mirrorCasters) {
            if (caster.isAlive()) {
                caster.setAttackCooldown(100);
                caster.getNavigation().stop();
            }
        }
        LivingEntity target = this.lich.getTarget();
        if (target == null || !target.isAlive()) {
            this.endMirror();
            return;
        }
        if (this.mirrorTicks == MIRROR_WARN) {
            for (int i = 0; i < this.mirrorCasters.size(); i++) {
                Lich caster = this.mirrorCasters.get(i);
                if (!caster.isAlive()) {
                    continue;
                }
                Vec3 from = caster.position();
                Vec3 spot = this.mirrorSpots.get(i);
                caster.teleportTo(spot.x, spot.y, spot.z);
                faceTarget(caster, target);
                sendTeleport(caster, from, spot);
                caster.level().playSound(null, spot.x, spot.y, spot.z, TFSounds.LICH_TELEPORT.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
        int shots = LichConfig.MIRROR_ROUNDS.get() * this.mirrorCasters.size();
        int sinceFirst = this.mirrorTicks - MIRROR_WARN - MIRROR_FIRST_SHOT;
        if (sinceFirst >= 0 && sinceFirst % MIRROR_INTERVAL == 0 && sinceFirst / MIRROR_INTERVAL < shots) {
            Lich caster = this.mirrorCasters.get((sinceFirst / MIRROR_INTERVAL) % this.mirrorCasters.size());
            if (caster.isAlive()) {
                caster.setTarget(target);
                faceTarget(caster, target);
                caster.launchProjectileAt(new LichBolt(caster.level(), caster));
                caster.swing(InteractionHand.MAIN_HAND);
            }
        }
        if (sinceFirst > MIRROR_INTERVAL * shots) {
            this.endMirror();
        }
    }

    private void endMirror() {
        this.mirrorTicks = -1;
        for (Lich caster : this.mirrorCasters) {
            if (caster.isAlive()) {
                caster.setAttackCooldown(80);
            }
        }
        this.mirrorCasters.clear();
        this.mirrorSpots.clear();
        this.globalCooldown = GLOBAL_GAP;
        this.debug("Mirror Corridor ended");
    }

    private void tickCandles(int phase, boolean targetValid) {
        if (!LichConfig.CANDLE_ENABLED.get()) {
            return;
        }
        this.candleCheck++;
        if (this.candleCheck % CANDLE_CHECK_INTERVAL != 0) {
            return;
        }
        List<SoulCandle> candles = this.lich.level().getEntitiesOfClass(SoulCandle.class, this.lich.getBoundingBox().inflate(40.0D, 20.0D, 40.0D),
            candle -> candle.isAlive() && candle.isOwnedBy(this.lich));
        int count = candles.size();
        if (this.litCandles > 0 && count == 0 && phase == 2) {
            this.startStagger();
        }
        this.litCandles = count;
        if (count == 0 && this.candleCooldown > 0) {
            this.candleCooldown -= CANDLE_CHECK_INTERVAL;
        }
        if (phase != 2) {
            return;
        }
        if (count == 0 && this.candleCooldown <= 0 && targetValid) {
            this.lightCandles();
            return;
        }
        if (count > 0 && this.candleCheck % 20 == 0) {
            for (LichMinion minion : this.lich.level().getEntitiesOfClass(LichMinion.class, this.lich.getBoundingBox().inflate(32.0D, 16.0D, 32.0D),
                minion -> minion.master == this.lich && minion.isAlive())) {
                minion.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
                if (count >= 2) {
                    minion.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
                }
                if (count >= 3) {
                    minion.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, true, false));
                }
            }
        }
    }

    private void lightCandles() {
        Level level = this.lich.level();
        int wanted = LichConfig.CANDLE_COUNT.get();
        float base = this.lich.getRandom().nextFloat() * Mth.TWO_PI;
        Vec3 eye = this.lich.getEyePosition();
        int placed = 0;
        for (int i = 0; i < wanted; i++) {
            for (int attempt = 0; attempt < 8; attempt++) {
                float angle = base + Mth.TWO_PI * i / wanted + (this.lich.getRandom().nextFloat() - 0.5F) * 0.6F;
                double distance = 6.0D + this.lich.getRandom().nextDouble() * 4.0D;
                double x = this.lich.getX() + Mth.cos(angle) * distance;
                double z = this.lich.getZ() + Mth.sin(angle) * distance;
                double ground = NagaBossController.findGround(level, x, this.lich.getY(), z);
                if (Double.isNaN(ground)) {
                    continue;
                }
                double y = ground + 1.6D;
                AABB box = ModEntities.SOUL_CANDLE.get().getDimensions().makeBoundingBox(x, y, z);
                if (!level.noCollision(box)) {
                    continue;
                }
                Vec3 top = new Vec3(x, y + 0.6D, z);
                if (level.clip(new ClipContext(eye, top, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.lich)).getType() != HitResult.Type.MISS) {
                    continue;
                }
                SoulCandle candle = new SoulCandle(level, this.lich);
                candle.moveTo(x, y, z, 0.0F, 0.0F);
                level.addFreshEntity(candle);
                ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.BEAM, this.lich.getId(),
                    (float) eye.x, (float) eye.y, (float) eye.z, (float) top.x, (float) top.y, (float) top.z, 0.37F, 0.99F, 0.89F));
                placed++;
                break;
            }
        }
        this.candleCooldown = LichConfig.CANDLE_COOLDOWN_TICKS.get();
        if (placed > 0) {
            this.lich.swing(InteractionHand.MAIN_HAND);
            this.lich.playSound(SoundEvents.SOUL_ESCAPE, 3.0F, 0.6F);
        }
        this.debug("Soul Candles lit {}/{}", placed, wanted);
    }

    private void startStagger() {
        this.staggerTicks = LichConfig.CANDLE_STAGGER_TICKS.get();
        if (this.siphonTicks >= 0) {
            this.endSiphon(true);
        }
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.STAGGER, this.lich.getId(), this.staggerTicks));
        this.lich.playSound(TFSounds.LICH_HURT.get(), 2.5F, 0.5F);
        this.debug("All Soul Candles snuffed, staggered for {} ticks", this.staggerTicks);
    }

    private void startSiphon(LivingEntity target) {
        this.siphonTicks = 0;
        this.siphonTarget = target;
        int duration = LichConfig.SIPHON_DURATION.get();
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.CHAIN_START, this.lich.getId(), target.getId(), KIND_SIPHON, SIPHON_WINDUP, duration));
        this.lich.swing(InteractionHand.MAIN_HAND);
        this.lich.playSound(TFSounds.SCEPTER_DRAIN.get(), 2.0F, 0.6F);
        this.debug("Life Siphon on {}", target.getName().getString());
    }

    private void tickSiphon() {
        if (this.siphonTicks < 0) {
            return;
        }
        this.siphonTicks++;
        LivingEntity target = this.siphonTarget;
        if (target == null || !target.isAlive() || this.lich.distanceTo(target) > LichConfig.SIPHON_BREAK_RANGE.get() || !this.lich.hasLineOfSight(target)) {
            this.endSiphon(true);
            return;
        }
        this.lich.getLookControl().setLookAt(target, 30.0F, 30.0F);
        int active = this.siphonTicks - SIPHON_WINDUP;
        if (active > 0 && active % 20 == 0) {
            float before = target.getHealth();
            float damage = this.getAttackDamage() * LichConfig.SIPHON_MULTIPLIER.get().floatValue();
            if (target.hurt(this.lich.damageSources().indirectMagic(this.lich, this.lich), damage)) {
                float dealt = Math.max(0.0F, before - target.getHealth());
                float heal = dealt * LichConfig.SIPHON_HEAL_RATIO.get().floatValue();
                this.lich.heal(heal);
                this.debug("Life Siphon dealt {} healed {}", dealt, heal);
            }
        }
        if (active >= LichConfig.SIPHON_DURATION.get()) {
            this.endSiphon(false);
        }
    }

    private void endSiphon(boolean broken) {
        this.siphonTicks = -1;
        this.siphonTarget = null;
        this.siphonCooldown = LichConfig.SIPHON_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        float brokenFlag = 0.0F;
        if (broken) {
            brokenFlag = 1.0F;
            this.lich.playSound(SoundEvents.CHAIN_BREAK, 2.0F, 0.6F);
        }
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.CHAIN_END, this.lich.getId(), KIND_SIPHON, brokenFlag));
        this.debug("Life Siphon ended, broken {}", broken);
    }

    private void tryStartAbsorb() {
        LichMinion nearest = null;
        double best = Double.MAX_VALUE;
        for (LichMinion minion : this.lich.level().getEntitiesOfClass(LichMinion.class, this.lich.getBoundingBox().inflate(32.0D, 16.0D, 32.0D),
            minion -> minion.master == this.lich && minion.isAlive())) {
            double distance = this.lich.distanceToSqr(minion);
            if (distance < best) {
                best = distance;
                nearest = minion;
            }
        }
        if (nearest == null) {
            return;
        }
        this.absorbTicks = 0;
        this.absorbMinion = nearest;
        this.lich.setScepterTime();
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.CHAIN_START, this.lich.getId(), nearest.getId(), KIND_ABSORB, 0, LichConfig.ABSORB_CHANNEL_TICKS.get()));
        this.lich.playSound(TFSounds.SCEPTER_DRAIN.get(), 2.0F, 0.9F);
        this.debug("Absorb channel started on minion {}", nearest.getId());
    }

    private void tickAbsorb() {
        if (this.absorbTicks < 0) {
            return;
        }
        this.absorbTicks++;
        LichMinion minion = this.absorbMinion;
        if (minion == null || !minion.isAlive() || minion.isRemoved()) {
            this.endAbsorb(true);
            return;
        }
        this.lich.getNavigation().stop();
        this.lich.getLookControl().setLookAt(minion, 30.0F, 30.0F);
        if (this.absorbTicks < LichConfig.ABSORB_CHANNEL_TICKS.get()) {
            return;
        }
        float heal = minion.getHealth();
        minion.discard();
        if (this.lich.level() instanceof ServerLevel serverLevel) {
            LifedrainScepterItem.animateTargetShatter(serverLevel, minion);
        }
        SoundEvent deathSound = EntityUtil.getDeathSound(minion);
        if (deathSound != null) {
            this.lich.level().playSound(null, minion.blockPosition(), deathSound, SoundSource.HOSTILE, 1.0F, minion.getVoicePitch());
        }
        this.lich.playSound(TFSounds.LICH_POP_MOB.get(), 3.0F, 0.4F + this.lich.getRandom().nextFloat() * 0.2F);
        this.lich.heal(heal);
        this.lich.swing(InteractionHand.MAIN_HAND);
        this.debug("Absorbed minion for {}", heal);
        this.endAbsorb(false);
    }

    private void endAbsorb(boolean broken) {
        this.absorbTicks = -1;
        this.absorbMinion = null;
        this.absorbCooldown = 60;
        this.lich.resetScepterTime();
        float brokenFlag = 0.0F;
        if (broken) {
            brokenFlag = 1.0F;
            this.lich.playSound(SoundEvents.CHAIN_BREAK, 2.0F, 0.8F);
            this.debug("Absorb channel interrupted");
        }
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.CHAIN_END, this.lich.getId(), KIND_ABSORB, brokenFlag));
    }

    private boolean startBlink(LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = target.position().subtract(this.lich.position()).multiply(1.0D, 0.0D, 1.0D);
        }
        if (flat.lengthSqr() < 1.0E-4D) {
            return false;
        }
        flat = flat.normalize();
        Level level = this.lich.level();
        double x = target.getX() - flat.x * BLINK_BEHIND;
        double z = target.getZ() - flat.z * BLINK_BEHIND;
        double ground = NagaBossController.findGround(level, x, target.getY(), z);
        if (Double.isNaN(ground)) {
            return false;
        }
        AABB box = this.lich.getType().getDimensions().makeBoundingBox(x, ground, z);
        if (!level.noCollision(this.lich, box)) {
            return false;
        }
        this.blinkSpot = new Vec3(x, ground, z);
        this.blinkYaw = NagaBossController.yawTo(this.blinkSpot, target.position());
        this.blinkTicks = 0;
        int warn = LichConfig.BLINK_WARNING_TICKS.get();
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.SIGIL, this.lich.getId(),
            (float) x, (float) ground + 1.1F, (float) z, this.blinkYaw, warn + 3, SIGIL_BLINK));
        level.playSound(null, x, ground, z, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE, 2.0F, 1.2F);
        this.debug("Twilight Blink marked behind {}", target.getName().getString());
        return true;
    }

    private void tickBlink() {
        if (this.blinkTicks < 0) {
            return;
        }
        this.blinkTicks++;
        if (this.blinkTicks < LichConfig.BLINK_WARNING_TICKS.get()) {
            return;
        }
        this.blinkTicks = -1;
        this.blinkCooldown = LichConfig.BLINK_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        Vec3 from = this.lich.position();
        this.lich.teleportTo(this.blinkSpot.x, this.blinkSpot.y, this.blinkSpot.z);
        this.lich.setYRot(this.blinkYaw);
        this.lich.yBodyRot = this.blinkYaw;
        this.lich.yHeadRot = this.blinkYaw;
        this.lich.getNavigation().stop();
        sendTeleport(this.lich, from, this.blinkSpot);
        this.lich.swing(InteractionHand.MAIN_HAND);
        Level level = this.lich.level();
        level.playSound(null, this.blinkSpot.x, this.blinkSpot.y, this.blinkSpot.z, TFSounds.LICH_TELEPORT.get(), SoundSource.HOSTILE, 1.0F, 1.2F);
        level.playSound(null, this.blinkSpot.x, this.blinkSpot.y, this.blinkSpot.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.7F);
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.SLASH, this.lich.getId(),
            (float) this.blinkSpot.x, (float) this.blinkSpot.y, (float) this.blinkSpot.z, this.blinkYaw, (float) BLINK_REACH));
        float damage = this.getAttackDamage() * LichConfig.BLINK_MULTIPLIER.get().floatValue();
        double knockback = LichConfig.BLINK_KNOCKBACK.get();
        int hits = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.blinkSpot, this.blinkSpot).inflate(BLINK_REACH + 1.0D, 2.5D, BLINK_REACH + 1.0D))) {
            if (living instanceof Lich || living instanceof LichMinion || !living.isAlive()) {
                continue;
            }
            double dx = living.getX() - this.blinkSpot.x;
            double dz = living.getZ() - this.blinkSpot.z;
            double reach = BLINK_REACH + living.getBbWidth() * 0.5D;
            if (dx * dx + dz * dz > reach * reach) {
                continue;
            }
            float angle = NagaBossController.yawTo(this.blinkSpot, living.position());
            if (dx * dx + dz * dz > 0.36D && Math.abs(Mth.wrapDegrees(angle - this.blinkYaw)) > BLINK_HALF_ARC + 10.0F) {
                continue;
            }
            if (living.hurt(this.lich.damageSources().mobAttack(this.lich), damage)) {
                living.knockback(knockback, -dx, -dz);
                hits++;
            }
        }
        this.debug("Twilight Blink hit {} targets for {}", hits, damage);
    }

    private void startCrown() {
        this.crownTicks = 0;
        this.crownBaseY = this.lich.getY();
        this.lich.setNoGravity(true);
        this.lich.getNavigation().stop();
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.CROWN_START, this.lich.getId(),
            CROWN_DURATION, LichConfig.CROWN_PULL_RANGE.get().floatValue(), LichConfig.CROWN_RADIUS.get().floatValue()));
        this.lich.playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.5F);
        this.lich.playSound(TFSounds.LICH_AMBIENT.get(), 3.0F, 0.6F);
        this.debug("Crown Collapse channel started at health {}", this.lich.getHealth());
    }

    private void tickCrown() {
        if (this.crownTicks < 0) {
            return;
        }
        this.crownTicks++;
        this.lich.getNavigation().stop();
        this.lich.getMoveControl().setWantedPosition(this.lich.getX(), this.lich.getY(), this.lich.getZ(), 0.0D);
        double rise = this.crownBaseY + CROWN_RISE - this.lich.getY();
        this.lich.setDeltaMovement(0.0D, Mth.clamp(rise * 0.25D, -0.1D, 0.15D), 0.0D);
        Level level = this.lich.level();
        double range = LichConfig.CROWN_PULL_RANGE.get();
        double strength = LichConfig.CROWN_PULL_STRENGTH.get();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.lich.getBoundingBox().inflate(range, 6.0D, range))) {
            if (living instanceof Lich || !living.isAlive()) {
                continue;
            }
            if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            double dx = this.lich.getX() - living.getX();
            double dz = this.lich.getZ() - living.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance > range || distance < 0.8D) {
                continue;
            }
            double factor = strength * (1.0D - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (factor <= 0.0D) {
                continue;
            }
            living.setDeltaMovement(living.getDeltaMovement().add(dx / distance * factor, 0.0D, dz / distance * factor));
            living.hurtMarked = true;
        }
        if (this.crownTicks < CROWN_DURATION) {
            return;
        }
        double radius = LichConfig.CROWN_RADIUS.get();
        float damage = this.getAttackDamage() * LichConfig.CROWN_MULTIPLIER.get().floatValue();
        double knockback = LichConfig.CROWN_KNOCKBACK.get();
        int hits = 0;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.lich.getBoundingBox().inflate(radius, 4.0D, radius))) {
            if (living instanceof Lich || !living.isAlive()) {
                continue;
            }
            double dx = living.getX() - this.lich.getX();
            double dz = living.getZ() - this.lich.getZ();
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (living.hurt(this.lich.damageSources().mobAttack(this.lich), damage)) {
                living.knockback(knockback, -dx, -dz);
                hits++;
            }
        }
        ModNetwork.sendTracking(this.lich, new LichFxPacket(LichFxPacket.CROWN_BURST, this.lich.getId(),
            (float) this.lich.getX(), (float) this.crownBaseY, (float) this.lich.getZ(), (float) radius));
        level.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, this.lich.getX(), this.lich.getY(), this.lich.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        this.crownTicks = -1;
        this.lich.setNoGravity(false);
        this.crownCooldown = LichConfig.CROWN_COOLDOWN_TICKS.get();
        this.globalCooldown = GLOBAL_GAP;
        this.debug("Crown Collapse burst hit {} targets for {}", hits, damage);
    }

    public static void sendTeleport(Lich caster, Vec3 from, Vec3 to) {
        ModNetwork.sendTracking(caster, new LichFxPacket(LichFxPacket.TELEPORT, caster.getId(),
            (float) from.x, (float) from.y, (float) from.z, (float) to.x, (float) to.y, (float) to.z, caster.yBodyRot));
    }

    private static void faceTarget(Lich caster, LivingEntity target) {
        float yaw = NagaBossController.yawTo(caster.position(), target.position());
        caster.setYRot(yaw);
        caster.yBodyRot = yaw;
        caster.yHeadRot = yaw;
    }

    private void debug(String message, Object... args) {
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[Lich " + this.lich.getId() + "] " + message, args);
        }
    }
}
