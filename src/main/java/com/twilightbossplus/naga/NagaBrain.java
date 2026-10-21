package com.twilightbossplus.naga;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.naga.skill.BurrowAmbush;
import com.twilightbossplus.naga.skill.FangSnap;
import com.twilightbossplus.naga.skill.FrenziedHiss;
import com.twilightbossplus.naga.skill.NagaSkill;
import com.twilightbossplus.naga.skill.QuakeSlam;
import com.twilightbossplus.naga.skill.SerpentLunge;
import com.twilightbossplus.naga.skill.TailLash;
import com.twilightbossplus.naga.skill.VenomBreath;
import com.twilightbossplus.naga.skill.VenomSpit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.entity.PartEntity;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

public final class NagaBrain extends BossBrain<Naga> {
    private static final UUID HEALTH_UUID = UUID.fromString("5d3b7a52-6f0e-4b8e-9b1a-2a7c1e0f4a11");
    private static final UUID ATTACK_UUID = UUID.fromString("8a1f2c34-0b6d-4e2a-9f3c-7d5e6b4a2c22");
    private static final UUID RAGE_SPEED_UUID = UUID.fromString("c7e4d9a1-3f2b-4c8d-a6e5-1b9f0d3c4e33");
    private static final double SKILL_RANGE = 40.0;

    private enum SkillId {
        SERPENT_LUNGE, VENOM_SPIT, TAIL_LASH, QUAKE_SLAM, BURROW_AMBUSH, VENOM_BREATH, SERPENT_CHAIN, FANG_SNAP
    }

    private final Naga naga;
    private final int[] cooldowns = new int[SkillId.values().length];
    @Nullable
    private NagaSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 40;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;
    private boolean pendingDaze;

    public NagaBrain(Naga naga) {
        super(naga);
        this.naga = naga;
    }

    @Override
    protected double skillKnockback() {
        return NagaConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof Naga;
    }

    public Naga naga() {
        return this.naga;
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
        return this.stageThree;
    }

    public void tick() {
        if (!NagaConfig.ENABLED.get()) {
            if (this.current != null) {
                this.endCurrent();
            }
            return;
        }
        if (!this.initialized) {
            this.initialize();
        }
        this.tickHazards();
        if (this.naga.isDeadOrDying()) {
            if (this.current != null) {
                this.endCurrent();
            }
            return;
        }
        if (this.stageThree && this.naga.tickCount % 100 == 0) {
            NagaFx.aura(this, this.naga, 120);
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
        if (this.pendingDaze) {
            this.pendingDaze = false;
            this.naga.getMovementPattern().doDaze();
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
        this.naga.setInvisible(false);
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", NagaConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        this.applyModifier(Attributes.ATTACK_DAMAGE, ATTACK_UUID, "Twilight Boss Plus attack", NagaConfig.ATTACK_MULTIPLIER.get() - 1.0, false);
        float ratio = this.healthRatio();
        this.stageTwo = ratio <= NagaConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= NagaConfig.PHASE_THREE_HEALTH.get();
        if (this.stageThree) {
            this.applyRageSpeed();
        }
    }

    private void applyRageSpeed() {
        this.applyTransient(Attributes.MOVEMENT_SPEED, RAGE_SPEED_UUID, "Twilight Boss Plus rage speed", NagaConfig.RAGE_SPEED_BONUS.get());
    }

    private boolean checkStages() {
        float ratio = this.healthRatio();
        if (!this.stageTwo && ratio <= NagaConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            TwilightBossPlus.debug("Naga {} entered phase two at health ratio {}", this.naga.getId(), ratio);
            if (NagaConfig.BURROW_AMBUSH.enabled.get() && this.target() != null) {
                this.forceSkill(SkillId.BURROW_AMBUSH, new BurrowAmbush(this));
                return true;
            }
        }
        if (!this.stageThree && ratio <= NagaConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            this.applyRageSpeed();
            TwilightBossPlus.debug("Naga {} entered phase three at health ratio {}", this.naga.getId(), ratio);
            if (NagaConfig.FRENZIED_HISS.enabled.get()) {
                this.forceSkill(null, new FrenziedHiss(this));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(@Nullable SkillId id, NagaSkill skill) {
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
        if (target == null || this.naga.isDazed() || this.naga.isCharging()) {
            return;
        }
        double distance = this.naga.distanceTo(target);
        if (distance > SKILL_RANGE) {
            return;
        }
        if (this.ready(SkillId.TAIL_LASH, NagaConfig.TAIL_LASH) && TailLash.shouldTrigger(this)) {
            this.startSkill(SkillId.TAIL_LASH, new TailLash(this));
            return;
        }
        if (this.ready(SkillId.FANG_SNAP, NagaConfig.FANG_SNAP) && distance < FangSnap.TRIGGER_RANGE) {
            this.startSkill(SkillId.FANG_SNAP, new FangSnap(this));
            return;
        }
        if (this.naga.getMovementPattern().getState() != NagaMovementPattern.MovementState.CIRCLE) {
            return;
        }
        List<SkillId> options = new ArrayList<>();
        if (this.ready(SkillId.SERPENT_LUNGE, NagaConfig.SERPENT_LUNGE) && distance > 7.0) {
            options.add(SkillId.SERPENT_LUNGE);
        }
        if (this.ready(SkillId.VENOM_SPIT, NagaConfig.VENOM_SPIT) && distance > 5.0) {
            options.add(SkillId.VENOM_SPIT);
        }
        if (this.stageTwo) {
            if (this.ready(SkillId.QUAKE_SLAM, NagaConfig.QUAKE_SLAM) && distance < 14.0) {
                options.add(SkillId.QUAKE_SLAM);
            }
            if (this.ready(SkillId.BURROW_AMBUSH, NagaConfig.BURROW_AMBUSH)) {
                options.add(SkillId.BURROW_AMBUSH);
            }
        }
        if (this.stageThree) {
            if (this.ready(SkillId.VENOM_BREATH, NagaConfig.VENOM_BREATH) && distance < 12.0) {
                options.add(SkillId.VENOM_BREATH);
            }
            if (this.ready(SkillId.SERPENT_CHAIN, NagaConfig.SERPENT_CHAIN) && distance > 6.0) {
                options.add(SkillId.SERPENT_CHAIN);
            }
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.naga.getRandom().nextInt(options.size()));
        this.startSkill(chosen, this.create(chosen));
    }

    private NagaSkill create(SkillId id) {
        return switch (id) {
            case SERPENT_LUNGE -> new SerpentLunge(this, 1, NagaConfig.LUNGE_WINDUP.get(), NagaConfig.SERPENT_LUNGE.multiplier.get());
            case VENOM_SPIT -> new VenomSpit(this);
            case TAIL_LASH -> new TailLash(this);
            case QUAKE_SLAM -> new QuakeSlam(this);
            case BURROW_AMBUSH -> new BurrowAmbush(this);
            case VENOM_BREATH -> new VenomBreath(this);
            case SERPENT_CHAIN -> new SerpentLunge(this, NagaConfig.CHAIN_COUNT.get(), Math.max(6, NagaConfig.LUNGE_WINDUP.get() * 3 / 5), NagaConfig.SERPENT_CHAIN.multiplier.get());
            case FANG_SNAP -> new FangSnap(this);
        };
    }

    private boolean ready(SkillId id, NagaConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(@Nullable SkillId id, NagaSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Naga {} starts skill {}", this.naga.getId(), skill.name());
        skill.start();
    }

    private void endCurrent() {
        NagaSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        double multiplier = 1.0;
        if (this.stageThree) {
            multiplier = NagaConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = (int) (this.config(id).cooldown.get() * multiplier);
        }
        this.globalCooldown = (int) (NagaConfig.SKILL_INTERVAL.get() * multiplier);
        TwilightBossPlus.debug("Naga {} ends skill {}", this.naga.getId(), skill.name());
    }

    private NagaConfig.Skill config(SkillId id) {
        return switch (id) {
            case SERPENT_LUNGE -> NagaConfig.SERPENT_LUNGE;
            case VENOM_SPIT -> NagaConfig.VENOM_SPIT;
            case TAIL_LASH -> NagaConfig.TAIL_LASH;
            case QUAKE_SLAM -> NagaConfig.QUAKE_SLAM;
            case BURROW_AMBUSH -> NagaConfig.BURROW_AMBUSH;
            case VENOM_BREATH -> NagaConfig.VENOM_BREATH;
            case SERPENT_CHAIN -> NagaConfig.SERPENT_CHAIN;
            case FANG_SNAP -> NagaConfig.FANG_SNAP;
        };
    }

    public void requestDaze() {
        this.pendingDaze = true;
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.clearHazards();
    }

    public List<Entity> activeSegments() {
        List<Entity> result = new ArrayList<>();
        PartEntity<?>[] parts = this.naga.getParts();
        if (parts == null) {
            return result;
        }
        for (PartEntity<?> part : parts) {
            if (part.getBbWidth() > 0.1F) {
                result.add(part);
            }
        }
        return result;
    }

}
