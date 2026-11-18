package com.twilightbossplus.yeti;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.skill.AvalanchePounce;
import com.twilightbossplus.yeti.skill.FrostClap;
import com.twilightbossplus.yeti.skill.PermafrostField;
import com.twilightbossplus.yeti.skill.RollingSnowball;
import com.twilightbossplus.yeti.skill.WhiteoutRampage;
import com.twilightbossplus.yeti.skill.YetiSkill;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.monster.Yeti;
import twilightforest.init.TFSounds;

public final class YetiBrain extends BossBrain<AlphaYeti> {
    private static final UUID HEALTH_UUID = UUID.fromString("7b2e9c41-6d3a-4f85-a1c9-3e8b5d2f7a14");
    private static final double SKILL_RANGE = 32.0;

    private enum SkillId {
        FROST_CLAP, AVALANCHE_POUNCE, ROLLING_SNOWBALL, PERMAFROST_FIELD
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    @Nullable
    private YetiSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 40;
    private int ballIds;
    private float lastHealth = -1.0F;
    private float damageTaken;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;

    public YetiBrain(AlphaYeti yeti) {
        super(yeti);
    }

    @Override
    protected double skillKnockback() {
        return YetiConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof AlphaYeti || entity instanceof Yeti;
    }

    @Override
    public double attack() {
        return YetiConfig.BASE_ATTACK.get() * YetiConfig.ATTACK_MULTIPLIER.get();
    }

    public AlphaYeti yeti() {
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

    public int nextBallId() {
        this.ballIds++;
        return this.mob.getId() * 1000 + this.ballIds % 1000;
    }

    public void freeze(LivingEntity victim) {
        int add = YetiConfig.FREEZE_TICKS.get();
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
        this.freeze(victim);
        return true;
    }

    public void slam(Vec3 center, double radius, double multiplier) {
        for (LivingEntity victim : this.victimsAround(center, radius, 3.0)) {
            this.frost(victim, multiplier, center, 0.4, 0.5);
        }
        YetiFx.landing(this, center, radius);
        this.shake(center, 0.8F, 20.0F, 10);
        this.level().playSound(null, center.x, center.y, center.z, TFSounds.ALPHA_YETI_ICE.get(), SoundSource.HOSTILE, 2.0F, 0.6F);
        this.level().playSound(null, center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.5F);
    }

    public void tick() {
        if (!YetiConfig.ENABLED.get()) {
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
        float health = this.mob.getHealth();
        if (health < this.lastHealth) {
            this.damageTaken += this.lastHealth - health;
        }
        this.lastHealth = health;
        if (this.checkStages()) {
            return;
        }
        if (this.current != null) {
            if (!this.current.update()) {
                this.endCurrent();
            }
            return;
        }
        if (this.mob.isTired() || this.mob.isVehicle()) {
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
        if (YetiConfig.WHITEOUT_RAMPAGE.enabled.get() && this.damageTaken >= this.mob.getMaxHealth() * YetiConfig.RAMPAGE_DAMAGE_RATIO.get() && this.target() != null && this.mob.onGround()) {
            this.damageTaken = 0.0F;
            this.startSkill(null, new WhiteoutRampage(this));
            return;
        }
        this.selectSkill();
    }

    private void initialize() {
        this.initialized = true;
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", YetiConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        this.lastHealth = this.mob.getHealth();
        float ratio = this.healthRatio();
        this.stageTwo = ratio <= YetiConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= YetiConfig.PHASE_THREE_HEALTH.get();
    }

    private boolean checkStages() {
        float ratio = this.healthRatio();
        if (!this.stageTwo && ratio <= YetiConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            TwilightBossPlus.debug("Alpha Yeti {} entered phase two at health ratio {}", this.mob.getId(), ratio);
            if (YetiConfig.PERMAFROST_FIELD.enabled.get() && this.target() != null && !this.mob.isVehicle()) {
                this.forceSkill(SkillId.PERMAFROST_FIELD, new PermafrostField(this));
                return true;
            }
        }
        if (!this.stageThree && ratio <= YetiConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            TwilightBossPlus.debug("Alpha Yeti {} entered phase three at health ratio {}", this.mob.getId(), ratio);
            if (YetiConfig.WHITEOUT_RAMPAGE.enabled.get() && this.target() != null && !this.mob.isVehicle()) {
                this.damageTaken = 0.0F;
                this.forceSkill(null, new WhiteoutRampage(this));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(@Nullable SkillId id, YetiSkill skill) {
        if (this.current != null) {
            this.endCurrent();
        }
        if (this.mob.isTired()) {
            this.mob.setTired(false);
        }
        this.startSkill(id, skill);
    }

    private void selectSkill() {
        if (this.globalCooldown > 0) {
            return;
        }
        LivingEntity target = this.target();
        if (target == null || !this.mob.onGround()) {
            return;
        }
        double distance = this.mob.distanceTo(target);
        if (distance > SKILL_RANGE) {
            return;
        }
        boolean sight = this.mob.hasLineOfSight(target);
        List<SkillId> options = new ArrayList<>();
        if (this.ready(SkillId.FROST_CLAP, YetiConfig.FROST_CLAP) && distance < FrostClap.TRIGGER) {
            options.add(SkillId.FROST_CLAP);
        }
        if (this.ready(SkillId.AVALANCHE_POUNCE, YetiConfig.AVALANCHE_POUNCE) && sight && distance < 22.0) {
            options.add(SkillId.AVALANCHE_POUNCE);
        }
        if (this.ready(SkillId.ROLLING_SNOWBALL, YetiConfig.ROLLING_SNOWBALL) && sight && distance > 5.0 && distance < 30.0) {
            options.add(SkillId.ROLLING_SNOWBALL);
        }
        if (this.stageTwo && this.ready(SkillId.PERMAFROST_FIELD, YetiConfig.PERMAFROST_FIELD) && distance < 13.0) {
            options.add(SkillId.PERMAFROST_FIELD);
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        YetiSkill skill = switch (chosen) {
            case FROST_CLAP -> new FrostClap(this);
            case AVALANCHE_POUNCE -> new AvalanchePounce(this);
            case ROLLING_SNOWBALL -> new RollingSnowball(this);
            case PERMAFROST_FIELD -> new PermafrostField(this);
        };
        this.startSkill(chosen, skill);
    }

    private YetiConfig.Skill config(SkillId id) {
        return switch (id) {
            case FROST_CLAP -> YetiConfig.FROST_CLAP;
            case AVALANCHE_POUNCE -> YetiConfig.AVALANCHE_POUNCE;
            case ROLLING_SNOWBALL -> YetiConfig.ROLLING_SNOWBALL;
            case PERMAFROST_FIELD -> YetiConfig.PERMAFROST_FIELD;
        };
    }

    private boolean ready(SkillId id, YetiConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(@Nullable SkillId id, YetiSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Alpha Yeti {} starts skill {} at health ratio {}", this.mob.getId(), skill.name(), this.healthRatio());
        skill.start();
    }

    private void endCurrent() {
        YetiSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        double multiplier = 1.0;
        if (this.stageThree) {
            multiplier = YetiConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = Math.max(this.cooldowns[id.ordinal()], (int) (this.config(id).cooldown.get() * multiplier));
        }
        this.globalCooldown = (int) (YetiConfig.SKILL_INTERVAL.get() * multiplier);
        TwilightBossPlus.debug("Alpha Yeti {} ends skill {}", this.mob.getId(), skill.name());
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.clearHazards();
    }
}
