package com.twilightbossplus.minoshroom;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.common.BossBrain;
import com.twilightbossplus.config.MinoConfig;
import com.twilightbossplus.minoshroom.skill.AxeCleave;
import com.twilightbossplus.minoshroom.skill.AxeFissure;
import com.twilightbossplus.minoshroom.skill.BullRage;
import com.twilightbossplus.minoshroom.skill.GoreRush;
import com.twilightbossplus.minoshroom.skill.LabyrinthQuake;
import com.twilightbossplus.minoshroom.skill.MinoSkill;
import com.twilightbossplus.minoshroom.skill.MyceliumSnare;
import com.twilightbossplus.minoshroom.skill.SporeBurst;
import com.twilightbossplus.minoshroom.skill.Stunned;
import com.twilightbossplus.minoshroom.skill.Whirlwind;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import twilightforest.entity.boss.Minoshroom;

public final class MinoBrain extends BossBrain<Minoshroom> {
    private static final UUID HEALTH_UUID = UUID.fromString("4c1e7b93-2d5a-4f08-b6e1-8a3d9c2f5b66");
    private static final UUID ATTACK_UUID = UUID.fromString("e7a24d18-5b3c-4a9f-8e2d-1c6b0f4a7d77");
    private static final UUID RAGE_SPEED_UUID = UUID.fromString("1f8d3a6c-9e2b-4d71-a5c4-6b0e2d9f3a88");
    private static final double SKILL_RANGE = 32.0;

    private enum SkillId {
        GORE_RUSH, AXE_FISSURE, SPORE_BURST, WHIRLWIND, LABYRINTH_QUAKE, MYCELIUM_SNARE, AXE_CLEAVE
    }

    private final int[] cooldowns = new int[SkillId.values().length];
    @Nullable
    private MinoSkill current;
    @Nullable
    private SkillId currentId;
    private int globalCooldown = 40;
    private boolean initialized;
    private boolean stageTwo;
    private boolean stageThree;
    private boolean pendingStun;
    private boolean pendingRush;

    public MinoBrain(Minoshroom mino) {
        super(mino);
    }

    @Override
    protected double skillKnockback() {
        return MinoConfig.SKILL_KNOCKBACK.get();
    }

    @Override
    protected boolean isAlly(LivingEntity entity) {
        return entity instanceof Minoshroom;
    }

    public Minoshroom mino() {
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

    public boolean stunned() {
        return this.current instanceof Stunned;
    }

    public boolean enraged() {
        return this.stageThree;
    }

    public void requestStun() {
        this.pendingStun = true;
    }

    public void requestRush() {
        this.pendingRush = true;
    }

    public void tick() {
        if (!MinoConfig.ENABLED.get()) {
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
            MinoFx.aura(this, this.mob, 120);
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
        if (this.pendingStun) {
            this.pendingStun = false;
            this.pendingRush = false;
            if (MinoConfig.STUN_TICKS.get() > 0) {
                this.startSkill(null, new Stunned(this, MinoConfig.STUN_TICKS.get()));
                return;
            }
        }
        if (this.pendingRush) {
            this.pendingRush = false;
            if (this.target() != null) {
                this.startSkill(SkillId.GORE_RUSH, new GoreRush(this, 8, 1));
                return;
            }
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
        this.applyModifier(Attributes.MAX_HEALTH, HEALTH_UUID, "Twilight Boss Plus health", MinoConfig.HEALTH_MULTIPLIER.get() - 1.0, true);
        this.applyModifier(Attributes.ATTACK_DAMAGE, ATTACK_UUID, "Twilight Boss Plus attack", MinoConfig.ATTACK_MULTIPLIER.get() - 1.0, false);
        float ratio = this.healthRatio();
        this.stageTwo = ratio <= MinoConfig.PHASE_TWO_HEALTH.get();
        this.stageThree = ratio <= MinoConfig.PHASE_THREE_HEALTH.get();
        if (this.stageThree) {
            this.applyRageSpeed();
        }
    }

    private void applyRageSpeed() {
        this.applyTransient(Attributes.MOVEMENT_SPEED, RAGE_SPEED_UUID, "Twilight Boss Plus rage speed", MinoConfig.RAGE_SPEED_BONUS.get());
    }

    private boolean checkStages() {
        float ratio = this.healthRatio();
        if (!this.stageTwo && ratio <= MinoConfig.PHASE_TWO_HEALTH.get()) {
            this.stageTwo = true;
            TwilightBossPlus.debug("Minoshroom {} entered phase two at health ratio {}", this.mob.getId(), ratio);
            if (MinoConfig.LABYRINTH_QUAKE.enabled.get() && this.target() != null) {
                this.forceSkill(SkillId.LABYRINTH_QUAKE, new LabyrinthQuake(this));
                return true;
            }
        }
        if (!this.stageThree && ratio <= MinoConfig.PHASE_THREE_HEALTH.get()) {
            this.stageThree = true;
            this.applyRageSpeed();
            TwilightBossPlus.debug("Minoshroom {} entered phase three at health ratio {}", this.mob.getId(), ratio);
            if (MinoConfig.BULL_RAGE.enabled.get()) {
                this.forceSkill(null, new BullRage(this));
                return true;
            }
        }
        return false;
    }

    private void forceSkill(@Nullable SkillId id, MinoSkill skill) {
        if (this.current != null) {
            this.endCurrent();
        }
        this.pendingStun = false;
        this.pendingRush = false;
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
        if (this.ready(SkillId.GORE_RUSH, MinoConfig.GORE_RUSH) && sight && distance > 5.0 && distance < 22.0) {
            options.add(SkillId.GORE_RUSH);
        }
        if (this.ready(SkillId.AXE_FISSURE, MinoConfig.AXE_FISSURE) && distance < 14.0) {
            options.add(SkillId.AXE_FISSURE);
        }
        if (this.ready(SkillId.SPORE_BURST, MinoConfig.SPORE_BURST) && distance < 18.0) {
            options.add(SkillId.SPORE_BURST);
        }
        if (this.ready(SkillId.AXE_CLEAVE, MinoConfig.AXE_CLEAVE) && distance < AxeCleave.TRIGGER_RANGE) {
            options.add(SkillId.AXE_CLEAVE);
        }
        if (this.stageTwo) {
            if (this.ready(SkillId.WHIRLWIND, MinoConfig.WHIRLWIND) && distance < 10.0) {
                options.add(SkillId.WHIRLWIND);
            }
            if (this.ready(SkillId.LABYRINTH_QUAKE, MinoConfig.LABYRINTH_QUAKE) && distance < 12.0) {
                options.add(SkillId.LABYRINTH_QUAKE);
            }
            if (this.ready(SkillId.MYCELIUM_SNARE, MinoConfig.MYCELIUM_SNARE) && sight && distance > 4.0 && distance < 18.0) {
                options.add(SkillId.MYCELIUM_SNARE);
            }
        }
        if (options.isEmpty()) {
            return;
        }
        SkillId chosen = options.get(this.mob.getRandom().nextInt(options.size()));
        this.startSkill(chosen, this.create(chosen));
    }

    private MinoSkill create(SkillId id) {
        int charges = 1;
        if (this.stageThree) {
            charges = 2;
        }
        return switch (id) {
            case GORE_RUSH -> new GoreRush(this, MinoConfig.RUSH_WINDUP.get(), charges);
            case AXE_FISSURE -> new AxeFissure(this);
            case SPORE_BURST -> new SporeBurst(this);
            case WHIRLWIND -> new Whirlwind(this);
            case LABYRINTH_QUAKE -> new LabyrinthQuake(this);
            case MYCELIUM_SNARE -> new MyceliumSnare(this);
            case AXE_CLEAVE -> new AxeCleave(this);
        };
    }

    private MinoConfig.Skill config(SkillId id) {
        return switch (id) {
            case GORE_RUSH -> MinoConfig.GORE_RUSH;
            case AXE_FISSURE -> MinoConfig.AXE_FISSURE;
            case SPORE_BURST -> MinoConfig.SPORE_BURST;
            case WHIRLWIND -> MinoConfig.WHIRLWIND;
            case LABYRINTH_QUAKE -> MinoConfig.LABYRINTH_QUAKE;
            case MYCELIUM_SNARE -> MinoConfig.MYCELIUM_SNARE;
            case AXE_CLEAVE -> MinoConfig.AXE_CLEAVE;
        };
    }

    private boolean ready(SkillId id, MinoConfig.Skill config) {
        return config.enabled.get() && this.cooldowns[id.ordinal()] <= 0;
    }

    private void startSkill(@Nullable SkillId id, MinoSkill skill) {
        this.current = skill;
        this.currentId = id;
        TwilightBossPlus.debug("Minoshroom {} starts skill {}", this.mob.getId(), skill.name());
        skill.start();
    }

    private void endCurrent() {
        MinoSkill skill = this.current;
        SkillId id = this.currentId;
        this.current = null;
        this.currentId = null;
        if (skill == null) {
            return;
        }
        skill.end();
        double multiplier = 1.0;
        if (this.stageThree) {
            multiplier = MinoConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        if (id != null) {
            this.cooldowns[id.ordinal()] = Math.max(this.cooldowns[id.ordinal()], (int) (this.config(id).cooldown.get() * multiplier));
        }
        this.globalCooldown = (int) (MinoConfig.SKILL_INTERVAL.get() * multiplier);
        TwilightBossPlus.debug("Minoshroom {} ends skill {}", this.mob.getId(), skill.name());
    }

    public void stopAll() {
        if (this.current != null) {
            this.endCurrent();
        }
        this.clearHazards();
    }
}
