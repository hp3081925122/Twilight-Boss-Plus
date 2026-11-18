package com.twilightbossplus.knight;

import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.skill.ChainHook;
import com.twilightbossplus.knight.skill.ChainNoose;
import com.twilightbossplus.knight.skill.EchoSlash;
import com.twilightbossplus.knight.skill.KnightSkill;
import com.twilightbossplus.knight.skill.LancePhalanx;
import com.twilightbossplus.knight.skill.Oathblade;
import com.twilightbossplus.knight.skill.RevenantAxe;
import com.twilightbossplus.mixin.KnightPhantomAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;

public final class KnightBrain extends BossBrain<KnightPhantom> {
    private static final UUID HEALTH_UUID = UUID.fromString("4c8e2a17-93b5-4f0d-a6e1-7d2b9c3f5e48");
    private static final double SKILL_RANGE = 40.0;
    private static final int CONTROL_TIMEOUT = 4;

    private enum SkillId {
        ECHO_SLASH, REVENANT_AXE, CHAIN_HOOK, CHAIN_NOOSE, LANCE_PHALANX, OATHBLADE
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    private List<KnightPhantom> squad = new ArrayList<>();
    private int known = 1;
    @Nullable
    private KnightSkill current;
    @Nullable
    private SkillId currentId;
    @Nullable
    private KnightSkill controller;
    private int controlStamp;
    private int globalCooldown = 80;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;

    public KnightBrain(KnightPhantom knight) {
        super(knight);
    }

    public static KnightBrain of(KnightPhantom knight) {
        return ((KnightBrainHolder) knight).tbp$getBrain();
    }

    @Override
    protected double skillKnockback() {
        return KnightConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof KnightPhantom;
    }

    @Override
    public double attack() {
        return KnightConfig.BASE_ATTACK.get() * KnightConfig.ATTACK_MULTIPLIER.get();
    }

    public KnightPhantom knight() {
        return this.mob;
    }

    public boolean busy() {
        return this.controller != null;
    }

    @Nullable
    public KnightSkill controller() {
        return this.controller;
    }

    public boolean enraged() {
        return this.stageThree || this.alive().size() <= 1;
    }

    public boolean stageTwo() {
        return this.stageTwo;
    }

    public List<KnightPhantom> alive() {
        List<KnightPhantom> result = new ArrayList<>();
        for (KnightPhantom knight : this.squad) {
            if (knight.isAlive() && !knight.isRemoved()) {
                result.add(knight);
            }
        }
        if (!result.contains(this.mob) && this.mob.isAlive()) {
            result.add(this.mob);
        }
        return result;
    }

    public void claim(KnightSkill skill) {
        boolean fresh = this.controller == null;
        this.controller = skill;
        this.controlStamp = this.mob.tickCount;
        if (fresh) {
            ((KnightPhantomAccessor) this.mob).tbp$setCharging(true);
            this.mob.noPhysics = true;
        }
    }

    public void stamp() {
        this.controlStamp = this.mob.tickCount;
    }

    public void release() {
        if (this.controller == null) {
            return;
        }
        this.controller = null;
        this.mob.setDeltaMovement(Vec3.ZERO);
        this.mob.getMoveControl().setWantedPosition(this.mob.getX(), this.mob.getY() + 1.0, this.mob.getZ(), 0.5);
        this.mob.switchToFormation(KnightPhantom.Formation.WAITING_FOR_LEADER);
    }

    public void tick() {
        if (!KnightConfig.ENABLED.get()) {
            if (this.current != null) {
                this.endCurrent();
            }
            this.release();
            return;
        }
        if (!this.initialized) {
            this.initialize();
        }
        if (!Double.isFinite(this.mob.getX()) || !Double.isFinite(this.mob.getY()) || !Double.isFinite(this.mob.getZ())) {
            this.recover();
        }
        this.tickHazards();
        if (this.mob.isDeadOrDying()) {
            this.stopAll();
            return;
        }
        if (this.squad.isEmpty() || this.mob.tickCount % 10 == 0) {
            this.squad = this.mob.getNearbyKnights();
            this.known = Math.max(this.known, this.squad.size());
        }
        if (this.controller != null && this.mob.tickCount - this.controlStamp > CONTROL_TIMEOUT) {
            this.release();
        }
        if (!this.leader()) {
            if (this.current != null) {
                this.endCurrent();
            }
            float ratio = this.squadRatio();
            this.stageTwo = this.stageTwo || ratio <= KnightConfig.PHASE_TWO_HEALTH.get();
            this.stageThree = this.stageThree || ratio <= KnightConfig.PHASE_THREE_HEALTH.get();
            return;
        }
        if (this.checkStages()) {
            return;
        }
        if (this.current != null) {
            this.current.keep();
            if (!this.current.update()) {
                this.endCurrent();
            }
            return;
        }
        for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
                this.cooldowns[i]--;
            }
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        this.selectSkill();
    }

    private void recover() {
        Vec3 safe = Vec3.atBottomCenterOf(this.mob.blockPosition());
        GlobalPos home = this.mob.getRestrictionPoint();
        if (home != null) {
            safe = Vec3.atBottomCenterOf(home.pos());
        }
        if (!Double.isFinite(safe.x) || !Double.isFinite(safe.y) || !Double.isFinite(safe.z)) {
            safe = this.level().getSharedSpawnPos().getCenter();
        }
        this.mob.setDeltaMovement(Vec3.ZERO);
        this.mob.moveTo(safe.x, safe.y, safe.z, this.mob.getYRot(), 0.0F);
        this.mob.getMoveControl().setWantedPosition(safe.x, safe.y + 1.0, safe.z, 0.5);
    }

    private void initialize() {
        this.initialized = true;
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", KnightConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        this.squad = this.mob.getNearbyKnights();
        this.known = Math.max(1, this.squad.size());
        float ratio = this.squadRatio();
        this.stageTwo = ratio <= KnightConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= KnightConfig.PHASE_THREE_HEALTH.get();
    }

    private boolean leader() {
        for (KnightPhantom other : this.squad) {
            if (other == this.mob || !other.isAlive()) {
                continue;
            }
            if (other.getNumber() < this.mob.getNumber()) {
                return false;
            }
            if (other.getNumber() == this.mob.getNumber() && other.getId() < this.mob.getId()) {
                return false;
            }
        }
        return true;
    }

    public float squadRatio() {
        float health = 0.0F;
        for (KnightPhantom knight : this.alive()) {
            health += knight.getHealth();
        }
        return health / Math.max(1.0F, this.mob.getMaxHealth() * this.known);
    }

    private boolean checkStages() {
        float ratio = this.squadRatio();
        if (!this.stageTwo && ratio <= KnightConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            if (KnightConfig.LANCE_PHALANX.enabled.get() && this.target() != null && this.alive().size() >= 2) {
                this.forceSkill(SkillId.LANCE_PHALANX, new LancePhalanx(this, this.target()));
                return true;
            }
        }
        if (!this.stageThree && ratio <= KnightConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            if (KnightConfig.OATHBLADE.enabled.get() && this.target() != null) {
                this.forceSkill(SkillId.OATHBLADE, new Oathblade(this, this.target()));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(SkillId id, KnightSkill skill) {
        if (this.current != null) {
            this.endCurrent();
        }
        this.startSkill(id, skill);
    }

    @Nullable
    private KnightPhantom performer(Predicate<KnightPhantom> weapon, LivingEntity target) {
        List<KnightPhantom> free = new ArrayList<>();
        for (KnightPhantom knight : this.alive()) {
            if (!KnightBrain.of(knight).busy()) {
                free.add(knight);
            }
        }
        free.sort(Comparator.comparingDouble(knight -> knight.distanceToSqr(target)));
        for (KnightPhantom knight : free) {
            if (weapon.test(knight)) {
                return knight;
            }
        }
        if (free.isEmpty()) {
            return null;
        }
        return free.get(0);
    }

    private void selectSkill() {
        if (this.globalCooldown > 0) {
            return;
        }
        LivingEntity target = this.target();
        if (target == null) {
            for (KnightPhantom knight : this.alive()) {
                LivingEntity other = KnightBrain.of(knight).target();
                if (other != null) {
                    target = other;
                    break;
                }
            }
        }
        if (target == null) {
            return;
        }
        List<KnightPhantom> knights = this.alive();
        double nearest = Double.MAX_VALUE;
        for (KnightPhantom knight : knights) {
            nearest = Math.min(nearest, knight.distanceTo(target));
        }
        if (nearest > SKILL_RANGE) {
            return;
        }
        List<SkillId> options = new ArrayList<>();
        KnightPhantom sword = this.performer(KnightPhantom::isSwordKnight, target);
        KnightPhantom axe = this.performer(KnightPhantom::isAxeKnight, target);
        KnightPhantom pick = this.performer(KnightPhantom::isPickKnight, target);
        if (sword != null && this.ready(SkillId.ECHO_SLASH, KnightConfig.ECHO_SLASH) && sword.distanceTo(target) < 18.0) {
            options.add(SkillId.ECHO_SLASH);
        }
        if (axe != null && this.ready(SkillId.REVENANT_AXE, KnightConfig.REVENANT_AXE) && axe.distanceTo(target) > 3.0 && axe.distanceTo(target) < 24.0) {
            options.add(SkillId.REVENANT_AXE);
        }
        if (pick != null && this.ready(SkillId.CHAIN_HOOK, KnightConfig.CHAIN_HOOK) && pick.distanceTo(target) < ChainHook.RANGE) {
            options.add(SkillId.CHAIN_HOOK);
        }
        if (knights.size() >= ChainNoose.MIN_KNIGHTS && this.ready(SkillId.CHAIN_NOOSE, KnightConfig.CHAIN_NOOSE) && nearest < 20.0) {
            options.add(SkillId.CHAIN_NOOSE);
        }
        if (this.stageTwo && knights.size() >= 2 && this.ready(SkillId.LANCE_PHALANX, KnightConfig.LANCE_PHALANX)) {
            options.add(SkillId.LANCE_PHALANX);
        }
        if (this.stageThree && this.ready(SkillId.OATHBLADE, KnightConfig.OATHBLADE)) {
            options.add(SkillId.OATHBLADE);
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        KnightSkill skill = switch (chosen) {
            case ECHO_SLASH -> new EchoSlash(this, sword, target);
            case REVENANT_AXE -> new RevenantAxe(this, axe, target);
            case CHAIN_HOOK -> new ChainHook(this, pick, target);
            case CHAIN_NOOSE -> new ChainNoose(this, target);
            case LANCE_PHALANX -> new LancePhalanx(this, target);
            case OATHBLADE -> new Oathblade(this, target);
        };
        this.startSkill(chosen, skill);
    }

    private KnightConfig.Skill config(SkillId id) {
        return switch (id) {
            case ECHO_SLASH -> KnightConfig.ECHO_SLASH;
            case REVENANT_AXE -> KnightConfig.REVENANT_AXE;
            case CHAIN_HOOK -> KnightConfig.CHAIN_HOOK;
            case CHAIN_NOOSE -> KnightConfig.CHAIN_NOOSE;
            case LANCE_PHALANX -> KnightConfig.LANCE_PHALANX;
            case OATHBLADE -> KnightConfig.OATHBLADE;
        };
    }

    private boolean ready(SkillId id, KnightConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(SkillId id, KnightSkill skill) {
        this.current = skill;
        this.currentId = id;
        skill.start();
    }

    private void endCurrent() {
        KnightSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        double multiplier = 1.0;
        if (this.enraged()) {
            multiplier = KnightConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = Math.max(this.cooldowns[id.ordinal()], (int) (this.config(id).cooldown.get() * multiplier));
        }
        this.globalCooldown = (int) (KnightConfig.SKILL_INTERVAL.get() * multiplier);
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.controller = null;
        this.clearHazards();
    }
}
