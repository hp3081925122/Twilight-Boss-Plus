package com.twilightbossplus.lich;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.skill.CorpseDetonation;
import com.twilightbossplus.lich.skill.LichSkill;
import com.twilightbossplus.lich.skill.LifeDrain;
import com.twilightbossplus.lich.skill.ShadowBlink;
import com.twilightbossplus.lich.skill.ShadowCrossfire;
import com.twilightbossplus.lich.skill.ShieldBacklash;
import com.twilightbossplus.lich.skill.SoulPrison;
import com.twilightbossplus.lich.skill.TwilightCollapse;
import com.twilightbossplus.lich.skill.TwilightConstellation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.monster.LichMinion;

public final class LichBrain extends BossBrain<Lich> {
    private static final UUID HEALTH_UUID = UUID.fromString("2b6f4d0e-7a1c-4e93-8d52-6c0a9f3e1b44");
    private static final UUID ATTACK_UUID = UUID.fromString("9e3a1c75-4d2b-4f6e-a0b8-3f7c2d5e9a55");
    private static final double SKILL_RANGE = 32.0;
    private static final int HOLD_COOLDOWN = 70;
    private static final int RESUME_COOLDOWN = 40;

    private enum SkillId {
        SHADOW_CROSSFIRE, CONSTELLATION, SHIELD_BACKLASH, CORPSE_DETONATION, LIFE_DRAIN, SOUL_PRISON, SHADOW_BLINK
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    @Nullable
    private LichSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 40;
    private boolean initialized;
    private boolean enraged;
    private int lastShield = -1;

    public LichBrain(Lich lich) {
        super(lich);
    }

    @Override
    protected double skillKnockback() {
        return LichConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof Lich || entity instanceof LichMinion;
    }

    public Lich lich() {
        return this.mob;
    }

    public boolean busy() {
        return this.current != null;
    }

    public boolean invulnerable() {
        return this.current != null && this.current.invulnerable();
    }

    public boolean blocksMelee() {
        return this.current != null && this.current.blocksMelee();
    }

    public boolean enraged() {
        return this.enraged;
    }

    public void tick() {
        if (this.mob.isShadowClone()) {
            return;
        }
        if (!LichConfig.ENABLED.get()) {
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
        if (this.checkTriggers()) {
            return;
        }
        if (this.current != null) {
            this.holdVanillaAttacks();
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

    private void initialize() {
        this.initialized = true;
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", LichConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        this.applyModifier(Attributes.ATTACK_DAMAGE, ATTACK_UUID, "Twilight Boss Plus attack", LichConfig.ATTACK_MULTIPLIER.get() - 1.0, false);
        this.lastShield = this.mob.getShieldStrength();
        this.enraged = this.mob.getShieldStrength() <= 0 && this.healthRatio() <= LichConfig.RAGE_HEALTH.get();
    }

    private boolean checkTriggers() {
        int shield = this.mob.getShieldStrength();
        int previous = this.lastShield;
        this.lastShield = shield;
        if (shield < previous && !(this.current instanceof ShieldBacklash) && !(this.current instanceof TwilightCollapse)) {
            TwilightBossPlus.debug("Lich {} shield broke {} -> {}", this.mob.getId(), previous, shield);
            if (this.ready(SkillId.SHIELD_BACKLASH, LichConfig.SHIELD_BACKLASH) || shield <= 0) {
                this.forceSkill(SkillId.SHIELD_BACKLASH, new ShieldBacklash(this, shield <= 0));
                return true;
            }
        }
        if (!this.enraged && shield <= 0 && this.healthRatio() <= LichConfig.RAGE_HEALTH.get()) {
            this.enraged = true;
            TwilightBossPlus.debug("Lich {} entered rage at health ratio {}", this.mob.getId(), this.healthRatio());
            if (LichConfig.TWILIGHT_COLLAPSE.enabled.get()) {
                this.forceSkill(null, new TwilightCollapse(this));
                return true;
            }
        }
        return false;
    }

    private void holdVanillaAttacks() {
        this.mob.setAttackCooldown(Math.max(this.mob.getAttackCooldown(), HOLD_COOLDOWN));
        for (Lich clone : this.clones()) {
            clone.setAttackCooldown(Math.max(clone.getAttackCooldown(), HOLD_COOLDOWN));
        }
    }

    public List<Lich> clones() {
        List<Lich> result = new ArrayList<>();
        for (Lich other : this.lich().getAllClones()) {
            if (other != this.mob && other.isShadowClone() && other.getMaster() == this.mob && other.isAlive()) {
                result.add(other);
            }
        }
        return result;
    }

    public List<LichMinion> minions() {
        return this.level().getEntitiesOfClass(LichMinion.class, this.mob.getBoundingBox().inflate(32.0, 16.0, 32.0),
                minion -> minion.master == this.mob && minion.isAlive());
    }

    private void forceSkill(@Nullable SkillId id, LichSkill skill) {
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
        boolean sight = this.mob.hasLineOfSight(target);
        int phase = this.mob.getPhase();
        List<SkillId> options = new ArrayList<>();
        if (this.ready(SkillId.CONSTELLATION, LichConfig.CONSTELLATION) && distance < 24.0) {
            options.add(SkillId.CONSTELLATION);
        }
        if (phase == 1) {
            if (this.ready(SkillId.SHADOW_CROSSFIRE, LichConfig.SHADOW_CROSSFIRE) && sight && distance < 22.0) {
                options.add(SkillId.SHADOW_CROSSFIRE);
            }
        } else {
            if (this.ready(SkillId.LIFE_DRAIN, LichConfig.LIFE_DRAIN) && sight && distance < 16.0) {
                options.add(SkillId.LIFE_DRAIN);
            }
            if (this.ready(SkillId.SOUL_PRISON, LichConfig.SOUL_PRISON) && distance < 20.0) {
                options.add(SkillId.SOUL_PRISON);
            }
        }
        if (phase == 2 && this.ready(SkillId.CORPSE_DETONATION, LichConfig.CORPSE_DETONATION) && !this.minions().isEmpty()) {
            options.add(SkillId.CORPSE_DETONATION);
        }
        if (phase == 3 && this.ready(SkillId.SHADOW_BLINK, LichConfig.SHADOW_BLINK)) {
            options.add(SkillId.SHADOW_BLINK);
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        this.startSkill(chosen, this.create(chosen));
    }

    private LichSkill create(SkillId id) {
        return switch (id) {
            case SHADOW_CROSSFIRE -> new ShadowCrossfire(this);
            case CONSTELLATION -> new TwilightConstellation(this);
            case SHIELD_BACKLASH -> new ShieldBacklash(this, false);
            case CORPSE_DETONATION -> new CorpseDetonation(this);
            case LIFE_DRAIN -> new LifeDrain(this);
            case SOUL_PRISON -> new SoulPrison(this);
            case SHADOW_BLINK -> new ShadowBlink(this);
        };
    }

    private LichConfig.Skill config(SkillId id) {
        return switch (id) {
            case SHADOW_CROSSFIRE -> LichConfig.SHADOW_CROSSFIRE;
            case CONSTELLATION -> LichConfig.CONSTELLATION;
            case SHIELD_BACKLASH -> LichConfig.SHIELD_BACKLASH;
            case CORPSE_DETONATION -> LichConfig.CORPSE_DETONATION;
            case LIFE_DRAIN -> LichConfig.LIFE_DRAIN;
            case SOUL_PRISON -> LichConfig.SOUL_PRISON;
            case SHADOW_BLINK -> LichConfig.SHADOW_BLINK;
        };
    }

    private boolean ready(SkillId id, LichConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(@Nullable SkillId id, LichSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Lich {} starts skill {} in phase {}", this.mob.getId(), skill.name(), this.mob.getPhase());
        this.holdVanillaAttacks();
        skill.start();
    }

    private void endCurrent() {
        LichSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        double multiplier = 1.0;
        if (this.enraged) {
            multiplier = LichConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = (int) (this.config(id).cooldown.get() * multiplier);
        }
        this.globalCooldown = (int) (LichConfig.SKILL_INTERVAL.get() * multiplier);
        this.mob.setAttackCooldown(RESUME_COOLDOWN);
        for (Lich clone : this.clones()) {
            clone.setAttackCooldown(RESUME_COOLDOWN + this.mob.getRandom().nextInt(20));
        }
        TwilightBossPlus.debug("Lich {} ends skill {}", this.mob.getId(), skill.name());
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.clearHazards();
    }
}
