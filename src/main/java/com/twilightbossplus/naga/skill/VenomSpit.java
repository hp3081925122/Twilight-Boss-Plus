package com.twilightbossplus.naga.skill;

import com.twilightbossplus.config.NagaConfig;
import com.twilightbossplus.naga.NagaBrain;
import com.twilightbossplus.naga.hazard.VenomGlob;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class VenomSpit extends NagaSkill {
    private static final int WINDUP = 16;
    private static final int SHOT_GAP = 6;
    private static final double GRAVITY = 0.05;

    private final int shots;
    private int fired;

    public VenomSpit(NagaBrain brain) {
        super(brain);
        if (brain.enraged()) {
            this.shots = 5;
        } else {
            this.shots = 3;
        }
    }

    @Override
    public String name() {
        return "venom_spit";
    }

    @Override
    public void start() {
        super.start();
        this.naga.playSound(TFSounds.NAGA_HISS.get(), 2.5F, 1.3F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (target != null) {
            this.face(target.position(), 15.0F);
        }
        if (this.age >= WINDUP && (this.age - WINDUP) % SHOT_GAP == 0 && this.fired < this.shots) {
            if (target != null) {
                this.fire(target);
            }
            this.fired++;
        }
        return this.age < WINDUP + SHOT_GAP * this.shots + 6;
    }

    private void fire(LivingEntity target) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.naga.getYRot());
        Vec3 origin = this.naga.getEyePosition().add(forward.scale(1.4));
        Vec3 aim = target.position().add(target.getDeltaMovement().scale(10.0));
        if (this.fired > 0) {
            double spread = 3.5;
            aim = aim.add((this.naga.getRandom().nextDouble() - 0.5) * 2.0 * spread, 0.0, (this.naga.getRandom().nextDouble() - 0.5) * 2.0 * spread);
        }
        double ground = this.brain.groundY(aim.x, aim.z, target.getY());
        Vec3 landing = new Vec3(aim.x, ground, aim.z);
        double distance = origin.distanceTo(landing);
        int flight = (int) Math.max(12, Math.min(34, 12 + distance * 0.8));
        double vx = (landing.x - origin.x) / flight;
        double vz = (landing.z - origin.z) / flight;
        double vy = (landing.y - origin.y + 0.5 * GRAVITY * flight * flight) / flight;
        VenomGlob glob = new VenomGlob(origin, new Vec3(vx, vy, vz), GRAVITY, flight, landing,
                NagaConfig.VENOM_SPIT.multiplier.get(), NagaConfig.SPIT_POOL_MULTIPLIER.get(), NagaConfig.SPIT_POOL_TICKS.get());
        this.brain.addHazard(glob);
        glob.announce(this.brain);
        this.naga.playSound(SoundEvents.LLAMA_SPIT, 2.0F, 0.6F + this.naga.getRandom().nextFloat() * 0.2F);
    }
}
