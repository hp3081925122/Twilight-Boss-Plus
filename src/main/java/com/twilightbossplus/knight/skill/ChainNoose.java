package com.twilightbossplus.knight.skill;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightFx;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.init.TFSounds;

public final class ChainNoose extends KnightSkill {
    public static final int MIN_KNIGHTS = 3;
    private static final int MAX_KNIGHTS = 5;
    private static final int GATHER = 26;
    private static final int TIGHTEN = 80;
    private static final double END_RADIUS = 1.6;
    private static final double CHAIN_HEIGHT = 0.35;
    private static final int HIT_COOLDOWN = 12;

    private final List<KnightPhantom> ring = new ArrayList<>();
    private final Map<Integer, Integer> lastHit = new HashMap<>();
    private boolean[] broken = new boolean[0];
    private float[] startHealth = new float[0];
    private Vec3 center = Vec3.ZERO;
    private double startRadius;
    private double spin;
    private double offset;

    public ChainNoose(KnightBrain brain, LivingEntity target) {
        super(brain, target);
    }

    @Override
    public String name() {
        return "ChainNoose";
    }

    @Override
    public void start() {
        this.center = this.floor(this.victim.position());
        List<KnightPhantom> knights = this.brain.alive();
        knights.sort(Comparator.comparingDouble(knight -> knight.distanceToSqr(this.victim)));
        for (KnightPhantom knight : knights) {
            if (this.ring.size() >= MAX_KNIGHTS) {
                break;
            }
            if (!KnightBrain.of(knight).busy()) {
                this.ring.add(knight);
            }
        }
        this.startRadius = 7.0;
        this.spin = 0.022;
        if (this.brain.stageTwo()) {
            this.startRadius = 8.0;
        }
        if (this.brain.enraged()) {
            this.spin = 0.032;
        }
        if (this.mob.getRandom().nextBoolean()) {
            this.spin = -this.spin;
        }
        this.offset = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
        this.broken = new boolean[this.ring.size()];
        this.startHealth = new float[this.ring.size()];
        for (int i = 0; i < this.ring.size(); i++) {
            this.enlist(this.ring.get(i));
            this.startHealth[i] = this.ring.get(i).getHealth();
        }
        KnightFx.noose(this.brain, this.center, this.ring, GATHER, TIGHTEN, this.startRadius, END_RADIUS);
        this.sound(TFSounds.KNIGHT_PHANTOM_AMBIENT.get(), this.center, 2.0F, 0.5F);
        this.sound(SoundEvents.CHAIN_PLACE, this.center, 2.0F, 0.5F);
    }

    private double radius() {
        if (this.age < GATHER) {
            return this.startRadius;
        }
        float p = Mth.clamp((this.age - GATHER) / (float) TIGHTEN, 0.0F, 1.0F);
        float eased = p * p * (3.0F - 2.0F * p);
        return Mth.lerp(eased, this.startRadius, END_RADIUS);
    }

    private Vec3 anchor(KnightPhantom knight) {
        return new Vec3(knight.getX(), this.center.y + CHAIN_HEIGHT, knight.getZ());
    }

    @Override
    protected boolean tick() {
        if (this.ring.size() < MIN_KNIGHTS) {
            return false;
        }
        double radius = this.radius();
        if (this.age >= GATHER) {
            this.offset += this.spin;
        }
        int n = this.ring.size();
        for (int i = 0; i < n; i++) {
            KnightPhantom knight = this.ring.get(i);
            if (this.broken[i] || !this.active(knight)) {
                continue;
            }
            double a = this.offset + i * Mth.TWO_PI / n;
            Vec3 slot = new Vec3(this.center.x + Math.cos(a) * radius, this.center.y, this.center.z + Math.sin(a) * radius);
            double speed = 0.9;
            if (this.age >= GATHER) {
                speed = 1.2;
            }
            steer(knight, slot, speed);
            look(knight, this.center);
            if (this.age >= GATHER && this.startHealth[i] - knight.getHealth() >= knight.getMaxHealth() * KnightConfig.NOOSE_BREAK_RATIO.get()) {
                this.broken[i] = true;
                KnightFx.nooseBreak(this.brain, knight);
                this.sound(SoundEvents.CHAIN_BREAK, knight.position(), 2.0F, 0.6F);
                this.dismiss(knight);
            }
        }
        boolean anyChain = false;
        if (this.age >= GATHER - 6) {
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                KnightPhantom a = this.ring.get(i);
                KnightPhantom b = this.ring.get(j);
                if (this.broken[i] || this.broken[j] || !this.active(a) || !this.active(b)) {
                    continue;
                }
                anyChain = true;
                if (this.age >= GATHER) {
                    this.lash(this.anchor(a), this.anchor(b), a);
                }
            }
            if (!anyChain) {
                return false;
            }
        }
        if (this.age >= GATHER && this.age % 10 == 0) {
            this.sound(SoundEvents.CHAIN_STEP, this.center, 1.5F, 0.6F);
        }
        if (this.age >= GATHER + TIGHTEN) {
            this.snap();
            return false;
        }
        return true;
    }

    private void lash(Vec3 a, Vec3 b, KnightPhantom owner) {
        KnightBrain brain = KnightBrain.of(owner);
        for (LivingEntity victim : this.brain.victimsOnSegment(a, b, 0.35)) {
            double ground = this.brain.groundY(victim.getX(), victim.getZ(), victim.getY());
            if (victim.getY() - ground > 0.9) {
                continue;
            }
            Integer last = this.lastHit.get(victim.getId());
            if (last != null && this.age - last < HIT_COOLDOWN) {
                continue;
            }
            this.lastHit.put(victim.getId(), this.age);
            if (brain.damage(victim, KnightConfig.CHAIN_NOOSE.multiplier.get())) {
                brain.knockFrom(victim, this.center, 0.3, 0.25);
            }
        }
    }

    private void snap() {
        double radius = END_RADIUS + 1.0;
        for (LivingEntity victim : this.brain.victimsAround(this.center, radius, 3.0)) {
            if (this.brain.damage(victim, KnightConfig.NOOSE_SNAP_MULTIPLIER.get())) {
                this.brain.knockFrom(victim, this.center, 0.4, 0.35);
            }
        }
        KnightFx.nooseSnap(this.brain, this.center, radius);
        this.brain.shake(this.center, 0.45F, 16.0F, 12);
        this.sound(SoundEvents.CHAIN_BREAK, this.center, 2.5F, 0.4F);
        this.sound(SoundEvents.ANVIL_LAND, this.center, 1.2F, 0.5F);
    }
}
