package com.twilightbossplus.yeti.skill;

import com.twilightbossplus.config.YetiConfig;
import com.twilightbossplus.yeti.YetiBrain;
import com.twilightbossplus.yeti.YetiFx;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFSounds;

public final class PermafrostField extends YetiSkill {
    public static final double SIZE = 3.0;
    public static final int HALF = 4;
    private static final int WINDUP = 16;
    private static final int ERUPT = 6;

    private final int waves;
    private final int warn;
    private Vec3 center = Vec3.ZERO;
    private int parity;
    private int wave;
    private int waveStart = -1;

    public PermafrostField(YetiBrain brain) {
        super(brain);
        this.waves = YetiConfig.FIELD_WAVES.get();
        if (brain.enraged()) {
            this.warn = 14;
        } else {
            this.warn = 20;
        }
    }

    @Override
    public String name() {
        return "permafrost_field";
    }

    @Override
    public void start() {
        super.start();
        this.faceTarget(360.0F);
        YetiFx.pose(this.brain, this.mob, YetiFx.POSE_SLAM, WINDUP, WINDUP + 10);
        this.sound(TFSounds.ALPHA_YETI_ROAR.get(), this.mob.position(), 3.0F, 0.7F);
    }

    @Override
    protected boolean tick() {
        this.holdStill();
        if (this.age < WINDUP) {
            this.faceTarget(10.0F);
            return true;
        }
        if (this.age == WINDUP) {
            this.center = this.brain.ground(this.mob.position());
            this.parity = this.mob.getRandom().nextInt(2);
            this.waveStart = this.age;
            this.brain.shake(this.center, 0.6F, 18.0F, 8);
            this.sound(SoundEvents.GLASS_BREAK, this.center, 2.0F, 0.6F);
        }
        int local = this.age - this.waveStart;
        if (local == 0) {
            YetiFx.tiles(this.brain, this.center, SIZE, HALF, this.parity, this.warn);
            this.sound(SoundEvents.AMETHYST_BLOCK_CHIME, this.center, 2.5F, 0.6F);
        }
        if (local == this.warn) {
            this.erupt();
        }
        if (local >= this.warn + ERUPT) {
            this.wave++;
            if (this.wave >= this.waves) {
                return false;
            }
            this.parity = 1 - this.parity;
            this.waveStart = this.age + 1;
        }
        return true;
    }

    private void erupt() {
        double reach = SIZE * (HALF + 0.5) * 1.5;
        double multiplier = YetiConfig.PERMAFROST_FIELD.multiplier.get();
        for (LivingEntity victim : this.brain.victimsAround(this.center, reach, 4.0)) {
            int ix = (int) Math.round((victim.getX() - this.center.x) / SIZE);
            int iz = (int) Math.round((victim.getZ() - this.center.z) / SIZE);
            if (Math.abs(ix) > HALF || Math.abs(iz) > HALF) {
                continue;
            }
            boolean lit = Math.floorMod(ix + iz, 2) == this.parity || (ix == 0 && iz == 0);
            if (!lit) {
                continue;
            }
            Vec3 tile = new Vec3(this.center.x + ix * SIZE, this.center.y, this.center.z + iz * SIZE);
            this.brain.frost(victim, multiplier, tile, 0.1, 0.6);
        }
        YetiFx.spikes(this.brain, this.center, SIZE, HALF, this.parity);
        this.sound(TFSounds.ALPHA_YETI_ICE.get(), this.center, 2.5F, 0.7F);
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, this.center, 2.5F, 0.5F);
    }
}
