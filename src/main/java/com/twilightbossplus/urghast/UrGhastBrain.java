package com.twilightbossplus.urghast;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.UrGhastConfig;
import com.twilightbossplus.urghast.skill.CrimsonLightning;
import com.twilightbossplus.urghast.skill.TearPrism;
import com.twilightbossplus.urghast.skill.TearStorm;
import com.twilightbossplus.urghast.skill.TearVolley;
import com.twilightbossplus.urghast.skill.UrGhastSkill;
import com.twilightbossplus.urghast.skill.WailingCone;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.UrGhast;
import twilightforest.entity.monster.CarminiteGhastguard;

public final class UrGhastBrain extends BossBrain<UrGhast> {
    private static final UUID HEALTH_UUID = UUID.fromString("e3a71c52-0f6d-4b9e-8c24-91d5b7a3f60e");
    private static final double SKILL_RANGE = 64.0;

    private enum SkillId {
        TEAR_VOLLEY, WAILING_CONE, TEAR_PRISM, CRIMSON_LIGHTNING
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    @Nullable
    private UrGhastSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 60;
    private int tearIds;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;

    public UrGhastBrain(UrGhast ghast) {
        super(ghast);
    }

    @Override
    protected double skillKnockback() {
        return UrGhastConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof CarminiteGhastguard;
    }

    @Override
    public double attack() {
        return UrGhastConfig.BASE_ATTACK.get() * UrGhastConfig.ATTACK_MULTIPLIER.get();
    }

    public UrGhast ghast() {
        return this.mob;
    }

    public boolean stageTwo() {
        return this.stageTwo;
    }

    public boolean enraged() {
        return this.stageThree;
    }

    public int nextTearId() {
        this.tearIds++;
        return this.mob.getId() * 1000 + this.tearIds % 1000;
    }

    public Vec3 center() {
        return this.mob.position().add(0.0, this.mob.getBbHeight() * 0.5, 0.0);
    }

    public Vec3 mouth() {
        return this.center().add(this.mob.getViewVector(1.0F).scale(this.mob.getBbWidth() * 0.45));
    }

    @Nullable
    public LivingEntity focus() {
        LivingEntity target = this.target();
        if (target != null) {
            return target;
        }
        Player best = null;
        double bestDistance = SKILL_RANGE * SKILL_RANGE;
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.mob.getBoundingBox().inflate(SKILL_RANGE))) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            double distance = player.distanceToSqr(this.mob);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
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

    public void tick() {
        if (!UrGhastConfig.ENABLED.get()) {
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
        if (this.checkStages()) {
            return;
        }
        if (this.current != null) {
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
        if (this.mob.isInTantrum()) {
            if (UrGhastConfig.TEAR_STORM.enabled.get()) {
                this.startSkill(null, new TearStorm(this));
            }
            return;
        }
        this.selectSkill();
    }

    private void initialize() {
        this.initialized = true;
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", UrGhastConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        float ratio = this.healthRatio();
        this.stageTwo = ratio <= UrGhastConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= UrGhastConfig.PHASE_THREE_HEALTH.get();
    }

    private boolean checkStages() {
        float ratio = this.healthRatio();
        if (!this.stageTwo && ratio <= UrGhastConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            TwilightBossPlus.debug("Ur-Ghast {} entered phase two at health ratio {}", this.mob.getId(), ratio);
            LivingEntity target = this.focus();
            if (UrGhastConfig.TEAR_PRISM.enabled.get() && target != null) {
                this.forceSkill(SkillId.TEAR_PRISM, new TearPrism(this, target));
                return true;
            }
        }
        if (!this.stageThree && ratio <= UrGhastConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            TwilightBossPlus.debug("Ur-Ghast {} entered phase three at health ratio {}", this.mob.getId(), ratio);
            LivingEntity target = this.focus();
            if (UrGhastConfig.CRIMSON_LIGHTNING.enabled.get() && target != null) {
                this.forceSkill(SkillId.CRIMSON_LIGHTNING, new CrimsonLightning(this, target));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(SkillId id, UrGhastSkill skill) {
        if (this.current != null) {
            this.endCurrent();
        }
        this.startSkill(id, skill);
    }

    private void selectSkill() {
        if (this.globalCooldown > 0) {
            return;
        }
        LivingEntity target = this.focus();
        if (target == null) {
            return;
        }
        double distance = this.center().distanceTo(target.position());
        if (distance > SKILL_RANGE) {
            return;
        }
        List<SkillId> options = new ArrayList<>();
        if (this.ready(SkillId.TEAR_VOLLEY, UrGhastConfig.TEAR_VOLLEY)) {
            options.add(SkillId.TEAR_VOLLEY);
        }
        if (this.ready(SkillId.WAILING_CONE, UrGhastConfig.WAILING_CONE) && distance < WailingCone.RANGE) {
            options.add(SkillId.WAILING_CONE);
        }
        if (this.stageTwo && this.ready(SkillId.TEAR_PRISM, UrGhastConfig.TEAR_PRISM) && distance < 48.0) {
            options.add(SkillId.TEAR_PRISM);
        }
        if (this.stageThree && this.ready(SkillId.CRIMSON_LIGHTNING, UrGhastConfig.CRIMSON_LIGHTNING)) {
            options.add(SkillId.CRIMSON_LIGHTNING);
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        UrGhastSkill skill = switch (chosen) {
            case TEAR_VOLLEY -> new TearVolley(this, target);
            case WAILING_CONE -> new WailingCone(this, target);
            case TEAR_PRISM -> new TearPrism(this, target);
            case CRIMSON_LIGHTNING -> new CrimsonLightning(this, target);
        };
        this.startSkill(chosen, skill);
    }

    private UrGhastConfig.Skill config(SkillId id) {
        return switch (id) {
            case TEAR_VOLLEY -> UrGhastConfig.TEAR_VOLLEY;
            case WAILING_CONE -> UrGhastConfig.WAILING_CONE;
            case TEAR_PRISM -> UrGhastConfig.TEAR_PRISM;
            case CRIMSON_LIGHTNING -> UrGhastConfig.CRIMSON_LIGHTNING;
        };
    }

    private boolean ready(SkillId id, UrGhastConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(@Nullable SkillId id, UrGhastSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Ur-Ghast {} starts skill {} at health ratio {}", this.mob.getId(), skill.name(), this.healthRatio());
        skill.start();
    }

    private void endCurrent() {
        UrGhastSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        this.mob.setCharging(false);
        double multiplier = 1.0;
        if (this.stageThree) {
            multiplier = UrGhastConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = Math.max(this.cooldowns[id.ordinal()], (int) (this.config(id).cooldown.get() * multiplier));
        }
        this.globalCooldown = (int) (UrGhastConfig.SKILL_INTERVAL.get() * multiplier);
        TwilightBossPlus.debug("Ur-Ghast {} ends skill {}", this.mob.getId(), skill.name());
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.clearHazards();
    }
}
