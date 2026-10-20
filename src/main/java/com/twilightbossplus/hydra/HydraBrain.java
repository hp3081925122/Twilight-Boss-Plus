package com.twilightbossplus.hydra;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.HydraConfig;
import com.twilightbossplus.hydra.skill.EmberRain;
import com.twilightbossplus.hydra.skill.FlameSweep;
import com.twilightbossplus.hydra.skill.HydraSkill;
import com.twilightbossplus.hydra.skill.InfernoCrown;
import com.twilightbossplus.hydra.skill.JawStrike;
import com.twilightbossplus.hydra.skill.MagmaStomp;
import com.twilightbossplus.hydra.skill.MagmaVolley;
import com.twilightbossplus.hydra.skill.RegrowthRoar;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.Hydra;
import twilightforest.entity.boss.HydraHead;
import twilightforest.entity.boss.HydraHeadContainer;

public final class HydraBrain extends BossBrain<Hydra> {
    public static final int HEADS = 7;
    private static final UUID HEALTH_UUID = UUID.fromString("9b2f6c41-7d3e-4a58-b1c9-5e0a8d4f2c19");
    private static final double SKILL_RANGE = 32.0;
    private static final double MOUTH_OFFSET = 3.0;

    private enum SkillId {
        MAGMA_VOLLEY, FLAME_SWEEP, JAW_STRIKE, MAGMA_STOMP, INFERNO_CROWN, EMBER_RAIN
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    private final boolean[] aiming = new boolean[HEADS];
    private final float[] aimYaw = new float[HEADS];
    private final float[] aimPitch = new float[HEADS];
    @Nullable
    private HydraSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 60;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;

    public HydraBrain(Hydra hydra) {
        super(hydra);
    }

    @Override
    protected double skillKnockback() {
        return HydraConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof Hydra;
    }

    @Override
    public double attack() {
        return HydraConfig.BASE_ATTACK.get() * HydraConfig.ATTACK_MULTIPLIER.get();
    }

    public Hydra hydra() {
        return this.mob;
    }

    public boolean enraged() {
        return this.stageThree;
    }

    public HydraHeadContainer head(int index) {
        return this.mob.hc[index];
    }

    public boolean headAlive(int index) {
        return HydraHeads.alive(this.mob.hc[index]);
    }

    public List<Integer> aliveHeads(int... candidates) {
        List<Integer> result = new ArrayList<>();
        for (int index : candidates) {
            if (this.headAlive(index)) {
                result.add(index);
            }
        }
        return result;
    }

    public void aim(int index, float yaw, float pitch) {
        this.aiming[index] = true;
        this.aimYaw[index] = yaw;
        this.aimPitch[index] = pitch;
    }

    public void release(int index) {
        this.aiming[index] = false;
    }

    public boolean aiming(int index) {
        return this.aiming[index];
    }

    public float aimYaw(int index) {
        return this.aimYaw[index];
    }

    public float aimPitch(int index) {
        return this.aimPitch[index];
    }

    public Vec3 headPos(int index) {
        HydraHead head = this.mob.hc[index].headEntity;
        return head.position().add(0.0, 1.0, 0.0);
    }

    public Vec3 mouth(int index) {
        float yaw = this.aimYaw[index];
        float pitch = this.aimPitch[index];
        if (!this.aiming[index]) {
            HydraHead head = this.mob.hc[index].headEntity;
            yaw = head.getYRot();
            pitch = head.getXRot();
        }
        return this.headPos(index).add(Vec3.directionFromRotation(pitch, yaw).scale(MOUTH_OFFSET));
    }

    public void tick() {
        if (!HydraConfig.ENABLED.get()) {
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
        if (this.stageThree && this.mob.tickCount % 100 == 0) {
            HydraFx.aura(this, this.mob, 120);
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
        this.selectSkill();
    }

    private void initialize() {
        this.initialized = true;
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", HydraConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        float ratio = this.healthRatio();
        this.stageTwo = ratio <= HydraConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= HydraConfig.PHASE_THREE_HEALTH.get();
    }

    private boolean checkStages() {
        float ratio = this.healthRatio();
        if (!this.stageTwo && ratio <= HydraConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            TwilightBossPlus.debug("Hydra {} entered phase two at health ratio {}", this.mob.getId(), ratio);
            if (HydraConfig.INFERNO_CROWN.enabled.get() && this.target() != null) {
                this.forceSkill(SkillId.INFERNO_CROWN, new InfernoCrown(this));
                return true;
            }
        }
        if (!this.stageThree && ratio <= HydraConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            TwilightBossPlus.debug("Hydra {} entered phase three at health ratio {}", this.mob.getId(), ratio);
            if (HydraConfig.REGROWTH_ROAR.enabled.get()) {
                this.forceSkill(null, new RegrowthRoar(this));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(@Nullable SkillId id, HydraSkill skill) {
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
        List<SkillId> options = new ArrayList<>();
        if (this.ready(SkillId.MAGMA_VOLLEY, HydraConfig.MAGMA_VOLLEY) && distance > 6.0) {
            options.add(SkillId.MAGMA_VOLLEY);
        }
        if (this.ready(SkillId.FLAME_SWEEP, HydraConfig.FLAME_SWEEP) && distance < 20.0) {
            options.add(SkillId.FLAME_SWEEP);
        }
        if (this.ready(SkillId.JAW_STRIKE, HydraConfig.JAW_STRIKE) && distance > JawStrike.MIN_RANGE && distance < JawStrike.MAX_RANGE) {
            options.add(SkillId.JAW_STRIKE);
        }
        if (this.ready(SkillId.MAGMA_STOMP, HydraConfig.MAGMA_STOMP) && distance < MagmaStomp.TRIGGER_RANGE) {
            options.add(SkillId.MAGMA_STOMP);
        }
        if (this.stageTwo) {
            if (this.ready(SkillId.INFERNO_CROWN, HydraConfig.INFERNO_CROWN) && distance < 18.0) {
                options.add(SkillId.INFERNO_CROWN);
            }
            if (this.ready(SkillId.EMBER_RAIN, HydraConfig.EMBER_RAIN)) {
                options.add(SkillId.EMBER_RAIN);
            }
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        this.startSkill(chosen, this.create(chosen));
    }

    private HydraSkill create(SkillId id) {
        return switch (id) {
            case MAGMA_VOLLEY -> new MagmaVolley(this);
            case FLAME_SWEEP -> new FlameSweep(this);
            case JAW_STRIKE -> new JawStrike(this);
            case MAGMA_STOMP -> new MagmaStomp(this);
            case INFERNO_CROWN -> new InfernoCrown(this);
            case EMBER_RAIN -> new EmberRain(this);
        };
    }

    private HydraConfig.Skill config(SkillId id) {
        return switch (id) {
            case MAGMA_VOLLEY -> HydraConfig.MAGMA_VOLLEY;
            case FLAME_SWEEP -> HydraConfig.FLAME_SWEEP;
            case JAW_STRIKE -> HydraConfig.JAW_STRIKE;
            case MAGMA_STOMP -> HydraConfig.MAGMA_STOMP;
            case INFERNO_CROWN -> HydraConfig.INFERNO_CROWN;
            case EMBER_RAIN -> HydraConfig.EMBER_RAIN;
        };
    }

    private boolean ready(SkillId id, HydraConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(@Nullable SkillId id, HydraSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Hydra {} starts skill {}", this.mob.getId(), skill.name());
        skill.start();
    }

    private void endCurrent() {
        HydraSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        for (int i = 0; i < HEADS; i++) {
            this.aiming[i] = false;
        }
        double multiplier = 1.0;
        if (this.stageThree) {
            multiplier = HydraConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = Math.max(this.cooldowns[id.ordinal()], (int) (this.config(id).cooldown.get() * multiplier));
        }
        this.globalCooldown = (int) (HydraConfig.SKILL_INTERVAL.get() * multiplier);
        TwilightBossPlus.debug("Hydra {} ends skill {}", this.mob.getId(), skill.name());
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        for (int i = 0; i < HEADS; i++) {
            this.aiming[i] = false;
        }
        this.clearHazards();
    }
}
