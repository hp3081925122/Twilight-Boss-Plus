package com.twilightbossplus.queen;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.skill.AuroraMirrors;
import com.twilightbossplus.queen.skill.CrystalCourt;
import com.twilightbossplus.queen.skill.FrostWaltz;
import com.twilightbossplus.queen.skill.GlacialCoffin;
import com.twilightbossplus.queen.skill.MirrorShell;
import com.twilightbossplus.queen.skill.QueenSkill;
import com.twilightbossplus.queen.skill.RimeTrace;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.SnowQueen;
import twilightforest.entity.monster.IceCrystal;

public final class QueenBrain extends BossBrain<SnowQueen> {
    private static final UUID HEALTH_UUID = UUID.fromString("c4a19e72-58b3-4d0f-9e26-71f3b8a5d903");
    private static final double SKILL_RANGE = 32.0;
    private static final double ORBIT_RADIUS = 6.0;

    private enum SkillId {
        FROST_WALTZ, MIRROR_SHELL, RIME_TRACE, GLACIAL_COFFIN, CRYSTAL_COURT, AURORA_MIRRORS
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    @Nullable
    private QueenSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 40;
    private double orbitAngle;
    private int orbitDir = 1;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;
    private boolean shellActive;
    private boolean shellStruck;

    public QueenBrain(SnowQueen queen) {
        super(queen);
    }

    @Override
    protected double skillKnockback() {
        return SnowQueenConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof SnowQueen || entity instanceof IceCrystal;
    }

    @Override
    public double attack() {
        return super.attack() * SnowQueenConfig.ATTACK_MULTIPLIER.get();
    }

    public SnowQueen queen() {
        return this.mob;
    }

    public boolean busy() {
        return this.current != null;
    }

    public boolean blocksMelee() {
        return this.current != null && this.current.blocksMelee();
    }

    public boolean stageTwo() {
        return this.stageTwo;
    }

    public boolean enraged() {
        return this.stageThree;
    }

    public void setShell(boolean active) {
        this.shellActive = active;
        if (active) {
            this.shellStruck = false;
        }
    }

    public boolean shellStruck() {
        return this.shellStruck;
    }

    public float damageScale(float amount) {
        if (!this.shellActive || !SnowQueenConfig.ENABLED.get()) {
            return amount;
        }
        return (float) (amount * (1.0 - SnowQueenConfig.SHELL_DAMAGE_REDUCTION.get()));
    }

    public void onHurt(DamageSource source) {
        if (!this.shellActive || this.shellStruck) {
            return;
        }
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity living && living != this.mob && !this.isAlly(living)) {
            this.shellStruck = true;
            TwilightBossPlus.debug("Snow Queen {} mirror shell struck by {}", this.mob.getId(), attacker.getName().getString());
        }
    }

    public void freeze(LivingEntity victim, double scale) {
        int add = (int) (SnowQueenConfig.FREEZE_TICKS.get() * scale);
        if (add <= 0 || !victim.canFreeze()) {
            return;
        }
        victim.setTicksFrozen(Math.min(victim.getTicksFrozen() + add, victim.getTicksRequiredToFreeze() + 120));
    }

    public boolean frost(LivingEntity victim, double multiplier, Vec3 from, double horizontal, double vertical) {
        if (!this.damage(victim, multiplier)) {
            return false;
        }
        this.knockFrom(victim, from, horizontal, vertical);
        this.freeze(victim, 1.0);
        return true;
    }

    public void fly(Vec3 dest, double speed, double response) {
        Vec3 offset = dest.subtract(this.mob.position());
        double distance = offset.length();
        Vec3 desired = Vec3.ZERO;
        if (distance > 1.0E-3) {
            desired = offset.scale(Math.min(speed, distance * 0.3) / distance);
        }
        Vec3 now = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(now.lerp(desired, response));
    }

    public void brake() {
        Vec3 now = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(now.x * 0.5, (now.y + 0.05) * 0.5, now.z * 0.5);
    }

    public void faceToward(Vec3 point, float maxStep) {
        double dx = point.x - this.mob.getX();
        double dz = point.z - this.mob.getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float wanted = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float current = this.mob.getYRot();
        float yaw = current + Mth.clamp(Mth.wrapDegrees(wanted - current), -maxStep, maxStep);
        this.mob.setYRot(yaw);
        this.mob.yBodyRot = yaw;
        this.mob.yHeadRot = yaw;
    }

    public List<Player> players(double range) {
        List<Player> result = new ArrayList<>();
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.mob.getBoundingBox().inflate(range))) {
            if (player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                result.add(player);
            }
        }
        return result;
    }

    public int crystals() {
        return this.level().getEntitiesOfClass(IceCrystal.class, this.mob.getBoundingBox().inflate(32.0, 16.0, 32.0)).size();
    }

    public void tick() {
        if (!SnowQueenConfig.ENABLED.get()) {
            if (this.current != null) {
                this.endCurrent();
            }
            return;
        }
        if (!this.initialized) {
            this.initialize();
        }
        this.tickHazards();
        if (this.mob.isDeadOrDying()) {
            this.stopAll();
            return;
        }
        this.mob.getNavigation().stop();
        if (this.checkStages()) {
            return;
        }
        if (this.current != null) {
            if (!this.current.update()) {
                this.endCurrent();
            }
            return;
        }
        this.idle();
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

    private void idle() {
        LivingEntity target = this.target();
        if (target == null) {
            this.brake();
            return;
        }
        if (this.mob.getRandom().nextInt(160) == 0) {
            this.orbitDir = -this.orbitDir;
        }
        this.orbitAngle += 0.025 * this.orbitDir;
        double x = target.getX() + Math.cos(this.orbitAngle) * ORBIT_RADIUS;
        double z = target.getZ() + Math.sin(this.orbitAngle) * ORBIT_RADIUS;
        double floor = Math.max(target.getY(), this.groundY(x, z, target.getY() + 3.0));
        Vec3 spot = new Vec3(x, floor + SnowQueenConfig.HOVER_HEIGHT.get(), z);
        this.fly(spot, 0.28, 0.12);
        this.faceToward(target.position(), 20.0F);
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void initialize() {
        this.initialized = true;
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", SnowQueenConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        float ratio = this.healthRatio();
        this.stageTwo = ratio <= SnowQueenConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= SnowQueenConfig.PHASE_THREE_HEALTH.get();
        this.orbitAngle = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
    }

    private boolean checkStages() {
        float ratio = this.healthRatio();
        if (!this.stageTwo && ratio <= SnowQueenConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            TwilightBossPlus.debug("Snow Queen {} entered phase two at health ratio {}", this.mob.getId(), ratio);
            if (SnowQueenConfig.CRYSTAL_COURT.enabled.get() && this.target() != null) {
                this.forceSkill(SkillId.CRYSTAL_COURT, new CrystalCourt(this));
                return true;
            }
        }
        if (!this.stageThree && ratio <= SnowQueenConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            TwilightBossPlus.debug("Snow Queen {} entered phase three at health ratio {}", this.mob.getId(), ratio);
            if (SnowQueenConfig.AURORA_MIRRORS.enabled.get() && this.target() != null) {
                this.forceSkill(SkillId.AURORA_MIRRORS, new AuroraMirrors(this));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(SkillId id, QueenSkill skill) {
        if (this.current != null) {
            this.endCurrent();
        }
        this.startSkill(id, skill);
    }

    private void selectSkill() {
        if (this.globalCooldown > 0) {
            return;
        }
        LivingEntity target = this.target();
        if (target == null) {
            return;
        }
        double distance = this.mob.distanceTo(target);
        if (distance > SKILL_RANGE) {
            return;
        }
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean sight = this.mob.hasLineOfSight(target);
        List<SkillId> options = new ArrayList<>();
        if (this.ready(SkillId.FROST_WALTZ) && flat < 9.0) {
            options.add(SkillId.FROST_WALTZ);
        }
        if (this.ready(SkillId.MIRROR_SHELL) && flat < 6.5) {
            options.add(SkillId.MIRROR_SHELL);
        }
        if (this.ready(SkillId.RIME_TRACE) && sight && distance > 3.0 && distance < 20.0) {
            options.add(SkillId.RIME_TRACE);
        }
        if (this.ready(SkillId.GLACIAL_COFFIN) && sight && distance < 26.0) {
            options.add(SkillId.GLACIAL_COFFIN);
        }
        if (this.stageTwo && this.ready(SkillId.CRYSTAL_COURT) && this.crystals() < 2) {
            options.add(SkillId.CRYSTAL_COURT);
        }
        if (this.stageThree && this.ready(SkillId.AURORA_MIRRORS) && distance < 20.0) {
            options.add(SkillId.AURORA_MIRRORS);
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        QueenSkill skill = switch (chosen) {
            case FROST_WALTZ -> new FrostWaltz(this);
            case MIRROR_SHELL -> new MirrorShell(this);
            case RIME_TRACE -> new RimeTrace(this);
            case GLACIAL_COFFIN -> new GlacialCoffin(this);
            case CRYSTAL_COURT -> new CrystalCourt(this);
            case AURORA_MIRRORS -> new AuroraMirrors(this);
        };
        this.startSkill(chosen, skill);
    }

    private SnowQueenConfig.Skill config(SkillId id) {
        return switch (id) {
            case FROST_WALTZ -> SnowQueenConfig.FROST_WALTZ;
            case MIRROR_SHELL -> SnowQueenConfig.MIRROR_SHELL;
            case RIME_TRACE -> SnowQueenConfig.RIME_TRACE;
            case GLACIAL_COFFIN -> SnowQueenConfig.GLACIAL_COFFIN;
            case CRYSTAL_COURT -> SnowQueenConfig.CRYSTAL_COURT;
            case AURORA_MIRRORS -> SnowQueenConfig.AURORA_MIRRORS;
        };
    }

    private boolean ready(SkillId id) {
        return this.config(id).enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(SkillId id, QueenSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Snow Queen {} starts skill {} at health ratio {}", this.mob.getId(), skill.name(), this.healthRatio());
        skill.start();
    }

    private void endCurrent() {
        QueenSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        this.shellActive = false;
        double multiplier = 1.0;
        if (this.stageThree) {
            multiplier = SnowQueenConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = Math.max(this.cooldowns[id.ordinal()], (int) (this.config(id).cooldown.get() * multiplier));
        }
        this.globalCooldown = (int) (SnowQueenConfig.SKILL_INTERVAL.get() * multiplier);
        TwilightBossPlus.debug("Snow Queen {} ends skill {}", this.mob.getId(), skill.name());
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.clearHazards();
    }
}
