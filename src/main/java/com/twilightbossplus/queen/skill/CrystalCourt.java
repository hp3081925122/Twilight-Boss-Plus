package com.twilightbossplus.queen.skill;

import com.twilightbossplus.TwilightBossPlus;
import com.twilightbossplus.config.SnowQueenConfig;
import com.twilightbossplus.queen.QueenBrain;
import com.twilightbossplus.queen.QueenFx;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import twilightforest.entity.monster.IceCrystal;
import twilightforest.init.TFSounds;

public final class CrystalCourt extends QueenSkill {
    private static final int RISE = 14;
    private static final int CHANNEL = 24;
    private static final int RECOVER = 12;
    private static final double PULSE = 6.0;

    private final List<Vec3> rifts = new ArrayList<>();
    private Vec3 spot = Vec3.ZERO;

    public CrystalCourt(QueenBrain brain) {
        super(brain);
    }

    @Override
    public String name() {
        return "crystal_court";
    }

    @Override
    public void start() {
        super.start();
        this.spot = this.above(this.mob.position(), 6.0);
        QueenFx.pose(this.brain, this.mob, QueenFx.POSE_RAISE, RISE, RISE + CHANNEL);
        this.sound(TFSounds.SNOW_QUEEN_AMBIENT.get(), this.mob.position(), 2.5F, 0.5F);
    }

    @Override
    protected boolean tick() {
        if (this.age < RISE) {
            this.brain.fly(this.spot, 0.45, 0.3);
            this.faceTarget(15.0F);
            return true;
        }
        this.brain.brake();
        this.faceTarget(8.0F);
        int local = this.age - RISE;
        if (local == 0) {
            QueenFx.court(this.brain, this.mob, CHANNEL);
            LivingEntity target = this.target();
            Vec3 around = this.mob.position();
            if (target != null) {
                around = target.position();
            }
            int count = SnowQueenConfig.COURT_CRYSTALS.get();
            double offset = this.mob.getRandom().nextDouble() * Mth.TWO_PI;
            for (int i = 0; i < count; i++) {
                double a = offset + Mth.TWO_PI * i / Math.max(1, count);
                Vec3 p = new Vec3(around.x + Math.cos(a) * 5.0, 0.0, around.z + Math.sin(a) * 5.0);
                double floor = this.brain.groundY(p.x, p.z, around.y + 3.0);
                Vec3 rift = new Vec3(p.x, Math.max(floor, around.y) + 2.5, p.z);
                this.rifts.add(rift);
                QueenFx.courtRift(this.brain, rift);
            }
            this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, this.mob.position(), 2.5F, 0.6F);
        }
        if (local == CHANNEL) {
            LivingEntity target = this.target();
            for (Vec3 rift : this.rifts) {
                IceCrystal crystal = new IceCrystal(this.brain.level());
                crystal.moveTo(rift.x, rift.y - 0.5, rift.z, this.mob.getRandom().nextFloat() * 360.0F, 0.0F);
                if (!this.brain.level().noCollision(crystal)) {
                    crystal.moveTo(this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.0F, 0.0F);
                }
                if (target != null) {
                    crystal.setTarget(target);
                }
                crystal.setToDieIn30Seconds();
                this.brain.level().addFreshEntity(crystal);
                this.mob.gameEvent(GameEvent.ENTITY_PLACE, crystal);
                this.sound(SoundEvents.AMETHYST_CLUSTER_PLACE, rift, 1.5F, 0.8F);
            }
            TwilightBossPlus.debug("Snow Queen {} summoned {} ice crystals", this.mob.getId(), this.rifts.size());
            Vec3 ground = this.brain.ground(this.mob.position());
            for (LivingEntity victim : this.brain.victimsAround(ground, PULSE, 4.0)) {
                this.brain.frost(victim, SnowQueenConfig.CRYSTAL_COURT.multiplier.get(), ground, 0.7, 0.3);
            }
            QueenFx.pulse(this.brain, ground, PULSE);
            this.brain.shake(ground, 0.4F, 18.0F, 8);
            this.sound(SoundEvents.GLASS_BREAK, ground, 2.0F, 1.2F);
        }
        return local < CHANNEL + RECOVER;
    }
}
