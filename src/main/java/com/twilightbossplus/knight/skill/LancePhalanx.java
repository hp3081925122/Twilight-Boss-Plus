package com.twilightbossplus.knight.skill;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightFx;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.init.TFSounds;

public final class LancePhalanx extends KnightSkill {
    private static final int GATHER = 32;
    private static final int CHARGE = 26;
    private static final int REFORM = 26;
    private static final double BACK = 15.0;
    private static final double LENGTH = 30.0;
    private static final double SPACING = 2.4;
    private static final double LANCE = 2.8;

    private final List<KnightPhantom> line = new ArrayList<>();
    private final Set<Integer> hit = new HashSet<>();
    private Vec3 center = Vec3.ZERO;
    private Vec3 dir = Vec3.ZERO;
    private Vec3 side = Vec3.ZERO;
    private float[] offsets = new float[0];
    private int passes;
    private int pass;
    private int stage;
    private int timer;
    private float angle;

    public LancePhalanx(KnightBrain brain, LivingEntity target) {
        super(brain, target);
    }

    @Override
    public String name() {
        return "LancePhalanx";
    }

    @Override
    public void start() {
        List<KnightPhantom> knights = this.brain.alive();
        knights.removeIf(knight -> KnightBrain.of(knight).busy());
        knights.sort(Comparator.comparingInt(KnightPhantom::getNumber));
        this.line.addAll(knights);
        for (KnightPhantom knight : this.line) {
            this.enlist(knight);
        }
        this.passes = 1;
        if (this.brain.enraged()) {
            this.passes = 2;
        }
        this.angle = this.mob.getRandom().nextFloat() * Mth.TWO_PI;
        this.sound(TFSounds.KNIGHT_PHANTOM_AMBIENT.get(), this.victim.position(), 2.5F, 0.5F);
        this.setup();
    }

    private void setup() {
        this.center = this.floor(this.victim.position());
        this.dir = new Vec3(Math.cos(this.angle), 0.0, Math.sin(this.angle));
        this.side = new Vec3(-this.dir.z, 0.0, this.dir.x);
        int slots = this.line.size() + 1;
        int gap = this.mob.getRandom().nextInt(slots);
        this.offsets = new float[this.line.size()];
        float gapOffset = 0.0F;
        int index = 0;
        for (int j = 0; j < slots; j++) {
            float off = (float) ((j - (slots - 1) * 0.5) * SPACING);
            if (j == gap) {
                gapOffset = off;
                continue;
            }
            this.offsets[index] = off;
            index++;
        }
        this.hit.clear();
        this.stage = 0;
        this.timer = 0;
        KnightFx.phalanx(this.brain, this.center, this.angle, this.line, this.offsets, gapOffset, (float) BACK, (float) LENGTH, GATHER, CHARGE);
        this.sound(SoundEvents.ARMOR_EQUIP_IRON, this.center, 2.0F, 0.5F);
    }

    private Vec3 lane(int i, double along) {
        return this.center.add(this.side.scale(this.offsets[i])).add(this.dir.scale(along - BACK)).add(0.0, 0.1, 0.0);
    }

    @Override
    protected boolean tick() {
        if (this.crew.isEmpty()) {
            return false;
        }
        this.timer++;
        if (this.stage == 0) {
            for (int i = 0; i < this.line.size(); i++) {
                KnightPhantom knight = this.line.get(i);
                if (!this.active(knight)) {
                    continue;
                }
                steer(knight, this.lane(i, 0.0), 1.4);
                look(knight, this.lane(i, 0.0).add(this.dir.scale(5.0)));
            }
            if (this.timer >= GATHER) {
                this.stage = 1;
                this.timer = 0;
                this.sound(SoundEvents.HORSE_GALLOP, this.center, 3.0F, 0.6F);
                this.sound(TFSounds.KNIGHT_PHANTOM_THROW_AXE.get(), this.center, 2.0F, 0.4F);
            }
            return true;
        }
        if (this.stage == 1) {
            double along = LENGTH * this.timer / CHARGE;
            KnightBrain owner;
            for (int i = 0; i < this.line.size(); i++) {
                KnightPhantom knight = this.line.get(i);
                if (!this.active(knight)) {
                    continue;
                }
                steer(knight, this.lane(i, along), LENGTH / CHARGE + 0.4);
                look(knight, knight.position().add(this.dir.scale(5.0)));
                owner = KnightBrain.of(knight);
                Vec3 base = knight.position().add(0.0, 1.0, 0.0);
                Vec3 tip = base.add(this.dir.scale(LANCE));
                for (LivingEntity victim : this.brain.victimsOnSegment(base.subtract(this.dir.scale(0.8)), tip, 0.9)) {
                    if (!this.hit.add(victim.getId())) {
                        continue;
                    }
                    if (owner.damage(victim, KnightConfig.LANCE_PHALANX.multiplier.get())) {
                        owner.knock(victim, this.dir.x, this.dir.z, 0.55, 0.25);
                    }
                }
            }
            if (this.timer % 5 == 0) {
                this.sound(SoundEvents.HORSE_GALLOP, this.center.add(this.dir.scale(along - BACK)), 2.0F, 0.7F);
            }
            if (this.timer >= CHARGE) {
                this.pass++;
                if (this.pass >= this.passes) {
                    return false;
                }
                this.stage = 2;
                this.timer = 0;
            }
            return true;
        }
        for (KnightPhantom knight : this.line) {
            if (this.active(knight)) {
                steer(knight, knight.position(), 0.0);
            }
        }
        if (this.timer >= REFORM) {
            if (!this.valid()) {
                return false;
            }
            this.angle += Mth.HALF_PI;
            if (this.mob.getRandom().nextBoolean()) {
                this.angle += Mth.PI;
            }
            this.setup();
        }
        return true;
    }
}
