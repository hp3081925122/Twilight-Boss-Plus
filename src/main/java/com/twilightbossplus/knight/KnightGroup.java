package com.twilightbossplus.knight;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.GeneralConfig;
import com.twilightbossplus.config.KnightPhantomConfig;
import com.twilightbossplus.naga.NagaEvents;
import com.twilightbossplus.network.KnightFxPacket;
import com.twilightbossplus.network.ModNetwork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.entity.projectile.ThrownWep;
import twilightforest.init.TFDamageTypes;
import twilightforest.init.TFEntities;
import twilightforest.init.TFItems;
import twilightforest.init.TFSounds;

public final class KnightGroup {
    public static final String PHASE_TAG = "TwilightBossPlusKnightPhase";
    public static final String INITIAL_TAG = "TwilightBossPlusKnightInitial";
    private static final UUID OATH_UUID = UUID.fromString("7a2e9c41-d85b-4f3a-b016-4c9e83f2a7d0");
    private static final double SEARCH_RADIUS = 48.0D;
    private static final double SEND_RADIUS = 96.0D;
    private static final double CROSS_RADIUS = 7.0D;
    private static final int DASH_LINGER = 8;
    private static final int THROW_LINGER = 10;
    private static final int THRUST_TICKS = 6;
    private static final int ENCIRCLE_LINGER = 6;
    private static final int OATH_SYNC_INTERVAL = 40;
    private static final int EMPTY_LIMIT = 200;

    private final ServerLevel level;
    private final BlockPos home;
    private final List<KnightPhantom> members = new ArrayList<>();
    private final Map<Integer, KnightState> states = new HashMap<>();
    private final List<Echo> echoes = new ArrayList<>();
    private int initialCount;
    private int phase = 1;
    private int regroupTicks;
    private int encircleCooldown;
    private int crossCooldown;
    private int emptyTicks;
    private int oathStacks;
    private int oathSyncTimer;
    @Nullable
    private Encircle encircle;

    public KnightGroup(ServerLevel level, BlockPos home) {
        this.level = level;
        this.home = home;
    }

    public BlockPos getHome() {
        return this.home;
    }

    public boolean isRegrouping() {
        return this.regroupTicks > 0;
    }

    public boolean isFinished() {
        return this.members.isEmpty() && this.echoes.isEmpty() && this.emptyTicks > EMPTY_LIMIT;
    }

    public int getPhase() {
        return this.phase;
    }

    public int getInitialCount() {
        return this.initialCount;
    }

    public float getSpeedFactor() {
        if (this.phase < 3 || !KnightPhantomConfig.OATH_ENABLED.get()) {
            return 1.0F;
        }
        return 1.0F + (float) (this.oathStacks * KnightPhantomConfig.OATH_SPEED_PER_STACK.get());
    }

    public boolean blocksAttackAssignment() {
        return this.regroupTicks > 0 || this.encircle != null;
    }

    public void save(CompoundTag tag) {
        tag.putInt(PHASE_TAG, this.phase);
        tag.putInt(INITIAL_TAG, this.initialCount);
    }

    private KnightState state(KnightPhantom knight) {
        KnightState state = this.states.get(knight.getId());
        if (state == null) {
            state = new KnightState();
            state.lastFormation = knight.getCurrentFormation();
            this.states.put(knight.getId(), state);
            if (knight instanceof KnightDataHolder holder) {
                this.phase = Math.max(this.phase, holder.twilightbossplus$getSavedPhase());
                this.initialCount = Math.max(this.initialCount, holder.twilightbossplus$getSavedInitial());
            }
        }
        return state;
    }

    public void tick() {
        this.refreshMembers();
        this.tickEchoes();
        if (this.members.isEmpty()) {
            this.emptyTicks++;
            if (this.encircle != null) {
                this.endEncircle();
            }
            return;
        }
        this.emptyTicks = 0;
        if (this.encircleCooldown > 0) {
            this.encircleCooldown--;
        }
        if (this.crossCooldown > 0) {
            this.crossCooldown--;
        }
        this.updatePhase();
        this.updateOath();
        if (this.regroupTicks > 0) {
            this.tickRegroup();
            return;
        }
        for (KnightPhantom knight : this.members) {
            this.tickKnight(knight, this.state(knight));
        }
        if (this.encircle != null) {
            this.tickEncircle();
        }
    }

    private void refreshMembers() {
        this.members.clear();
        AABB box = new AABB(this.home).inflate(SEARCH_RADIUS);
        for (KnightPhantom knight : this.level.getEntitiesOfClass(KnightPhantom.class, box, LivingEntity::isAlive)) {
            if (knight.getRestrictionPoint() != null && knight.getRestrictionPoint().pos().equals(this.home)) {
                this.members.add(knight);
            }
        }
        this.members.sort(Comparator.comparingInt(KnightPhantom::getNumber));
        Set<Integer> alive = new HashSet<>();
        for (KnightPhantom knight : this.members) {
            alive.add(knight.getId());
            this.state(knight);
        }
        this.states.keySet().removeIf(id -> !alive.contains(id));
        this.initialCount = Math.max(this.initialCount, this.members.size());
    }

    private void updatePhase() {
        float maxHealth = this.members.get(0).getMaxHealth();
        float health = 0.0F;
        for (KnightPhantom knight : this.members) {
            health += knight.getHealth();
        }
        float ratio = health / Math.max(1.0F, maxHealth * Math.max(1, this.initialCount));
        int target = 1;
        if (ratio <= KnightPhantomConfig.PHASE2_THRESHOLD.get()) {
            target = 2;
        }
        if (ratio <= KnightPhantomConfig.PHASE3_THRESHOLD.get() || (this.members.size() == 1 && this.initialCount > 1)) {
            target = 3;
        }
        if (target > this.phase) {
            if (GeneralConfig.DEBUG_LOG.get()) {
                TwilightBossPlus.LOGGER.info("[KnightPhantom] group at {} phase {} -> {}, ratio {}", this.home, this.phase, target, ratio);
            }
            this.phase = target;
            this.startRegroup();
        }
    }

    private void updateOath() {
        int stacks = 0;
        if (this.phase >= 3 && KnightPhantomConfig.OATH_ENABLED.get()) {
            stacks = Math.min(KnightPhantomConfig.OATH_MAX_STACKS.get(), Math.max(0, this.initialCount - this.members.size()));
        }
        boolean changed = stacks != this.oathStacks;
        this.oathStacks = stacks;
        this.oathSyncTimer--;
        if (changed || this.oathSyncTimer <= 0) {
            this.oathSyncTimer = OATH_SYNC_INTERVAL;
            for (KnightPhantom knight : this.members) {
                NagaEvents.applyMultiplier(knight.getAttribute(Attributes.ATTACK_DAMAGE), OATH_UUID, "Boss Plus oath", 1.0D + stacks * KnightPhantomConfig.OATH_ATTACK_PER_STACK.get());
                ModNetwork.sendTracking(knight, new KnightFxPacket(KnightFxPacket.OATH, knight.getId(), stacks));
            }
        }
    }

    private Vec3 homeCenter() {
        return new Vec3(this.home.getX() + 0.5D, this.home.getY(), this.home.getZ() + 0.5D);
    }

    private void startRegroup() {
        this.regroupTicks = KnightPhantomConfig.REGROUP_TICKS.get();
        if (this.encircle != null) {
            this.endEncircle();
        }
        for (KnightPhantom knight : this.members) {
            KnightState state = this.state(knight);
            this.clearAttack(knight, state);
            knight.switchToFormation(KnightPhantom.Formation.HOVER);
            state.lastFormation = KnightPhantom.Formation.HOVER;
        }
        Vec3 center = this.homeCenter();
        ModNetwork.sendNear(this.level, center.x, center.y, center.z, SEND_RADIUS,
            new KnightFxPacket(KnightFxPacket.REGROUP, -1, (float) center.x, (float) center.y, (float) center.z, this.regroupTicks, this.phase));
        this.level.playSound(null, center.x, center.y, center.z, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    private void tickRegroup() {
        this.regroupTicks--;
        Vec3 center = this.homeCenter();
        int count = this.members.size();
        for (int i = 0; i < count; i++) {
            KnightPhantom knight = this.members.get(i);
            knight.setTicksProgress(0);
            double angle = Math.PI * 2.0D * i / count;
            double radius = 1.5D;
            if (count == 1) {
                radius = 0.0D;
            }
            Vec3 target = new Vec3(center.x + Math.cos(angle) * radius, center.y + 0.5D, center.z + Math.sin(angle) * radius);
            Vec3 current = knight.position();
            this.place(knight, current.add(target.subtract(current).scale(0.3D)), null);
            if (this.regroupTicks == 0) {
                knight.setTicksProgress(knight.getMaxTicksForFormation());
            }
        }
    }

    private void tickKnight(KnightPhantom knight, KnightState state) {
        KnightPhantom.Formation formation = knight.getCurrentFormation();
        if (formation != state.lastFormation) {
            this.onFormationChange(knight, state, state.lastFormation, formation);
            state.lastFormation = formation;
        }
        state.phaseAge++;
        if (this.encircle != null && this.encircle.participants.contains(knight)) {
            return;
        }
        if (formation == KnightPhantom.Formation.ATTACK_PLAYER_START) {
            this.tickWindup(knight, state);
        } else if (formation == KnightPhantom.Formation.ATTACK_PLAYER_ATTACK) {
            this.tickAttack(knight, state);
        }
        if (!state.followEchoes.isEmpty() && formation != KnightPhantom.Formation.ATTACK_PLAYER_START && formation != KnightPhantom.Formation.ATTACK_PLAYER_ATTACK) {
            this.releaseFollowEchoes(state);
        }
    }

    private void onFormationChange(KnightPhantom knight, KnightState state, KnightPhantom.Formation old, KnightPhantom.Formation now) {
        state.phaseAge = 0;
        boolean attacking = now == KnightPhantom.Formation.ATTACK_PLAYER_START || now == KnightPhantom.Formation.ATTACK_PLAYER_ATTACK;
        if (!attacking) {
            this.clearAttack(knight, state);
            return;
        }
        if (now == KnightPhantom.Formation.ATTACK_PLAYER_START) {
            state.hits.clear();
            state.locked = false;
            state.dashStart = null;
            state.dashEnd = null;
            LivingEntity target = knight.getTarget();
            if (this.isDasher(knight, state) && target != null) {
                ModNetwork.sendTracking(knight, new KnightFxPacket(KnightFxPacket.TRACK, knight.getId(), target.getId()));
            }
        } else {
            state.hits.clear();
            state.dashAge = 0;
        }
    }

    private void clearAttack(KnightPhantom knight, KnightState state) {
        state.locked = false;
        state.dashStart = null;
        state.dashEnd = null;
        state.cross = false;
        state.crossIndex = 0;
        state.hits.clear();
        if (state.swapped) {
            state.swapped = false;
            state.weaponOverride = null;
            knight.setNumber(knight.getNumber());
        }
    }

    private boolean isDasher(KnightPhantom knight, KnightState state) {
        return state.cross || knight.isSwordKnight();
    }

    private void tickWindup(KnightPhantom knight, KnightState state) {
        LivingEntity target = knight.getTarget();
        int track = KnightPhantomConfig.CHARGE_TRACK_TICKS.get();
        int lock = KnightPhantomConfig.CHARGE_LOCK_TICKS.get();
        if (!state.locked) {
            if (target != null && target.isAlive()) {
                if (target.blockPosition().distSqr(this.home) < 30.0D * 30.0D) {
                    knight.setChargePos(target.blockPosition());
                } else {
                    knight.setChargePos(this.home);
                }
            }
            if (state.cross && target != null) {
                double angle = state.crossAngle;
                Vec3 desired = new Vec3(target.getX() + Math.cos(angle) * CROSS_RADIUS, target.getY(), target.getZ() + Math.sin(angle) * CROSS_RADIUS);
                Vec3 current = knight.position();
                this.place(knight, current.add(desired.subtract(current).scale(0.2D)), target.position());
            }
            if (state.phaseAge >= track && this.isDasher(knight, state) && target != null) {
                this.lock(knight, state);
            }
        }
        if (state.locked && state.lockPos != null) {
            this.place(knight, state.lockPos, state.dashEnd);
        }
        if (state.phaseAge >= track + lock) {
            knight.setTicksProgress(knight.getMaxTicksForFormation());
        }
    }

    private void lock(KnightPhantom knight, KnightState state) {
        state.locked = true;
        Vec3 start = knight.position();
        BlockPos chargePos = knight.getChargePos();
        Vec3 point = new Vec3(chargePos.getX() + 0.5D, chargePos.getY(), chargePos.getZ() + 0.5D);
        Vec3 flat = new Vec3(point.x - start.x, 0.0D, point.z - start.z);
        if (flat.lengthSqr() < 0.25D) {
            flat = knight.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 dir = flat.normalize();
        Vec3 end = point.add(dir.scale(KnightPhantomConfig.CHARGE_OVERSHOOT.get()));
        state.lockPos = start;
        state.dashStart = start;
        state.dashEnd = end;
        state.dashDelay = 0;
        if (state.cross && state.crossIndex == 1) {
            state.dashDelay = KnightPhantomConfig.CROSS_SECOND_DELAY.get();
        }
        int lockTicks = KnightPhantomConfig.CHARGE_LOCK_TICKS.get();
        int dashTicks = this.dashTicks(start, end);
        float dim = 0.0F;
        if (state.cross && state.crossIndex == 1) {
            dim = 1.0F;
        }
        ModNetwork.sendTracking(knight, new KnightFxPacket(KnightFxPacket.LINE, knight.getId(),
            (float) start.x, (float) start.y, (float) start.z, (float) end.x, (float) end.y, (float) end.z, lockTicks + state.dashDelay, dim, dashTicks));
        knight.playSound(SoundEvents.ARMOR_EQUIP_NETHERITE, 1.5F, 0.6F);
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom {}] locked charge from {} to {} cross {}", knight.getId(), start, end, state.cross);
        }
        if (!state.followEchoes.isEmpty()) {
            this.launchFollowEchoes(knight, state, start, end, lockTicks);
        }
    }

    private int dashTicks(Vec3 start, Vec3 end) {
        double speed = KnightPhantomConfig.CHARGE_SPEED.get() * this.getSpeedFactor();
        return Mth.ceil(start.distanceTo(end) / Math.max(0.05D, speed));
    }

    private void tickAttack(KnightPhantom knight, KnightState state) {
        LivingEntity target = knight.getTarget();
        if (this.isDasher(knight, state) && state.dashStart != null && state.dashEnd != null) {
            this.tickDash(knight, state);
            return;
        }
        if (knight.isAxeKnight() && target != null) {
            int volleys = KnightPhantomConfig.AXE_VOLLEYS.get();
            int interval = KnightPhantomConfig.AXE_VOLLEY_INTERVAL.get();
            int glow = KnightPhantomConfig.AXE_GLOW_TICKS.get();
            int age = state.phaseAge - 1;
            for (int volley = 0; volley < volleys; volley++) {
                int glowTick = volley * interval;
                if (age == glowTick && glow > 0) {
                    ModNetwork.sendTracking(knight, new KnightFxPacket(KnightFxPacket.AXE_GLOW, knight.getId(), glow));
                }
                if (age == glowTick + glow) {
                    this.throwAxes(knight, this.throwOrigin(knight, knight.position(), knight.yBodyRot), target, KnightPhantomConfig.AXES_PER_VOLLEY.get());
                    if (volley == 0) {
                        for (Echo echo : state.followEchoes) {
                            this.throwAxes(knight, echo.pos.add(0.0D, knight.getBbHeight() * 0.82D, 0.0D), target, KnightPhantomConfig.AXES_PER_VOLLEY.get());
                        }
                    }
                }
            }
            if (age >= (volleys - 1) * interval + glow + THROW_LINGER) {
                knight.setTicksProgress(knight.getMaxTicksForFormation());
            }
            return;
        }
        if (knight.isPickKnight()) {
            int rings = KnightPhantomConfig.PICK_RINGS.get();
            int interval = KnightPhantomConfig.PICK_RING_INTERVAL.get();
            int age = state.phaseAge - 1;
            if (age % interval == 0 && age / interval < rings) {
                this.throwPickRing(knight, age / interval);
            }
            if (age >= (rings - 1) * interval + THROW_LINGER) {
                knight.setTicksProgress(knight.getMaxTicksForFormation());
            }
            return;
        }
        if (state.phaseAge > THROW_LINGER) {
            knight.setTicksProgress(knight.getMaxTicksForFormation());
        }
    }

    private void tickDash(KnightPhantom knight, KnightState state) {
        state.dashAge++;
        Vec3 start = state.dashStart;
        Vec3 end = state.dashEnd;
        if (state.dashAge <= state.dashDelay) {
            this.place(knight, start, end);
            return;
        }
        double length = start.distanceTo(end);
        double speed = KnightPhantomConfig.CHARGE_SPEED.get() * this.getSpeedFactor();
        double travelled = (state.dashAge - state.dashDelay) * speed;
        double progress = 1.0D;
        if (length > 1.0E-3D) {
            progress = Math.min(1.0D, travelled / length);
        }
        Vec3 pos = start.add(end.subtract(start).scale(progress));
        this.place(knight, pos, end);
        Vec3 dir = new Vec3(end.x - start.x, 0.0D, end.z - start.z);
        if (travelled <= length + speed) {
            if (state.dashAge == state.dashDelay + 1) {
                knight.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.6F);
            }
            this.sweep(knight, knight.getBoundingBox().inflate(0.4D), state.hits, dir, KnightPhantomConfig.CHARGE_MULTIPLIER.get(), KnightPhantomConfig.CHARGE_KNOCKBACK.get(), this.attackOf(knight), knight);
        }
        if (travelled >= length + speed * DASH_LINGER) {
            knight.setTicksProgress(knight.getMaxTicksForFormation());
        }
    }

    private void sweep(KnightPhantom owner, AABB box, Set<UUID> hits, Vec3 dir, double multiplier, double knockback, float attack, Entity source) {
        for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!canHit(living) || hits.contains(living.getUUID())) {
                continue;
            }
            hits.add(living.getUUID());
            this.hurt(owner, source, living, attack * (float) multiplier, knockback, dir.x, dir.z);
        }
    }

    private void hurt(KnightPhantom owner, Entity source, LivingEntity target, float amount, double knockback, double dx, double dz) {
        DamageSource damage = TFDamageTypes.getEntityDamageSource(this.level, TFDamageTypes.HAUNT, source);
        boolean hurt = target.hurt(damage, amount);
        if (hurt && knockback > 0.0D && (dx * dx + dz * dz) > 1.0E-6D) {
            target.knockback(Math.min(1.0D, knockback), -dx, -dz);
        }
        if (hurt && owner.isAlive()) {
            owner.setLastHurtMob(target);
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom {}] hit {} for {} (hurt {})", owner.getId(), target.getName().getString(), amount, hurt);
        }
    }

    public static boolean canHit(LivingEntity target) {
        if (!target.isAlive() || target instanceof KnightPhantom || !target.isAttackable()) {
            return false;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return true;
    }

    public float attackOf(KnightPhantom knight) {
        AttributeInstance attack = knight.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return KnightPhantomConfig.BASE_ATTACK.get().floatValue();
        }
        return (float) attack.getValue();
    }

    private Vec3 throwOrigin(KnightPhantom knight, Vec3 base, float bodyYaw) {
        float facing = bodyYaw * Mth.DEG_TO_RAD;
        return new Vec3(base.x + Mth.cos(facing), base.y + knight.getBbHeight() * 0.82D, base.z + Mth.sin(facing));
    }

    private void throwAxes(KnightPhantom knight, Vec3 from, LivingEntity target, int count) {
        double tx = target.getX() - from.x;
        double ty = target.getY(0.5D) - from.y;
        double tz = target.getZ() - from.z;
        double spread = KnightPhantomConfig.AXE_SPREAD_DEGREES.get() * Mth.DEG_TO_RAD;
        float damage = this.attackOf(knight) * KnightPhantomConfig.AXE_MULTIPLIER.get().floatValue();
        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0D) * spread;
            double cos = Math.cos(offset);
            double sin = Math.sin(offset);
            double rx = tx * cos - tz * sin;
            double rz = tx * sin + tz * cos;
            ThrownWep axe = new ThrownWep(TFEntities.THROWN_WEP.get(), this.level, knight).setItem(new ItemStack(TFItems.KNIGHTMETAL_AXE.get())).setDamage(damage);
            axe.moveTo(from.x, from.y, from.z, knight.getYRot(), knight.getXRot());
            axe.shoot(rx, ty, rz, 0.75F, 1.0F);
            this.level.addFreshEntity(axe);
        }
        this.level.playSound(null, from.x, from.y, from.z, TFSounds.KNIGHT_PHANTOM_THROW_AXE.get(), SoundSource.HOSTILE, 1.0F, 0.4F + (knight.getRandom().nextFloat() - knight.getRandom().nextFloat()) * 0.2F);
        knight.gameEvent(GameEvent.PROJECTILE_SHOOT);
    }

    private void throwPickRing(KnightPhantom knight, int ring) {
        int count = KnightPhantomConfig.PICKS_PER_RING.get();
        float step = Mth.TWO_PI / count;
        float base = 0.0F;
        if (ring % 2 == 1) {
            base = step * 0.5F;
        }
        float damage = this.attackOf(knight) * KnightPhantomConfig.PICK_MULTIPLIER.get().floatValue();
        double y = knight.getY() + knight.getBbHeight() * 0.82D;
        for (int i = 0; i < count; i++) {
            float angle = base + step * i;
            double vx = Mth.cos(angle);
            double vz = Mth.sin(angle);
            ThrownWep pick = new ThrownWep(TFEntities.THROWN_WEP.get(), this.level, knight).setDamage(damage).setVelocity(0.015F).setItem(new ItemStack(TFItems.KNIGHTMETAL_PICKAXE.get()));
            pick.moveTo(knight.getX() + vx, y, knight.getZ() + vz, angle * Mth.RAD_TO_DEG, knight.getXRot());
            pick.shoot(vx, 0.0D, vz, 0.5F, 1.0F);
            this.level.addFreshEntity(pick);
        }
        ModNetwork.sendTracking(knight, new KnightFxPacket(KnightFxPacket.PICK_RING, knight.getId(), (float) knight.getX(), (float) y, (float) knight.getZ(), base, count));
        knight.playSound(TFSounds.KNIGHT_PHANTOM_THROW_PICK.get(), 1.0F, 0.4F + (knight.getRandom().nextFloat() - knight.getRandom().nextFloat()) * 0.2F);
        knight.gameEvent(GameEvent.PROJECTILE_SHOOT);
    }

    private void place(KnightPhantom knight, Vec3 pos, @Nullable Vec3 facing) {
        knight.setPos(pos.x, pos.y, pos.z);
        knight.setDeltaMovement(Vec3.ZERO);
        if (facing != null) {
            double dx = facing.x - pos.x;
            double dz = facing.z - pos.z;
            if (dx * dx + dz * dz > 1.0E-4D) {
                float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
                knight.setYRot(yaw);
                knight.yBodyRot = yaw;
                knight.setYHeadRot(yaw);
            }
        }
    }

    @Nullable
    public Boolean onMelee(KnightPhantom knight, Entity entity) {
        if (!(entity instanceof LivingEntity target)) {
            return null;
        }
        if (this.regroupTicks > 0) {
            return false;
        }
        KnightState state = this.state(knight);
        boolean charging = knight.isChargingAtPlayer() && this.isDasher(knight, state);
        double multiplier = KnightPhantomConfig.CONTACT_MULTIPLIER.get();
        double knockback = 0.0D;
        if (charging) {
            if (state.hits.contains(target.getUUID())) {
                return false;
            }
            state.hits.add(target.getUUID());
            multiplier = KnightPhantomConfig.CHARGE_MULTIPLIER.get();
            knockback = KnightPhantomConfig.CHARGE_KNOCKBACK.get();
        }
        DamageSource damage = TFDamageTypes.getEntityDamageSource(this.level, TFDamageTypes.HAUNT, knight);
        boolean hurt = target.hurt(damage, this.attackOf(knight) * (float) multiplier);
        if (hurt) {
            if (knockback > 0.0D) {
                double dx = target.getX() - knight.getX();
                double dz = target.getZ() - knight.getZ();
                if (dx * dx + dz * dz > 1.0E-6D) {
                    target.knockback(Math.min(1.0D, knockback), -dx, -dz);
                }
            }
            knight.doEnchantDamageEffects(knight, target);
            knight.setLastHurtMob(target);
        }
        return hurt;
    }

    public void onFormationPicked(KnightPhantom leader) {
        KnightPhantom.Formation formation = leader.getCurrentFormation();
        if (formation != KnightPhantom.Formation.SMALL_CLOCKWISE && formation != KnightPhantom.Formation.SMALL_ANTICLOCKWISE) {
            return;
        }
        if (this.phase < 2 || !KnightPhantomConfig.ENCIRCLE_ENABLED.get() || this.encircleCooldown > 0 || this.encircle != null || this.regroupTicks > 0) {
            return;
        }
        LivingEntity target = leader.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        List<KnightPhantom> participants = new ArrayList<>();
        for (KnightPhantom knight : this.members) {
            if (!knight.isChargingAtPlayer()) {
                participants.add(knight);
            }
        }
        if (participants.size() < 3) {
            return;
        }
        this.startEncircle(participants, target);
    }

    public void onChargeAssigned(List<KnightPhantom> knights) {
        if (this.phase < 2 || knights.size() < 2) {
            return;
        }
        KnightPhantom first = null;
        for (KnightPhantom knight : knights) {
            if (knight.getCurrentFormation() == KnightPhantom.Formation.ATTACK_PLAYER_START) {
                first = knight;
                break;
            }
        }
        if (first == null) {
            return;
        }
        List<KnightPhantom> others = new ArrayList<>();
        for (KnightPhantom knight : knights) {
            if (knight != first && !knight.isChargingAtPlayer()) {
                others.add(knight);
            }
        }
        if (others.isEmpty()) {
            return;
        }
        KnightPhantom second = others.get(first.getRandom().nextInt(others.size()));
        KnightState firstState = this.state(first);
        KnightState secondState = this.state(second);
        if (KnightPhantomConfig.CROSS_ENABLED.get() && this.crossCooldown <= 0 && first.getRandom().nextDouble() < KnightPhantomConfig.CROSS_CHANCE.get()) {
            this.crossCooldown = KnightPhantomConfig.CROSS_COOLDOWN_TICKS.get();
            double angle = first.getRandom().nextDouble() * Math.PI * 2.0D;
            firstState.cross = true;
            firstState.crossIndex = 0;
            firstState.crossAngle = angle;
            secondState.cross = true;
            secondState.crossIndex = 1;
            secondState.crossAngle = angle + Math.PI * 0.5D;
            second.switchToFormation(KnightPhantom.Formation.ATTACK_PLAYER_START);
            if (GeneralConfig.DEBUG_LOG.get()) {
                TwilightBossPlus.LOGGER.info("[KnightPhantom] cross charge by {} and {}", first.getId(), second.getId());
            }
            return;
        }
        if (first.isSwordKnight() && second.isSwordKnight()) {
            secondState.swapped = true;
            secondState.weaponOverride = new ItemStack(TFItems.KNIGHTMETAL_AXE.get());
            second.setItemSlot(EquipmentSlot.MAINHAND, secondState.weaponOverride.copy());
        }
        second.switchToFormation(KnightPhantom.Formation.ATTACK_PLAYER_START);
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom] dual attack by {} and {} (swapped {})", first.getId(), second.getId(), secondState.swapped);
        }
    }

    @Nullable
    public ItemStack weaponOverride(KnightPhantom knight) {
        KnightState state = this.states.get(knight.getId());
        if (state == null || state.weaponOverride == null) {
            return null;
        }
        return state.weaponOverride.copy();
    }

    public boolean onLastKnightSwitch(KnightPhantom knight) {
        if (!KnightPhantomConfig.LAST_ENABLED.get() || this.initialCount <= 1) {
            return false;
        }
        KnightState state = this.state(knight);
        ItemStack next;
        if (knight.isSwordKnight()) {
            next = new ItemStack(TFItems.KNIGHTMETAL_AXE.get());
        } else if (knight.isAxeKnight()) {
            next = new ItemStack(TFItems.KNIGHTMETAL_PICKAXE.get());
        } else {
            next = new ItemStack(TFItems.KNIGHTMETAL_SWORD.get());
        }
        state.weaponOverride = next;
        state.swapped = false;
        knight.setItemSlot(EquipmentSlot.MAINHAND, next.copy());
        this.releaseFollowEchoes(state);
        if (next.is(TFItems.KNIGHTMETAL_PICKAXE.get())) {
            return true;
        }
        int count = KnightPhantomConfig.LAST_ECHO_COUNT.get();
        for (int i = 0; i < count; i++) {
            double side = (i / 2 + 1) * 1.8D;
            if (i % 2 == 1) {
                side = -side;
            }
            Echo echo = new Echo(KnightGroups.nextEchoId(), knight, knight.position(), this.attackOf(knight), KnightPhantomConfig.CHARGE_MULTIPLIER.get(), KnightPhantomConfig.CHARGE_KNOCKBACK.get());
            echo.follow = knight;
            echo.side = side;
            echo.slot = i;
            echo.pos = this.followPos(knight, side);
            state.followEchoes.add(echo);
            this.echoes.add(echo);
            ModNetwork.sendTracking(knight, new KnightFxPacket(KnightFxPacket.ECHO_SPAWN, echo.id,
                (float) echo.pos.x, (float) echo.pos.y, (float) echo.pos.z, knight.yBodyRot, knight.getId(), (float) side));
        }
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom {}] last knight switched to {} with {} echoes", knight.getId(), next.getItem(), state.followEchoes.size());
        }
        return true;
    }

    private Vec3 followPos(KnightPhantom knight, double side) {
        float yaw = knight.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(knight.getX() + Mth.cos(yaw) * side, knight.getY(), knight.getZ() + Mth.sin(yaw) * side);
    }

    private void launchFollowEchoes(KnightPhantom knight, KnightState state, Vec3 start, Vec3 end, int delay) {
        Vec3 main = end.subtract(start);
        double length = main.horizontalDistance();
        double baseAngle = Math.atan2(main.z, main.x);
        double offset = KnightPhantomConfig.LAST_ECHO_ANGLE.get() * Mth.DEG_TO_RAD;
        for (Echo echo : state.followEchoes) {
            double sign = 1.0D;
            if (echo.slot % 2 == 1) {
                sign = -1.0D;
            }
            double angle = baseAngle + sign * offset * (echo.slot / 2 + 1);
            Vec3 echoStart = echo.pos;
            Vec3 echoEnd = new Vec3(echoStart.x + Math.cos(angle) * length, end.y, echoStart.z + Math.sin(angle) * length);
            this.beginEchoDash(echo, echoStart, echoEnd, delay);
        }
        state.followEchoes.clear();
    }

    private void releaseFollowEchoes(KnightState state) {
        for (Echo echo : state.followEchoes) {
            echo.done = true;
        }
        state.followEchoes.clear();
    }

    private void beginEchoDash(Echo echo, Vec3 start, Vec3 end, int delay) {
        echo.follow = null;
        echo.start = start;
        echo.end = end;
        echo.pos = start;
        echo.delay = delay;
        echo.age = 0;
        int dashTicks = this.dashTicks(start, end);
        ModNetwork.sendNear(this.level, start.x, start.y, start.z, SEND_RADIUS, new KnightFxPacket(KnightFxPacket.LINE, -1,
            (float) start.x, (float) start.y, (float) start.z, (float) end.x, (float) end.y, (float) end.z, delay, 0.0F, dashTicks));
        ModNetwork.sendNear(this.level, start.x, start.y, start.z, SEND_RADIUS, new KnightFxPacket(KnightFxPacket.ECHO_DASH, echo.id,
            (float) start.x, (float) start.y, (float) start.z, (float) end.x, (float) end.y, (float) end.z, delay,
            (float) (KnightPhantomConfig.CHARGE_SPEED.get() * this.getSpeedFactor())));
    }

    public void onDeath(KnightPhantom knight) {
        if (!KnightPhantomConfig.ECHO_ENABLED.get()) {
            return;
        }
        int others = 0;
        for (KnightPhantom member : this.members) {
            if (member != knight && member.isAlive()) {
                others++;
            }
        }
        if (others == 0) {
            return;
        }
        Vec3 start = knight.position();
        LivingEntity target = knight.getTarget();
        if (target == null) {
            target = this.level.getNearestPlayer(knight, 32.0D);
        }
        Vec3 dir;
        double endY = start.y;
        if (target != null) {
            dir = new Vec3(target.getX() - start.x, 0.0D, target.getZ() - start.z);
            endY = target.getY();
        } else {
            dir = knight.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (dir.lengthSqr() < 1.0E-4D) {
            dir = new Vec3(1.0D, 0.0D, 0.0D);
        }
        dir = dir.normalize();
        double distance = KnightPhantomConfig.ECHO_DISTANCE.get();
        Vec3 end = new Vec3(start.x + dir.x * distance, endY, start.z + dir.z * distance);
        Echo echo = new Echo(KnightGroups.nextEchoId(), knight, start, this.attackOf(knight), KnightPhantomConfig.ECHO_MULTIPLIER.get(), KnightPhantomConfig.ECHO_KNOCKBACK.get());
        this.echoes.add(echo);
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        ModNetwork.sendNear(this.level, start.x, start.y, start.z, SEND_RADIUS, new KnightFxPacket(KnightFxPacket.ECHO_SPAWN, echo.id,
            (float) start.x, (float) start.y, (float) start.z, yaw, -1.0F, 0.0F));
        this.beginEchoDash(echo, start, end, KnightPhantomConfig.ECHO_DELAY_TICKS.get());
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom {}] death echo from {} to {}", knight.getId(), start, end);
        }
    }

    private void tickEchoes() {
        Iterator<Echo> iterator = this.echoes.iterator();
        while (iterator.hasNext()) {
            Echo echo = iterator.next();
            if (!echo.done) {
                this.tickEcho(echo);
            }
            if (echo.done) {
                iterator.remove();
                ModNetwork.sendNear(this.level, echo.pos.x, echo.pos.y, echo.pos.z, SEND_RADIUS, new KnightFxPacket(KnightFxPacket.ECHO_END, echo.id));
            }
        }
    }

    private void tickEcho(Echo echo) {
        if (echo.follow != null) {
            if (!echo.follow.isAlive()) {
                echo.done = true;
                return;
            }
            echo.pos = this.followPos(echo.follow, echo.side);
            return;
        }
        if (echo.start == null || echo.end == null) {
            echo.done = true;
            return;
        }
        echo.age++;
        if (echo.age <= echo.delay) {
            return;
        }
        double length = echo.start.distanceTo(echo.end);
        double speed = KnightPhantomConfig.CHARGE_SPEED.get() * this.getSpeedFactor();
        double travelled = (echo.age - echo.delay) * speed;
        double progress = 1.0D;
        if (length > 1.0E-3D) {
            progress = Math.min(1.0D, travelled / length);
        }
        echo.pos = echo.start.add(echo.end.subtract(echo.start).scale(progress));
        AABB box = new AABB(echo.pos.x - 1.1D, echo.pos.y - 0.5D, echo.pos.z - 1.1D, echo.pos.x + 1.1D, echo.pos.y + 4.0D, echo.pos.z + 1.1D);
        KnightPhantom owner = echo.owner;
        Vec3 dir = new Vec3(echo.end.x - echo.start.x, 0.0D, echo.end.z - echo.start.z);
        for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!canHit(living) || echo.hits.contains(living.getUUID())) {
                continue;
            }
            echo.hits.add(living.getUUID());
            this.hurt(owner, owner, living, echo.attack * (float) echo.multiplier, echo.knockback, dir.x, dir.z);
        }
        if (progress >= 1.0D) {
            echo.done = true;
        }
    }

    private void startEncircle(List<KnightPhantom> participants, LivingEntity target) {
        Vec3 center = target.position();
        participants.sort(Comparator.comparingDouble(knight -> Math.atan2(knight.getZ() - center.z, knight.getX() - center.x)));
        Encircle ring = new Encircle();
        ring.center = center;
        ring.participants.addAll(participants);
        KnightPhantom first = participants.get(0);
        ring.baseAngle = Math.atan2(first.getZ() - center.z, first.getX() - center.x);
        ring.openIndex = first.getRandom().nextInt(participants.size());
        this.encircle = ring;
        float[] data = new float[11 + participants.size()];
        data[0] = (float) center.x;
        data[1] = (float) center.y;
        data[2] = (float) center.z;
        data[3] = KnightPhantomConfig.ENCIRCLE_START_RADIUS.get().floatValue();
        data[4] = KnightPhantomConfig.ENCIRCLE_END_RADIUS.get().floatValue();
        data[5] = KnightPhantomConfig.ENCIRCLE_WARNING_TICKS.get();
        data[6] = KnightPhantomConfig.ENCIRCLE_SHRINK_TICKS.get();
        data[7] = THRUST_TICKS;
        data[8] = ring.openIndex;
        data[9] = KnightPhantomConfig.ENCIRCLE_THRUST_RADIUS.get().floatValue();
        data[10] = participants.size();
        for (int i = 0; i < participants.size(); i++) {
            data[11 + i] = participants.get(i).getId();
        }
        ModNetwork.sendNear(this.level, center.x, center.y, center.z, SEND_RADIUS, new KnightFxPacket(KnightFxPacket.ENCIRCLE, -1, data));
        this.level.playSound(null, center.x, center.y, center.z, SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.HOSTILE, 2.0F, 0.5F);
        if (GeneralConfig.DEBUG_LOG.get()) {
            TwilightBossPlus.LOGGER.info("[KnightPhantom] encircle at {} with {} knights, open segment {}", center, participants.size(), ring.openIndex);
        }
    }

    private double encircleRadius(int age) {
        int warn = KnightPhantomConfig.ENCIRCLE_WARNING_TICKS.get();
        int shrink = KnightPhantomConfig.ENCIRCLE_SHRINK_TICKS.get();
        double start = KnightPhantomConfig.ENCIRCLE_START_RADIUS.get();
        double end = KnightPhantomConfig.ENCIRCLE_END_RADIUS.get();
        if (age < warn) {
            return start;
        }
        if (age < warn + shrink) {
            return Mth.lerp((age - warn) / (double) shrink, start, end);
        }
        double thrust = Math.min(1.0D, (age - warn - shrink) / (double) THRUST_TICKS);
        return Mth.lerp(thrust, end, 0.8D);
    }

    private void tickEncircle() {
        Encircle ring = this.encircle;
        ring.age++;
        int warn = KnightPhantomConfig.ENCIRCLE_WARNING_TICKS.get();
        int shrink = KnightPhantomConfig.ENCIRCLE_SHRINK_TICKS.get();
        double radius = this.encircleRadius(ring.age);
        int count = ring.participants.size();
        List<Vec3> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            KnightPhantom knight = ring.participants.get(i);
            double angle = ring.baseAngle + Math.PI * 2.0D * i / count;
            Vec3 target = new Vec3(ring.center.x + Math.cos(angle) * radius, ring.center.y, ring.center.z + Math.sin(angle) * radius);
            positions.add(target);
            if (!knight.isAlive()) {
                continue;
            }
            if (ring.age < warn) {
                Vec3 current = knight.position();
                this.place(knight, current.add(target.subtract(current).scale(0.3D)), ring.center);
            } else {
                this.place(knight, target, ring.center);
            }
        }
        int interval = KnightPhantomConfig.ENCIRCLE_WALL_INTERVAL.get();
        if (ring.age >= warn && ring.age < warn + shrink && (ring.age - warn) % interval == 0) {
            this.wallPulse(ring, positions);
        }
        if (ring.age == warn + shrink + THRUST_TICKS) {
            this.thrust(ring);
        }
        if (ring.age >= warn + shrink + THRUST_TICKS + ENCIRCLE_LINGER) {
            this.endEncircle();
        }
    }

    private void wallPulse(Encircle ring, List<Vec3> positions) {
        int count = ring.participants.size();
        Set<UUID> hits = new HashSet<>();
        double multiplier = KnightPhantomConfig.ENCIRCLE_WALL_MULTIPLIER.get();
        double knockback = KnightPhantomConfig.ENCIRCLE_KNOCKBACK.get();
        for (int i = 0; i < count; i++) {
            if (i == ring.openIndex) {
                continue;
            }
            KnightPhantom a = ring.participants.get(i);
            KnightPhantom b = ring.participants.get((i + 1) % count);
            if (!a.isAlive() || !b.isAlive()) {
                continue;
            }
            Vec3 pa = positions.get(i);
            Vec3 pb = positions.get((i + 1) % count);
            AABB box = new AABB(Math.min(pa.x, pb.x) - 1.0D, ring.center.y - 1.0D, Math.min(pa.z, pb.z) - 1.0D,
                Math.max(pa.x, pb.x) + 1.0D, ring.center.y + 4.0D, Math.max(pa.z, pb.z) + 1.0D);
            for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (!canHit(living) || hits.contains(living.getUUID())) {
                    continue;
                }
                if (segmentDistance(living.getX(), living.getZ(), pa, pb) > 0.5D + living.getBbWidth() * 0.5D) {
                    continue;
                }
                hits.add(living.getUUID());
                this.hurt(a, a, living, this.attackOf(a) * (float) multiplier, knockback, ring.center.x - living.getX(), ring.center.z - living.getZ());
            }
        }
    }

    private static double segmentDistance(double x, double z, Vec3 a, Vec3 b) {
        double abx = b.x - a.x;
        double abz = b.z - a.z;
        double lengthSqr = abx * abx + abz * abz;
        double t = 0.0D;
        if (lengthSqr > 1.0E-6D) {
            t = Mth.clamp(((x - a.x) * abx + (z - a.z) * abz) / lengthSqr, 0.0D, 1.0D);
        }
        double px = a.x + abx * t - x;
        double pz = a.z + abz * t - z;
        return Math.sqrt(px * px + pz * pz);
    }

    private void thrust(Encircle ring) {
        double radius = KnightPhantomConfig.ENCIRCLE_THRUST_RADIUS.get();
        double multiplier = KnightPhantomConfig.ENCIRCLE_THRUST_MULTIPLIER.get();
        double knockback = KnightPhantomConfig.ENCIRCLE_KNOCKBACK.get();
        KnightPhantom source = null;
        for (KnightPhantom knight : ring.participants) {
            if (knight.isAlive()) {
                source = knight;
                break;
            }
        }
        if (source == null) {
            return;
        }
        AABB box = new AABB(ring.center.x - radius, ring.center.y - 1.0D, ring.center.z - radius, ring.center.x + radius, ring.center.y + 4.0D, ring.center.z + radius);
        for (LivingEntity living : this.level.getEntitiesOfClass(LivingEntity.class, box)) {
            double dx = living.getX() - ring.center.x;
            double dz = living.getZ() - ring.center.z;
            if (!canHit(living) || dx * dx + dz * dz > radius * radius) {
                continue;
            }
            this.hurt(source, source, living, this.attackOf(source) * (float) multiplier, knockback, dx, dz);
        }
        ModNetwork.sendNear(this.level, ring.center.x, ring.center.y, ring.center.z, SEND_RADIUS,
            new KnightFxPacket(KnightFxPacket.THRUST, -1, (float) ring.center.x, (float) ring.center.y, (float) ring.center.z, (float) radius));
        this.level.playSound(null, ring.center.x, ring.center.y, ring.center.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    private void endEncircle() {
        Encircle ring = this.encircle;
        this.encircle = null;
        this.encircleCooldown = KnightPhantomConfig.ENCIRCLE_COOLDOWN_TICKS.get();
        if (ring != null) {
            ModNetwork.sendNear(this.level, ring.center.x, ring.center.y, ring.center.z, SEND_RADIUS, new KnightFxPacket(KnightFxPacket.ENCIRCLE_END, -1));
        }
    }

    private static final class KnightState {
        private KnightPhantom.Formation lastFormation;
        private int phaseAge;
        private boolean locked;
        @Nullable
        private Vec3 lockPos;
        @Nullable
        private Vec3 dashStart;
        @Nullable
        private Vec3 dashEnd;
        private int dashAge;
        private int dashDelay;
        private boolean cross;
        private int crossIndex;
        private double crossAngle;
        private boolean swapped;
        @Nullable
        private ItemStack weaponOverride;
        private final Set<UUID> hits = new HashSet<>();
        private final List<Echo> followEchoes = new ArrayList<>();
    }

    private static final class Echo {
        private final int id;
        private final KnightPhantom owner;
        private final float attack;
        private final double multiplier;
        private final double knockback;
        private final Set<UUID> hits = new HashSet<>();
        private Vec3 pos;
        @Nullable
        private Vec3 start;
        @Nullable
        private Vec3 end;
        @Nullable
        private KnightPhantom follow;
        private double side;
        private int slot;
        private int delay;
        private int age;
        private boolean done;

        private Echo(int id, KnightPhantom owner, Vec3 pos, float attack, double multiplier, double knockback) {
            this.id = id;
            this.owner = owner;
            this.pos = pos;
            this.attack = attack;
            this.multiplier = multiplier;
            this.knockback = knockback;
        }
    }

    private static final class Encircle {
        private final List<KnightPhantom> participants = new ArrayList<>();
        private Vec3 center;
        private double baseAngle;
        private int openIndex;
        private int age;
    }
}
