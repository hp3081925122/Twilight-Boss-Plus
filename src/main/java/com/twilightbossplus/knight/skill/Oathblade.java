package com.twilightbossplus.knight.skill;

import com.twilightbossplus.config.KnightConfig;
import com.twilightbossplus.knight.KnightBrain;
import com.twilightbossplus.knight.KnightFx;
import com.twilightbossplus.knight.hazard.FlyingBlade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.init.TFSounds;

public final class Oathblade extends KnightSkill {
    private static final int GATHER = 30;
    private static final int FORM = 40;
    private static final int LOCK = 14;
    private static final int FALL = 6;
    private static final int RECOVER = 16;
    private static final double HEIGHT = 10.0;
    private static final double RING = 5.5;
    private static final double RING_HEIGHT = 7.0;
    private static final double IMPACT = 3.5;
    private static final double BLADE_SPEED = 0.75;
    private static final double BLADE_RANGE = 14.0;

    private final List<KnightPhantom> circle = new ArrayList<>();
    private Vec3 sword = Vec3.ZERO;
    private Vec3 landing = Vec3.ZERO;
    private float bladeAngle;
    private double turn;

    public Oathblade(KnightBrain brain, LivingEntity target) {
        super(brain, target);
    }

    @Override
    public String name() {
        return "Oathblade";
    }

    @Override
    public void start() {
        List<KnightPhantom> knights = this.brain.alive();
        knights.removeIf(knight -> KnightBrain.of(knight).busy());
        knights.sort(Comparator.comparingInt(KnightPhantom::getNumber));
        this.circle.addAll(knights);
        for (KnightPhantom knight : this.circle) {
            this.enlist(knight);
        }
        Vec3 ground = this.floor(this.victim.position());
        this.sword = ground.add(0.0, HEIGHT, 0.0);
        this.turn = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
        KnightFx.oath(this.brain, this.circle, this.sword, GATHER, FORM);
        this.sound(TFSounds.KNIGHT_PHANTOM_AMBIENT.get(), ground, 3.0F, 0.4F);
        this.sound(SoundEvents.BEACON_ACTIVATE, ground, 2.0F, 0.6F);
    }

    @Override
    protected boolean tick() {
        if (this.crew.isEmpty() && this.age < GATHER + FORM + LOCK) {
            return false;
        }
        int forged = GATHER + FORM;
        if (this.age < forged) {
            if (this.valid()) {
                Vec3 ground = this.floor(this.victim.position());
                this.sword = new Vec3(Mth.lerp(0.1, this.sword.x, ground.x), ground.y + HEIGHT, Mth.lerp(0.1, this.sword.z, ground.z));
            }
            if (this.age % 2 == 0) {
                KnightFx.oathPos(this.brain, this.sword);
            }
            if (this.age == GATHER) {
                this.sound(SoundEvents.BEACON_POWER_SELECT, this.sword, 2.5F, 0.5F);
            }
        }
        if (this.age == forged) {
            this.landing = this.floor(this.sword);
            this.bladeAngle = this.mob.getRandom().nextFloat() * Mth.TWO_PI;
            KnightFx.oathLock(this.brain, this.landing, IMPACT, LOCK, KnightConfig.OATH_BLADES.get(), this.bladeAngle, BLADE_RANGE);
            this.sound(SoundEvents.ANVIL_PLACE, this.sword, 2.0F, 0.5F);
        }
        if (this.age == forged + LOCK) {
            KnightFx.oathFall(this.brain, this.landing, FALL, BLADE_SPEED);
            this.sound(SoundEvents.TRIDENT_THUNDER, this.landing, 2.0F, 1.2F);
        }
        if (this.age == forged + LOCK + FALL) {
            this.impact();
        }
        this.turn += 0.03;
        Vec3 hub = this.sword;
        if (this.age >= forged) {
            hub = this.landing.add(0.0, HEIGHT, 0.0);
        }
        int n = this.circle.size();
        for (int i = 0; i < n; i++) {
            KnightPhantom knight = this.circle.get(i);
            if (!this.active(knight)) {
                continue;
            }
            double a = this.turn + i * Mth.TWO_PI / n;
            Vec3 slot = new Vec3(hub.x + Math.cos(a) * RING, hub.y - HEIGHT + RING_HEIGHT, hub.z + Math.sin(a) * RING);
            steer(knight, slot, 0.9);
            look(knight, hub);
        }
        return this.age < forged + LOCK + FALL + RECOVER;
    }

    private void impact() {
        for (LivingEntity victim : this.brain.victimsAround(this.landing, IMPACT, 3.5)) {
            if (this.brain.damage(victim, KnightConfig.OATHBLADE.multiplier.get())) {
                this.brain.knockFrom(victim, this.landing, 0.6, 0.4);
            }
        }
        this.brain.shake(this.landing, 0.8F, 24.0F, 20);
        this.sound(SoundEvents.GENERIC_EXPLODE, this.landing, 2.0F, 0.6F);
        this.sound(SoundEvents.ANVIL_LAND, this.landing, 2.0F, 0.4F);
        int blades = KnightConfig.OATH_BLADES.get();
        for (int i = 0; i < blades; i++) {
            double a = this.bladeAngle + i * Mth.TWO_PI / blades;
            Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            Vec3 origin = this.landing.add(0.0, 0.8, 0.0).add(dir.scale(1.2));
            double range = BLADE_RANGE;
            HitResult hit = this.brain.level().clip(new ClipContext(origin, origin.add(dir.scale(BLADE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob));
            if (hit.getType() != HitResult.Type.MISS) {
                range = hit.getLocation().distanceTo(origin);
            }
            this.brain.addHazard(new FlyingBlade(origin, dir, BLADE_SPEED, range, KnightConfig.OATH_BLADE_MULTIPLIER.get()));
        }
    }
}
