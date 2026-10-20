package com.twilightbossplus.lich.skill;

import com.twilightbossplus.config.LichConfig;
import com.twilightbossplus.lich.LichBrain;
import com.twilightbossplus.lich.LichColors;
import com.twilightbossplus.lich.LichFx;
import com.twilightbossplus.lich.hazard.StarFall;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class TwilightConstellation extends LichSkill {
    private static final int WINDUP = 14;
    private static final int INTERVAL = 9;
    private static final int FUSE = 26;
    private static final double LEAD = 8.0;

    private final int stars;
    private int placed;

    public TwilightConstellation(LichBrain brain) {
        super(brain);
        this.stars = LichConfig.CONSTELLATION_STARS.get();
    }

    @Override
    public String name() {
        return "TwilightConstellation";
    }

    @Override
    public void start() {
        super.start();
        LichFx.orb(this.brain, this.lich, 2.6, 0.6, WINDUP + this.stars * INTERVAL, LichColors.TWILIGHT, LichFx.ORB_STAR);
        this.sound(SoundEvents.EVOKER_PREPARE_SUMMON, this.lich.position(), 1.6F, 1.3F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        LivingEntity target = this.target();
        if (target != null) {
            this.lookAt(target);
        }
        if (this.age >= WINDUP && this.placed < this.stars && (this.age - WINDUP) % INTERVAL == 0) {
            if (target == null) {
                return false;
            }
            Vec3 motion = target.getDeltaMovement();
            Vec3 spot = target.position().add(motion.x * LEAD, 0.0, motion.z * LEAD);
            if (this.placed > 0) {
                double angle = this.lich.getRandom().nextDouble() * Math.PI * 2.0;
                double offset = this.lich.getRandom().nextDouble() * 1.5;
                spot = spot.add(Math.cos(angle) * offset, 0.0, Math.sin(angle) * offset);
            }
            spot = this.brain.ground(spot);
            StarFall star = new StarFall(spot, FUSE, LichConfig.CONSTELLATION.multiplier.get());
            this.brain.addHazard(star);
            star.announce(this.brain);
            this.cast();
            this.sound(SoundEvents.AMETHYST_BLOCK_CHIME, spot, 2.0F, 0.5F + this.placed * 0.12F);
            this.placed++;
        }
        return this.placed < this.stars || this.age < WINDUP + this.stars * INTERVAL + 6;
    }
}
